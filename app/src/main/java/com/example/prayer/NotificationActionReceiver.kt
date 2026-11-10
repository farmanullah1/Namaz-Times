package com.example.prayer

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.NamazTimesApplication
import com.example.data.local.entity.PrayerOccurrenceEntity
import com.example.data.local.entity.ScheduledAlarmEntity
import com.example.domain.model.PrayerName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Notification actions validate the occurrence before acting. Marking prayed writes a real history
 * record; snoozing creates a short-lived occurrence without changing the Adhan/Iqamah timetable.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val application = context.applicationContext as? NamazTimesApplication ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_MARK_PRAYED -> markPrayed(application, intent, context)
                    ACTION_SNOOZE -> snooze(application, intent, context)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Notification action failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun markPrayed(application: NamazTimesApplication, intent: Intent, context: Context) {
        val prayer = PrayerName.fromKey(intent.getStringExtra(EXTRA_PRAYER)) ?: return
        val date = intent.getStringExtra(EXTRA_TRACKING_DATE)?.let { LocalDate.parse(it) } ?: return
        val occurrenceId = intent.getStringExtra(EXTRA_OCCURRENCE_ID)
        val occurrence = occurrenceId?.let { application.graph.database.prayerDao().occurrenceById(it) }
        val mosqueId = intent.getStringExtra(EXTRA_MOSQUE_ID)

        application.graph.prayerHistoryRepository.markPrayed(
            prayer = prayer,
            trackingDate = date,
            occurrence = occurrence?.let {
                com.example.domain.model.PrayerOccurrence(
                    id = it.id,
                    prayer = PrayerName.fromKey(it.prayer) ?: prayer,
                    eventType = com.example.domain.model.PrayerEventType.ADHAN,
                    instant = java.time.Instant.ofEpochMilli(it.instantMillis),
                    localDate = date,
                    mosqueId = it.mosqueId,
                    placeId = it.placeId,
                    source = com.example.domain.model.TimeSource.entries.firstOrNull { source -> source.name == it.source }
                        ?: com.example.domain.model.TimeSource.CALCULATED,
                    scheduleRevision = it.scheduleRevision
                )
            },
            mosqueId = mosqueId,
            placeId = null,
            context = "Marked from notification",
            tags = "",
            note = ""
        )

        occurrenceId?.let {
            NotificationManagerCompat.from(context)
                .cancel(PrayerAlarmScheduler.stableRequestCode("notification|$it"))
        }
        application.graph.rebuildCoordinator.rebuild("Prayer marked from notification")
    }

    private suspend fun snooze(application: NamazTimesApplication, intent: Intent, context: Context) {
        val occurrenceId = intent.getStringExtra(EXTRA_OCCURRENCE_ID) ?: return
        val dao = application.graph.database.prayerDao()
        val original = dao.occurrenceById(occurrenceId) ?: return
        val triggerAt = System.currentTimeMillis() + 10 * 60 * 1000L

        val snoozed = PrayerOccurrenceEntity(
            id = "snooze|${original.id}|$triggerAt",
            prayer = original.prayer,
            eventType = original.eventType,
            instantMillis = triggerAt,
            localDate = original.localDate,
            mosqueId = original.mosqueId,
            placeId = original.placeId,
            source = original.source,
            scheduleRevision = original.scheduleRevision,
            isEnabled = true,
            offsetMinutes = 0,
            deliveredAtMillis = null
        )
        dao.upsertOccurrences(listOf(snoozed))
        val requestCode = PrayerAlarmScheduler.stableRequestCode(snoozed.id)
        val result = application.graph.alarmScheduler.schedule(snoozed.id, requestCode, triggerAt)
        if (result.isSuccess) {
            dao.upsertAlarm(
                ScheduledAlarmEntity(
                    eventId = snoozed.id,
                    requestCode = requestCode,
                    scheduledAtMillis = triggerAt,
                    revision = snoozed.scheduleRevision,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        NotificationManagerCompat.from(context)
            .cancel(PrayerAlarmScheduler.stableRequestCode("notification|$occurrenceId"))
    }

    companion object {
        const val ACTION_MARK_PRAYED = "com.example.namaztimes.MARK_PRAYED"
        const val ACTION_SNOOZE = "com.example.namaztimes.SNOOZE"
        const val EXTRA_OCCURRENCE_ID = "occurrence_id"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_TRACKING_DATE = "tracking_date"
        const val EXTRA_MOSQUE_ID = "mosque_id"
        private const val TAG = "NotificationAction"
    }
}
