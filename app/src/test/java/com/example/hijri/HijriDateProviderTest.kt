package com.example.hijri

import com.example.domain.model.HijriSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HijriDateProviderTest {

    private val provider = HijriDateProvider()
    private val date = LocalDate.of(2026, 11, 10)

    @Test
    fun `formatted date is a Hijri label`() {
        val info = provider.hijriFor(date, HijriSettings(), maghribPassed = false)
        assertTrue(info.formatted.endsWith("AH"))
        assertTrue(info.formattedArabic.contains("هـ"))
        assertEquals(date, info.originalGregorianDate)
    }

    @Test
    fun `positive adjustment moves the Hijri day forward`() {
        val base = provider.hijriFor(date, HijriSettings(adjustmentDays = 0), maghribPassed = false)
        val adjusted = provider.hijriFor(date, HijriSettings(adjustmentDays = 1), maghribPassed = false)
        assertNotEquals(base.formatted, adjusted.formatted)
        assertEquals(date.plusDays(1), adjusted.displayedGregorianDate)
        assertTrue(adjusted.isAdjusted)
    }

    @Test
    fun `sunset boundary only advances the date after Maghrib`() {
        val beforeMaghrib = provider.hijriFor(
            date,
            HijriSettings(adjustmentDays = 0, sunsetBoundary = true),
            maghribPassed = false
        )
        val afterMaghrib = provider.hijriFor(
            date,
            HijriSettings(adjustmentDays = 0, sunsetBoundary = true),
            maghribPassed = true
        )
        assertFalse(beforeMaghrib.usedSunsetBoundary)
        assertEquals(date, beforeMaghrib.displayedGregorianDate)
        assertTrue(afterMaghrib.usedSunsetBoundary)
        assertEquals(date.plusDays(1), afterMaghrib.displayedGregorianDate)
        assertNotEquals(beforeMaghrib.formatted, afterMaghrib.formatted)
    }

    @Test
    fun `negative adjustment moves the Hijri day back`() {
        val adjusted = provider.hijriFor(date, HijriSettings(adjustmentDays = -2), maghribPassed = false)
        assertEquals(date.minusDays(2), adjusted.displayedGregorianDate)
        assertEquals(-2, adjusted.adjustmentDays)
    }
}
