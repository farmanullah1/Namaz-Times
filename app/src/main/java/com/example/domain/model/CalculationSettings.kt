package com.example.domain.model

/** Presets are calculation settings only — never a claim about any mosque's timetable. */
enum class CalculationPreset(
    val title: String,
    val defaultFajrAngle: Double,
    val defaultIshaAngle: Double,
    val defaultIshaIntervalMinutes: Int? = null
) {
    KARACHI("Karachi (Univ. of Islamic Sciences)", 18.0, 18.0),
    MUSLIM_WORLD_LEAGUE("Muslim World League", 18.0, 17.0),
    EGYPTIAN("Egyptian General Authority", 19.5, 17.5),
    UMM_AL_QURA("Umm al-Qura, Makkah", 18.5, 18.0, 90),
    MOONSIGHTING_COMMITTEE("Moonsighting Committee", 18.0, 18.0),
    CUSTOM("Other / manual angles", 18.0, 18.0)
}

/** Standard Asr uses one shadow-length factor; Hanafi uses two. */
enum class AsrShadowFactor(val title: String, val description: String) {
    STANDARD("Standard Asr (1 shadow)", "Shadow equals the object's length. Common to Shafi'i, Maliki, Hanbali and Ahl al-Hadith practice."),
    HANAFI("Hanafi Asr (2 shadows)", "Shadow equals twice the object's length.")
}

enum class HighLatitudeRuleOption(val title: String) {
    MIDDLE_OF_THE_NIGHT("Middle of the night"),
    SEVENTH_OF_THE_NIGHT("Seventh of the night"),
    TWILIGHT_ANGLE("Twilight angle")
}

data class PerPrayerAdjustments(
    val fajr: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
) {
    fun forPrayer(prayer: PrayerName): Int = when (prayer) {
        PrayerName.FAJR -> fajr
        PrayerName.DHUHR -> dhuhr
        PrayerName.ASR -> asr
        PrayerName.MAGHRIB -> maghrib
        PrayerName.ISHA -> isha
    }
}

/**
 * Calculation configuration. Suggested fallback for a Pakistan-based Ahl al-Hadith user:
 * Karachi preset, Standard Asr, Middle of the Night, zero minute adjustments.
 * Never label a preset as a religious ruling; the user must verify with their mosque/scholar.
 */
data class CalculationSettings(
    val preset: CalculationPreset = CalculationPreset.KARACHI,
    val fajrAngle: Double = 18.0,
    val ishaAngle: Double = 18.0,
    val ishaIntervalMinutes: Int? = null,
    val asr: AsrShadowFactor = AsrShadowFactor.STANDARD,
    val highLatitude: HighLatitudeRuleOption = HighLatitudeRuleOption.MIDDLE_OF_THE_NIGHT,
    val adjustments: PerPrayerAdjustments = PerPrayerAdjustments(),
    val roundingToNearestMinute: Boolean = true
) {
    companion object {
        /** Applies a preset's documented angles/interval while preserving Asr and adjustments. */
        fun applyPreset(current: CalculationSettings, preset: CalculationPreset): CalculationSettings =
            current.copy(
                preset = preset,
                fajrAngle = preset.defaultFajrAngle,
                ishaAngle = preset.defaultIshaAngle,
                ishaIntervalMinutes = preset.defaultIshaIntervalMinutes
            )

        fun described(): String =
            "Calculated prayer times are estimates based on the selected astronomical method and settings. " +
                "Your selected mosque's published timetable may differ. Follow the mosque or trusted scholar you normally follow."
    }
}
