package com.example.location

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.domain.model.SavedPlace
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Registers only the most relevant saved places (Android limits active geofences, currently 100).
 * Requires background location, which the app requests only after the user enables automation.
 * Manual selection remains fully functional when geofencing is unavailable.
 */
class GeofenceManager(private val context: Context) {

    private val client: GeofencingClient? = try {
        LocationServices.getGeofencingClient(context)
    } catch (error: Exception) {
        Log.w(TAG, "Geofencing client unavailable", error)
        null
    }

    suspend fun sync(places: List<SavedPlace>, hasBackgroundPermission: Boolean): Result<Int> {
        if (!hasBackgroundPermission) {
            return Result.failure(SecurityException("Background location permission is required for automatic place switching."))
        }
        val geofencingClient = client
            ?: return Result.failure(IllegalStateException("Geofencing is not available on this device."))
        val pendingIntent = geofencePendingIntent()
            ?: return Result.failure(IllegalStateException("Could not create the geofence broadcast."))

        val eligible = places.asSequence()
            .filter { it.isEnabled && it.latitude != null && it.longitude != null }
            .sortedWith(compareBy<SavedPlace> { it.priorityRank }.thenBy { it.displayName })
            .take(MAX_GEOFENCES)
            .toList()

        try {
            await(geofencingClient.removeGeofences(pendingIntent))
            if (eligible.isEmpty()) return Result.success(0)

            val geofences = eligible.map { place ->
                Geofence.Builder()
                    .setRequestId(place.id)
                    .setCircularRegion(
                        place.latitude!!,
                        place.longitude!!,
                        place.geofenceRadiusMeters.coerceAtLeast(MIN_RADIUS_METERS).toFloat()
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()
            }
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build()
            await(geofencingClient.addGeofences(request, pendingIntent))
            return Result.success(geofences.size)
        } catch (error: SecurityException) {
            return Result.failure(error)
        } catch (error: Exception) {
            Log.w(TAG, "Geofence registration failed", error)
            return Result.failure(error)
        }
    }

    suspend fun removeAll(): Result<Unit> {
        val pendingIntent = geofencePendingIntent() ?: return Result.success(Unit)
        return try {
            await(client?.removeGeofences(pendingIntent) ?: return Result.success(Unit))
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    fun geofencePendingIntent(): PendingIntent? {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE
            data = Uri.parse("namaz-times://geofence")
        }
        return PendingIntent.getBroadcast(
            context,
            GEOFENCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private suspend fun <T> await(task: Task<T>): T = suspendCancellableCoroutine { continuation ->
        task.addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
            .addOnFailureListener { error -> if (continuation.isActive) continuation.cancel(error) }
    }

    companion object {
        const val ACTION_GEOFENCE = "com.example.namaztimes.GEOFENCE"
        const val GEOFENCE_REQUEST_CODE = 770_001
        const val MAX_GEOFENCES = 95
        const val MIN_RADIUS_METERS = 100
        private const val TAG = "GeofenceManager"
    }
}
