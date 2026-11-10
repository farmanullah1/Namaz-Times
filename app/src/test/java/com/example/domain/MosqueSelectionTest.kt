package com.example.domain

import com.example.domain.model.CandidatePlace
import com.example.domain.model.LocationMode
import com.example.domain.model.MosqueSelection
import com.example.domain.model.SelectionState
import com.example.domain.model.resolveMosqueSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MosqueSelectionTest {

    private val now = Instant.parse("2026-11-10T12:00:00Z")

    private fun candidate(
        id: String,
        mosqueId: String?,
        distance: Double = 1000.0,
        inside: Boolean = false,
        priority: Boolean = false,
        priorityRank: Int = 100,
        enabled: Boolean = true,
        archived: Boolean = false
    ) = CandidatePlace(
        id = id,
        mosqueProfileId = mosqueId,
        distanceMeters = distance,
        priorityRank = priorityRank,
        isInsideBoundary = inside,
        isPriorityLocation = priority,
        isEnabled = enabled,
        isArchived = archived
    )

    @Test
    fun `temporary override wins over everything while active`() {
        val selection = resolveMosqueSelection(
            state = SelectionState(
                mode = LocationMode.AUTOMATIC,
                temporaryMosqueId = "temp",
                temporaryExpiresAt = now.plusSeconds(600),
                manuallySelectedMosqueId = "manual"
            ),
            candidates = listOf(candidate("p", "boundary", inside = true)),
            now = now
        )
        assertEquals(MosqueSelection.Selected("temp", "Temporary user override"), selection)
    }

    @Test
    fun `expired temporary override is ignored`() {
        val selection = resolveMosqueSelection(
            state = SelectionState(
                mode = LocationMode.AUTOMATIC,
                temporaryMosqueId = "temp",
                temporaryExpiresAt = now.minusSeconds(1)
            ),
            candidates = listOf(candidate("p", "mosque", distance = 500.0)),
            now = now
        )
        assertEquals(MosqueSelection.Selected("mosque", "Nearest saved mosque"), selection)
    }

    @Test
    fun `manual lock beats boundary and priority`() {
        val selection = resolveMosqueSelection(
            state = SelectionState(
                mode = LocationMode.MANUAL,
                manuallySelectedMosqueId = "manual"
            ),
            candidates = listOf(
                candidate("a", "boundary", inside = true),
                candidate("b", "priority", priority = true)
            ),
            now = now
        )
        assertEquals(MosqueSelection.Selected("manual", "Manual selection"), selection)
    }

    @Test
    fun `active boundary beats priority and nearest`() {
        val selection = resolveMosqueSelection(
            state = SelectionState(mode = LocationMode.AUTOMATIC),
            candidates = listOf(
                candidate("far", "boundary-mosque", distance = 5000.0, inside = true),
                candidate("near", "priority-mosque", distance = 100.0, priority = true)
            ),
            now = now
        )
        assertEquals(MosqueSelection.Selected("boundary-mosque", "Active saved place boundary"), selection)
    }

    @Test
    fun `priority place beats a nearer non-priority mosque`() {
        val selection = resolveMosqueSelection(
            state = SelectionState(mode = LocationMode.AUTOMATIC),
            candidates = listOf(
                candidate("near", "near-mosque", distance = 100.0),
                candidate("priority", "priority-mosque", distance = 8000.0, priority = true)
            ),
            now = now
        )
        assertEquals(MosqueSelection.Selected("priority-mosque", "User priority place"), selection)
    }

    @Test
    fun `nearest saved mosque wins inside maximum distance`() {
        val selection = resolveMosqueSelection(
            state = SelectionState(mode = LocationMode.AUTOMATIC, maximumNearestDistanceMeters = 20_000.0),
            candidates = listOf(
                candidate("far", "far-mosque", distance = 15_000.0),
                candidate("near", "near-mosque", distance = 900.0)
            ),
            now = now
        )
        assertEquals(MosqueSelection.Selected("near-mosque", "Nearest saved mosque"), selection)
    }

    @Test
    fun `outside every threshold uses calculated fallback`() {
        val selection = resolveMosqueSelection(
            state = SelectionState(mode = LocationMode.AUTOMATIC, maximumNearestDistanceMeters = 20_000.0),
            candidates = listOf(candidate("far", "far-mosque", distance = 40_000.0)),
            now = now
        )
        assertTrue(selection is MosqueSelection.UseCalculatedFallback)
    }

    @Test
    fun `disabled and archived candidates are ignored`() {
        val selection = resolveMosqueSelection(
            state = SelectionState(mode = LocationMode.AUTOMATIC),
            candidates = listOf(
                candidate("disabled", "disabled-mosque", distance = 100.0, enabled = false),
                candidate("archived", "archived-mosque", distance = 200.0, archived = true),
                candidate("valid", "valid-mosque", distance = 500.0)
            ),
            now = now
        )
        assertEquals(MosqueSelection.Selected("valid-mosque", "Nearest saved mosque"), selection)
    }

    @Test
    fun `tie breaks by priority rank then distance then id`() {
        val selection = resolveMosqueSelection(
            state = SelectionState(mode = LocationMode.AUTOMATIC),
            candidates = listOf(
                candidate("b", "mosque-b", distance = 100.0, priorityRank = 5),
                candidate("a", "mosque-a", distance = 100.0, priorityRank = 5),
                candidate("c", "mosque-c", distance = 100.0, priorityRank = 1)
            ),
            now = now
        )
        assertEquals(MosqueSelection.Selected("mosque-c", "Nearest saved mosque"), selection)
    }
}
