package com.example.core.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.PrayerEventType
import com.example.domain.model.PrayerName
import com.example.domain.model.ReminderSoundMode
import com.example.prayer.NotificationActionReceiver
import com.example.prayer.PrayerAlarmScheduler

data class ReminderCopy(
    val title: String,
    val body: String,
    val bigText: String,
    val channelId: String
)

/** Builds notifications with explicit, validated PendingIntents and no stale payload trust. */
class PrayerNotificationFactory(private val context: Context) {

    fun copyFor(
        prayer: PrayerName,
        eventType: PrayerEventType,
        timeLabel: String,
        mosqueName: String?,
        sourceLabel: String,
        iqamahLabel: String?,
        offsetMinutes: Int,
        inexact: Boolean
    ): ReminderCopy {
        val arabic = prayer.arabicTitle
        val title = when (eventType) {
            PrayerEventType.ADHAN -> "${prayer.title} — Adhan"
            PrayerEventType.IQAMAH -> "${prayer.title} — Iqamah"
            PrayerEventType.PRE_ADHAN -> "${prayer.title} in $offsetMinutes minutes"
            PrayerEventType.PRE_IQAMAH -> "${prayer.title} Iqamah in $offsetMinutes minutes"
            PrayerEventType.KHUTBAH -> "Friday Khutbah"
            PrayerEventType.JUMUAH -> "Jumuah"
        }
        val place = mosqueName ?: "Calculated times"
        val body = when (eventType) {
            PrayerEventType.ADHAN -> "$timeLabel · $place · $sourceLabel"
            PrayerEventType.IQAMAH -> "$timeLabel · $place" + (iqamahLabel?.let { " · Adhan $it" } ?: "")
            PrayerEventType.PRE_ADHAN -> "Adhan at $timeLabel · $place"
            PrayerEventType.PRE_IQAMAH -> "Iqamah at $timeLabel · $place"
            PrayerEventType.KHUTBAH -> "Khutbah starts $timeLabel · $place"
            PrayerEventType.JUMUAH -> "Jumuah begins $timeLabel · $place"
        }
        val precision = if (inexact) "\nAndroid may delay this reminder because exact alarm access is unavailable." else ""
        return ReminderCopy(
            title = title,
            body = body,
            bigText = "$body\n$arabic$precision",
            channelId = NotificationChannels.channelIdFor(eventType.name)
        )
    }

    fun build(
        occurrenceId: String,
        prayer: PrayerName,
        trackingDate: String,
        mosqueId: String?,
        copy: ReminderCopy,
        soundMode: ReminderSoundMode
    ): android.app.Notification {
        val notificationId = PrayerAlarmScheduler.stableRequestCode("notification|$occurrenceId")
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DESTINATION_TODAY)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, copy.channelId)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setColor(Color.parseColor("#0D6B62"))
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.bigText))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(
                0,
                "Mark as prayed",
                actionPendingIntent(
                    action = NotificationActionReceiver.ACTION_MARK_PRAYED,
                    requestCode = PrayerAlarmScheduler.stableRequestCode("mark|$occurrenceId"),
                    occurrenceId = occurrenceId,
                    prayer = prayer.name,
                    trackingDate = trackingDate,
                    mosqueId = mosqueId
                )
            )
            .addAction(
                0,
                "Snooze 10m",
                actionPendingIntent(
                    action = NotificationActionReceiver.ACTION_SNOOZE,
                    requestCode = PrayerAlarmScheduler.stableRequestCode("snooze|$occurrenceId"),
                    occurrenceId = occurrenceId,
                    prayer = prayer.name,
                    trackingDate = trackingDate,
                    mosqueId = mosqueId
                )
            )
            .addAction(
                0,
                "Open Qibla",
                PendingIntent.getActivity(
                    context,
                    PrayerAlarmScheduler.stableRequestCode("qibla|$occurrenceId"),
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DESTINATION_QIBLA)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

        when (soundMode) {
            ReminderSoundMode.SILENT -> builder.setSilent(true).setVibrate(null)
            ReminderSoundMode.VIBRATE -> builder
                .setSound(null)
                .setVibrate(longArrayOf(0, 350, 250, 350))
            ReminderSoundMode.SHORT_TONE -> builder.setVibrate(longArrayOf(0, 250))
            ReminderSoundMode.CUSTOM_AUDIO -> builder.setVibrate(longArrayOf(0, 250))
            ReminderSoundMode.SYSTEM -> Unit
        }
        return builder.build()
    }

    private fun actionPendingIntent(
        action: String,
        requestCode: Int,
        occurrenceId: String,
        prayer: String,
        trackingDate: String,
        mosqueId: String?
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            data = Uri.parse("namaz-times://action/$action/$occurrenceId")
            putExtra(NotificationActionReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(NotificationActionReceiver.EXTRA_PRAYER, prayer)
            putExtra(NotificationActionReceiver.EXTRA_TRACKING_DATE, trackingDate)
            putExtra(NotificationActionReceiver.EXTRA_MOSQUE_ID, mosqueId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
