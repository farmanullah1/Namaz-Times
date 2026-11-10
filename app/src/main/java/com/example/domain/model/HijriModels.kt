package com.example.domain.model

import java.time.LocalDate

data class HijriInfo(
    val day: Int,
    val month: Int,
    val year: Int,
    val monthName: String,
    val monthNameArabic: String,
    val formatted: String,
    val formattedArabic: String,
    val adjustmentDays: Int,
    val sourceLabel: String,
    val originalGregorianDate: LocalDate,
    val displayedGregorianDate: LocalDate,
    val usedSunsetBoundary: Boolean
) {
    val isAdjusted: Boolean get() = adjustmentDays != 0
}

data class HijriSettings(
    val adjustmentDays: Int = 0,
    val sunsetBoundary: Boolean = false,
    val source: String = SOURCE_UMM_AL_QURA
) {
    companion object {
        const val SOURCE_UMM_AL_QURA = "UMM_AL_QURA"
    }
}
