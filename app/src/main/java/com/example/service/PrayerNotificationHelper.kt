package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object PrayerNotificationHelper {
    private const val CHANNEL_ID = "prayer_reminder_channel"
    private const val CHANNEL_NAME = "Salah & Iqamah Reminders"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Reminders for Azan, Salah, and Mosque Iqamah times"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPrayerAlert(
        context: Context,
        prayerName: String,
        mosqueName: String,
        azanTime: String,
        iqamahTime: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$prayerName Time - $mosqueName")
            .setContentText("Azan: $azanTime | Iqamah: $iqamahTime")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("It is time for $prayerName prayer at $mosqueName.\nAzan: $azanTime • Iqamah: $iqamahTime\n\"Indeed, prayer has been decreed upon the believers a decree of specified times.\" (Surah An-Nisa: 103)")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(prayerName.hashCode(), builder.build())
    }
}
