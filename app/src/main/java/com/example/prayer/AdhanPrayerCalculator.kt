@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.prayer

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.data.DateComponents
import com.batoulapps.adhan2.HighLatitudeRule
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.model.Rounding
import com.example.domain.model.AsrShadowFactor
import com.example.domain.model.CalculatedTimes
import com.example.domain.model.CalculationPreset
import com.example.domain.model.CalculationSettings
import com.example.domain.model.HighLatitudeRuleOption
import java.time.Instant
import java.time.LocalDate

/**
 * Prayer time astronomy comes from com.batoulapps.adhan:adhan2, a maintained implementation of
 * the Adhan solar-position algorithms. The library returns UTC instants; callers convert them to
 * the profile's ZoneId for display. No hardcoded time offsets are applied.
 */
class AdhanPrayerCalculator {

    fun calculate(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        settings: CalculationSettings
    ): CalculatedTimes {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90." }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180." }
        require(settings.fajrAngle in 0.0..30.0) { "Fajr angle must be between 0 and 30 degrees." }
        require(settings.ishaAngle in 0.0..30.0) { "Isha angle must be between 0 and 30 degrees." }

        val parameters = settings.preset.toCalculationMethod().parameters.copy(
            fajrAngle = settings.fajrAngle,
            ishaAngle = settings.ishaAngle,
            ishaInterval = settings.ishaIntervalMinutes ?: 0,
            madhab = if (settings.asr == AsrShadowFactor.STANDARD) Madhab.SHAFI else Madhab.HANAFI,
            highLatitudeRule = settings.highLatitude.toLibraryRule(),
            prayerAdjustments = PrayerAdjustments(
                fajr = settings.adjustments.fajr,
                dhuhr = settings.adjustments.dhuhr,
                asr = settings.adjustments.asr,
                maghrib = settings.adjustments.maghrib,
                isha = settings.adjustments.isha
            ),
            rounding = Rounding.NEAREST
        )

        val times = PrayerTimes(
            Coordinates(latitude, longitude),
            DateComponents(date.year, date.monthValue, date.dayOfMonth),
            parameters
        )

        return CalculatedTimes(
            fajr = times.fajr.toJavaInstant(),
            sunrise = times.sunrise.toJavaInstant(),
            dhuhr = times.dhuhr.toJavaInstant(),
            asr = times.asr.toJavaInstant(),
            maghrib = times.maghrib.toJavaInstant(),
            isha = times.isha.toJavaInstant()
        )
    }

    /** Safe wrapper used by UI: returns a failure reason instead of fabricating times. */
    fun calculateOrNull(
        latitude: Double?,
        longitude: Double?,
        date: LocalDate,
        settings: CalculationSettings
    ): CalculatedTimes? {
        if (latitude == null || longitude == null) return null
        return try {
            calculate(latitude, longitude, date, settings)
        } catch (error: IllegalArgumentException) {
            null
        }
    }

    private fun CalculationPreset.toCalculationMethod(): CalculationMethod = when (this) {
        CalculationPreset.KARACHI -> CalculationMethod.KARACHI
        CalculationPreset.MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        CalculationPreset.EGYPTIAN -> CalculationMethod.EGYPTIAN
        CalculationPreset.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA
        CalculationPreset.MOONSIGHTING_COMMITTEE -> CalculationMethod.MOON_SIGHTING_COMMITTEE
        CalculationPreset.CUSTOM -> CalculationMethod.OTHER
    }

    private fun HighLatitudeRuleOption.toLibraryRule(): HighLatitudeRule = when (this) {
        HighLatitudeRuleOption.MIDDLE_OF_THE_NIGHT -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        HighLatitudeRuleOption.SEVENTH_OF_THE_NIGHT -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
        HighLatitudeRuleOption.TWILIGHT_ANGLE -> HighLatitudeRule.TWILIGHT_ANGLE
    }

    private fun kotlin.time.Instant.toJavaInstant(): Instant =
        Instant.ofEpochMilli(toEpochMilliseconds())
}
