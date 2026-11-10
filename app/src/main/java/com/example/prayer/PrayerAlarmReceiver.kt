package com.example.prayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.NamazTimesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_EVENT_ID) ?: return
        val pendingResult = goAsync()
        val application = context.applicationContext as? NamazTimesApplication
        if (application == null) {
            pendingResult.finish()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                application.graph.prayerNotificationDispatcher.deliver(eventId)
            } catch (error: Exception) {
                Log.e("PrayerAlarmReceiver", "Alarm handling failed for $eventId", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
