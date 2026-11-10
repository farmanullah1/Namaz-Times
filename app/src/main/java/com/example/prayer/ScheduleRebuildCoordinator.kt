package com.example.prayer

import android.content.Context
import android.util.Log
import com.example.data.local.NamazDatabase
import com.example.data.local.entity.PrayerOccurrenceEntity
import com.example.data.local.entity.ScheduledAlarmEntity
import com.example.data.repository.MosqueRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.MosqueProfile
import com.example.domain.model.PrayerEventType
import com.example.domain.model.PrayerName
import com.example.domain.model.ReminderPreference
import com.example.domain.model.ReminderSoundMode
import com.example.widgets.WidgetUpdateCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant

data class RebuildOutcome(
    val succeeded: Boolean,
    val scheduledEvents: Int,
    val inexactFallback: Boolean,
    val message: String
)

/**
 * Single serialized, idempotent schedule update.
 *
 * A rebuild: resolves the active timetable, regenerates bounded occurrences, cancels obsolete
 * alarms, schedules only events from the current revision, and updates the widget. Running it twice
 * with unchanged data schedules nothing new and never creates duplicate reminders.
 */
class ScheduleRebuildCoordinator(
    private val context: Context,
    private val database: NamazDatabase,
    private val settingsRepository: SettingsRepository,
    private val factory: PrayerOccurrenceFactory,
    private val alarmScheduler: PrayerAlarmScheduler
) {

    private val mutex = Mutex()
    private val horizonDays = 3L

    suspend fun rebuild(reason: String): RebuildOutcome = mutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching { rebuildLocked(reason) }.getOrElse { error ->
                Log.e(TAG, "Schedule rebuild failed: $reason", error)
                settingsRepository.setRebuildState(
                    successfulAt = null,
                    reason = reason,
                    succeeded = false,
                    needsRepair = true
                )
                RebuildOutcome(false, 0, alarmScheduler.isUsingInexactFallback(), "Reminders could not be rebuilt.")
            }
        }
    }

    private suspend fun rebuildLocked(reason: String): RebuildOutcome {
        val settings = settingsRepository.current()
        val snapshot = MosqueRepository(database).snapshot()
        val scheduleService = PrayerScheduleService(factory)
        val now = Instant.now()

        val today = scheduleService.buildToday(settings, snapshot, now)
        val mosque = today.mosque
        val place = today.place
        val zone = today.zone

        val dates = (0L until horizonDays).map { today.today.plusDays(it) }
        val builds = dates.map { date ->
            factory.buildDay(
                date = date,
                mosque = mosque,
                place = place,
                schedules = snapshot.schedules,
                entries = snapshot.entries,
                overrides = snapshot.overrides,
                settings = settings,
                travelMode = today.travelMode
            )
        }

        val reminderPrefs = snapshot.reminderPreferences
            .filter { mosque != null && it.mosqueId == mosque.id }
            .associateBy { it.prayer }

        val desiredOccurrences = mutableListOf<PrayerOccurrenceEntity>()
        builds.forEach { build ->
            build.occurrences.forEach { occurrence ->
                val preference = occurrence.mosqueId?.let { reminderPrefs[occurrence.prayer] }
                val allowed = when (occurrence.eventType) {
                    PrayerEventType.ADHAN -> preference?.adhanEnabled ?: true
                    PrayerEventType.IQAMAH -> preference?.iqamahEnabled ?: true
                    PrayerEventType.KHUTBAH, PrayerEventType.JUMUAH -> settings.fridayRemindersEnabled
                    else -> true
                }
                if (!allowed) return@forEach

                if (occurrence.instant.isAfter(now)) {
                    desiredOccurrences += occurrence.toEntity()
                }
            }

            addPreReminders(
                buildOccurrences = build.occurrences,
                preferenceFor = { prayer -> reminderPrefs[prayer] },
                now = now,
                mosque = mosque,
                output = desiredOccurrences
            )
        }

        // Occurrences are a derived cache; history and logs are separate and unaffected.
        database.prayerDao().deleteOccurrencesFromDate(today.today.toString())
        if (desiredOccurrences.isNotEmpty()) {
            database.prayerDao().upsertOccurrences(desiredOccurrences)
        }
        database.prayerDao().deleteOccurrencesBefore(now.toEpochMilli() - 7L * 24 * 60 * 60 * 1000)

        val existingAlarms = database.prayerDao().getScheduledAlarms()
        val existingById = existingAlarms.associateBy { it.eventId }
        val schedulingErrors = mutableListOf<String>()
        val newAlarms = mutableListOf<ScheduledAlarmEntity>()
        val usedCodes = mutableMapOf<Int, String>()

        if (settings.remindersEnabled) {
            desiredOccurrences
                .filter { it.instantMillis > now.toEpochMilli() }
                .sortedBy { it.instantMillis }
                .forEach { occurrence ->
                    val existing = existingById[occurrence.id]
                    val requestCode = existing?.requestCode ?: allocateRequestCode(occurrence.id, usedCodes)
                    usedCodes[requestCode] = occurrence.id
                    val keepExisting = existing != null &&
                        existing.requestCode == requestCode &&
                        existing.scheduledAtMillis == occurrence.instantMillis &&
                        existing.revision == occurrence.scheduleRevision
                    if (keepExisting) {
                        newAlarms += existing!!
                        return@forEach
                    }

                    if (existing != null) {
                        alarmScheduler.cancel(existing.eventId, existing.requestCode)
                        database.prayerDao().deleteAlarm(existing.eventId)
                    }
                    val result = alarmScheduler.schedule(
                        eventId = occurrence.id,
                        requestCode = requestCode,
                        triggerAtMillis = occurrence.instantMillis
                    )
                    if (result.isSuccess) {
                        val alarm = ScheduledAlarmEntity(
                            eventId = occurrence.id,
                            requestCode = requestCode,
                            scheduledAtMillis = occurrence.instantMillis,
                            revision = occurrence.scheduleRevision,
                            createdAt = System.currentTimeMillis()
                        )
                        database.prayerDao().upsertAlarm(alarm)
                        newAlarms += alarm
                    } else {
                        schedulingErrors += result.exceptionOrNull()?.message ?: "Unknown scheduling error"
                    }
                }
        }

        val desiredIds = newAlarms.map { it.eventId }.toSet()
        existingAlarms.filter { it.eventId !in desiredIds }.forEach { stale ->
            alarmScheduler.cancel(stale.eventId, stale.requestCode)
            database.prayerDao().deleteAlarm(stale.eventId)
        }

        if (schedulingErrors.isNotEmpty()) {
            Log.w(TAG, "Some reminders could not be scheduled: ${schedulingErrors.first()}")
        }

        val needsRepair = schedulingErrors.isNotEmpty()
        settingsRepository.setRebuildState(
            successfulAt = if (needsRepair) null else System.currentTimeMillis(),
            reason = reason,
            succeeded = !needsRepair,
            needsRepair = needsRepair
        )

        runCatching { WidgetUpdateCoordinator(context).refreshAll() }

        val zoneLabel = zone.id
        val message = when {
            !settings.remindersEnabled -> "Schedule updated for $zoneLabel. Reminders are currently off."
            needsRepair -> "Timetable saved, but some reminders need repair. Open Reminder reliability to retry."
            newAlarms.isEmpty() -> "No future reminders are available to schedule."
            else -> "Schedule updated. ${newAlarms.size} future reminder(s) armed."
        }
        return RebuildOutcome(
            succeeded = !needsRepair,
            scheduledEvents = newAlarms.size,
            inexactFallback = alarmScheduler.isUsingInexactFallback(),
            message = message
        )
    }

    private fun addPreReminders(
        buildOccurrences: List<com.example.domain.model.PrayerOccurrence>,
        preferenceFor: (PrayerName) -> ReminderPreference?,
        now: Instant,
        mosque: MosqueProfile?,
        output: MutableList<PrayerOccurrenceEntity>
    ) {
        buildOccurrences
            .filter { it.prayer != PrayerName.DHUHR || it.eventType != PrayerEventType.KHUTBAH }
            .forEach { occurrence ->
                val preference = preferenceFor(occurrence.prayer) ?: return@forEach
                if (occurrence.eventType == PrayerEventType.ADHAN) {
                    val offset = preference.preAdhanMinutes ?: return@forEach
                    if (offset <= 0) return@forEach
                    val trigger = occurrence.instant.minusSeconds(offset * 60L)
                    if (trigger.isAfter(now)) {
                        output += preOccurrence(occurrence, trigger, PrayerEventType.PRE_ADHAN, offset, mosque)
                    }
                }
                if (occurrence.eventType == PrayerEventType.IQAMAH) {
                    val offset = preference.preIqamahMinutes ?: return@forEach
                    if (offset <= 0) return@forEach
                    val trigger = occurrence.instant.minusSeconds(offset * 60L)
                    if (trigger.isAfter(now)) {
                        output += preOccurrence(occurrence, trigger, PrayerEventType.PRE_IQAMAH, offset, mosque)
                    }
                }
            }
    }

    private fun preOccurrence(
        base: com.example.domain.model.PrayerOccurrence,
        trigger: Instant,
        type: PrayerEventType,
        offset: Int,
        mosque: MosqueProfile?
    ): PrayerOccurrenceEntity = PrayerOccurrenceEntity(
        id = "${base.id}|${type.name}|$offset",
        prayer = base.prayer.name,
        eventType = type.name,
        instantMillis = trigger.toEpochMilli(),
        localDate = base.localDate.toString(),
        mosqueId = base.mosqueId,
        placeId = base.placeId,
        source = base.source.name,
        scheduleRevision = base.scheduleRevision,
        isEnabled = true,
        offsetMinutes = offset,
        deliveredAtMillis = null
    )

    private fun allocateRequestCode(eventId: String, used: Map<Int, String>): Int {
        var salt = 0
        while (salt < 64) {
            val code = PrayerAlarmScheduler.stableRequestCode(eventId, salt)
            if (used[code] == null || used[code] == eventId) return code
            salt += 1
        }
        return PrayerAlarmScheduler.stableRequestCode(eventId, eventId.length)
    }

    companion object {
        private const val TAG = "ScheduleRebuild"
    }
}

private fun com.example.domain.model.PrayerOccurrence.toEntity(): PrayerOccurrenceEntity =
    PrayerOccurrenceEntity(
        id = id,
        prayer = prayer.name,
        eventType = eventType.name,
        instantMillis = instant.toEpochMilli(),
        localDate = localDate.toString(),
        mosqueId = mosqueId,
        placeId = placeId,
        source = source.name,
        scheduleRevision = scheduleRevision,
        isEnabled = isEnabled,
        offsetMinutes = offsetMinutes,
        deliveredAtMillis = null
    )
