package com.example.data.hijri

import java.util.Calendar
import kotlin.math.floor

data class HijriDate(
    val day: Int,
    val month: Int, // 1 to 12
    val year: Int,
    val monthNameEn: String,
    val monthNameAr: String,
    val dayOfWeekEn: String,
    val dayOfWeekAr: String,
    val specialEvent: String? = null,
    val isSunnahFastDay: Boolean = false // Ayyam al-Beed (13, 14, 15) or Monday/Thursday
)

object HijriCalendarHelper {

    private val MONTH_NAMES_EN = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhul-Qi'dah", "Dhul-Hijjah"
    )

    private val MONTH_NAMES_AR = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
        "جمادى الأولى", "جمادى الثانية", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    private val DAY_NAMES_EN = listOf(
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    )

    private val DAY_NAMES_AR = listOf(
        "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    )

    /**
     * Converts a Gregorian Calendar date into a Hijri date with an optional moon-sighting adjustment offset.
     */
    fun getHijriDate(calendar: Calendar = Calendar.getInstance(), adjustmentDays: Int = 0): HijriDate {
        val workingCal = calendar.clone() as Calendar
        if (adjustmentDays != 0) {
            workingCal.add(Calendar.DAY_OF_YEAR, adjustmentDays)
        }

        val dayOfWeekIndex = workingCal.get(Calendar.DAY_OF_WEEK) - 1
        val dayOfWeekEn = DAY_NAMES_EN.getOrElse(dayOfWeekIndex) { "Today" }
        val dayOfWeekAr = DAY_NAMES_AR.getOrElse(dayOfWeekIndex) { "" }

        val gYear = workingCal.get(Calendar.YEAR)
        val gMonth = workingCal.get(Calendar.MONTH) + 1
        val gDay = workingCal.get(Calendar.DAY_OF_MONTH)

        // Convert to Julian Day
        var y = gYear
        var m = gMonth
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + gDay + b - 1524.5

        // Convert JD to Hijri using Kuwaiti algorithm
        val z = jd - 1948440 + 10632
        val n = floor((z - 1) / 10631.0)
        val zRem = z - 10631 * n + 354
        val j = (floor((10985 - zRem) / 5316.0)) * (floor((50 * zRem) / 17719.0)) +
                (floor(zRem / 5670.0)) * (floor((43 * zRem) / 15238.0))
        val zRem2 = zRem - (floor((30 - j) / 15.0)) * (floor((17719 * j) / 50.0)) -
                (floor(j / 16.0)) * (floor((15238 * j) / 43.0)) + 29
        val hMonth = (floor((24 * zRem2) / 709.0)).toInt()
        val hDay = (zRem2 - floor((709 * hMonth) / 24.0)).toInt()
        val hYear = (30 * n + j - 30).toInt()

        val clampedMonth = hMonth.coerceIn(1, 12)
        val clampedDay = hDay.coerceIn(1, 30)

        val monthEn = MONTH_NAMES_EN[clampedMonth - 1]
        val monthAr = MONTH_NAMES_AR[clampedMonth - 1]

        val specialEvent = getSpecialEvent(clampedMonth, clampedDay)
        val isAyyamAlBeed = clampedDay in 13..15
        val isMondayOrThursday = workingCal.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.MONDAY, Calendar.THURSDAY)

        return HijriDate(
            day = clampedDay,
            month = clampedMonth,
            year = hYear,
            monthNameEn = monthEn,
            monthNameAr = monthAr,
            dayOfWeekEn = dayOfWeekEn,
            dayOfWeekAr = dayOfWeekAr,
            specialEvent = specialEvent,
            isSunnahFastDay = isAyyamAlBeed || isMondayOrThursday
        )
    }

    private fun getSpecialEvent(month: Int, day: Int): String? {
        return when {
            month == 1 && day == 1 -> "Islamic New Year (1st Muharram)"
            month == 1 && (day == 9 || day == 10) -> "Day of Ashura (Fasting Sunnah)"
            month == 3 && day == 12 -> "12th Rabi' al-Awwal"
            month == 7 && day == 27 -> "Isra and Mi'raj"
            month == 8 && day == 15 -> "Mid-Sha'ban"
            month == 9 && day == 1 -> "First Day of Ramadan"
            month == 9 && day in 21..30 && day % 2 == 1 -> "Odd Night of Ramadan (Seek Laylat al-Qadr)"
            month == 10 && day == 1 -> "Eid al-Fitr (1st Shawwal)"
            month == 12 && day == 8 -> "Day of Tarwiyah (Hajj begins)"
            month == 12 && day == 9 -> "Day of Arafah (Fasting expiates 2 years of sins)"
            month == 12 && day == 10 -> "Eid al-Adha (10th Dhul-Hijjah)"
            month == 12 && day in 11..13 -> "Days of Tashreeq"
            day in 13..15 -> "White Days Fast (Ayyam al-Beed)"
            else -> null
        }
    }

    fun getUpcomingEvents(adjustmentDays: Int = 0): List<Pair<String, String>> {
        val today = getHijriDate(adjustmentDays = adjustmentDays)
        return listOf(
            "1st Ramadan" to "Start of the blessed month of fasting",
            "Laylat al-Qadr" to "The Night of Decree (Last 10 nights of Ramadan)",
            "Eid al-Fitr" to "1st Shawwal celebration",
            "Day of Arafah" to "9th Dhul-Hijjah - Great day of forgiveness",
            "Eid al-Adha" to "10th Dhul-Hijjah - Feast of Sacrifice",
            "Day of Ashura" to "10th Muharram - Sunnah fast with 9th",
            "Ayyam al-Beed" to "13th, 14th, 15th of each Islamic month"
        )
    }

    fun getArabicDigits(number: Int): String {
        val western = number.toString()
        val arabic = StringBuilder()
        for (ch in western) {
            when (ch) {
                '0' -> arabic.append('٠')
                '1' -> arabic.append('١')
                '2' -> arabic.append('٢')
                '3' -> arabic.append('٣')
                '4' -> arabic.append('٤')
                '5' -> arabic.append('٥')
                '6' -> arabic.append('٦')
                '7' -> arabic.append('٧')
                '8' -> arabic.append('٨')
                '9' -> arabic.append('٩')
                else -> arabic.append(ch)
            }
        }
        return arabic.toString()
    }
}
