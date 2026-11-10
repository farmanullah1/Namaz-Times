package com.example

import android.content.Context
import com.example.data.local.NamazDatabase
import com.example.data.repository.BackupRepository
import com.example.data.repository.MosqueRepository
import com.example.data.repository.PrayerHistoryRepository
import com.example.data.repository.QuranRepository
import com.example.data.repository.SettingsRepository
import com.example.location.ActiveSelectionCoordinator
import com.example.location.GeofenceManager
import com.example.location.LocationRepository
import com.example.prayer.AdhanPrayerCalculator
import com.example.prayer.MosqueScheduleResolver
import com.example.prayer.PrayerAlarmScheduler
import com.example.prayer.PrayerNotificationDispatcher
import com.example.prayer.PrayerOccurrenceFactory
import com.example.prayer.PrayerScheduleService
import com.example.prayer.PrayerSilenceManager
import com.example.prayer.ScheduleRebuildCoordinator

/**
 * Single application graph. Everything is local: Room for durable records, DataStore for
 * preferences, and Android system APIs for alarms, notifications, location and sensors.
 */
class AppGraph(context: Context) {

    private val appContext = context.applicationContext

    val database: NamazDatabase = NamazDatabase.get(appContext)
    val settingsRepository = SettingsRepository(appContext)
    val mosqueRepository = MosqueRepository(database)
    val prayerHistoryRepository = PrayerHistoryRepository(database)
    val quranRepository = QuranRepository(database)

    val adhanCalculator = AdhanPrayerCalculator()
    val scheduleResolver = MosqueScheduleResolver()
    val occurrenceFactory = PrayerOccurrenceFactory(adhanCalculator, scheduleResolver)
    val prayerScheduleService = PrayerScheduleService(occurrenceFactory)

    val alarmScheduler = PrayerAlarmScheduler(appContext)
    val rebuildCoordinator = ScheduleRebuildCoordinator(
        context = appContext,
        database = database,
        settingsRepository = settingsRepository,
        factory = occurrenceFactory,
        alarmScheduler = alarmScheduler
    )
    val silenceManager = PrayerSilenceManager(appContext, settingsRepository, alarmScheduler)
    val prayerNotificationDispatcher = PrayerNotificationDispatcher(
        context = appContext,
        database = database,
        settingsRepository = settingsRepository,
        rebuildCoordinator = rebuildCoordinator,
        silenceManager = silenceManager
    )

    val locationRepository = LocationRepository(appContext)
    val geofenceManager = GeofenceManager(appContext)
    val activeSelection = ActiveSelectionCoordinator(
        settingsRepository = settingsRepository,
        mosqueRepository = mosqueRepository,
        locationRepository = locationRepository,
        rebuildCoordinator = rebuildCoordinator,
        geofenceManager = geofenceManager
    )

    val backupRepository = BackupRepository(appContext, database, settingsRepository)
}
