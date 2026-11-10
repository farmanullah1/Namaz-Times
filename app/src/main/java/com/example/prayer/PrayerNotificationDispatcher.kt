package com.example.prayer

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.core.notifications.PrayerNotificationFactory
import com.example.core.notifications.ReminderAudioPlayer
import com.example.data.local.NamazDatabase
import com.example.data.repository.SettingsRepository
import com.example.domain.model.PrayerEventType
import com.example.domain.model.PrayerName
import com.example.domain.model.ReminderSoundMode
import com.example.domain.model.WallClock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Resolves a fired alarm from persisted state, rejects stale/duplicate events, posts at most one
 * notification, records delivery, then asks the coordinator to schedule the next event.
 */
class PrayerNotificationDispatcher(
    private val context: Context,
    private val database: NamazDatabase,
    private val settingsRepository: SettingsRepository,
    private val rebuildCoordinator: ScheduleRebuildCoordinator,
    private val silenceManager: PrayerSilenceManager
) {

    private val factory = PrayerNotificationFactory(context)
    private val audioPlayer = ReminderAudioPlayer(context)

    suspend fun deliver(eventId: String) {
        val dao = database.prayerDao()
        val occurrence = dao.occurrenceById(eventId)
        if (occurrence == null) {
            Log.i(TAG, "Ignoring unknown occurrence $eventId")
            return
        }
        if (!occurrence.isEnabled) return
        if (occurrence.deliveredAtMillis != null) {
            Log.i(TAG, "Occurrence $eventId already delivered")
            return
        }

        val settings = settingsRepository.current()
        if (!settings.remindersEnabled) {
            rebuildCoordinator.rebuild("Reminders disabled before delivery")
            return
        }

        val now = Instant.now()
        val maximumLatenessMillis = 2 * 60 * 60 * 1000L
        if (now.toEpochMilli() > occurrence.instantMillis + maximumLatenessMillis) {
            Log.i(TAG, "Occurrence $eventId is stale and will not be shown")
            rebuildCoordinator.rebuild("Stale reminder skipped")
            return
        }

        val prayer = PrayerName.fromKey(occurrence.prayer) ?: return
        val eventType = PrayerEventType.entries.firstOrNull { it.name == occurrence.eventType } ?: return

        if (occurrence.mosqueId != null) {
            val mosque = database.mosqueDao().getProfile(occurrence.mosqueId)
            val stillValid = mosque != null &&
                mosque.isEnabled &&
                !mosque.isArchived &&
                mosque.scheduleRevision == occurrence.scheduleRevision
            if (!stillValid) {
                Log.i(TAG, "Occurrence $eventId belongs to a stale timetable revision")
                rebuildCoordinator.rebuild("Stale timetable revision rejected")
                return
            }
        }

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            dao.markDelivered(occurrence.id, System.currentTimeMillis())
            rebuildCoordinator.rebuild("Notification permission missing")
            return
        }

        val zone = resolveZone(occurrence.mosqueId)
        val timeLabel = formatTime(occurrence.instantMillis, zone)
        val iqamahLabel = if (eventType == PrayerEventType.ADHAN) {
            findIqamahLabel(occurrence, zone)
        } else {
            null
        }
        val mosqueName = occurrence.mosqueId?.let { database.mosqueDao().getProfile(it)?.name }
        val sourceLabel = com.example.domain.model.TimeSource.entries
            .firstOrNull { it.name == occurrence.source }?.label ?: occurrence.source

        val copy = factory.copyFor(
            prayer = prayer,
            eventType = eventType,
            timeLabel = timeLabel,
            mosqueName = mosqueName,
            sourceLabel = sourceLabel,
            iqamahLabel = iqamahLabel,
            offsetMinutes = occurrence.offsetMinutes,
            inexact = !PrayerAlarmScheduler(context).canUseExactAlarms()
        )

        val soundMode = resolveSoundMode(occurrence.mosqueId, prayer, settings.reminderSoundMode)
        val notification = factory.build(
            occurrenceId = occurrence.id,
            prayer = prayer,
            trackingDate = occurrence.localDate,
            mosqueId = occurrence.mosqueId,
            copy = copy,
            soundMode = soundMode
        )

        NotificationManagerCompat.from(context)
            .notify(PrayerAlarmScheduler.stableRequestCode("notification|${occurrence.id}"), notification)
        dao.markDelivered(occurrence.id, System.currentTimeMillis())

        when (soundMode) {
            ReminderSoundMode.CUSTOM_AUDIO -> {
                val played = audioPlayer.playCustomAudio(settings.customAdhanUri)
                if (!played) {
                    audioPlayer.playShortTone()
                    settingsRepository.update { it.copy(remindersNeedRepair = true) }
                }
            }
            ReminderSoundMode.SHORT_TONE -> audioPlayer.playShortTone()
            else -> Unit
        }

        if (settings.spokenAnnouncementEnabled) {
            audioPlayer.speak("${prayer.title} ${if (eventType == PrayerEventType.IQAMAH) "Iqamah" else "Adhan"}")
        }

        if (eventType == PrayerEventType.ADHAN || eventType == PrayerEventType.IQAMAH) {
            if (settings.prayerSilenceEnabled && eventType == PrayerEventType.ADHAN) {
                silenceManager.silenceForPrayer()
            }
        }

        rebuildCoordinator.rebuild("Reminder delivered")
    }

    private fun resolveSoundMode(
        mosqueId: String?,
        prayer: PrayerName,
        globalMode: ReminderSoundMode
    ): ReminderSoundMode {
        if (mosqueId == null) return globalMode
        val rows = runCatching { database.mosqueDao().getReminderPreferences(mosqueId) }.getOrNull()
        val stored = rows?.firstOrNull { it.prayer == prayer.name }?.soundMode
        return ReminderSoundMode.entries.firstOrNull { it.name == stored } ?: globalMode
    }

    private suspend fun findIqamahLabel(
        occurrence: com.example.data.local.entity.PrayerOccurrenceEntity,
        zone: ZoneId
    ): String? {
        val rows = database.prayerDao().occurrencesFrom(occurrence.instantMillis - 6 * 60 * 60 * 1000L)
        val match = rows.firstOrNull {
            it.prayer == occurrence.prayer &&
                it.eventType == PrayerEventType.IQAMAH.name &&
                it.localDate == occurrence.localDate &&
                it.mosqueId == occurrence.mosqueId
        } ?: return null
        return formatTime(match.instantMillis, zone)
    }

    private fun resolveZone(mosqueId: String?): ZoneId {
        val mosque = mosqueId?.let { database.mosqueDao().getProfile(it) }
        return runCatching { ZoneId.of(mosque?.timezoneId ?: ZoneId.systemDefault().id) }
            .getOrDefault(ZoneId.systemDefault())
    }

    private fun formatTime(instantMillis: Long, zone: ZoneId): String {
        val local = Instant.ofEpochMilli(instantMillis).atZone(zone).toLocalTime()
        return com.example.core.time.formatClock(local.hour, local.minute)
    }

    companion object {
        private const val TAG = "PrayerNotification"
    }
}
