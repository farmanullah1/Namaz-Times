package com.example.prayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Permission-aware AlarmManager wrapper. Each event owns an explicit, stable PendingIntent identity
 * (action + unique data URI + request code), so rebuilding can always cancel exactly what it made.
 * When exact alarms are unavailable the documented inexact fallback is used and reported upwards.
 */
class PrayerAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun canUseExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun isUsingInexactFallback(): Boolean = !canUseExactAlarms()

    fun schedule(eventId: String, requestCode: Int, triggerAtMillis: Long): Result<Unit> {
        val manager = alarmManager ?: return Result.failure(IllegalStateException("AlarmManager unavailable"))
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return Result.failure(IllegalArgumentException("Alarm time must be in the future"))
        }
        val pendingIntent = buildPendingIntent(eventId, requestCode)
            ?: return Result.failure(IllegalStateException("Could not create an alarm PendingIntent"))
        return try {
            if (canUseExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Result.success(Unit)
        } catch (error: SecurityException) {
            Result.failure(error)
        } catch (error: RuntimeException) {
            Result.failure(error)
        }
    }

    fun cancel(eventId: String, requestCode: Int) {
        val manager = alarmManager ?: return
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM
            data = Uri.parse("$SCHEME://prayer-alarm/$eventId")
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            manager.cancel(it)
            it.cancel()
        }
    }

    fun scheduleSilenceRestore(triggerAtMillis: Long): Result<Unit> {
        val manager = alarmManager ?: return Result.failure(IllegalStateException("AlarmManager unavailable"))
        val intent = Intent(context, SilenceRestoreReceiver::class.java).apply {
            action = ACTION_SILENCE_RESTORE
            data = Uri.parse("$SCHEME://silence-restore")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SILENCE_RESTORE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            if (canUseExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Result.success(Unit)
        } catch (error: SecurityException) {
            Result.failure(error)
        }
    }

    private fun buildPendingIntent(eventId: String, requestCode: Int): PendingIntent? {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM
            data = Uri.parse("$SCHEME://prayer-alarm/$eventId")
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.namaztimes.PRAYER_ALARM"
        const val ACTION_SILENCE_RESTORE = "com.example.namaztimes.SILENCE_RESTORE"
        const val SCHEME = "namaz-times"
        const val EXTRA_EVENT_ID = "event_id"
        const val SILENCE_RESTORE_REQUEST_CODE = 990_001

        /**
         * Deterministic 31-bit request code derived from the stable event key. Collisions are
         * resolved by the rebuild coordinator, which checks already-used codes.
         */
        fun stableRequestCode(eventId: String, salt: Int = 0): Int {
            var hash = 2166136261L xor salt.toLong()
            for (character in eventId) {
                hash = hash xor character.code.toLong()
                hash = (hash * 16777619L) and 0xFFFFFFFFL
            }
            return (hash and 0x7FFFFFFF).toInt()
        }
    }
}
