package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Thin permission-aware wrapper around the fused provider with a platform last-known fallback. */
class LocationRepository(private val context: Context) {

    fun hasForegroundLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackgroundLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    suspend fun requestFreshLocation(): LocationFix? {
        if (!hasForegroundLocationPermission()) return null
        if (!isLocationEnabled()) return null

        val client = try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (error: Exception) {
            Log.w(TAG, "Fused location provider unavailable; using platform provider.", error)
            return platformLastKnown()?.toFix("platform")
        }

        val task = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        val location = suspendCancellableCoroutine<Location?> { continuation ->
            task.addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
                .addOnFailureListener { error ->
                    Log.w(TAG, "Fused location request failed", error)
                    if (continuation.isActive) continuation.resume(null)
                }
        }
        return (location ?: platformLastKnown())?.toFix("fused")
    }

    @SuppressLint("MissingPermission")
    private fun platformLastKnown(): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        if (!hasForegroundLocationPermission()) return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        var best: Location? = null
        for (provider in providers) {
            val candidate = try {
                manager.getLastKnownLocation(provider)
            } catch (error: SecurityException) {
                Log.w(TAG, "Location permission was revoked", error)
                null
            } ?: continue
            if (best == null || candidate.accuracy < best.accuracy) best = candidate
        }
        return best
    }

    private fun Location.toFix(source: String): LocationFix = LocationFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
        timestampMillis = if (time > 0) time else System.currentTimeMillis()
    )

    companion object {
        private const val TAG = "LocationRepository"
    }
}
