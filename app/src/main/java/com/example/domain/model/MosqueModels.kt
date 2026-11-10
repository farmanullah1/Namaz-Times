package com.example.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class ScheduleKind(val title: String) {
    FIXED("Fixed daily"),
    WEEKLY("Recurring weekly"),
    DATE_RANGE("Seasonal / date range"),
    DATE_OVERRIDE("One-date override")
}

data class SavedPlace(
    val id: String,
    val displayName: String,
    val latitude: Double?,
    val longitude: Double?,
    val geofenceRadiusMeters: Int = 250,
    val timezoneId: String,
    val mosqueProfileId: String?,
    val priorityRank: Int = 100,
    val isEnabled: Boolean = true,
    val isPriorityLocation: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class MosqueProfile(
    val id: String,
    val name: String,
    val addressLabel: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezoneId: String,
    val isEnabled: Boolean = true,
    val isArchived: Boolean = false,
    val userPriority: Int = 100,
    val notes: String = "",
    val scheduleRevision: Long = 0L,
    val defaultReminderProfileId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class MosqueSchedule(
    val id: String,
    val mosqueId: String,
    val kind: ScheduleKind,
    val label: String = "",
    val weekday: Int? = null,
    val effectiveFrom: LocalDate? = null,
    val effectiveUntil: LocalDate? = null,
    val revision: Long = 0L,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class ScheduleEntry(
    val scheduleId: String,
    val prayer: PrayerName,
    val adhan: String? = null,
    val iqamah: String? = null,
    val adhanReminder: Boolean = true,
    val iqamahReminder: Boolean = true,
    val note: String = "",
    val khutbah: String? = null,
    val jumuah: String? = null,
    val secondJumuah: String? = null,
    val fridayNote: String = ""
)

data class ManualOverride(
    val mosqueId: String,
    val date: LocalDate,
    val prayer: PrayerName,
    val adhan: String? = null,
    val iqamah: String? = null
)

enum class ReminderSoundMode(val title: String) {
    SYSTEM("System notification sound"),
    SHORT_TONE("Short reminder tone"),
    SILENT("Silent notification"),
    VIBRATE("Vibration only"),
    CUSTOM_AUDIO("Imported Adhan audio")
}

data class ReminderPreference(
    val mosqueId: String,
    val prayer: PrayerName,
    val adhanEnabled: Boolean = true,
    val iqamahEnabled: Boolean = true,
    val preAdhanMinutes: Int? = null,
    val preIqamahMinutes: Int? = null,
    val soundMode: ReminderSoundMode = ReminderSoundMode.SYSTEM,
    val customSoundUri: String? = null
)

/** One resolved timetable for one local date, with the layer that supplied each field. */
data class EffectiveTimetable(
    val mosqueId: String,
    val revision: Long,
    val primaryKind: ScheduleKind?,
    val primaryLabel: String,
    val slots: Map<PrayerName, TimetableSlot>,
    val khutbah: String? = null,
    val jumuah: String? = null,
    val secondJumuah: String? = null,
    val fridayNote: String = ""
) {
    val hasAnyTime: Boolean = slots.values.any { it.adhan != null || it.iqamah != null }
}

data class TimetableSlot(
    val adhan: LocalTime? = null,
    val adhanSourceKind: ScheduleKind? = null,
    val iqamah: LocalTime? = null,
    val iqamahSourceKind: ScheduleKind? = null,
    val note: String = "",
    val adhanReminder: Boolean = true,
    val iqamahReminder: Boolean = true
)

/** Parsing/formatting helpers for user-entered 24-hour wall-clock times. */
object WallClock {
    private val pattern = Regex("^([01]?\\d|2[0-3]):([0-5]\\d)$")

    fun parse(value: String?): LocalTime? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val match = pattern.matchEntire(trimmed) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        return LocalTime.of(hour, minute)
    }

    fun format(value: LocalTime?): String? = value?.let { String.format("%02d:%02d", it.hour, it.minute) }

    fun isValid(value: String): Boolean = parse(value) != null
}

/** Distance helper used by place selection and the Qibla screen. */
object GeoMath {
    private const val EARTH_RADIUS_METERS = 6371000.0

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dPhi / 2) * kotlin.math.sin(dPhi / 2) +
            kotlin.math.cos(phi1) * kotlin.math.cos(phi2) *
            kotlin.math.sin(dLambda / 2) * kotlin.math.sin(dLambda / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun isValidLatitude(value: Double): Boolean = value in -90.0..90.0 && !value.isNaN()
    fun isValidLongitude(value: Double): Boolean = value in -180.0..180.0 && !value.isNaN()
}

data class LastLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long,
    val source: String
) {
    fun asInstant(): Instant = Instant.ofEpochMilli(timestampMillis)
}
