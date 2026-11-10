package com.example.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class QuranResetRule(val title: String) {
    MIDNIGHT("Local midnight"),
    CUSTOM_TIME("Custom time"),
    FAJR("Fajr"),
    MAGHRIB("Maghrib")
}

data class QuranSettings(
    val dailyGoal: Int = 1,
    val surplusCreditEnabled: Boolean = false,
    val resetRule: QuranResetRule = QuranResetRule.MIDNIGHT,
    val customResetTime: LocalTime = LocalTime.MIDNIGHT,
    val streakCountsSurplus: Boolean = true,
    val startedOn: LocalDate? = null,
    val editionTotalRukus: Int? = null,
    val editionLabel: String = ""
)

data class QuranDayInput(
    val dateKey: LocalDate,
    val dailyGoal: Int,
    val completedRukus: Int
)

data class QuranDayResult(
    val dateKey: LocalDate,
    val openingBalance: Int,
    val requiredTarget: Int,
    val completedRukus: Int,
    val remainingForToday: Int,
    val closingBalance: Int,
    val backlogAfterClose: Int,
    val surplusCreditAfterClose: Int
) {
    val targetSatisfied: Boolean get() = completedRukus >= requiredTarget
    val coveredBySurplus: Boolean get() = requiredTarget > 0 && completedRukus == 0 && openingBalance < 0
}

data class QuranSnapshot(
    val todayDateKey: LocalDate = LocalDate.now(),
    val target: Int = 1,
    val completedToday: Int = 0,
    val remainingToday: Int = 0,
    val backlog: Int = 0,
    val surplusCredit: Int = 0,
    val streak: Long = 0,
    val totalCompleted: Int = 0,
    val completionPercent: Int? = null,
    val resetExplanation: String = "",
    val recentDays: List<QuranDayResult> = emptyList(),
    val todayClosedTargetSatisfied: Boolean = false
)

object QuranLedgerEngine {

    fun calculate(
        days: List<QuranDayInput>,
        initialBalance: Int = 0,
        surplusCreditEnabled: Boolean
    ): List<QuranDayResult> {
        require(initialBalance >= 0 || surplusCreditEnabled) {
            "Negative initial balance requires surplus credit to be enabled"
        }

        var balance = initialBalance
        return days.sortedBy { it.dateKey }.map { day ->
            require(day.dailyGoal >= 0) { "Daily goal cannot be negative" }
            require(day.completedRukus >= 0) { "Completed Rukus cannot be negative" }

            val opening = balance
            val target = (day.dailyGoal + opening).coerceAtLeast(0)
            val remaining = (target - day.completedRukus).coerceAtLeast(0)

            balance = opening + day.dailyGoal - day.completedRukus
            if (!surplusCreditEnabled && balance < 0) balance = 0

            QuranDayResult(
                dateKey = day.dateKey,
                openingBalance = opening,
                requiredTarget = target,
                completedRukus = day.completedRukus,
                remainingForToday = remaining,
                closingBalance = balance,
                backlogAfterClose = balance.coerceAtLeast(0),
                surplusCreditAfterClose = (-balance).coerceAtLeast(0)
            )
        }
    }

    /** A day counts when the user's recorded reading meets the computed target. */
    fun streak(results: List<QuranDayResult>, today: LocalDate, countSurplusCovered: Boolean): Long {
        if (results.isEmpty()) return 0
        val sorted = results.sortedBy { it.dateKey }
        var index = sorted.indexOfLast { it.dateKey <= today }
        if (index < 0) return 0
        // The active day only breaks the streak once it has ended; start from yesterday when today is incomplete.
        if (sorted[index].dateKey == today && !sorted[index].targetSatisfied) {
            index -= 1
        }
        var streak = 0L
        while (index >= 0) {
            val day = sorted[index]
            val satisfied = day.targetSatisfied || (countSurplusCovered && day.coveredBySurplus)
            if (!satisfied) break
            streak += 1
            index -= 1
        }
        return streak
    }
}

/** Determines which Quran tracking day an instant belongs to. */
object QuranDayBoundary {

    fun dateKeyFor(
        now: Instant,
        zone: ZoneId,
        rule: QuranResetRule,
        customTime: LocalTime,
        todaysFajr: Instant?,
        todaysMaghrib: Instant?
    ): LocalDate {
        val localDate = now.atZone(zone).toLocalDate()
        val localTime = now.atZone(zone).toLocalTime()
        return when (rule) {
            QuranResetRule.MIDNIGHT -> localDate
            QuranResetRule.CUSTOM_TIME -> if (localTime < customTime) localDate.minusDays(1) else localDate
            QuranResetRule.FAJR -> {
                val boundary = todaysFajr ?: localDate.atTime(4, 0).atZone(zone).toInstant()
                if (now < boundary) localDate.minusDays(1) else localDate
            }
            QuranResetRule.MAGHRIB -> {
                val boundary = todaysMaghrib ?: localDate.atTime(18, 0).atZone(zone).toInstant()
                if (now < boundary) localDate.minusDays(1) else localDate
            }
        }
    }

    fun explain(settings: QuranSettings): String = when (settings.resetRule) {
        QuranResetRule.MIDNIGHT ->
            "Reading day resets at 12:00 AM (local time). Entries before midnight count for today."
        QuranResetRule.CUSTOM_TIME ->
            "Reading day resets at ${WallClock.format(settings.customResetTime)} local time. Earlier entries count for the previous day."
        QuranResetRule.FAJR ->
            "Reading day resets at the selected Fajr time (calculated Fajr when no mosque time is entered)."
        QuranResetRule.MAGHRIB ->
            "Reading day resets at the selected Maghrib time (calculated Maghrib when no mosque time is entered)."
    }
}
