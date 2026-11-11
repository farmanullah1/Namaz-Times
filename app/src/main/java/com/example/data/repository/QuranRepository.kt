package com.example.data.repository

import com.example.data.local.Mappers.toDomain
import com.example.data.local.NamazDatabase
import com.example.data.local.QuranSession
import com.example.data.local.entity.QuranGoalEntity
import com.example.data.local.entity.QuranSessionEntity
import com.example.domain.model.QuranDayBoundary
import com.example.domain.model.QuranDayInput
import com.example.domain.model.QuranLedgerEngine
import com.example.domain.model.QuranSettings
import com.example.domain.model.QuranSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class QuranRepository(private val database: NamazDatabase) {

    val sessions: Flow<List<QuranSession>> = database.quranDao().observeSessions()
        .map { rows -> rows.map { it.toDomain() } }

    suspend fun sessionsForDate(date: LocalDate): List<QuranSession> =
        database.quranDao().getSessionsForDate(date.toString()).map { it.toDomain() }

    suspend fun addRukus(
        dateKey: LocalDate,
        count: Int,
        occurredAt: Instant = Instant.now(),
        surah: String? = null,
        rukuNumber: Int? = null,
        note: String = "",
        isCorrection: Boolean = false,
        correctionReason: String? = null
    ): Result<Unit> {
        if (count <= 0) return Result.failure(IllegalArgumentException("Ruku count must be greater than zero."))
        if (count > 50) return Result.failure(IllegalArgumentException("Enter 50 Rukus or fewer at a time."))
        if (isCorrection && correctionReason.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("A correction needs a short reason."))
        }
        database.quranDao().upsertSession(
            QuranSessionEntity(
                id = UUID.randomUUID().toString(),
                dateKey = dateKey.toString(),
                occurredAtMillis = occurredAt.toEpochMilli(),
                rukus = count,
                surah = surah?.trim()?.takeIf { it.isNotEmpty() },
                rukuNumber = rukuNumber,
                note = note.trim(),
                isCorrection = isCorrection,
                correctionReason = correctionReason?.trim(),
                createdAt = System.currentTimeMillis()
            )
        )
        return Result.success(Unit)
    }

    suspend fun undoLast(): QuranSession? {
        val latest = database.quranDao().latestSession() ?: return null
        database.quranDao().deleteSession(latest.id)
        return latest.toDomain()
    }

    suspend fun deleteRukuSession(surah: String, rukuNumber: Int) {
        database.quranDao().deleteSessionBySurahAndRuku(surah, rukuNumber)
    }

    suspend fun markedRukuKeys(): Set<String> {
        return database.quranDao().getRukuMarkedSessions()
            .mapNotNull {
                val s = it.surah ?: return@mapNotNull null
                val r = it.rukuNumber ?: return@mapNotNull null
                "$s:$r"
            }
            .toSet()
    }

    /** Sets a day's total by appending a correction delta; the original entries stay in the audit trail. */
    suspend fun correctDayTotal(date: LocalDate, newTotal: Int, reason: String): Result<Int> {
        if (newTotal < 0) return Result.failure(IllegalArgumentException("Total cannot be negative."))
        if (newTotal > 100) return Result.failure(IllegalArgumentException("Enter 100 Rukus or fewer for a day."))
        val current = sessionsForDate(date).sumOf { it.rukus }
        val delta = newTotal - current
        if (delta == 0) return Result.success(0)
        return if (delta > 0) {
            addRukus(
                dateKey = date,
                count = delta,
                surah = null,
                note = "Correction",
                isCorrection = true,
                correctionReason = reason
            ).map { delta }
        } else {
            // A negative correction is recorded with the sign preserved by the ledger's sum.
            database.quranDao().upsertSession(
                QuranSessionEntity(
                    id = UUID.randomUUID().toString(),
                    dateKey = date.toString(),
                    occurredAtMillis = System.currentTimeMillis(),
                    rukus = delta,
                    surah = null,
                    rukuNumber = null,
                    note = "Correction",
                    isCorrection = true,
                    correctionReason = reason.trim(),
                    createdAt = System.currentTimeMillis()
                )
            )
            Result.success(delta)
        }
    }

    suspend fun setDailyGoal(goal: Int, effectiveFrom: LocalDate): Result<Unit> {
        if (goal < 0 || goal > 50) return Result.failure(IllegalArgumentException("Daily goal must be between 0 and 50 Rukus."))
        database.quranDao().upsertGoal(
            QuranGoalEntity(effectiveFrom = effectiveFrom.toString(), dailyGoal = goal)
        )
        return Result.success(Unit)
    }

    suspend fun goalHistory(): List<Pair<LocalDate, Int>> =
        database.quranDao().getGoals().map { LocalDate.parse(it.effectiveFrom) to it.dailyGoal }

    suspend fun ensureStartedOn(settings: QuranSettings): QuranSettings {
        if (settings.startedOn != null) return settings
        return settings.copy(startedOn = LocalDate.now())
    }

    /**
     * Builds the signed Ruku ledger from the user's own entries and the goal history.
     * Missing days from the first tracking date are filled with zero readings so backlog accrues.
     */
    suspend fun snapshot(todayKey: LocalDate, settings: QuranSettings): QuranSnapshot {
        val allSessions = database.quranDao().getSessions().map { it.toDomain() }
        val goals = database.quranDao().getGoals()
        val start = settings.startedOn ?: allSessions.minOfOrNull { it.dateKey } ?: todayKey

        val sessionsByDate = allSessions.groupBy { it.dateKey }
        val inputs = mutableListOf<QuranDayInput>()
        var date = start
        while (!date.isAfter(todayKey)) {
            val goal = goalForDate(goals, date)
            val completed = sessionsByDate[date]?.sumOf { it.rukus } ?: 0
            inputs += QuranDayInput(dateKey = date, dailyGoal = goal, completedRukus = completed.coerceAtLeast(0))
            date = date.plusDays(1)
        }
        if (inputs.isEmpty()) {
            inputs += QuranDayInput(todayKey, goalForDate(goals, todayKey), 0)
        }

        val results = QuranLedgerEngine.calculate(inputs, surplusCreditEnabled = settings.surplusCreditEnabled)
        val today = results.firstOrNull { it.dateKey == todayKey } ?: results.last()
        val previous = results.filter { it.dateKey.isBefore(todayKey) }
        val openingForToday = today.openingBalance
        // The effective goal comes from goal history, so a change made today only applies from tomorrow.
        val goalForToday = goalForDate(goals, todayKey)
        val targetForToday = (goalForToday + openingForToday).coerceAtLeast(0)
        val completedToday = today.completedRukus
        val total = results.sumOf { it.completedRukus }.coerceAtLeast(0)

        return QuranSnapshot(
            todayDateKey = todayKey,
            target = targetForToday,
            completedToday = completedToday,
            remainingToday = (targetForToday - completedToday).coerceAtLeast(0),
            backlog = (openingForToday + goalForToday - completedToday).coerceAtLeast(0),
            surplusCredit = (-(openingForToday + goalForToday - completedToday)).coerceAtLeast(0),
            streak = QuranLedgerEngine.streak(results, todayKey, settings.streakCountsSurplus),
            totalCompleted = total,
            completionPercent = settings.editionTotalRukus?.takeIf { it > 0 }?.let { totalRukus ->
                ((total.toDouble() / totalRukus) * 100).toInt().coerceIn(0, 100)
            },
            resetExplanation = QuranDayBoundary.explain(settings),
            recentDays = previous.takeLast(14).reversed(),
            todayClosedTargetSatisfied = completedToday >= targetForToday
        )
    }

    private fun goalForDate(goals: List<QuranGoalEntity>, date: LocalDate): Int {
        if (goals.isEmpty()) return 1
        return goals
            .filter { !LocalDate.parse(it.effectiveFrom).isAfter(date) }
            .maxByOrNull { it.effectiveFrom }
            ?.dailyGoal
            ?: goals.minByOrNull { it.effectiveFrom }?.dailyGoal
            ?: 1
    }

    suspend fun deleteAll() {
        database.quranDao().deleteAllSessions()
        database.quranDao().deleteAllGoals()
    }

    suspend fun sessionCount(): Int = database.quranDao().getSessions().size

    suspend fun firstSessionDate(): LocalDate? =
        database.quranDao().getSessions().minOfOrNull { it.dateKey }?.let { LocalDate.parse(it) }
}
