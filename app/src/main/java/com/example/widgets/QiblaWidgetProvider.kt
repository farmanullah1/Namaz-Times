package com.example.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QiblaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        refresh(context, intArrayOf())
    }

    private fun refresh(context: Context, ids: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val targetIds = if (ids.isEmpty()) {
                    AppWidgetManager.getInstance(context)
                        .getAppWidgetIds(ComponentName(context, this@QiblaWidgetProvider.javaClass))
                } else {
                    ids
                }
                WidgetUpdateCoordinator(context).refreshIds(this@QiblaWidgetProvider.javaClass, targetIds)
            } catch (error: Exception) {
                Log.e("QiblaWidget", "Widget refresh failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
