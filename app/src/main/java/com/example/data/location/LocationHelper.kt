package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.example.data.db.MosqueLocation
import com.example.data.qibla.QiblaHelper

object LocationHelper {

    @SuppressLint("MissingPermission")
    fun getLastKnownCoordinates(context: Context): Pair<Double, Double>? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null

        for (provider in providers) {
            try {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }

        return bestLocation?.let { Pair(it.latitude, it.longitude) }
    }

    /**
     * Finds the closest mosque from a list of mosques relative to given coordinates.
     */
    fun findClosestMosque(
        userLat: Double,
        userLng: Double,
        mosques: List<MosqueLocation>
    ): Pair<MosqueLocation, Double>? {
        if (mosques.isEmpty()) return null

        var closest: MosqueLocation? = null
        var minDistanceKm = Double.MAX_VALUE

        for (mosque in mosques) {
            val dist = QiblaHelper.calculateDistanceKm(userLat, userLng, mosque.latitude, mosque.longitude)
            if (dist < minDistanceKm) {
                minDistanceKm = dist
                closest = mosque
            }
        }

        return closest?.let { Pair(it, minDistanceKm) }
    }
}
