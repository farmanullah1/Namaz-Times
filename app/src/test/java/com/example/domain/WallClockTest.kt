package com.example.domain

import com.example.domain.model.WallClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class WallClockTest {

    @Test
    fun `valid twenty four hour values parse`() {
        assertEquals(LocalTime.of(5, 30), WallClock.parse("05:30"))
        assertEquals(LocalTime.of(5, 30), WallClock.parse("5:30"))
        assertEquals(LocalTime.MIDNIGHT, WallClock.parse("00:00"))
        assertEquals(LocalTime.of(23, 59), WallClock.parse("23:59"))
        assertEquals(LocalTime.of(5, 30), WallClock.parse(" 05:30 "))
    }

    @Test
    fun `invalid values are rejected instead of coerced`() {
        assertNull(WallClock.parse(null))
        assertNull(WallClock.parse(""))
        assertNull(WallClock.parse("24:00"))
        assertNull(WallClock.parse("05:60"))
        assertNull(WallClock.parse("5:3"))
        assertNull(WallClock.parse("05.30"))
        assertNull(WallClock.parse("not a time"))
    }

    @Test
    fun `formatting always uses two digits`() {
        assertEquals("05:30", WallClock.format(LocalTime.of(5, 30)))
        assertEquals("00:00", WallClock.format(LocalTime.MIDNIGHT))
        assertNull(WallClock.format(null))
    }

    @Test
    fun `isValid mirrors parse`() {
        assertTrue(WallClock.isValid("18:05"))
        assertFalse(WallClock.isValid("18:5"))
    }
}
