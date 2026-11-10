package com.example.prayer

import com.example.data.repository.MosqueSnapshot
import com.example.domain.model.AppSettings
import com.example.domain.model.EffectiveTimetable
import com.example.domain.model.LocationMode
import com.example.domain.model.MosqueProfile
import com.example.domain.model.NextPrayerInfo
import com.example.domain.model.PrayerEventType
import com.example.domain.model.PrayerName
import com.example.domain.model.PrayerOccurrence
import com.example.domain.model.ResolvedPrayerTime
import com.example.domain.model.SavedPlace
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TodayContext(
    val today: LocalDate,
    val zone: ZoneId,
    val mosque: MosqueProfile?,
    val place: SavedPlace?,
    val resolved: Map<PrayerName, ResolvedPrayerTime>,
    val sunrise: Instant?,
    val timetable: EffectiveTimetable?,
    val next: NextPrayerInfo?,
    val currentPrayer: PrayerName?,
    val nextResolved: ResolvedPrayerTime?,
    val usedCalculatedOnly: Boolean,
    val travelMode: Boolean,
    val coordinateSource: String,
    val calculationLabel: String,
    val occurrences: List<PrayerOccurrence>,
    val scheduleRevision: Long
)

/**
 * Next prayer is selected from real instants over the previous/current/next local dates, never by
 * comparing "HH:mm" strings. At 00:30 the upcoming Fajr wins; yesterday's Isha is never re-labelled.
 */
class PrayerScheduleService(private val factory: PrayerOccurrenceFactory) {

    fun buildToday(
        settings: AppSettings,
        snapshot: MosqueSnapshot,
        now: Instant
    ): TodayContext {
        val zone = factory.resolveZone(
            snapshot.mosque(settings.manualMosqueId),
            snapshot.place(settings.manualPlaceId)
        )
        val today = now.atZone(zone).toLocalDate()
        val temporaryActive = settings.temporaryMosqueId != null &&
            (settings.temporaryExpiresAtMillis ?: 0L) > now.toEpochMilli()

        val mosqueId = when {
            temporaryActive -> settings.temporaryMosqueId
            settings.locationMode == LocationMode.MANUAL -> settings.manualMosqueId
            settings.locationMode == LocationMode.TEMPORARY -> settings.temporaryMosqueId ?: settings.manualMosqueId
            else -> settings.lastAutoMosqueId ?: settings.manualMosqueId
        }
        val placeId = when (settings.locationMode) {
            LocationMode.MANUAL -> settings.manualPlaceId
            LocationMode.AUTOMATIC -> settings.lastAutoPlaceId ?: settings.manualPlaceId
            LocationMode.TEMPORARY -> settings.lastAutoPlaceId ?: settings.manualPlaceId
        }
        val mosque = snapshot.mosque(mosqueId)
        val place = snapshot.place(placeId) ?: snapshot.places.firstOrNull { it.mosqueProfileId == mosqueId }
        val travelMode = mosque == null

        val builds = listOf(today.minusDays(1), today, today.plusDays(1)).map { date ->
            factory.buildDay(
                date = date,
                mosque = mosque,
                place = place,
                schedules = snapshot.schedules,
                entries = snapshot.entries,
                overrides = snapshot.overrides,
                settings = settings,
                travelMode = travelMode
            )
        }

        val todayBuild = builds.first { it.date == today }
        val allAdhan = builds.flatMap { build ->
            build.occurrences.filter { it.eventType == PrayerEventType.ADHAN }
        }.sortedBy { it.instant }

        val next = allAdhan.firstOrNull { it.instant.isAfter(now) }
        val current = allAdhan.lastOrNull { !it.instant.isAfter(now) }

        val nextBuild = next?.let { occurrence -> builds.firstOrNull { it.date == occurrence.localDate } }
        val nextResolved = next?.let { nextBuild?.resolved?.get(it.prayer) }

        return TodayContext(
            today = today,
            zone = todayBuild.zone,
            mosque = mosque,
            place = place,
            resolved = todayBuild.resolved,
            sunrise = todayBuild.sunrise,
            timetable = todayBuild.timetable,
            next = next?.let {
                NextPrayerInfo(
                    occurrence = it,
                    prayer = it.prayer,
                    instant = it.instant,
                    source = it.source,
                    mosqueName = mosque?.name,
                    iqamah = nextResolved?.iqamah,
                    isTomorrow = it.localDate != today
                )
            },
            currentPrayer = current?.prayer,
            nextResolved = nextResolved,
            usedCalculatedOnly = todayBuild.usedCalculatedOnly,
            travelMode = travelMode,
            coordinateSource = todayBuild.coordinateSource,
            calculationLabel = todayBuild.calculationLabel,
            occurrences = todayBuild.occurrences,
            scheduleRevision = mosque?.scheduleRevision ?: 0L
        )
    }

    /** Confirms a mosque is still eligible for reminders. */
    fun isEligible(mosque: MosqueProfile?): Boolean =
        mosque != null && mosque.isEnabled && !mosque.isArchived
}
