package com.example.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class LastSelectionDecision(
    val timestampMillis: Long,
    val accuracyMeters: Float,
    val mode: LocationMode,
    val selectedMosqueId: String?,
    val selectedPlaceId: String?,
    val reason: String,
    val confidence: String
)

data class AppSettings(
    val onboardingComplete: Boolean = false,
    val themeMode: String = "system",
    val calculation: CalculationSettings = CalculationSettings(),
    val hijriAdjustmentDays: Int = 0,
    val hijriSunsetBoundary: Boolean = false,
    val hijriSource: String = HijriSettings.SOURCE_UMM_AL_QURA,

    val locationMode: LocationMode = LocationMode.MANUAL,
    val manualMosqueId: String? = null,
    val manualPlaceId: String? = null,
    val temporaryMosqueId: String? = null,
    val temporaryExpiresAtMillis: Long? = null,
    val temporaryPreviousMode: LocationMode = LocationMode.MANUAL,
    val automaticLocationEnabled: Boolean = false,
    val lastAutoMosqueId: String? = null,
    val lastAutoPlaceId: String? = null,
    val lastSelectionReason: String = "",
    val maximumNearestDistanceMeters: Double = 20_000.0,
    val accuracyThresholdMeters: Float = 100f,
    val maxFixAgeSeconds: Long = 120L,
    val dwellSeconds: Long = 120L,
    val boundaryMarginMeters: Double = 50.0,
    val lastLocation: LastLocation? = null,
    val lastDecision: LastSelectionDecision? = null,

    val remindersEnabled: Boolean = false,
    val reminderSoundMode: ReminderSoundMode = ReminderSoundMode.SYSTEM,
    val customAdhanUri: String? = null,
    val spokenAnnouncementEnabled: Boolean = false,
    val fridayRemindersEnabled: Boolean = true,
    val lastSuccessfulRebuildMillis: Long? = null,
    val lastRebuildReason: String = "",
    val lastRebuildSucceeded: Boolean = true,
    val remindersNeedRepair: Boolean = false,

    val prayerSilenceEnabled: Boolean = false,
    val prayerSilenceMinutes: Int = 10,
    val previousDndFilter: Int? = null,
    val dndSilencedAtMillis: Long? = null,
    val dndRestoreAtMillis: Long? = null,

    val quran: QuranSettings = QuranSettings(),
    val qadaEnabled: Boolean = false
) {
    val temporaryActiveAt: (Instant) -> Boolean = { now ->
        temporaryMosqueId != null && temporaryExpiresAtMillis != null && temporaryExpiresAtMillis > now.toEpochMilli()
    }
}

fun LocalTime.plusMinutesSafe(minutes: Int): LocalTime = this.plusMinutes(minutes.toLong())

fun LocalDate.weekdayIso(): Int = dayOfWeek.value

fun DayOfWeek.isoNumber(): Int = value

fun Instant.localDateIn(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()
