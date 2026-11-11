package com.example.ui

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NamazTimesApplication
import com.example.core.notifications.NotificationChannels
import com.example.core.notifications.PrayerChannel
import com.example.core.notifications.ReminderAudioPlayer
import com.example.data.local.PrayerHistoryRecord
import com.example.data.local.QadaRecord
import com.example.data.local.QuranSession
import com.example.data.repository.BackupPreview
import com.example.data.repository.MosqueSnapshot
import com.example.domain.model.AppSettings
import com.example.domain.model.CalculationSettings
import com.example.domain.model.HijriInfo
import com.example.domain.model.HijriSettings
import com.example.domain.model.LocationMode
import com.example.domain.model.ManualOverride
import com.example.domain.model.MosqueProfile
import com.example.domain.model.MosqueSchedule
import com.example.domain.model.PrayerEventType
import com.example.domain.model.PrayerName
import com.example.domain.model.QuranDayBoundary
import com.example.domain.model.QuranResetRule
import com.example.domain.model.QuranSettings
import com.example.domain.model.QuranSnapshot
import com.example.domain.model.QuranStructureData
import com.example.domain.model.ReminderPreference
import com.example.domain.model.ReminderSoundMode
import com.example.domain.model.SavedPlace
import com.example.domain.model.ScheduleEntry
import com.example.hijri.HijriDateProvider
import com.example.prayer.TodayContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class AppUiState(
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val snapshot: MosqueSnapshot = MosqueSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
    val today: TodayContext? = null,
    val history: List<PrayerHistoryRecord> = emptyList(),
    val recentHistory: List<PrayerHistoryRecord> = emptyList(),
    val qada: List<QadaRecord> = emptyList(),
    val quran: QuranSnapshot = QuranSnapshot(),
    val quranSessionsToday: List<QuranSession> = emptyList(),
    val markedRukuKeys: Set<String> = emptySet(),
    val hijri: HijriInfo? = null,
    val nowMillis: Long = System.currentTimeMillis(),
    val countdownMillis: Long = 0,
    val message: String? = null,
    val exactAlarmAvailable: Boolean = true,
    val notificationsAllowed: Boolean = true,
    val adhanChannelEnabled: Boolean = true,
    val locationPermissionGranted: Boolean = false,
    val backgroundLocationGranted: Boolean = false,
    val locationServicesEnabled: Boolean = false,
    val batteryOptimizationIgnored: Boolean = true,
    val busy: Boolean = false
) {
    val activeMosque: MosqueProfile? get() = today?.mosque
    val activePlace: SavedPlace? get() = today?.place
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val graph = (application as NamazTimesApplication).graph
    private val hijriProvider = HijriDateProvider()
    private val audioPlayer = ReminderAudioPlayer(application)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var lastReloadedDate: LocalDate? = null

    init {
        viewModelScope.launch {
            graph.settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            runCatching { graph.silenceManager.restoreIfDue() }
            if (graph.activeSelection.expireTemporaryIfNeeded()) {
                _uiState.update { it.copy(message = "The temporary mosque override expired.") }
            }
            reloadAll()
        }
        viewModelScope.launch { ticker() }
    }

    private suspend fun ticker() {
        while (true) {
            delay(1000)
            val state = _uiState.value
            val next = state.today?.next?.instant?.toEpochMilli()
            val now = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    nowMillis = now,
                    countdownMillis = next?.minus(now)?.coerceAtLeast(0) ?: 0
                )
            }
            val today = state.today ?: continue
            val localDate = Instant.ofEpochMilli(now).atZone(today.zone).toLocalDate()
            if (localDate != lastReloadedDate || (next != null && now >= next)) {
                reloadAll()
            }
            val tempExpiry = state.settings.temporaryExpiresAtMillis
            if (tempExpiry != null && tempExpiry <= now) {
                graph.activeSelection.expireTemporaryIfNeeded()
                reloadAll()
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun postMessage(message: String) = _uiState.update { it.copy(message = message) }

    fun reloadAll(block: suspend () -> Unit = {}) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
                refreshFromStorage()
            } catch (error: Exception) {
                postMessage(error.message ?: "The app could not refresh its data.")
            }
        }
    }

    suspend fun refreshFromStorage() {
        val context = getApplication<Application>()
        val settings = graph.settingsRepository.current()
        val snapshot = graph.mosqueRepository.snapshot()
        val now = Instant.now()
        val today = graph.prayerScheduleService.buildToday(settings, snapshot, now)
        lastReloadedDate = today.today

        val history = graph.prayerHistoryRepository.historyForDate(today.today)
        val recent = graph.prayerHistoryRepository.historyBetween(today.today.minusDays(29), today.today)
        val qada = graph.prayerHistoryRepository.qadaRecords()

        var quranSettings = settings.quran.copy(
            startedOn = settings.quran.startedOn ?: graph.quranRepository.firstSessionDate() ?: today.today
        )
        val goals = graph.quranRepository.goalHistory()
        if (goals.isEmpty() && quranSettings.startedOn != null) {
            graph.quranRepository.setDailyGoal(quranSettings.dailyGoal, quranSettings.startedOn!!)
        }
        if (quranSettings != settings.quran) {
            graph.settingsRepository.setQuranSettings(quranSettings)
        }

        val quranKey = quranDateKey(quranSettings, today, now)
        val quranSnapshot = graph.quranRepository.snapshot(quranKey, quranSettings)
        val sessions = graph.quranRepository.sessionsForDate(quranKey)
        val markedRukus = graph.quranRepository.markedRukuKeys()

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

        val exactAlarmAvailable = graph.alarmScheduler.canUseExactAlarms()
        val notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val adhanChannel = NotificationChannels.isChannelEnabled(context, PrayerChannel.ADHAN)
        val batteryIgnored = try {
            val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            power?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } catch (error: Exception) {
            true
        }

        _uiState.update {
            it.copy(
                loading = false,
                settings = settings.copy(quran = quranSettings),
                snapshot = snapshot,
                today = today,
                history = history,
                recentHistory = recent,
                qada = qada,
                quran = quranSnapshot,
                quranSessionsToday = sessions,
                markedRukuKeys = markedRukus,
                hijri = hijri,
                nowMillis = now.toEpochMilli(),
                countdownMillis = today.next?.instant?.toEpochMilli()?.minus(now.toEpochMilli())?.coerceAtLeast(0) ?: 0,
                exactAlarmAvailable = exactAlarmAvailable,
                notificationsAllowed = notificationsAllowed,
                adhanChannelEnabled = adhanChannel,
                locationPermissionGranted = graph.locationRepository.hasForegroundLocationPermission(),
                backgroundLocationGranted = graph.locationRepository.hasBackgroundLocationPermission(),
                locationServicesEnabled = graph.locationRepository.isLocationEnabled(),
                batteryOptimizationIgnored = batteryIgnored
            )
        }
    }

    private suspend fun quranDateKey(settings: QuranSettings, today: TodayContext, now: Instant): LocalDate =
        QuranDayBoundary.dateKeyFor(
            now = now,
            zone = today.zone,
            rule = settings.resetRule,
            customTime = settings.customResetTime,
            todaysFajr = today.resolved[PrayerName.FAJR]?.adhan,
            todaysMaghrib = today.resolved[PrayerName.MAGHRIB]?.adhan
        )

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            try {
                block()
                refreshFromStorage()
            } catch (error: Exception) {
                postMessage(error.message ?: "That action could not be completed.")
            } finally {
                _uiState.update { it.copy(busy = false) }
            }
        }
    }

    // ------------------------------------------------------------------ location & selection

    fun refreshLocation() = action {
        val update = graph.activeSelection.refreshFromLocation("Manual refresh")
        postMessage(update.message)
        if (update.changed) {
            graph.rebuildCoordinator.rebuild("Location selection changed")
        }
    }

    fun selectManual(mosqueId: String?, placeId: String?) = action {
        graph.settingsRepository.setManualSelection(mosqueId, placeId)
        graph.rebuildCoordinator.rebuild("Manual mosque selection")
        postMessage("Manual selection saved.")
    }

    fun selectTemporary(mosqueId: String, placeId: String?, hours: Int) = action {
        graph.settingsRepository.setTemporarySelection(mosqueId, placeId, hours)
        graph.rebuildCoordinator.rebuild("Temporary override started")
        postMessage("Temporary selection active for $hours hour(s).")
    }

    fun returnToAutomatic() = action {
        graph.settingsRepository.setAutomaticLocationEnabled(true)
        val update = graph.activeSelection.refreshFromLocation("Returned to automatic")
        graph.rebuildCoordinator.rebuild("Automatic mode")
        postMessage(update.message)
    }

    fun setConvenienceMode(mode: LocationMode) = action {
        graph.settingsRepository.setLocationMode(mode)
        graph.rebuildCoordinator.rebuild("Location mode changed")
    }

    fun useCalculatedForCurrentLocation() = action {
        graph.settingsRepository.setManualSelection(null, null)
        graph.rebuildCoordinator.rebuild("Travel / calculated mode")
        postMessage("Using calculated times for your current location.")
    }

    fun enableAutomaticSwitching() = action {
        graph.settingsRepository.setAutomaticLocationEnabled(true)
        val update = graph.activeSelection.refreshFromLocation("Automatic switching enabled")
        if (graph.locationRepository.hasBackgroundLocationPermission()) {
            val result = graph.geofenceManager.sync(
                graph.mosqueRepository.snapshot().places,
                hasBackgroundPermission = true
            )
            if (result.isFailure) postMessage("Geofences could not be registered. Manual selection still works.")
        }
        graph.rebuildCoordinator.rebuild("Automatic switching")
        postMessage(update.message)
    }

    fun syncGeofences() = action {
        val result = graph.geofenceManager.sync(
            graph.mosqueRepository.snapshot().places,
            graph.locationRepository.hasBackgroundLocationPermission()
        )
        postMessage(
            result.fold(
                onSuccess = { count -> "$count saved place boundary(ies) registered." },
                onFailure = { it.message ?: "Geofences could not be registered." }
            )
        )
    }

    // ------------------------------------------------------------------ places & mosques

    fun savePlace(place: SavedPlace) = action {
        graph.mosqueRepository.upsertPlace(place)
        graph.rebuildCoordinator.rebuild("Saved place updated")
        postMessage("Place saved.")
    }

    fun deletePlace(placeId: String) = action {
        graph.mosqueRepository.deletePlace(placeId)
        graph.rebuildCoordinator.rebuild("Saved place deleted")
        postMessage("Place deleted.")
    }

    fun saveMosque(mosque: MosqueProfile) = action {
        graph.mosqueRepository.upsertMosque(mosque)
        graph.rebuildCoordinator.rebuild("Mosque profile saved")
        postMessage("Mosque profile saved.")
    }

    fun deleteMosque(mosqueId: String) = action {
        graph.mosqueRepository.deleteMosque(mosqueId)
        graph.rebuildCoordinator.rebuild("Mosque profile deleted")
        postMessage("Mosque profile and its timetables were deleted.")
    }

    fun saveSchedule(
        mosqueId: String,
        schedule: MosqueSchedule,
        entries: List<ScheduleEntry>,
        confirmIqamahBeforeAdhan: Boolean
    ) = action {
        val result = graph.mosqueRepository.saveSchedule(mosqueId, schedule, entries, confirmIqamahBeforeAdhan)
        result.fold(
            onSuccess = {
                val outcome = graph.rebuildCoordinator.rebuild("Timetable revision $it")
                postMessage(outcome.message)
            },
            onFailure = { postMessage(it.message ?: "The timetable could not be saved.") }
        )
    }

    fun deleteSchedule(schedule: MosqueSchedule) = action {
        graph.mosqueRepository.deleteSchedule(schedule)
        graph.rebuildCoordinator.rebuild("Timetable deleted")
        postMessage("Timetable deleted.")
    }

    fun saveManualOverride(override: ManualOverride) = action {
        graph.mosqueRepository.saveManualOverride(override)
        graph.rebuildCoordinator.rebuild("Manual prayer override")
        postMessage("Override saved for ${override.date}.")
    }

    fun saveReminderPreferences(preferences: List<ReminderPreference>) = action {
        graph.mosqueRepository.saveReminderPreferences(preferences)
        graph.rebuildCoordinator.rebuild("Reminder preferences changed")
        postMessage("Reminder preferences saved.")
    }

    // ------------------------------------------------------------------ prayer history

    fun markPrayed(prayer: PrayerName, context: String, tags: String, note: String) = action {
        val today = _uiState.value.today ?: return@action
        val occurrence = today.occurrences.firstOrNull {
            it.prayer == prayer && it.eventType == PrayerEventType.ADHAN
        }
        graph.prayerHistoryRepository.markPrayed(
            prayer = prayer,
            trackingDate = today.today,
            occurrence = occurrence,
            mosqueId = today.mosque?.id,
            placeId = today.place?.id,
            context = context,
            tags = tags,
            note = note
        )
        postMessage("${prayer.title} marked as prayed.")
    }

    fun undoPrayed(recordId: String) = action {
        graph.prayerHistoryRepository.undo(recordId)
        postMessage("Prayer record removed.")
    }

    fun updateHistoryRecord(record: PrayerHistoryRecord) = action {
        graph.prayerHistoryRepository.updateRecord(record)
        postMessage("Prayer record updated.")
    }

    fun addQada(prayer: PrayerName, date: LocalDate, note: String) = action {
        graph.prayerHistoryRepository.addQada(prayer, date, note)
        postMessage("Qada record added.")
    }

    fun deleteQada(id: String) = action {
        graph.prayerHistoryRepository.deleteQada(id)
        postMessage("Qada record removed.")
    }

    fun setQadaEnabled(enabled: Boolean) = action {
        graph.settingsRepository.setQadaEnabled(enabled)
    }

    // ------------------------------------------------------------------ Quran

    private suspend fun quranContext(): Pair<QuranSettings, LocalDate> {
        val settings = graph.settingsRepository.current()
        val snapshot = graph.mosqueRepository.snapshot()
        val today = graph.prayerScheduleService.buildToday(settings, snapshot, Instant.now())
        return settings.quran to quranDateKey(settings.quran, today, Instant.now())
    }

    fun addQuranRukus(count: Int, surah: String?, rukuNumber: Int?, note: String) = action {
        val (_, dateKey) = quranContext()
        val result = graph.quranRepository.addRukus(
            dateKey = dateKey,
            count = count,
            surah = textOrNull(surah),
            rukuNumber = rukuNumber,
            note = note
        )
        result.onFailure { postMessage(it.message ?: "The reading entry could not be saved.") }
    }

    fun undoLastRuku() = action {
        val removed = graph.quranRepository.undoLast()
        postMessage(if (removed != null) "Last reading entry removed." else "There is no reading entry to undo.")
    }

    fun toggleRukuCheckoff(surahName: String, rukuNumber: Int, isChecked: Boolean) = action {
        val (_, dateKey) = quranContext()
        if (isChecked) {
            graph.quranRepository.addRukus(
                dateKey = dateKey,
                count = 1,
                surah = surahName,
                rukuNumber = rukuNumber,
                note = "Checked from Ruku checklist"
            )
            postMessage("Ruku $rukuNumber of $surahName marked read.")
        } else {
            graph.quranRepository.deleteRukuSession(surahName, rukuNumber)
            postMessage("Ruku $rukuNumber of $surahName unmarked.")
        }
    }

    fun quickAddRukus(count: Int) = action {
        val (_, dateKey) = quranContext()
        val result = graph.quranRepository.addRukus(
            dateKey = dateKey,
            count = count,
            note = "Quick log (+$count)"
        )
        result.fold(
            onSuccess = { postMessage("Added $count Ruku${if (count > 1) "s" else ""} for today.") },
            onFailure = { postMessage(it.message ?: "Could not log Rukus.") }
        )
    }

    fun setKhatamTargetDate(targetDate: LocalDate) = action {
        val snapshot = _uiState.value.quran
        val remaining = (QuranStructureData.TOTAL_RUKUS - snapshot.totalCompleted).coerceAtLeast(1)
        val pace = QuranStructureData.calculateRequiredPace(remaining, targetDate = targetDate)
        val current = graph.settingsRepository.current().quran
        val (_, todayKey) = quranContext()
        graph.quranRepository.setDailyGoal(pace, todayKey.plusDays(1))
        graph.settingsRepository.setQuranSettings(current.copy(dailyGoal = pace))
        postMessage("Khatam target set. Daily goal updated to $pace Rukus per day.")
    }

    fun correctQuranDay(date: LocalDate, newTotal: Int, reason: String) = action {
        val result = graph.quranRepository.correctDayTotal(date, newTotal, reason)
        result.fold(
            onSuccess = { postMessage("Reading day corrected. Ledger recalculated.") },
            onFailure = { postMessage(it.message ?: "The correction could not be saved.") }
        )
    }

    fun updateQuranSettings(newSettings: QuranSettings) = action {
        val current = graph.settingsRepository.current().quran
        val (_, todayKey) = quranContext()
        if (newSettings.dailyGoal != current.dailyGoal) {
            graph.quranRepository.setDailyGoal(newSettings.dailyGoal, todayKey.plusDays(1))
            postMessage("Daily goal updated. It applies from the next reading day.")
        }
        graph.settingsRepository.setQuranSettings(newSettings)
    }

    fun setQuranResetRule(rule: QuranResetRule) = action {
        val current = graph.settingsRepository.current().quran
        graph.settingsRepository.setQuranSettings(current.copy(resetRule = rule))
        postMessage("Reading-day reset rule updated.")
    }

    fun setQuranCustomResetTime(time: LocalTime) = action {
        val current = graph.settingsRepository.current().quran
        graph.settingsRepository.setQuranSettings(
            current.copy(resetRule = QuranResetRule.CUSTOM_TIME, customResetTime = time)
        )
        postMessage("Reading-day reset time updated.")
    }

    fun setQuranSurplus(enabled: Boolean) = action {
        val current = graph.settingsRepository.current().quran
        graph.settingsRepository.setQuranSettings(current.copy(surplusCreditEnabled = enabled))
    }

    fun setQuranEditionTotal(total: Int?, label: String) = action {
        val current = graph.settingsRepository.current().quran
        graph.settingsRepository.setQuranSettings(
            current.copy(editionTotalRukus = total, editionLabel = label)
        )
        postMessage(if (total != null) "Quran edition total saved." else "Overall percentage disabled.")
    }

    // ------------------------------------------------------------------ settings

    fun setCalculation(calculation: CalculationSettings) = action {
        graph.settingsRepository.setCalculation(calculation)
        val outcome = graph.rebuildCoordinator.rebuild("Calculation settings changed")
        postMessage(outcome.message)
    }

    fun setHijri(adjustmentDays: Int, sunsetBoundary: Boolean) = action {
        graph.settingsRepository.setHijri(adjustmentDays, sunsetBoundary)
        postMessage("Hijri display settings saved.")
    }

    fun setThemeMode(mode: String) = action {
        graph.settingsRepository.setThemeMode(mode)
    }

    fun setRemindersEnabled(enabled: Boolean) = action {
        graph.settingsRepository.setRemindersEnabled(enabled)
        val outcome = graph.rebuildCoordinator.rebuild(if (enabled) "Reminders enabled" else "Reminders disabled")
        postMessage(outcome.message)
    }

    fun onNotificationPermissionResult(granted: Boolean) = action {
        if (granted) {
            graph.settingsRepository.setRemindersEnabled(true)
            graph.rebuildCoordinator.rebuild("Notification permission granted")
            postMessage("Notifications enabled. Adhan and Iqamah reminders are armed.")
        } else {
            postMessage("Notification permission denied. Timetables still work; reminders will not appear.")
        }
    }

    fun setReminderSound(mode: ReminderSoundMode, customUri: String?) = action {
        graph.settingsRepository.setReminderSound(mode, customUri)
        graph.rebuildCoordinator.rebuild("Reminder sound changed")
        postMessage("Reminder sound preference saved.")
    }

    fun setSpokenAnnouncements(enabled: Boolean) = action {
        graph.settingsRepository.setSpokenAnnouncements(enabled)
    }

    fun setFridayReminders(enabled: Boolean) = action {
        graph.settingsRepository.setFridayReminders(enabled)
        graph.rebuildCoordinator.rebuild("Friday reminder setting")
    }

    fun testReminderSound() {
        val settings = _uiState.value.settings
        when (settings.reminderSoundMode) {
            ReminderSoundMode.CUSTOM_AUDIO -> {
                val played = audioPlayer.playCustomAudio(settings.customAdhanUri)
                if (!played) postMessage("Imported audio could not be played. Choose a file again.")
                else postMessage("Playing your imported Adhan audio.")
            }
            ReminderSoundMode.SILENT -> postMessage("Reminder sound is set to silent.")
            ReminderSoundMode.VIBRATE -> postMessage("Reminder sound is set to vibration only.")
            else -> {
                audioPlayer.playShortTone()
                postMessage("Played the system reminder tone.")
            }
        }
    }

    fun setPrayerSilence(enabled: Boolean, minutes: Int) = action {
        graph.settingsRepository.setSilenceSettings(enabled, minutes)
        if (enabled && !graph.silenceManager.hasPolicyAccess()) {
            postMessage("Do Not Disturb access is needed before automatic silence can work.")
        } else {
            postMessage("Automatic silence setting saved.")
        }
    }

    fun restoreSilenceNow() = action {
        val restored = graph.silenceManager.restoreNow()
        postMessage(if (restored) "Do Not Disturb state restored." else "There is no app-managed silence to restore.")
    }

    fun setSelectionThresholds(
        maxDistanceMeters: Double,
        accuracyMeters: Float,
        dwellSeconds: Long,
        marginMeters: Double,
        maxFixAgeSeconds: Long
    ) = action {
        graph.settingsRepository.setSelectionThresholds(
            maximumNearestDistanceMeters = maxDistanceMeters,
            accuracyMeters = accuracyMeters,
            dwellSeconds = dwellSeconds,
            marginMeters = marginMeters,
            maxFixAgeSeconds = maxFixAgeSeconds
        )
        postMessage("Location thresholds saved.")
    }

    fun rebuildReminders() = action {
        val outcome = graph.rebuildCoordinator.rebuild("Manual reminder repair")
        postMessage(outcome.message)
    }

    fun refreshPermissions() = action {
        // refreshFromStorage re-reads every system permission state.
    }

    // ------------------------------------------------------------------ backup

    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        graph.backupRepository.exportJson()
    }

    fun previewBackup(raw: String): BackupPreview = graph.backupRepository.preview(raw)

    fun importBackup(raw: String, replace: Boolean, onDone: (Result<BackupPreview>) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val result = withContext(Dispatchers.IO) {
                graph.backupRepository.import(raw, replace)
            }
            result.fold(
                onSuccess = { preview ->
                    graph.rebuildCoordinator.rebuild("Backup imported")
                    refreshFromStorage()
                    postMessage("Imported ${preview.totalRecords} record(s).")
                },
                onFailure = { postMessage(it.message ?: "The backup could not be imported.") }
            )
            _uiState.update { it.copy(busy = false) }
            onDone(result)
        }
    }

    fun completeOnboarding() = action {
        graph.settingsRepository.setOnboardingComplete()
        graph.rebuildCoordinator.rebuild("Onboarding complete")
        postMessage("Setup saved. You can change everything later in More.")
    }

    suspend fun exportMosqueJson(mosqueId: String): String? = withContext(Dispatchers.IO) {
        graph.mosqueRepository.exportMosqueJson(mosqueId)
    }

    fun importMosqueJson(raw: String) = action {
        val result = graph.mosqueRepository.importMosqueJson(raw)
        result.fold(
            onSuccess = {
                graph.rebuildCoordinator.rebuild("Mosque profile imported")
                postMessage("Mosque profile imported as a new profile.")
            },
            onFailure = { postMessage(it.message ?: "The mosque profile could not be imported.") }
        )
    }

    fun finishOnboarding(
        mosque: MosqueProfile?,
        place: SavedPlace?,
        scheduleEntries: List<ScheduleEntry>,
        mode: LocationMode,
        remindersEnabled: Boolean,
        automaticSwitching: Boolean,
        silenceEnabled: Boolean,
        silenceMinutes: Int,
        useCalculatedFallback: Boolean = false
    ) = action {
        if (mosque != null) {
            graph.mosqueRepository.upsertMosque(mosque)
            val hasTimes = scheduleEntries.any { !it.adhan.isNullOrBlank() || !it.iqamah.isNullOrBlank() }
            if (hasTimes) {
                val now = System.currentTimeMillis()
                val schedule = MosqueSchedule(
                    id = java.util.UUID.randomUUID().toString(),
                    mosqueId = mosque.id,
                    kind = com.example.domain.model.ScheduleKind.FIXED,
                    label = "Initial timetable",
                    createdAt = now,
                    updatedAt = now
                )
                graph.mosqueRepository.saveSchedule(
                    mosqueId = mosque.id,
                    schedule = schedule,
                    entries = scheduleEntries.map { it.copy(scheduleId = schedule.id) },
                    confirmIqamahBeforeAdhan = true
                )
            }
        }
        if (place != null) {
            graph.mosqueRepository.upsertPlace(place.copy(mosqueProfileId = mosque?.id ?: place.mosqueProfileId))
        }
        if (useCalculatedFallback) {
            graph.settingsRepository.setManualSelection(null, null)
        } else {
            when (mode) {
                LocationMode.MANUAL -> graph.settingsRepository.setManualSelection(mosque?.id, place?.id)
                LocationMode.AUTOMATIC -> graph.settingsRepository.setAutomaticLocationEnabled(true)
                LocationMode.TEMPORARY -> graph.settingsRepository.setTemporarySelection(
                    mosqueId = mosque?.id ?: return@action,
                    placeId = place?.id,
                    durationHours = 2
                )
            }
        }
        graph.settingsRepository.setRemindersEnabled(remindersEnabled)
        graph.settingsRepository.setSilenceSettings(silenceEnabled, silenceMinutes)
        if (automaticSwitching) {
            graph.settingsRepository.setAutomaticLocationEnabled(true)
        }
        graph.settingsRepository.setOnboardingComplete()
        graph.rebuildCoordinator.rebuild("Onboarding complete")
        postMessage("Setup saved. Everything can be changed later in More.")
    }

    private fun textOrNull(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }
}
