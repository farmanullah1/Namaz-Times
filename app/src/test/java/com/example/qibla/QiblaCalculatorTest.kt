package com.example.qibla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaCalculatorTest {

    @Test
    fun `Karachi bearing points west south west`() {
        val bearing = QiblaCalculator.bearingFromTrueNorth(24.8607, 67.0011)
        assertTrue("Karachi bearing was $bearing", bearing in 250.0..262.0)
        assertEquals("W", QiblaCalculator.cardinalDirection(bearing))
    }

    @Test
    fun `London bearing points east south east`() {
        val bearing = QiblaCalculator.bearingFromTrueNorth(51.5074, -0.1278)
        assertTrue("London bearing was $bearing", bearing in 115.0..122.0)
    }

    @Test
    fun `New York bearing points north east`() {
        val bearing = QiblaCalculator.bearingFromTrueNorth(40.7128, -74.0060)
        assertTrue("New York bearing was $bearing", bearing in 55.0..60.0)
    }

    @Test
    fun `bearing is always normalized into zero to 360`() {
        listOf(
            24.8607 to 67.0011,
            51.5074 to -0.1278,
            -33.8688 to 151.2093,
            -1.2921 to 36.8219
        ).forEach { (lat, lon) ->
            val bearing = QiblaCalculator.bearingFromTrueNorth(lat, lon)
            assertTrue("Bearing $bearing out of range", bearing >= 0.0 && bearing < 360.0)
        }
    }

    @Test
    fun `angular difference handles the wrap around north`() {
        assertEquals(10.0, QiblaCalculator.angularDifference(355.0, 5.0), 0.0001)
        assertEquals(10.0, QiblaCalculator.angularDifference(5.0, 355.0), 0.0001)
        assertEquals(180.0, QiblaCalculator.angularDifference(0.0, 180.0), 0.0001)
        assertEquals(0.0, QiblaCalculator.angularDifference(42.0, 42.0), 0.0001)
    }

    @Test
    fun `distance to the Kaaba from Karachi is plausible`() {
        val distance = QiblaCalculator.distanceToKaabaKm(24.8607, 67.0011)
        assertTrue("Distance was $distance", distance in 2_800.0..3_100.0)
    }

    @Test
    fun `cardinal labels cover the compass`() {
        assertEquals("N", QiblaCalculator.cardinalDirection(0.0))
        assertEquals("NE", QiblaCalculator.cardinalDirection(45.0))
        assertEquals("E", QiblaCalculator.cardinalDirection(90.0))
        assertEquals("S", QiblaCalculator.cardinalDirection(180.0))
        assertEquals("NW", QiblaCalculator.cardinalDirection(315.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid latitude is rejected`() {
        QiblaCalculator.bearingFromTrueNorth(120.0, 0.0)
    }
}
