package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.calculator.PrayerCalculator
import com.example.data.hijri.HijriCalendarHelper
import com.example.data.model.CalculationMethod
import com.example.data.model.FiqhSchool
import com.example.data.qibla.QiblaHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Noor Salah", appName)
    }

    @Test
    fun `verify Qibla direction from Karachi to Makkah`() {
        // Karachi coordinates (Defence / Clifton)
        val lat = 24.8016
        val lng = 67.0673

        val bearing = QiblaHelper.calculateQiblaBearing(lat, lng)
        // Karachi Qibla direction is roughly 268 degrees (WSW)
        assertTrue("Bearing should be near 268 degrees: $bearing", bearing in 260.0..275.0)

        val distance = QiblaHelper.calculateDistanceToMakkahKm(lat, lng)
        assertTrue("Distance should be approx 2806 km: $distance", distance in 2700.0..3000.0)
    }

    @Test
    fun `verify Ahl al-Hadith Asr calculation earlier than Hanafi`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JUNE, 15) // Summer solstice sample
        val lat = 24.8016
        val lng = 67.0673

        val ahlHadithTimes = PrayerCalculator.calculate(
            calendar = cal,
            latitude = lat,
            longitude = lng,
            timezoneOffsetHours = 5.0,
            fiqh = FiqhSchool.AHL_AL_HADITH,
            method = CalculationMethod.KARACHI
        )

        val hanafiTimes = PrayerCalculator.calculate(
            calendar = cal,
            latitude = lat,
            longitude = lng,
            timezoneOffsetHours = 5.0,
            fiqh = FiqhSchool.HANAFI,
            method = CalculationMethod.KARACHI
        )

        // In Ahl al-Hadith (1x shadow), Asr starts earlier than Hanafi (2x shadow)
        assertTrue(
            "Ahl al-Hadith Asr should be earlier than Hanafi Asr: ${ahlHadithTimes.asr} vs ${hanafiTimes.asr}",
            ahlHadithTimes.asr < hanafiTimes.asr
        )
    }

    @Test
    fun `verify Hijri date formatting`() {
        val cal = Calendar.getInstance()
        val hijri = HijriCalendarHelper.getHijriDate(cal)
        assertTrue("Hijri month must be valid", hijri.month in 1..12)
        assertTrue("Hijri day must be valid", hijri.day in 1..30)
        assertTrue("Hijri year must be around 1448", hijri.year in 1445..1455)
    }
}
