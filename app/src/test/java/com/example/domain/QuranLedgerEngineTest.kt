package com.example.domain

import com.example.domain.model.QuranDayInput
import com.example.domain.model.QuranLedgerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QuranLedgerEngineTest {

    private val start = LocalDate.of(2026, 11, 1)

    private fun days(vararg completed: Int, goal: Int = 1): List<QuranDayInput> =
        completed.mapIndexed { index, value ->
            QuranDayInput(start.plusDays(index.toLong()), goal, value)
        }

    @Test
    fun `day with no reading creates one ruku backlog against a goal of one`() {
        val result = QuranLedgerEngine.calculate(days(0), surplusCreditEnabled = false).single()
        assertEquals(0, result.openingBalance)
        assertEquals(1, result.requiredTarget)
        assertEquals(0, result.completedRukus)
        assertEquals(1, result.remainingForToday)
        assertEquals(1, result.backlogAfterClose)
        assertEquals(0, result.surplusCreditAfterClose)
        assertFalse(result.targetSatisfied)
    }

    @Test
    fun `meeting the goal closes the day at zero`() {
        val result = QuranLedgerEngine.calculate(days(1), surplusCreditEnabled = false).single()
        assertEquals(1, result.requiredTarget)
        assertEquals(0, result.remainingForToday)
        assertEquals(0, result.closingBalance)
        assertTrue(result.targetSatisfied)
    }

    @Test
    fun `reading two rukus against goal one does not create surplus when disabled`() {
        val result = QuranLedgerEngine.calculate(days(2), surplusCreditEnabled = false).single()
        assertEquals(1, result.requiredTarget)
        assertEquals(0, result.remainingForToday)
        assertEquals(0, result.closingBalance)
        assertEquals(0, result.surplusCreditAfterClose)
    }

    @Test
    fun `backlog raises the target on the next day`() {
        val results = QuranLedgerEngine.calculate(days(0, 0, 3), surplusCreditEnabled = false)
        assertEquals(1, results[1].openingBalance)
        assertEquals(3, results[2].requiredTarget)
        assertEquals(3, results[2].completedRukus)
        assertEquals(0, results[2].closingBalance)
        assertTrue(results[2].targetSatisfied)
    }

    @Test
    fun `surplus credit lowers the next target`() {
        val results = QuranLedgerEngine.calculate(days(3, 0), surplusCreditEnabled = true)
        assertEquals(-2, results[0].closingBalance)
        assertEquals(2, results[0].surplusCreditAfterClose)
        assertEquals(-2, results[1].openingBalance)
        assertEquals(0, results[1].requiredTarget)
        assertEquals(0, results[1].remainingForToday)
        assertEquals(-1, results[1].closingBalance)
    }

    @Test
    fun `surplus credit is capped by zero balance when disabled`() {
        val results = QuranLedgerEngine.calculate(days(5, 0), surplusCreditEnabled = false)
        assertEquals(0, results[0].closingBalance)
        assertEquals(1, results[1].requiredTarget)
        assertEquals(1, results[1].remainingForToday)
    }

    @Test
    fun `a zero daily goal never requires reading`() {
        val result = QuranLedgerEngine.calculate(days(0, goal = 0), surplusCreditEnabled = false).single()
        assertEquals(0, result.requiredTarget)
        assertEquals(0, result.remainingForToday)
        assertEquals(0, result.closingBalance)
        assertTrue(result.targetSatisfied)
    }

    @Test
    fun `streak ignores an incomplete current day but counts completed previous days`() {
        val results = QuranLedgerEngine.calculate(days(1, 1, 0), surplusCreditEnabled = false)
        val today = start.plusDays(2)
        val streak = QuranLedgerEngine.streak(results, today, countSurplusCovered = true)
        assertEquals(2L, streak)
    }

    @Test
    fun `streak breaks on a missed day`() {
        val results = QuranLedgerEngine.calculate(days(1, 0, 2), surplusCreditEnabled = false)
        val streak = QuranLedgerEngine.streak(results, start.plusDays(2), countSurplusCovered = true)
        assertEquals(1L, streak)
    }

    @Test
    fun `dates are processed in chronological order`() {
        val unordered = listOf(
            QuranDayInput(start.plusDays(1), 1, 0),
            QuranDayInput(start, 1, 1)
        )
        val results = QuranLedgerEngine.calculate(unordered, surplusCreditEnabled = false)
        assertEquals(start, results.first().dateKey)
        assertEquals(start.plusDays(1), results.last().dateKey)
    }
}
