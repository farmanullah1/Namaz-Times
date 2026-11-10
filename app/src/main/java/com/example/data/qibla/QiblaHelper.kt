package com.example.data.qibla

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object QiblaHelper {
    const val MAKKAH_LATITUDE = 21.422487
    const val MAKKAH_LONGITUDE = 39.826206

    /**
     * Calculates the true north bearing (in degrees, 0..360) pointing from (userLat, userLng) to the Holy Kaaba in Makkah.
     */
    fun calculateQiblaBearing(userLat: Double, userLng: Double): Double {
        val phi1 = Math.toRadians(userLat)
        val phi2 = Math.toRadians(MAKKAH_LATITUDE)
        val deltaLambda = Math.toRadians(MAKKAH_LONGITUDE - userLng)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        val bearingRad = atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)
        return (bearingDeg + 360.0) % 360.0
    }

    /**
     * Calculates distance to Makkah in kilometers using the Haversine formula.
     */
    fun calculateDistanceToMakkahKm(userLat: Double, userLng: Double): Double {
        return calculateDistanceKm(userLat, userLng, MAKKAH_LATITUDE, MAKKAH_LONGITUDE)
    }

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    fun getCompassDirectionText(degrees: Double): String {
        val normalized = (degrees + 360.0) % 360.0
        return when {
            normalized in 337.5..360.0 || normalized in 0.0..<22.5 -> "North (N)"
            normalized in 22.5..<67.5 -> "North-East (NE)"
            normalized in 67.5..<112.5 -> "East (E)"
            normalized in 112.5..<157.5 -> "South-East (SE)"
            normalized in 157.5..<202.5 -> "South (S)"
            normalized in 202.5..<247.5 -> "South-West (SW)"
            normalized in 247.5..<292.5 -> "West (W)"
            else -> "North-West (NW)"
        }
    }
}
