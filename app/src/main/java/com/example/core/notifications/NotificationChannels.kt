package com.example.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.R

enum class PrayerChannel(val id: String, val titleRes: Int, val importance: Int) {
    ADHAN("prayer_adhan", R.string.notification_channel_adhan, NotificationManager.IMPORTANCE_HIGH),
    IQAMAH("prayer_iqamah", R.string.notification_channel_iqamah, NotificationManager.IMPORTANCE_HIGH),
    PRE_REMINDER("prayer_pre_reminder", R.string.notification_channel_pre, NotificationManager.IMPORTANCE_DEFAULT),
    FRIDAY("prayer_friday", R.string.notification_channel_friday, NotificationManager.IMPORTANCE_HIGH),
    RELIABILITY("prayer_reliability", R.string.notification_channel_reliability, NotificationManager.IMPORTANCE_LOW)
}

object NotificationChannels {

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        PrayerChannel.entries.forEach { channel ->
            val existing = manager.getNotificationChannel(channel.id)
            if (existing == null) {
                val notificationChannel = NotificationChannel(
                    channel.id,
                    context.getString(channel.titleRes),
                    channel.importance
                ).apply {
                    description = when (channel) {
                        PrayerChannel.ADHAN -> "Adhan-time reminders for your selected mosque."
                        PrayerChannel.IQAMAH -> "Iqamah-time reminders entered in your mosque timetable."
                        PrayerChannel.PRE_REMINDER -> "Advance reminders before Adhan or Iqamah."
                        PrayerChannel.FRIDAY -> "Khutbah and Jumuah reminders."
                        PrayerChannel.RELIABILITY -> "Warnings when reminders need repair or permissions change."
                    }
                    enableVibration(channel != PrayerChannel.RELIABILITY)
                    enableLights(true)
                    if (channel == PrayerChannel.RELIABILITY) {
                        setSound(
                            null,
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .build()
                        )
                    } else {
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    }
                }
                manager.createNotificationChannel(notificationChannel)
            }
        }
    }

    fun isChannelEnabled(context: Context, channel: PrayerChannel): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val existing = manager.getNotificationChannel(channel.id) ?: return false
        return existing.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun channelIdFor(eventType: String): String = when (eventType) {
        "IQAMAH", "PRE_IQAMAH" -> PrayerChannel.IQAMAH.id
        "PRE_ADHAN" -> PrayerChannel.PRE_REMINDER.id
        "KHUTBAH", "JUMUAH" -> PrayerChannel.FRIDAY.id
        else -> PrayerChannel.ADHAN.id
    }
}
