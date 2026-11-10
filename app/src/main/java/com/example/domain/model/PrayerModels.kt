package com.example.domain.model

import java.time.Instant
import java.time.LocalDate

/** The five obligatory prayers. Sunrise is always an informational calculated event. */
enum class PrayerName(val title: String, val arabicTitle: String) {
    FAJR("Fajr", "الفجر"),
    DHUHR("Dhuhr", "الظهر"),
    ASR("Asr", "العصر"),
    MAGHRIB("Maghrib", "المغرب"),
    ISHA("Isha", "العشاء");

    companion object {
        val obligatories: List<PrayerName> = entries.toList()
        fun fromKey(key: String?): PrayerName? = entries.firstOrNull { it.name == key }
    }
}

enum class PrayerEventType {
    ADHAN,
    IQAMAH,
    PRE_ADHAN,
    PRE_IQAMAH,
    KHUTBAH,
    JUMUAH
}

/** Where a displayed time came from. Every UI surface must label it. */
enum class TimeSource(val label: String) {
    MOSQUE_TIMETABLE("Mosque timetable"),
    MANUAL_OVERRIDE("Manual override"),
    CALCULATED("Calculated"),
    TRAVEL_CALCULATED("Calculated — Travel mode")
}

data class CalculatedTimes(
    val fajr: Instant,
    val sunrise: Instant,
    val dhuhr: Instant,
    val asr: Instant,
    val maghrib: Instant,
    val isha: Instant
)

/** A single resolved reminder/display occurrence, always bound to an explicit source. */
data class PrayerOccurrence(
    val id: String,
    val prayer: PrayerName,
    val eventType: PrayerEventType,
    val instant: Instant,
    val localDate: LocalDate,
    val mosqueId: String?,
    val placeId: String?,
    val source: TimeSource,
    val scheduleRevision: Long,
    val isEnabled: Boolean = true,
    val offsetMinutes: Int = 0
)

data class ResolvedPrayerTime(
    val prayer: PrayerName,
    val adhan: Instant?,
    val iqamah: Instant?,
    val adhanSource: TimeSource?,
    val iqamahSource: TimeSource?,
    val note: String = ""
)

data class NextPrayerInfo(
    val occurrence: PrayerOccurrence,
    val prayer: PrayerName,
    val instant: Instant,
    val source: TimeSource,
    val mosqueName: String?,
    val iqamah: Instant?,
    val isTomorrow: Boolean
)

enum class LocationMode(val title: String) {
    AUTOMATIC("Automatic"),
    MANUAL("Manual"),
    TEMPORARY("Temporary")
}
