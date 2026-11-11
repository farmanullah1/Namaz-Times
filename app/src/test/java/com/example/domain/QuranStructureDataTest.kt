package com.example.domain

import com.example.domain.model.QuranStructureData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QuranStructureDataTest {

    @Test
    fun testQuranStructureIntegrity() {
        assertEquals("Quran must have 114 Surahs", 114, QuranStructureData.surahs.size)
        assertEquals("Quran must have 30 Juz", 30, QuranStructureData.juzList.size)
        assertEquals("Quran canonical Rukus total must be 558", 558, QuranStructureData.TOTAL_RUKUS)

        val computedTotalRukus = QuranStructureData.surahs.sumOf { it.rukusCount }
        assertEquals("Sum of Surah Rukus must match TOTAL_RUKUS", 558, computedTotalRukus)
    }

    @Test
    fun testGlobalRukuLookup() {
        val (fatihah, ruku1) = QuranStructureData.findSurahForGlobalRuku(1)
        assertEquals("Al-Fatihah", fatihah.nameEnglish)
        assertEquals(1, ruku1)

        val (baqarah, baqarahRuku1) = QuranStructureData.findSurahForGlobalRuku(2)
        assertEquals("Al-Baqarah", baqarah.nameEnglish)
        assertEquals(1, baqarahRuku1)

        val (baqarahLast, baqarahRuku40) = QuranStructureData.findSurahForGlobalRuku(41)
        assertEquals("Al-Baqarah", baqarahLast.nameEnglish)
        assertEquals(40, baqarahRuku40)

        val (nas, nasRuku) = QuranStructureData.findSurahForGlobalRuku(558)
        assertEquals("An-Nas", nas.nameEnglish)
        assertEquals(1, nasRuku)
    }

    @Test
    fun testKhatamPaceCalculation() {
        val today = LocalDate.of(2026, 1, 1)

        // 300 Rukus remaining over 30 days -> 10 Rukus/day
        val pace30 = QuranStructureData.calculateRequiredPace(300, today, today.plusDays(30))
        assertEquals(10, pace30)

        // 558 Rukus remaining over 30 days (Ramadan) -> ceil(558 / 30) = 19 Rukus/day
        val ramadanPace = QuranStructureData.calculateRequiredPace(558, today, today.plusDays(30))
        assertEquals(19, ramadanPace)

        // 558 Rukus remaining over 365 days (1 year) -> ceil(558 / 365) = 2 Rukus/day
        val yearPace = QuranStructureData.calculateRequiredPace(558, today, today.plusDays(365))
        assertEquals(2, yearPace)
    }
}
