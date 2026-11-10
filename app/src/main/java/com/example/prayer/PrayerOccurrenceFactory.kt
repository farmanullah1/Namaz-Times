package com.example.prayer

import com.example.domain.model.AppSettings
import com.example.domain.model.CalculatedTimes
import com.example.domain.model.EffectiveTimetable
import com.example.domain.model.GeoMath
import com.example.domain.model.ManualOverride
import com.example.domain.model.MosqueProfile
import com.example.domain.model.MosqueSchedule
import com.example.domain.model.PrayerEventType
import com.example.domain.model.PrayerName
import com.example.domain.model.PrayerOccurrence
import com.example.domain.model.ResolvedPrayerTime
import com.example.domain.model.SavedPlace
import com.example.domain.model.ScheduleEntry
import com.example.domain.model.TimeSource
import com.example.domain.model.WallClock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Creates the explicit event list for one local date. Every event carries its source and the
 * schedule revision it came from. Missing mosque values are never replaced with invented times:
 * an Adhan may fall back to calculation, but a missing Iqamah stays missing.
 */
class PrayerOccurrenceFactory(
    private val calculator: AdhanPrayerCalculator,
    private val resolver: MosqueScheduleResolver
) {

    data class DayBuild(
        val date: LocalDate,
        val zone: ZoneId,
        val resolved: Map<PrayerName, ResolvedPrayerTime>,
        val sunrise: Instant?,
        val timetable: EffectiveTimetable?,
        val occurrences: List<PrayerOccurrence>,
        val usedCalculatedOnly: Boolean,
        val coordinateSource: String,
        val calculationLabel: String
    )

    fun buildDay(
        date: LocalDate,
        mosque: MosqueProfile?,
        place: SavedPlace?,
        schedules: List<MosqueSchedule>,
        entries: List<ScheduleEntry>,
        overrides: List<ManualOverride>,
        settings: AppSettings,
        travelMode: Boolean
    ): DayBuild {
        val zone = resolveZone(mosque, place)
        val latitude = firstValidLatitude(mosque, place, settings)
        val longitude = firstValidLongitude(mosque, place, settings)
        val coordinateSource = when {
            mosque?.latitude != null && mosque.longitude != null -> "Mosque coordinates"
            place?.latitude != null && place.longitude != null -> "Place coordinates"
            settings.lastLocation != null -> "Device location"
            else -> "No location"
        }

        val calculated = if (latitude != null && longitude != null) {
            calculator.calculateOrNull(latitude, longitude, date, settings.calculation)
        } else {
            null
        }

        val timetable = if (mosque != null && !travelMode) {
            resolver.resolve(date, mosque, schedules, entries, overrides)
        } else {
            null
        }

        val dateOverrides = overrides.filter { mosque != null && it.mosqueId == mosque.id && it.date == date }
        val resolved = LinkedHashMap<PrayerName, ResolvedPrayerTime>()
        val occurrences = mutableListOf<PrayerOccurrence>()
        val revision = mosque?.scheduleRevision ?: 0L

        PrayerName.obligatories.forEach { prayer ->
            val slot = timetable?.slots?.get(prayer)
            val override = dateOverrides.firstOrNull { it.prayer == prayer }
            val manualAdhan = WallClock.parse(override?.adhan)
            val manualIqamah = WallClock.parse(override?.iqamah)
            val slotAdhan = slot?.adhan
            val slotIqamah = slot?.iqamah

            val calculatedAdhan = calculated?.forPrayer(prayer)

            val adhanLocal = manualAdhan ?: slotAdhan
            val iqamahLocal = manualIqamah ?: slotIqamah

            val adhanSource: TimeSource? = when {
                manualAdhan != null -> TimeSource.MANUAL_OVERRIDE
                slotAdhan != null -> TimeSource.MOSQUE_TIMETABLE
                calculatedAdhan != null && travelMode -> TimeSource.TRAVEL_CALCULATED
                calculatedAdhan != null -> TimeSource.CALCULATED
                else -> null
            }
            val iqamahSource: TimeSource? = when {
                manualIqamah != null -> TimeSource.MANUAL_OVERRIDE
                slotIqamah != null -> TimeSource.MOSQUE_TIMETABLE
                else -> null
            }

            val adhanInstant = when {
                adhanLocal != null -> combine(date, adhanLocal, zone)
                calculatedAdhan != null -> calculatedAdhan
                else -> null
            }
            val iqamahInstant = iqamahLocal?.let { combine(date, it, zone) }
            val note = when {
                manualAdhan != null || manualIqamah != null -> "Manual override for ${date.format(DateTimeFormatter.ISO_DATE)}"
                else -> slot?.note.orEmpty()
            }

            resolved[prayer] = ResolvedPrayerTime(
                prayer = prayer,
                adhan = adhanInstant,
                iqamah = iqamahInstant,
                adhanSource = adhanSource,
                iqamahSource = iqamahSource,
                note = note
            )

            if (adhanInstant != null) {
                val allowed = slot?.adhanReminder ?: true
                if (allowed) {
                    occurrences += occurrence(
                        id = eventKey(mosque, revision, date, prayer, PrayerEventType.ADHAN, 0),
                        prayer = prayer,
                        type = PrayerEventType.ADHAN,
                        instant = adhanInstant,
                        date = date,
                        mosqueId = mosque?.id,
                        placeId = place?.id,
                        source = adhanSource ?: TimeSource.CALCULATED,
                        revision = revision
                    )
                }
            }
            if (iqamahInstant != null) {
                val allowed = slot?.iqamahReminder ?: true
                if (allowed) {
                    occurrences += occurrence(
                        id = eventKey(mosque, revision, date, prayer, PrayerEventType.IQAMAH, 0),
                        prayer = prayer,
                        type = PrayerEventType.IQAMAH,
                        instant = iqamahInstant,
                        date = date,
                        mosqueId = mosque?.id,
                        placeId = place?.id,
                        source = iqamahSource ?: TimeSource.MOSQUE_TIMETABLE,
                        revision = revision
                    )
                }
            }
        }

        if (date.dayOfWeek == DayOfWeek.FRIDAY && timetable != null) {
            val khutbah = WallClock.parse(timetable.khutbah)
            val jumuah = WallClock.parse(timetable.jumuah)
            val secondJumuah = WallClock.parse(timetable.secondJumuah)
            if (khutbah != null) {
                occurrences += occurrence(
                    id = eventKey(mosque, revision, date, PrayerName.DHUHR, PrayerEventType.KHUTBAH, 0),
                    prayer = PrayerName.DHUHR,
                    type = PrayerEventType.KHUTBAH,
                    instant = combine(date, khutbah, zone),
                    date = date,
                    mosqueId = mosque?.id,
                    placeId = place?.id,
                    source = TimeSource.MOSQUE_TIMETABLE,
                    revision = revision
                )
            }
            if (jumuah != null) {
                occurrences += occurrence(
                    id = eventKey(mosque, revision, date, PrayerName.DHUHR, PrayerEventType.JUMUAH, 0),
                    prayer = PrayerName.DHUHR,
                    type = PrayerEventType.JUMUAH,
                    instant = combine(date, jumuah, zone),
                    date = date,
                    mosqueId = mosque?.id,
                    placeId = place?.id,
                    source = TimeSource.MOSQUE_TIMETABLE,
                    revision = revision
                )
            }
            if (secondJumuah != null) {
                occurrences += occurrence(
                    id = eventKey(mosque, revision, date, PrayerName.DHUHR, PrayerEventType.JUMUAH, 1),
                    prayer = PrayerName.DHUHR,
                    type = PrayerEventType.JUMUAH,
                    instant = combine(date, secondJumuah, zone),
                    date = date,
                    mosqueId = mosque?.id,
                    placeId = place?.id,
                    source = TimeSource.MOSQUE_TIMETABLE,
                    revision = revision
                )
            }
        }

        val usedCalculatedOnly = timetable == null ||
            timetable.slots.values.none { it.adhan != null || it.iqamah != null }

        return DayBuild(
            date = date,
            zone = zone,
            resolved = resolved,
            sunrise = calculated?.sunrise,
            timetable = timetable,
            occurrences = occurrences.sortedBy { it.instant },
            usedCalculatedOnly = usedCalculatedOnly,
            coordinateSource = coordinateSource,
            calculationLabel = settings.calculation.preset.title
        )
    }

    private fun occurrence(
        id: String,
        prayer: PrayerName,
        type: PrayerEventType,
        instant: Instant,
        date: LocalDate,
        mosqueId: String?,
        placeId: String?,
        source: TimeSource,
        revision: Long,
        offsetMinutes: Int = 0
    ) = PrayerOccurrence(
        id = id,
        prayer = prayer,
        eventType = type,
        instant = instant,
        localDate = date,
        mosqueId = mosqueId,
        placeId = placeId,
        source = source,
        scheduleRevision = revision,
        offsetMinutes = offsetMinutes
    )

    fun eventKey(
        mosque: MosqueProfile?,
        revision: Long,
        date: LocalDate,
        prayer: PrayerName,
        type: PrayerEventType,
        offsetMinutes: Int
    ): String = "${mosque?.id ?: "calculated"}|$revision|$date|${prayer.name}|${type.name}|$offsetMinutes"

    fun combine(date: LocalDate, time: LocalTime, zone: ZoneId): Instant =
        date.atTime(time).atZone(zone).toInstant()

    fun resolveZone(mosque: MosqueProfile?, place: SavedPlace?): ZoneId {
        val id = mosque?.timezoneId ?: place?.timezoneId
        return runCatching { ZoneId.of(id ?: ZoneId.systemDefault().id) }.getOrDefault(ZoneId.systemDefault())
    }

    private fun firstValidLatitude(
        mosque: MosqueProfile?,
        place: SavedPlace?,
        settings: AppSettings
    ): Double? = sequenceOf(
        mosque?.latitude,
        place?.latitude,
        settings.lastLocation?.latitude
    ).firstOrNull { it != null && GeoMath.isValidLatitude(it) }

    private fun firstValidLongitude(
        mosque: MosqueProfile?,
        place: SavedPlace?,
        settings: AppSettings
    ): Double? = sequenceOf(
        mosque?.longitude,
        place?.longitude,
        settings.lastLocation?.longitude
    ).firstOrNull { it != null && GeoMath.isValidLongitude(it) }

    private fun CalculatedTimes.forPrayer(prayer: PrayerName): Instant = when (prayer) {
        PrayerName.FAJR -> fajr
        PrayerName.DHUHR -> dhuhr
        PrayerName.ASR -> asr
        PrayerName.MAGHRIB -> maghrib
        PrayerName.ISHA -> isha
    }
}
