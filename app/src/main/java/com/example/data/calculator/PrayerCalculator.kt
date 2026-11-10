package com.example.data.calculator

import com.example.data.model.CalculationMethod
import com.example.data.model.FiqhSchool
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Astronomical prayer time calculator implementing standard solar coordinates.
 * Explicitly supports Ahl al-Hadith (Standard Asr shadow factor 1.0) and custom mosque schedules.
 */
object PrayerCalculator {

    data class CalculatedTimes(
        val fajr: Double,     // Hours in local day (e.g. 5.25 = 05:15)
        val sunrise: Double,
        val dhuhr: Double,
        val asr: Double,
        val sunset: Double,
        val maghrib: Double,
        val isha: Double,
        val tahajjudEnd: Double
    )

    fun calculate(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        timezoneOffsetHours: Double = (calendar.timeZone.rawOffset + calendar.timeZone.dstSavings) / 3600000.0,
        fiqh: FiqhSchool = FiqhSchool.AHL_AL_HADITH,
        method: CalculationMethod = CalculationMethod.KARACHI
    ): CalculatedTimes {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val jd = julianDate(year, month, day) - (longitude / (15.0 * 24.0))

        // Solar coordinates
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(degToRad(g)) + 0.020 * sin(degToRad(2 * g)))

        val e = 23.439 - 0.00000036 * d
        val ra = radToDeg(atan2(cos(degToRad(e)) * sin(degToRad(l)), cos(degToRad(l)))) / 15.0
        val rightAscension = fixHour(ra)

        val declination = radToDeg(Math.asin(sin(degToRad(e)) * sin(degToRad(l))))
        val eqt = q / 15.0 - fixHour(rightAscension)

        // Midday (Dhuhr)
        val noon = fixHour(12.0 + timezoneOffsetHours - (longitude / 15.0) - eqt)

        // Sunrise & Sunset angle (accounting for refraction & solar diameter: 0.833 deg)
        val sunAlt = 0.833
        val sunriseDiff = sunAngleTime(sunAlt, latitude, declination)
        val sunrise = fixHour(noon - sunriseDiff)
        val sunset = fixHour(noon + sunriseDiff)

        // Fajr (Dawn angle from method)
        val fajrDiff = sunAngleTime(method.fajrAngle, latitude, declination)
        val fajr = fixHour(noon - fajrDiff)

        // Asr calculation (Ahl al-Hadith: factor 1.0; Hanafi: factor 2.0)
        val asrFactor = fiqh.shadowFactor
        val asrAngle = radToDeg(atan(1.0 / (asrFactor + tan(degToRad(abs(latitude - declination))))))
        val asrDiff = sunAltitudeDiff(asrAngle, latitude, declination)
        val asr = fixHour(noon + asrDiff)

        // Maghrib = Sunset
        val maghrib = sunset

        // Isha
        val ishaDiff = sunAngleTime(method.ishaAngle, latitude, declination)
        val isha = fixHour(noon + ishaDiff)

        // Tahajjud: last third of the night
        // Night duration = (Fajr tomorrow + 24 - Maghrib today)
        val nightDuration = (fajr + 24.0) - maghrib
        val tahajjudStart = fixHour(maghrib + (nightDuration * (2.0 / 3.0)))

        return CalculatedTimes(
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = noon,
            asr = asr,
            sunset = sunset,
            maghrib = maghrib,
            isha = isha,
            tahajjudEnd = tahajjudStart
        )
    }

    private fun sunAngleTime(angle: Double, lat: Double, dec: Double): Double {
        val cosDiff = (-sin(degToRad(angle)) - sin(degToRad(lat)) * sin(degToRad(dec))) /
                (cos(degToRad(lat)) * cos(degToRad(dec)))
        val clamped = cosDiff.coerceIn(-1.0, 1.0)
        return radToDeg(acos(clamped)) / 15.0
    }

    private fun sunAltitudeDiff(altAngle: Double, lat: Double, dec: Double): Double {
        val sinAlt = sin(degToRad(altAngle))
        val cosDiff = (sinAlt - sin(degToRad(lat)) * sin(degToRad(dec))) /
                (cos(degToRad(lat)) * cos(degToRad(dec)))
        val clamped = cosDiff.coerceIn(-1.0, 1.0)
        return radToDeg(acos(clamped)) / 15.0
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    fun formatHour(hourFraction: Double): String {
        val totalMinutes = (fixHour(hourFraction) * 60).toInt()
        val h = (totalMinutes / 60) % 24
        val m = totalMinutes % 60
        return String.format("%02d:%02d", h, m)
    }

    fun addMinutesToTime(timeStr: String, minutesToAdd: Int): String {
        val parts = timeStr.split(":")
        if (parts.size != 2) return timeStr
        val h = parts[0].toIntOrNull() ?: return timeStr
        val m = parts[1].toIntOrNull() ?: return timeStr
        var totalMin = (h * 60 + m + minutesToAdd) % (24 * 60)
        if (totalMin < 0) totalMin += 24 * 60
        val newH = totalMin / 60
        val newM = totalMin % 60
        return String.format("%02d:%02d", newH, newM)
    }

    fun timeStringToEpochMillis(timeStr: String, targetCalendar: Calendar = Calendar.getInstance()): Long {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val cal = targetCalendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, m)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun degToRad(deg: Double): Double = deg * Math.PI / 180.0
    private fun radToDeg(rad: Double): Double = rad * 180.0 / Math.PI
    private fun fixHour(hour: Double): Double {
        var h = hour - 24.0 * floor(hour / 24.0)
        if (h < 0) h += 24.0
        return h
    }
    private fun fixAngle(deg: Double): Double {
        var a = deg - 360.0 * floor(deg / 360.0)
        if (a < 0) a += 360.0
        return a
    }
}
