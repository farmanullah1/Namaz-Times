package com.example.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.NamazTimesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Rebuilds future reminders after reboot, time/timezone change, app update or an exact-alarm
 * permission change. Alarms themselves do not survive these events, so this must be idempotent.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val application = context.applicationContext as? NamazTimesApplication ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                application.graph.rebuildCoordinator.rebuild(
                    "System trigger: ${intent.action ?: "unknown"}"
                )
            } catch (error: Exception) {
                Log.e(TAG, "Rebuild after system event failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
