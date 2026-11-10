package com.example

import android.app.Application
import com.example.core.notifications.NotificationChannels

class NamazTimesApplication : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureChannels(this)
        graph = AppGraph(this)
    }
}
