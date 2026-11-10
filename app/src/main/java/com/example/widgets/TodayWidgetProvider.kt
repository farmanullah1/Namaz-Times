package com.example.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context, appWidgetIds, this.javaClass)
    }

    override fun onEnabled(context: Context) {
        refresh(context, intArrayOf(), this.javaClass)
    }

    private fun refresh(context: Context, ids: IntArray, providerClass: Class<*>) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val targetIds = if (ids.isEmpty()) {
                    AppWidgetManager.getInstance(context)
                        .getAppWidgetIds(android.content.ComponentName(context, providerClass))
                } else {
                    ids
                }
                WidgetUpdateCoordinator(context).refreshIds(providerClass, targetIds)
            } catch (error: Exception) {
                Log.e("TodayWidget", "Widget refresh failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
