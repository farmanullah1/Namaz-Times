package com.example.location

import com.example.domain.model.AppSettings
import com.example.domain.model.CandidatePlace
import com.example.domain.model.GeoMath
import com.example.domain.model.MosqueProfile
import com.example.domain.model.SavedPlace

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long
)

data class LocationEvaluation(
    val candidates: List<CandidatePlace>,
    val confidence: CompassConfidence,
    val stale: Boolean,
    val accuracyRejected: Boolean,
    val message: String,
    val usable: Boolean
)

enum class CompassConfidence { HIGH, MODERATE, LOW }

/**
 * Evaluates one location fix without ever moving the active mosque on a single noisy point.
 * Boundary hysteresis: to enter a place the fix must be inside radius minus margin; to leave the
 * currently active place the fix must be confidently beyond radius plus margin.
 * A longer dwell for a new place is enforced by [applyDwellGate].
 */
class LocationConfidenceEvaluator {

    fun evaluate(
        fix: LocationFix?,
        places: List<SavedPlace>,
        mosques: List<MosqueProfile>,
        settings: AppSettings,
        nowMillis: Long,
        currentAcceptedMosqueId: String?
    ): LocationEvaluation {
        if (fix == null) {
            return LocationEvaluation(
                candidates = emptyList(),
                confidence = CompassConfidence.LOW,
                stale = false,
                accuracyRejected = false,
                message = "No location fix yet. Manual selection remains available.",
                usable = false
            )
        }

        val ageMillis = nowMillis - fix.timestampMillis
        val stale = ageMillis > settings.maxFixAgeSeconds * 1000L
        val accuracyRejected = fix.accuracyMeters > settings.accuracyThresholdMeters

        val mosqueById = mosques.associateBy { it.id }
        val candidates = places.mapNotNull { place ->
            val latitude = place.latitude
            val longitude = place.longitude
            if (latitude == null || longitude == null) return@mapNotNull null
            if (!GeoMath.isValidLatitude(latitude) || !GeoMath.isValidLongitude(longitude)) return@mapNotNull null

            val distance = GeoMath.distanceMeters(fix.latitude, fix.longitude, latitude, longitude)
            val mosque = place.mosqueProfileId?.let { mosqueById[it] }
            val isCurrent = mosque != null && mosque.id == currentAcceptedMosqueId
            val margin = if (isCurrent) settings.boundaryMarginMeters else -settings.boundaryMarginMeters
            val inside = distance <= place.geofenceRadiusMeters + margin

            CandidatePlace(
                id = place.id,
                mosqueProfileId = place.mosqueProfileId,
                distanceMeters = distance,
                priorityRank = place.priorityRank,
                isInsideBoundary = inside,
                isPriorityLocation = place.isPriorityLocation,
                isEnabled = place.isEnabled,
                isArchived = mosque?.isArchived ?: false
            )
        }

        if (stale || accuracyRejected) {
            val reason = when {
                stale -> "Location fix is stale (${ageMillis / 1000}s old). Keeping the last confirmed selection."
                else -> "Reported GPS accuracy (${fix.accuracyMeters.toInt()} m) is worse than your " +
                    "${settings.accuracyThresholdMeters.toInt()} m threshold. Keeping the last confirmed selection."
            }
            return LocationEvaluation(
                candidates = candidates,
                confidence = CompassConfidence.LOW,
                stale = stale,
                accuracyRejected = accuracyRejected,
                message = reason,
                usable = false
            )
        }

        val confidence = when {
            fix.accuracyMeters <= 35f -> CompassConfidence.HIGH
            fix.accuracyMeters <= settings.accuracyThresholdMeters -> CompassConfidence.MODERATE
            else -> CompassConfidence.LOW
        }

        return LocationEvaluation(
            candidates = candidates,
            confidence = confidence,
            stale = false,
            accuracyRejected = false,
            message = "Location accuracy ${fix.accuracyMeters.toInt()} m · ${confidence.name.lowercase()} confidence",
            usable = true
        )
    }
}

data class DwellingDecision(
    val acceptedMosqueId: String?,
    val acceptedPlaceId: String?,
    val pendingMosqueId: String?,
    val pendingSinceMillis: Long?,
    val reason: String,
    val confirmed: Boolean
)

/**
 * Requires a proposed boundary change to remain stable for the configured dwell period.
 * The current selection is retained while the candidate is being confirmed.
 */
fun applyDwellGate(
    proposedMosqueId: String?,
    proposedPlaceId: String?,
    currentMosqueId: String?,
    currentPlaceId: String?,
    pendingMosqueId: String?,
    pendingSinceMillis: Long?,
    settings: AppSettings,
    nowMillis: Long
): DwellingDecision {
    if (proposedMosqueId == null) {
        return DwellingDecision(
            acceptedMosqueId = currentMosqueId,
            acceptedPlaceId = currentPlaceId,
            pendingMosqueId = null,
            pendingSinceMillis = null,
            reason = "No saved place matched. Keeping the current selection.",
            confirmed = false
        )
    }

    if (proposedMosqueId == currentMosqueId) {
        return DwellingDecision(
            acceptedMosqueId = proposedMosqueId,
            acceptedPlaceId = proposedPlaceId,
            pendingMosqueId = null,
            pendingSinceMillis = null,
            reason = "Stayed in the same saved place",
            confirmed = true
        )
    }

    val dwellMillis = settings.dwellSeconds * 1000L
    if (pendingMosqueId != proposedMosqueId || pendingSinceMillis == null) {
        return DwellingDecision(
            acceptedMosqueId = currentMosqueId,
            acceptedPlaceId = currentPlaceId,
            pendingMosqueId = proposedMosqueId,
            pendingSinceMillis = nowMillis,
            reason = "Confirming the new place before switching",
            confirmed = false
        )
    }

    val elapsed = nowMillis - pendingSinceMillis
    return if (elapsed >= dwellMillis) {
        DwellingDecision(
            acceptedMosqueId = proposedMosqueId,
            acceptedPlaceId = proposedPlaceId,
            pendingMosqueId = null,
            pendingSinceMillis = null,
            reason = "Place boundary confirmed after dwell period",
            confirmed = true
        )
    } else {
        DwellingDecision(
            acceptedMosqueId = currentMosqueId,
            acceptedPlaceId = currentPlaceId,
            pendingMosqueId = proposedMosqueId,
            pendingSinceMillis = pendingSinceMillis,
            reason = "Location uncertain — keeping the current mosque for ${(dwellMillis - elapsed) / 1000}s",
            confirmed = false
        )
    }
}
