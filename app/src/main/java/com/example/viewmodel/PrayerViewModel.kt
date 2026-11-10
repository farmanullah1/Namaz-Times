package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.calculator.PrayerCalculator
import com.example.data.db.AppDatabase
import com.example.data.db.MosqueLocation
import com.example.data.hijri.HijriCalendarHelper
import com.example.data.hijri.HijriDate
import com.example.data.location.LocationHelper
import com.example.data.model.CalculationMethod
import com.example.data.model.DayPrayerSchedule
import com.example.data.model.FiqhSchool
import com.example.data.model.PrayerItem
import com.example.data.model.PrayerName
import com.example.data.qibla.QiblaHelper
import com.example.data.repository.MosqueRepository
import com.example.service.PrayerNotificationHelper
import com.example.ui.qibla.CompassSensorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class PrayerUiState(
    val activeMosque: MosqueLocation? = null,
    val schedule: DayPrayerSchedule? = null,
    val hijriDate: HijriDate = HijriCalendarHelper.getHijriDate(),
    val hijriAdjustmentDays: Int = 0,
    val fiqhSchool: FiqhSchool = FiqhSchool.AHL_AL_HADITH,
    val calculationMethod: CalculationMethod = CalculationMethod.KARACHI,
    val allMosques: List<MosqueLocation> = emptyList(),
    val autoLocationEnabled: Boolean = true,
    val userCoordinates: Pair<Double, Double>? = null,
    val compassHeading: Float = 0f,
    val hasCompassSensor: Boolean = true,
    val isQiblaAligned: Boolean = false,
    val qiblaBearing: Double = 0.0,
    val distanceToMakkahKm: Double = 0.0,
    val nextPrayerCountdownFormatted: String = "00:00:00",
    val toastMessage: String? = null
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MosqueRepository
    val compassSensorManager: CompassSensorManager

    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = MosqueRepository(database.mosqueDao())
        compassSensorManager = CompassSensorManager(application)

        PrayerNotificationHelper.initNotificationChannel(application)

        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultMosquesExist()
        }

        // Observe mosques from repository
        viewModelScope.launch {
            repository.allMosques.collect { mosquesList ->
                val currentActive = mosquesList.find { it.isActive } ?: mosquesList.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    allMosques = mosquesList,
                    activeMosque = currentActive
                )
                refreshPrayerSchedule()
            }
        }

        // Start compass heading listener
        viewModelScope.launch {
            compassSensorManager.azimuthHeading.collect { heading ->
                val bearing = _uiState.value.qiblaBearing
                val diff = abs(normalizeAngle(heading - bearing.toFloat()))
                val isAligned = diff <= 3.5f

                _uiState.value = _uiState.value.copy(
                    compassHeading = heading,
                    isQiblaAligned = isAligned
                )
            }
        }

        viewModelScope.launch {
            compassSensorManager.hasSensor.collect { hasSensor ->
                _uiState.value = _uiState.value.copy(hasCompassSensor = hasSensor)
            }
        }

        // Try detecting initial location
        tryDetectLocation()

        // Continuous 1-second ticker for countdown and prayer progress
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                updateCountdownAndProgress()
            }
        }
    }

    fun startCompass() {
        compassSensorManager.startListening()
    }

    fun stopCompass() {
        compassSensorManager.stopListening()
    }

    fun setManualCompassHeading(heading: Float) {
        compassSensorManager.setManualHeading(heading)
    }

    fun tryDetectLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            val coords = LocationHelper.getLastKnownCoordinates(getApplication())
            coords?.let { (lat, lng) ->
                updateUserCoordinates(lat, lng)
            }
        }
    }

    fun updateUserCoordinates(lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val mosques = _uiState.value.allMosques
            var newActive = _uiState.value.activeMosque

            if (_uiState.value.autoLocationEnabled && mosques.isNotEmpty()) {
                val closest = LocationHelper.findClosestMosque(lat, lng, mosques)
                if (closest != null) {
                    val (mosque, distKm) = closest
                    // If within mosque radius or closest found
                    if (distKm * 1000.0 <= mosque.radiusMeters || distKm < 15.0) {
                        if (newActive?.id != mosque.id) {
                            repository.setActiveMosque(mosque.id)
                            newActive = mosque
                        }
                    }
                }
            }

            _uiState.value = _uiState.value.copy(
                userCoordinates = Pair(lat, lng),
                activeMosque = newActive
            )
            refreshPrayerSchedule()
        }
    }

    fun selectActiveMosque(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setActiveMosque(id)
        }
    }

    fun saveMosque(mosque: MosqueLocation) {
        viewModelScope.launch(Dispatchers.IO) {
            if (mosque.id == 0L) {
                val newId = repository.insert(mosque)
                if (mosque.isActive) {
                    repository.setActiveMosque(newId)
                }
            } else {
                repository.update(mosque)
            }
        }
    }

    fun deleteMosque(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(id)
        }
    }

    fun setFiqhSchool(fiqh: FiqhSchool) {
        _uiState.value = _uiState.value.copy(fiqhSchool = fiqh)
        refreshPrayerSchedule()
    }

    fun setCalculationMethod(method: CalculationMethod) {
        _uiState.value = _uiState.value.copy(calculationMethod = method)
        refreshPrayerSchedule()
    }

    fun setHijriAdjustment(adjustmentDays: Int) {
        val newHijri = HijriCalendarHelper.getHijriDate(adjustmentDays = adjustmentDays)
        _uiState.value = _uiState.value.copy(
            hijriAdjustmentDays = adjustmentDays,
            hijriDate = newHijri
        )
        refreshPrayerSchedule()
    }

    fun toggleAutoLocation(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoLocationEnabled = enabled)
        if (enabled) {
            tryDetectLocation()
        }
    }

    fun triggerTestNotification(prayer: PrayerItem) {
        val mosque = _uiState.value.activeMosque ?: return
        PrayerNotificationHelper.showPrayerAlert(
            context = getApplication(),
            prayerName = prayer.name.title,
            mosqueName = mosque.name,
            azanTime = prayer.azanTime,
            iqamahTime = prayer.iqamahTime
        )
    }

    fun clearToastMessage() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    private fun refreshPrayerSchedule() {
        val mosque = _uiState.value.activeMosque ?: _uiState.value.allMosques.firstOrNull() ?: return
        val now = Calendar.getInstance()
        val nowMillis = now.timeInMillis

        val lat = mosque.latitude
        val lng = mosque.longitude

        val qiblaBearing = QiblaHelper.calculateQiblaBearing(lat, lng)
        val distanceToMakkah = QiblaHelper.calculateDistanceToMakkahKm(lat, lng)

        val calTimes = PrayerCalculator.calculate(
            calendar = now,
            latitude = lat,
            longitude = lng,
            fiqh = _uiState.value.fiqhSchool,
            method = _uiState.value.calculationMethod
        )

        // Build individual prayer items
        // If mosque is custom, use entered custom times, otherwise use calculated with offsets
        val fajrAzan = if (mosque.isCustomTimes) mosque.fajrAzan else PrayerCalculator.formatHour(calTimes.fajr)
        val fajrIqamah = if (mosque.isCustomTimes) mosque.fajrIqamah else PrayerCalculator.addMinutesToTime(fajrAzan, mosque.fajrIqamahOffsetMin)

        val sunriseTime = if (mosque.isCustomTimes && mosque.sunriseTime.isNotBlank()) mosque.sunriseTime else PrayerCalculator.formatHour(calTimes.sunrise)

        val dhuhrAzan = if (mosque.isCustomTimes) mosque.dhuhrAzan else PrayerCalculator.formatHour(calTimes.dhuhr)
        val dhuhrIqamah = if (mosque.isCustomTimes) mosque.dhuhrIqamah else PrayerCalculator.addMinutesToTime(dhuhrAzan, mosque.dhuhrIqamahOffsetMin)

        val asrAzan = if (mosque.isCustomTimes) mosque.asrAzan else PrayerCalculator.formatHour(calTimes.asr)
        val asrIqamah = if (mosque.isCustomTimes) mosque.asrIqamah else PrayerCalculator.addMinutesToTime(asrAzan, mosque.asrIqamahOffsetMin)

        val maghribAzan = if (mosque.isCustomTimes) mosque.maghribAzan else PrayerCalculator.formatHour(calTimes.maghrib)
        val maghribIqamah = if (mosque.isCustomTimes) mosque.maghribIqamah else PrayerCalculator.addMinutesToTime(maghribAzan, mosque.maghribIqamahOffsetMin)

        val ishaAzan = if (mosque.isCustomTimes) mosque.ishaAzan else PrayerCalculator.formatHour(calTimes.isha)
        val ishaIqamah = if (mosque.isCustomTimes) mosque.ishaIqamah else PrayerCalculator.addMinutesToTime(ishaAzan, mosque.ishaIqamahOffsetMin)

        val tahajjudTime = PrayerCalculator.formatHour(calTimes.tahajjudEnd)

        val rawPrayers = listOf(
            PrayerItem(
                name = PrayerName.FAJR,
                azanTime = fajrAzan,
                iqamahTime = fajrIqamah,
                azanEpochMillis = PrayerCalculator.timeStringToEpochMillis(fajrAzan, now),
                iqamahEpochMillis = PrayerCalculator.timeStringToEpochMillis(fajrIqamah, now)
            ),
            PrayerItem(
                name = PrayerName.SUNRISE,
                azanTime = sunriseTime,
                iqamahTime = "-",
                azanEpochMillis = PrayerCalculator.timeStringToEpochMillis(sunriseTime, now),
                iqamahEpochMillis = PrayerCalculator.timeStringToEpochMillis(sunriseTime, now)
            ),
            PrayerItem(
                name = PrayerName.DHUHR,
                azanTime = dhuhrAzan,
                iqamahTime = dhuhrIqamah,
                azanEpochMillis = PrayerCalculator.timeStringToEpochMillis(dhuhrAzan, now),
                iqamahEpochMillis = PrayerCalculator.timeStringToEpochMillis(dhuhrIqamah, now)
            ),
            PrayerItem(
                name = PrayerName.ASR,
                azanTime = asrAzan,
                iqamahTime = asrIqamah,
                azanEpochMillis = PrayerCalculator.timeStringToEpochMillis(asrAzan, now),
                iqamahEpochMillis = PrayerCalculator.timeStringToEpochMillis(asrIqamah, now)
            ),
            PrayerItem(
                name = PrayerName.MAGHRIB,
                azanTime = maghribAzan,
                iqamahTime = maghribIqamah,
                azanEpochMillis = PrayerCalculator.timeStringToEpochMillis(maghribAzan, now),
                iqamahEpochMillis = PrayerCalculator.timeStringToEpochMillis(maghribIqamah, now)
            ),
            PrayerItem(
                name = PrayerName.ISHA,
                azanTime = ishaAzan,
                iqamahTime = ishaIqamah,
                azanEpochMillis = PrayerCalculator.timeStringToEpochMillis(ishaAzan, now),
                iqamahEpochMillis = PrayerCalculator.timeStringToEpochMillis(ishaIqamah, now)
            ),
            PrayerItem(
                name = PrayerName.TAHAJJUD,
                azanTime = tahajjudTime,
                iqamahTime = "-",
                azanEpochMillis = PrayerCalculator.timeStringToEpochMillis(tahajjudTime, now),
                iqamahEpochMillis = PrayerCalculator.timeStringToEpochMillis(tahajjudTime, now)
            )
        )

        // Find current and next prayer among salat
        val salatPrayers = rawPrayers.filter { it.name.isSalat }
        var current: PrayerItem? = null
        var next: PrayerItem? = null

        for (i in salatPrayers.indices) {
            val p = salatPrayers[i]
            if (nowMillis < p.azanEpochMillis) {
                next = p
                current = if (i > 0) salatPrayers[i - 1] else salatPrayers.last()
                break
            }
        }

        // If after Isha, next is tomorrow's Fajr
        if (next == null) {
            val tomorrowFajrCal = now.clone() as Calendar
            tomorrowFajrCal.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowFajrMillis = PrayerCalculator.timeStringToEpochMillis(fajrAzan, tomorrowFajrCal)
            val tomorrowFajrIqamahMillis = PrayerCalculator.timeStringToEpochMillis(fajrIqamah, tomorrowFajrCal)

            next = PrayerItem(
                name = PrayerName.FAJR,
                azanTime = fajrAzan,
                iqamahTime = fajrIqamah,
                azanEpochMillis = tomorrowFajrMillis,
                iqamahEpochMillis = tomorrowFajrIqamahMillis
            )
            current = salatPrayers.last() // Isha
        }

        val enrichedPrayers = rawPrayers.map { p ->
            p.copy(
                isNext = p.name == next?.name,
                isCurrent = p.name == current?.name
            )
        }

        val gregorianFmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(now.time)
        val hijri = HijriCalendarHelper.getHijriDate(now, _uiState.value.hijriAdjustmentDays)
        val hijriFmtEn = "${hijri.day} ${hijri.monthNameEn} ${hijri.year} AH"
        val hijriFmtAr = "${HijriCalendarHelper.getArabicDigits(hijri.day)} ${hijri.monthNameAr} ${HijriCalendarHelper.getArabicDigits(hijri.year)} هـ"

        val schedule = DayPrayerSchedule(
            mosque = mosque,
            prayers = enrichedPrayers,
            currentPrayer = current,
            nextPrayer = next,
            timeRemainingMillis = (next.azanEpochMillis - nowMillis).coerceAtLeast(0L),
            progress = calculateProgress(current, next, nowMillis),
            hijriDate = hijriFmtEn,
            hijriDateArabic = hijriFmtAr,
            gregorianDate = gregorianFmt,
            qiblaBearing = qiblaBearing,
            distanceToMakkahKm = distanceToMakkah
        )

        _uiState.value = _uiState.value.copy(
            schedule = schedule,
            hijriDate = hijri,
            qiblaBearing = qiblaBearing,
            distanceToMakkahKm = distanceToMakkah
        )
    }

    private fun updateCountdownAndProgress() {
        val schedule = _uiState.value.schedule ?: return
        val next = schedule.nextPrayer ?: return
        val current = schedule.currentPrayer
        val nowMillis = System.currentTimeMillis()

        val diff = (next.azanEpochMillis - nowMillis).coerceAtLeast(0L)
        if (diff == 0L) {
            // Prayer time arrived, trigger alert and refresh
            triggerTestNotification(next)
            refreshPrayerSchedule()
            return
        }

        val hours = (diff / (1000 * 60 * 60)) % 24
        val minutes = (diff / (1000 * 60)) % 60
        val seconds = (diff / 1000) % 60
        val formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        val progress = calculateProgress(current, next, nowMillis)

        _uiState.value = _uiState.value.copy(
            nextPrayerCountdownFormatted = formatted,
            schedule = schedule.copy(
                timeRemainingMillis = diff,
                progress = progress
            )
        )
    }

    private fun calculateProgress(current: PrayerItem?, next: PrayerItem?, nowMillis: Long): Float {
        if (current == null || next == null) return 0f
        var start = current.azanEpochMillis
        val end = next.azanEpochMillis
        if (start >= end) {
            start -= 24 * 60 * 60 * 1000L
        }
        val total = (end - start).coerceAtLeast(1L)
        val elapsed = (nowMillis - start).coerceIn(0L, total)
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }
}
