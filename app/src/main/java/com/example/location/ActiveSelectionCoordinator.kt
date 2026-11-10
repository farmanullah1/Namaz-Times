package com.example.location

import com.example.data.repository.MosqueRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.LastLocation
import com.example.domain.model.LastSelectionDecision
import com.example.domain.model.LocationMode
import com.example.domain.model.MosqueSelection
import com.example.domain.model.SelectionState
import com.example.domain.model.resolveMosqueSelection
import com.example.prayer.ScheduleRebuildCoordinator
import com.example.qibla.CompassAccuracy
import java.time.Instant

data class SelectionUpdate(
    val message: String,
    val changed: Boolean,
    val mosqueId: String?,
    val placeId: String?,
    val confidence: CompassAccuracy
)

/**
 * Applies one location fix through accuracy checks, hysteresis and the dwell gate, then persists the
 * decision. Selection changes trigger a schedule rebuild; failures never clear the current mosque.
 */
class ActiveSelectionCoordinator(
    private val settingsRepository: SettingsRepository,
    private val mosqueRepository: MosqueRepository,
    private val locationRepository: LocationRepository,
    private val rebuildCoordinator: ScheduleRebuildCoordinator,
    private val geofenceManager: GeofenceManager
) {

    private val evaluator = LocationConfidenceEvaluator()
    private var pendingMosqueId: String? = null
    private var pendingSinceMillis: Long? = null

    suspend fun expireTemporaryIfNeeded(): Boolean {
        val settings = settingsRepository.current()
        val expiresAt = settings.temporaryExpiresAtMillis ?: return false
        if (expiresAt > System.currentTimeMillis()) return false
        settingsRepository.clearTemporarySelection()
        rebuildCoordinator.rebuild("Temporary selection expired")
        return true
    }

    suspend fun refreshFromLocation(reason: String): SelectionUpdate {
        expireTemporaryIfNeeded()
        val settings = settingsRepository.current()
        if (!settings.automaticLocationEnabled || settings.locationMode != LocationMode.AUTOMATIC) {
            return SelectionUpdate(
                message = "Automatic place switching is off. Manual selection is active.",
                changed = false,
                mosqueId = settings.manualMosqueId,
                placeId = settings.manualPlaceId,
                confidence = CompassAccuracy.UNAVAILABLE
            )
        }
        if (!locationRepository.hasForegroundLocationPermission()) {
            return SelectionUpdate(
                message = "Location permission is not granted. Manual selection remains available.",
                changed = false,
                mosqueId = settings.lastAutoMosqueId,
                placeId = null,
                confidence = CompassAccuracy.UNAVAILABLE
            )
        }
        if (!locationRepository.isLocationEnabled()) {
            return SelectionUpdate(
                message = "Location services are disabled. Keeping the last confirmed place.",
                changed = false,
                mosqueId = settings.lastAutoMosqueId,
                placeId = settings.lastAutoPlaceId,
                confidence = CompassAccuracy.UNAVAILABLE
            )
        }

        val fix = locationRepository.requestFreshLocation()
            ?: return SelectionUpdate(
                message = "No usable location fix yet. Keeping the last confirmed selection.",
                changed = false,
                mosqueId = settings.lastAutoMosqueId,
                placeId = settings.lastAutoPlaceId,
                confidence = CompassAccuracy.LOW
            )

        settingsRepository.saveLastLocation(
            LastLocation(
                latitude = fix.latitude,
                longitude = fix.longitude,
                accuracyMeters = fix.accuracyMeters,
                timestampMillis = fix.timestampMillis,
                source = "fused"
            )
        )

        val snapshot = mosqueRepository.snapshot()
        val now = Instant.now()
        val evaluation = evaluator.evaluate(
            fix = fix,
            places = snapshot.places,
            mosques = snapshot.mosques,
            settings = settings,
            nowMillis = now.toEpochMilli(),
            currentAcceptedMosqueId = settings.lastAutoMosqueId
        )

        if (!evaluation.usable) {
            return SelectionUpdate(
                message = evaluation.message,
                changed = false,
                mosqueId = settings.lastAutoMosqueId,
                placeId = settings.lastAutoPlaceId,
                confidence = CompassAccuracy.LOW
            )
        }

        val selection = resolveMosqueSelection(
            state = SelectionState(
                mode = LocationMode.AUTOMATIC,
                currentAcceptedMosqueId = settings.lastAutoMosqueId,
                maximumNearestDistanceMeters = settings.maximumNearestDistanceMeters
            ),
            candidates = evaluation.candidates,
            now = now
        )

        val selectedMosqueId = (selection as? MosqueSelection.Selected)?.mosqueId
        val selectedPlace = if (selectedMosqueId == null) {
            null
        } else {
            evaluation.candidates
                .filter { it.mosqueProfileId == selectedMosqueId }
                .minByOrNull { it.distanceMeters }
        }

        if (selectedMosqueId == null) {
            val wasSelected = settings.lastAutoMosqueId != null
            pendingMosqueId = null
            pendingSinceMillis = null
            settingsRepository.saveAutomaticDecision(
                mosqueId = null,
                placeId = null,
                reason = "Outside saved places — calculated times",
                decision = LastSelectionDecision(
                    timestampMillis = now.toEpochMilli(),
                    accuracyMeters = fix.accuracyMeters,
                    mode = LocationMode.AUTOMATIC,
                    selectedMosqueId = null,
                    selectedPlaceId = null,
                    reason = "No saved place matched",
                    confidence = evaluation.confidence.name
                )
            )
            if (wasSelected) rebuildCoordinator.rebuild("Outside saved places")
            return SelectionUpdate(
                message = "Outside saved places. Use calculated times for this location or add it as a place.",
                changed = wasSelected,
                mosqueId = null,
                placeId = null,
                confidence = confidenceOf(evaluation.confidence)
            )
        }

        val dwell = applyDwellGate(
            proposedMosqueId = selectedMosqueId,
            proposedPlaceId = selectedPlace?.id,
            currentMosqueId = settings.lastAutoMosqueId,
            currentPlaceId = settings.lastAutoPlaceId,
            pendingMosqueId = pendingMosqueId,
            pendingSinceMillis = pendingSinceMillis,
            settings = settings,
            nowMillis = now.toEpochMilli()
        )
        pendingMosqueId = dwell.pendingMosqueId
        pendingSinceMillis = dwell.pendingSinceMillis

        val changed = dwell.confirmed && dwell.acceptedMosqueId != settings.lastAutoMosqueId
        val finalMosque = dwell.acceptedMosqueId
        val finalPlace = dwell.acceptedPlaceId

        settingsRepository.saveAutomaticDecision(
            mosqueId = finalMosque,
            placeId = finalPlace,
            reason = dwell.reason,
            decision = LastSelectionDecision(
                timestampMillis = now.toEpochMilli(),
                accuracyMeters = fix.accuracyMeters,
                mode = LocationMode.AUTOMATIC,
                selectedMosqueId = finalMosque,
                selectedPlaceId = finalPlace,
                reason = reason,
                confidence = evaluation.confidence.name
            )
        )

        if (changed) {
            rebuildCoordinator.rebuild("Automatic place switch")
            geofenceManager.sync(snapshot.places, locationRepository.hasBackgroundLocationPermission())
        }

        return SelectionUpdate(
            message = if (changed) {
                "Switched to ${snapshot.mosque(finalMosque)?.name ?: "the saved place"}"
            } else {
                dwell.reason
            },
            changed = changed,
            mosqueId = finalMosque,
            placeId = finalPlace,
            confidence = confidenceOf(evaluation.confidence)
        )
    }

    private fun confidenceOf(confidence: CompassConfidence): CompassAccuracy = when (confidence) {
        CompassConfidence.HIGH -> CompassAccuracy.HIGH
        CompassConfidence.MODERATE -> CompassAccuracy.MODERATE
        CompassConfidence.LOW -> CompassAccuracy.LOW
    }
}
