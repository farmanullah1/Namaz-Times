package com.example.qibla

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Qibla direction is a bearing from TRUE north, in degrees clockwise (0..360).
 * Device heading (magnetic north, corrected by declination when available) is a separate value.
 */
object QiblaCalculator {
    const val KAABA_LATITUDE = 21.4224779
    const val KAABA_LONGITUDE = 39.8251832

    fun bearingFromTrueNorth(latitude: Double, longitude: Double): Double {
        require(latitude in -90.0..90.0) { "Invalid latitude" }
        require(longitude in -180.0..180.0) { "Invalid longitude" }
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - longitude)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val degrees = Math.toDegrees(atan2(y, x))
        return (degrees + 360.0) % 360.0
    }

    fun distanceToKaabaKm(latitude: Double, longitude: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(KAABA_LATITUDE - latitude)
        val dLon = Math.toRadians(KAABA_LONGITUDE - longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LATITUDE)) *
            sin(dLon / 2) * sin(dLon / 2)
        return earthRadiusKm * 2 * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }

    fun cardinalDirection(degrees: Double): String {
        val normalized = (degrees + 360.0) % 360.0
        val labels = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((normalized + 22.5) / 45.0).toInt() % 8
        return labels[index]
    }

    /** Shortest angular difference between two bearings, in degrees (0..180). */
    fun angularDifference(a: Double, b: Double): Double {
        val diff = Math.abs((a - b + 540.0) % 360.0 - 180.0)
        return diff
    }
}

enum class CompassAccuracy(val label: String) {
    HIGH("High confidence"),
    MODERATE("Moderate confidence"),
    LOW("Low confidence — calibrate device"),
    UNAVAILABLE("Sensor unavailable")
}
