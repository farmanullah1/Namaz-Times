package com.example.hijri

import com.example.domain.model.HijriInfo
import com.example.domain.model.HijriSettings
import java.time.LocalDate
import java.time.chrono.HijrahDate

/**
 * Hijri date source: java.time's Hijrah chronology (Umm al-Qura data on Android).
 * An algorithmic date is never presented as an official local moon-sighting announcement.
 */
class HijriDateProvider {

    fun hijriFor(
        date: LocalDate,
        settings: HijriSettings,
        maghribPassed: Boolean
    ): HijriInfo {
        val boundaryDate = if (settings.sunsetBoundary && maghribPassed) date.plusDays(1) else date
        val adjustedDate = boundaryDate.plusDays(settings.adjustmentDays.toLong())
        val hijri = HijrahDate.from(adjustedDate)

        val day = hijri.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
        val month = hijri.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
        val year = hijri.get(java.time.temporal.ChronoField.YEAR)

        val monthName = monthNames.getOrElse(month - 1) { "Month $month" }
        val monthArabic = monthNamesArabic.getOrElse(month - 1) { "" }

        return HijriInfo(
            day = day,
            month = month,
            year = year,
            monthName = monthName,
            monthNameArabic = monthArabic,
            formatted = "$day $monthName $year AH",
            formattedArabic = "${toArabicDigits(day)} $monthArabic ${toArabicDigits(year)} هـ",
            adjustmentDays = settings.adjustmentDays,
            sourceLabel = "Umm al-Qura Hijri chronology (calculated)",
            originalGregorianDate = date,
            displayedGregorianDate = adjustedDate,
            usedSunsetBoundary = settings.sunsetBoundary && maghribPassed
        )
    }

    private fun toArabicDigits(value: Int): String = value.toString().map { ch ->
        if (ch in '0'..'9') '٠' + (ch - '0') else ch
    }.joinToString("")

    companion object {
        private val monthNames = listOf(
            "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
            "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhul-Qi'dah", "Dhul-Hijjah"
        )
        private val monthNamesArabic = listOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
            "جمادى الأولى", "جمادى الثانية", "رجب", "شعبان",
            "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        )
    }
}
