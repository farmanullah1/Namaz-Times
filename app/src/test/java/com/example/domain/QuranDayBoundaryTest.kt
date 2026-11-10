package com.example.domain

import com.example.domain.model.QuranDayBoundary
import com.example.domain.model.QuranResetRule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class QuranDayBoundaryTest {

    private val zone = ZoneId.of("Asia/Karachi")
    private val date = LocalDate.of(2026, 11, 10)

    private fun at(hour: Int, minute: Int) = date.atTime(hour, minute).atZone(zone).toInstant()

    @Test
    fun `midnight uses the local date`() {
        assertEquals(
            date,
            QuranDayBoundary.dateKeyFor(
                now = at(0, 30),
                zone = zone,
                rule = QuranResetRule.MIDNIGHT,
                customTime = LocalTime.MIDNIGHT,
                todaysFajr = null,
                todaysMaghrib = null
            )
        )
    }

    @Test
    fun `custom reset keeps early entries on the previous reading day`() {
        val ruleTime = LocalTime.of(3, 0)
        assertEquals(
            date.minusDays(1),
            QuranDayBoundary.dateKeyFor(at(2, 0), zone, QuranResetRule.CUSTOM_TIME, ruleTime, null, null)
        )
        assertEquals(
            date,
            QuranDayBoundary.dateKeyFor(at(3, 0), zone, QuranResetRule.CUSTOM_TIME, ruleTime, null, null)
        )
    }

    @Test
    fun `fajr reset uses the resolved fajr instant`() {
        val fajr = at(5, 30)
        assertEquals(
            date.minusDays(1),
            QuranDayBoundary.dateKeyFor(at(4, 0), zone, QuranResetRule.FAJR, LocalTime.MIDNIGHT, fajr, null)
        )
        assertEquals(
            date,
            QuranDayBoundary.dateKeyFor(at(6, 0), zone, QuranResetRule.FAJR, LocalTime.MIDNIGHT, fajr, null)
        )
    }

    @Test
    fun `maghrib reset uses the resolved maghrib instant`() {
        val maghrib = at(17, 55)
        assertEquals(
            date.minusDays(1),
            QuranDayBoundary.dateKeyFor(at(17, 0), zone, QuranResetRule.MAGHRIB, LocalTime.MIDNIGHT, null, maghrib)
        )
        assertEquals(
            date,
            QuranDayBoundary.dateKeyFor(at(18, 30), zone, QuranResetRule.MAGHRIB, LocalTime.MIDNIGHT, null, maghrib)
        )
    }

    @Test
    fun `missing fajr falls back to a documented local boundary instead of failing`() {
        assertEquals(
            date.minusDays(1),
            QuranDayBoundary.dateKeyFor(at(3, 0), zone, QuranResetRule.FAJR, LocalTime.MIDNIGHT, null, null)
        )
    }
}
