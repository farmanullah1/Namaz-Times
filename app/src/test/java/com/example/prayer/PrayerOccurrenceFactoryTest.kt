package com.example.prayer

import com.example.domain.model.AppSettings
import com.example.domain.model.MosqueProfile
import com.example.domain.model.MosqueSchedule
import com.example.domain.model.PrayerEventType
import com.example.domain.model.PrayerName
import com.example.domain.model.SavedPlace
import com.example.domain.model.ScheduleEntry
import com.example.domain.model.ScheduleKind
import com.example.domain.model.TimeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PrayerOccurrenceFactoryTest {

    private val factory = PrayerOccurrenceFactory(AdhanPrayerCalculator(), MosqueScheduleResolver())
    private val zone = ZoneId.of("Asia/Karachi")
    private val date = LocalDate.of(2026, 11, 10)
    private val mosque = MosqueProfile(
        id = "mosque-1",
        name = "Test Mosque",
        latitude = 24.8607,
        longitude = 67.0011,
        timezoneId = zone.id
    )
    private val place = SavedPlace(
        id = "place-1",
        displayName = "Home",
        latitude = 24.8607,
        longitude = 67.0011,
        timezoneId = zone.id,
        mosqueProfileId = mosque.id
    )
    private val schedule = MosqueSchedule(
        id = "schedule-1",
        mosqueId = mosque.id,
        kind = ScheduleKind.FIXED,
        revision = 1L
    )

    private fun entries() = listOf(
        ScheduleEntry("schedule-1", PrayerName.FAJR, "05:30", "05:45"),
        ScheduleEntry("schedule-1", PrayerName.DHUHR, "13:00", null),
        ScheduleEntry("schedule-1", PrayerName.ASR, "16:15", "16:30"),
        ScheduleEntry("schedule-1", PrayerName.MAGHRIB, "17:55", "18:05"),
        ScheduleEntry("schedule-1", PrayerName.ISHA, "19:20", null)
    )

    @Test
    fun `mosque timetable supplies adhan and iqamah with the timetable source`() {
        val build = factory.buildDay(
            date = date,
            mosque = mosque,
            place = place,
            schedules = listOf(schedule),
            entries = entries(),
            overrides = emptyList(),
            settings = AppSettings(),
            travelMode = false
        )

        val fajr = build.resolved[PrayerName.FAJR]!!
        assertEquals(java.time.LocalTime.of(5, 30).atDate(date).atZone(zone).toInstant(), fajr.adhan)
        assertEquals(java.time.LocalTime.of(5, 45).atDate(date).atZone(zone).toInstant(), fajr.iqamah)
        assertEquals(TimeSource.MOSQUE_TIMETABLE, fajr.adhanSource)
        assertEquals(TimeSource.MOSQUE_TIMETABLE, fajr.iqamahSource)
    }

    @Test
    fun `missing iqamah is not fabricated from calculation`() {
        val build = factory.buildDay(
            date = date,
            mosque = mosque,
            place = place,
            schedules = listOf(schedule),
            entries = entries(),
            overrides = emptyList(),
            settings = AppSettings(),
            travelMode = false
        )

        val dhuhr = build.resolved[PrayerName.DHUHR]!!
        assertNotNull(dhuhr.adhan)
        assertNull(dhuhr.iqamah)
        assertTrue(
            build.occurrences.none { it.prayer == PrayerName.DHUHR && it.eventType == PrayerEventType.IQAMAH }
        )
    }

    @Test
    fun `travel mode labels calculated times as travel calculated`() {
        val build = factory.buildDay(
            date = date,
            mosque = null,
            place = null,
            schedules = emptyList(),
            entries = emptyList(),
            overrides = emptyList(),
            settings = AppSettings(
                lastLocation = com.example.domain.model.LastLocation(
                    latitude = 24.8607,
                    longitude = 67.0011,
                    accuracyMeters = 10f,
                    timestampMillis = 1_700_000_000_000L,
                    source = "test"
                )
            ),
            travelMode = true
        )

        val fajr = build.resolved[PrayerName.FAJR]!!
        assertEquals(TimeSource.TRAVEL_CALCULATED, fajr.adhanSource)
        assertNull(fajr.iqamah)
        assertTrue(build.usedCalculatedOnly)
    }

    @Test
    fun `calculated fallback inside a mosque is labelled calculated`() {
        val build = factory.buildDay(
            date = date,
            mosque = mosque,
            place = place,
            schedules = emptyList(),
            entries = emptyList(),
            overrides = emptyList(),
            settings = AppSettings(),
            travelMode = false
        )

        val fajr = build.resolved[PrayerName.FAJR]!!
        assertEquals(TimeSource.CALCULATED, fajr.adhanSource)
        assertNull(fajr.iqamah)
    }

    @Test
    fun `adhan reminder switch suppresses only that alert`() {
        val customEntries = entries().map { entry ->
            if (entry.prayer == PrayerName.FAJR) entry.copy(adhanReminder = false) else entry
        }
        val build = factory.buildDay(
            date = date,
            mosque = mosque,
            place = place,
            schedules = listOf(schedule),
            entries = customEntries,
            overrides = emptyList(),
            settings = AppSettings(),
            travelMode = false
        )

        assertTrue(
            build.occurrences.none { it.prayer == PrayerName.FAJR && it.eventType == PrayerEventType.ADHAN }
        )
        assertTrue(
            build.occurrences.any { it.prayer == PrayerName.FAJR && it.eventType == PrayerEventType.IQAMAH }
        )
    }

    @Test
    fun `one date override changes the resolved time and occurrence`() {
        val build = factory.buildDay(
            date = date,
            mosque = mosque,
            place = place,
            schedules = listOf(schedule),
            entries = entries(),
            overrides = listOf(
                com.example.domain.model.ManualOverride(
                    mosqueId = mosque.id,
                    date = date,
                    prayer = PrayerName.FAJR,
                    adhan = "05:20"
                )
            ),
            settings = AppSettings(),
            travelMode = false
        )

        val fajr = build.resolved[PrayerName.FAJR]!!
        assertEquals(java.time.LocalTime.of(5, 20).atDate(date).atZone(zone).toInstant(), fajr.adhan)
        assertEquals(TimeSource.MANUAL_OVERRIDE, fajr.adhanSource)
    }

    @Test
    fun `event keys include the schedule revision`() {
        val key = factory.eventKey(mosque, 7L, date, PrayerName.FAJR, PrayerEventType.ADHAN, 0)
        assertEquals("mosque-1|7|2026-11-10|FAJR|ADHAN|0", key)
    }
}
