package com.example.prayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.NamazTimesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SilenceRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val application = context.applicationContext as? NamazTimesApplication
        if (application == null) {
            pendingResult.finish()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                application.graph.silenceManager.restoreIfDue(force = true)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
