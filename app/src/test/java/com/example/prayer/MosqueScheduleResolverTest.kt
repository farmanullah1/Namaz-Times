package com.example.prayer

import com.example.domain.model.MosqueProfile
import com.example.domain.model.MosqueSchedule
import com.example.domain.model.ScheduleEntry
import com.example.domain.model.ScheduleKind
import com.example.domain.model.PrayerName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class MosqueScheduleResolverTest {

    private val resolver = MosqueScheduleResolver()
    private val mosque = MosqueProfile(
        id = "mosque-1",
        name = "Test Mosque",
        timezoneId = "Asia/Karachi"
    )

    private fun schedule(
        id: String,
        kind: ScheduleKind,
        weekday: Int? = null,
        from: LocalDate? = null,
        until: LocalDate? = null,
        revision: Long = 1L
    ) = MosqueSchedule(
        id = id,
        mosqueId = mosque.id,
        kind = kind,
        weekday = weekday,
        effectiveFrom = from,
        effectiveUntil = until,
        revision = revision
    )

    private fun entry(
        scheduleId: String,
        prayer: PrayerName,
        adhan: String? = null,
        iqamah: String? = null,
        note: String = ""
    ) = ScheduleEntry(
        scheduleId = scheduleId,
        prayer = prayer,
        adhan = adhan,
        iqamah = iqamah,
        note = note
    )

    @Test
    fun `one date override beats seasonal weekly and fixed`() {
        val friday = LocalDate.of(2026, 11, 13)
        val fixed = schedule("fixed", ScheduleKind.FIXED)
        val weekly = schedule("weekly", ScheduleKind.WEEKLY, weekday = 5)
        val seasonal = schedule("season", ScheduleKind.DATE_RANGE, from = friday.minusDays(3), until = friday.plusDays(3))
        val override = schedule("override", ScheduleKind.DATE_OVERRIDE, from = friday)

        val resolved = resolver.resolve(
            date = friday,
            mosque = mosque,
            schedules = listOf(fixed, weekly, seasonal, override),
            entries = listOf(
                entry("fixed", PrayerName.FAJR, "05:30", "05:45"),
                entry("weekly", PrayerName.FAJR, "05:15", "05:30"),
                entry("season", PrayerName.FAJR, "05:00", "05:15"),
                entry("override", PrayerName.FAJR, "04:45", "05:00")
            )
        )

        assertEquals(LocalTime.of(4, 45), resolved.slots[PrayerName.FAJR]?.adhan)
        assertEquals(LocalTime.of(5, 0), resolved.slots[PrayerName.FAJR]?.iqamah)
        assertEquals(ScheduleKind.DATE_OVERRIDE, resolved.slots[PrayerName.FAJR]?.adhanSourceKind)
        assertEquals(ScheduleKind.DATE_OVERRIDE, resolved.primaryKind)
    }

    @Test
    fun `per field fallback keeps a higher layer iqamah when the adhan is missing`() {
        val date = LocalDate.of(2026, 11, 10)
        val fixed = schedule("fixed", ScheduleKind.FIXED)
        val seasonal = schedule("season", ScheduleKind.DATE_RANGE, from = date.minusDays(1), until = date.plusDays(1))

        val resolved = resolver.resolve(
            date = date,
            mosque = mosque,
            schedules = listOf(fixed, seasonal),
            entries = listOf(
                entry("fixed", PrayerName.DHUHR, adhan = "13:00", iqamah = "13:15"),
                entry("season", PrayerName.DHUHR, adhan = null, iqamah = "13:20")
            )
        )

        val dhuhr = resolved.slots[PrayerName.DHUHR]!!
        assertEquals(LocalTime.of(13, 0), dhuhr.adhan)
        assertEquals(LocalTime.of(13, 20), dhuhr.iqamah)
        assertEquals(ScheduleKind.FIXED, dhuhr.adhanSourceKind)
        assertEquals(ScheduleKind.DATE_RANGE, dhuhr.iqamahSourceKind)
    }

    @Test
    fun `weekly only applies on its weekday`() {
        val friday = LocalDate.of(2026, 11, 13)
        val saturday = friday.plusDays(1)
        val fixed = schedule("fixed", ScheduleKind.FIXED)
        val fridaySchedule = schedule("friday", ScheduleKind.WEEKLY, weekday = 5)

        val fridayResolved = resolver.resolve(
            friday, mosque, listOf(fixed, fridaySchedule),
            listOf(
                entry("fixed", PrayerName.DHUHR, "13:00", "13:15"),
                entry("friday", PrayerName.DHUHR, "12:30", "12:45")
            )
        )
        val saturdayResolved = resolver.resolve(
            saturday, mosque, listOf(fixed, fridaySchedule),
            listOf(
                entry("fixed", PrayerName.DHUHR, "13:00", "13:15"),
                entry("friday", PrayerName.DHUHR, "12:30", "12:45")
            )
        )

        assertEquals(LocalTime.of(12, 30), fridayResolved.slots[PrayerName.DHUHR]?.adhan)
        assertEquals(LocalTime.of(13, 0), saturdayResolved.slots[PrayerName.DHUHR]?.adhan)
    }

    @Test
    fun `missing iqamah stays missing instead of being invented`() {
        val resolved = resolver.resolve(
            date = LocalDate.of(2026, 11, 10),
            mosque = mosque,
            schedules = listOf(schedule("fixed", ScheduleKind.FIXED)),
            entries = listOf(entry("fixed", PrayerName.MAGHRIB, adhan = "18:05"))
        )

        val maghrib = resolved.slots[PrayerName.MAGHRIB]!!
        assertEquals(LocalTime.of(18, 5), maghrib.adhan)
        assertNull(maghrib.iqamah)
    }

    @Test
    fun `manual date override wins over every timetable layer`() {
        val date = LocalDate.of(2026, 11, 10)
        val resolved = resolver.resolve(
            date = date,
            mosque = mosque,
            schedules = listOf(schedule("fixed", ScheduleKind.FIXED)),
            entries = listOf(entry("fixed", PrayerName.FAJR, "05:30", "05:45")),
            overrides = listOf(
                com.example.domain.model.ManualOverride(
                    mosqueId = mosque.id,
                    date = date,
                    prayer = PrayerName.FAJR,
                    adhan = "05:20",
                    iqamah = "05:35"
                )
            )
        )

        assertEquals(LocalTime.of(5, 20), resolved.slots[PrayerName.FAJR]?.adhan)
        assertEquals(LocalTime.of(5, 35), resolved.slots[PrayerName.FAJR]?.iqamah)
        assertEquals(ScheduleKind.DATE_OVERRIDE, resolved.slots[PrayerName.FAJR]?.adhanSourceKind)
    }
}
