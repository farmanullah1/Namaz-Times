package com.example.domain.model

import java.time.Instant

/**
 * Mosque/place selection resolution. This is a pure, unit-tested implementation of the
 * required priority: temporary override > manual lock > active boundary > priority place >
 * nearest saved mosque inside the configured distance > calculated fallback.
 */
data class CandidatePlace(
    val id: String,
    val mosqueProfileId: String?,
    val distanceMeters: Double,
    val priorityRank: Int,
    val isInsideBoundary: Boolean,
    val isPriorityLocation: Boolean,
    val isEnabled: Boolean,
    val isArchived: Boolean
)

data class SelectionState(
    val mode: LocationMode,
    val temporaryMosqueId: String? = null,
    val temporaryExpiresAt: Instant? = null,
    val manuallySelectedMosqueId: String? = null,
    val currentAcceptedMosqueId: String? = null,
    val maximumNearestDistanceMeters: Double = 20_000.0
)

sealed interface MosqueSelection {
    data class Selected(val mosqueId: String, val reason: String) : MosqueSelection
    data object NeedsUserChoice : MosqueSelection
    data object UseCalculatedFallback : MosqueSelection
}

fun resolveMosqueSelection(
    state: SelectionState,
    candidates: List<CandidatePlace>,
    now: Instant
): MosqueSelection {
    val temporaryStillActive = state.temporaryMosqueId != null &&
        state.temporaryExpiresAt?.isAfter(now) == true

    if (temporaryStillActive) {
        return MosqueSelection.Selected(state.temporaryMosqueId!!, "Temporary user override")
    }

    if (state.mode == LocationMode.MANUAL && state.manuallySelectedMosqueId != null) {
        return MosqueSelection.Selected(state.manuallySelectedMosqueId, "Manual selection")
    }

    val valid = candidates.filter { it.isEnabled && !it.isArchived && it.mosqueProfileId != null }

    if (state.mode == LocationMode.AUTOMATIC) {
        val boundary = valid.filter { it.isInsideBoundary }
        if (boundary.isNotEmpty()) {
            val winner = boundary.sortedWith(candidateComparator).first()
            return MosqueSelection.Selected(winner.mosqueProfileId!!, "Active saved place boundary")
        }
    }

    val priority = valid.filter { it.isPriorityLocation }
    if (priority.isNotEmpty()) {
        val winner = priority.sortedWith(candidateComparator).first()
        return MosqueSelection.Selected(winner.mosqueProfileId!!, "User priority place")
    }

    val nearest = valid
        .filter { it.distanceMeters <= state.maximumNearestDistanceMeters }
        .sortedWith(candidateComparator)
        .firstOrNull()

    return nearest?.let {
        MosqueSelection.Selected(it.mosqueProfileId!!, "Nearest saved mosque")
    } ?: MosqueSelection.UseCalculatedFallback
}

private val candidateComparator: Comparator<CandidatePlace> =
    compareBy<CandidatePlace> { it.priorityRank }
        .thenBy { it.distanceMeters }
        .thenBy { it.id }
