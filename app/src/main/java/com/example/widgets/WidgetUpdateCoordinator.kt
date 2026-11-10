package com.example.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.core.time.formatClock
import com.example.data.local.NamazDatabase
import com.example.data.repository.MosqueRepository
import com.example.data.repository.SettingsRepository
import com.example.hijri.HijriDateProvider
import com.example.domain.model.HijriSettings
import com.example.domain.model.PrayerEventType
import com.example.domain.model.PrayerName
import com.example.prayer.AdhanPrayerCalculator
import com.example.prayer.MosqueScheduleResolver
import com.example.prayer.PrayerOccurrenceFactory
import com.example.prayer.PrayerScheduleService
import java.time.Instant

data class WidgetSnapshot(
    val mosqueName: String,
    val nextPrayerTitle: String,
    val nextPrayerArabic: String,
    val timeLabel: String,
    val iqamahLabel: String?,
    val countdownLabel: String,
    val sourceLabel: String,
    val hijriLabel: String,
    val stale: Boolean
)

/**
 * Classic RemoteViews widgets (no Glance dependency). Countdowns are refreshed periodically or on
 * events; a widget never pretends to tick every second, and stale data is labelled as such.
 */
class WidgetUpdateCoordinator(private val context: Context) {

    private val database = NamazDatabase.get(context)
    private val settingsRepository = SettingsRepository(context)
    private val factory = PrayerOccurrenceFactory(AdhanPrayerCalculator(), MosqueScheduleResolver())
    private val scheduleService = PrayerScheduleService(factory)
    private val hijriProvider = HijriDateProvider()

    suspend fun refreshAll() {
        refreshIds(TodayWidgetProvider::class.java, widgetIds(TodayWidgetProvider::class.java))
        refreshIds(CombinedWidgetProvider::class.java, widgetIds(CombinedWidgetProvider::class.java))
    }

    suspend fun refreshIds(providerClass: Class<*>, ids: IntArray) {
        if (ids.isEmpty()) return
        val snapshot = computeSnapshot() ?: run {
            renderSetup(providerClass, ids)
            return
        }
        val manager = AppWidgetManager.getInstance(context)
        ids.forEach { widgetId ->
            val layout = if (providerClass == CombinedWidgetProvider::class.java) {
                R.layout.widget_combined
            } else {
                R.layout.widget_today
            }
            val views = RemoteViews(context.packageName, layout)
            views.setTextViewText(R.id.widget_mosque, snapshot.mosqueName)
            views.setTextViewText(R.id.widget_prayer, snapshot.nextPrayerTitle)
            views.setTextViewText(R.id.widget_time, snapshot.timeLabel)
            views.setTextViewText(R.id.widget_countdown, snapshot.countdownLabel)
            views.setTextViewText(R.id.widget_status, snapshot.sourceLabel)
            if (layout == R.layout.widget_combined) {
                views.setTextViewText(R.id.widget_hijri, snapshot.hijriLabel)
                views.setOnClickPendingIntent(R.id.widget_qibla, openAppIntent(MainActivity.DESTINATION_QIBLA, 9001))
            }
            views.setTextViewText(R.id.widget_iqamah, snapshot.iqamahLabel ?: "Iqamah not entered")
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(MainActivity.DESTINATION_TODAY, 9000))
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun renderSetup(providerClass: Class<*>, ids: IntArray) {
        val manager = AppWidgetManager.getInstance(context)
        ids.forEach { widgetId ->
            val layout = if (providerClass == CombinedWidgetProvider::class.java) {
                R.layout.widget_combined
            } else {
                R.layout.widget_today
            }
            val views = RemoteViews(context.packageName, layout)
            views.setTextViewText(R.id.widget_mosque, "Namaz Times")
            views.setTextViewText(R.id.widget_prayer, "Set up your mosque")
            views.setTextViewText(R.id.widget_time, "—")
            views.setTextViewText(R.id.widget_iqamah, "No timetable yet")
            views.setTextViewText(R.id.widget_countdown, "Open the app to add a place and mosque.")
            views.setTextViewText(R.id.widget_status, "Tap to set up")
            if (layout == R.layout.widget_combined) {
                views.setTextViewText(R.id.widget_hijri, "")
            }
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(MainActivity.DESTINATION_TODAY, 9000))
            manager.updateAppWidget(widgetId, views)
        }
    }

    private suspend fun computeSnapshot(): WidgetSnapshot? {
        val settings = settingsRepository.current()
        val snapshot = MosqueRepository(database).snapshot()
        val now = Instant.now()
        val today = scheduleService.buildToday(settings, snapshot, now)
        val next = today.next ?: return null

        val zone = today.zone
        val timeLabel = formatClock(next.instant, zone)
        val iqamahLabel = next.iqamah?.let { formatClock(it, zone) }
        val countdownMillis = next.instant.toEpochMilli() - now.toEpochMilli()
        val countdown = when {
            next.isTomorrow -> "Tomorrow · ${formatCountdown(countdownMillis)}"
            else -> formatCountdown(countdownMillis)
        }
        val maghrib = today.resolved[PrayerName.MAGHRIB]?.adhan
        val maghribPassed = maghrib != null && now.isAfter(maghrib)
        val hijri = hijriProvider.hijriFor(
            date = today.today,
            settings = HijriSettings(
                adjustmentDays = settings.hijriAdjustmentDays,
                sunsetBoundary = settings.hijriSunsetBoundary
            ),
            maghribPassed = maghribPassed
        )
        val stale = settings.lastSuccessfulRebuildMillis == null ||
            now.toEpochMilli() - settings.lastSuccessfulRebuildMillis > 24 * 60 * 60 * 1000L

        return WidgetSnapshot(
            mosqueName = today.mosque?.name ?: "Calculated — travel mode",
            nextPrayerTitle = next.prayer.title,
            nextPrayerArabic = next.prayer.arabicTitle,
            timeLabel = timeLabel,
            iqamahLabel = iqamahLabel,
            countdownLabel = countdown,
            sourceLabel = if (stale) {
                "${next.source.label} · last updated earlier"
            } else {
                "${next.source.label} · ${today.calculationLabel}"
            },
            hijriLabel = hijri.formatted + if (hijri.isAdjusted) " (adjusted)" else "",
            stale = stale
        )
    }

    private fun formatCountdown(millis: Long): String {
        val totalMinutes = (millis / 60_000L).coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 24 -> "in ${hours / 24}d ${hours % 24}h"
            hours > 0 -> "in ${hours}h ${minutes}m"
            else -> "in ${minutes}m"
        }
    }

    private fun openAppIntent(destination: String, requestCode: Int): android.app.PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse("namaz-times://open/$destination")
            putExtra(MainActivity.EXTRA_DESTINATION, destination)
        }
        return android.app.PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun widgetIds(providerClass: Class<*>): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, providerClass))
}
