package com.example.data.model

import com.example.data.db.MosqueLocation

enum class PrayerName(
    val title: String,
    val arabicTitle: String,
    val isSalat: Boolean = true
) {
    FAJR("Fajr", "الفجر"),
    SUNRISE("Sunrise", "الشروق", isSalat = false),
    DHUHR("Dhuhr", "الظهر"),
    ASR("Asr", "العصر"),
    MAGHRIB("Maghrib", "المغرب"),
    ISHA("Isha", "العشاء"),
    TAHAJJUD("Tahajjud / Qiyam", "التهجد", isSalat = false)
}

enum class FiqhSchool(val title: String, val shadowFactor: Double, val description: String) {
    AHL_AL_HADITH(
        "Ahl al-Hadith (Standard)",
        1.0,
        "Asr begins when shadow equals object length (Shafi'i, Maliki, Hanbali, Ahl al-Hadith)"
    ),
    HANAFI(
        "Hanafi",
        2.0,
        "Asr begins when shadow equals twice object length"
    )
}

enum class CalculationMethod(
    val title: String,
    val fajrAngle: Double,
    val ishaAngle: Double
) {
    KARACHI("Karachi (Univ. of Islamic Sciences)", 18.0, 18.0),
    MWL("Muslim World League (MWL)", 18.0, 17.0),
    UMM_AL_QURA("Umm al-Qura, Makkah", 18.5, 19.0),
    EGYPT("Egyptian General Authority", 19.5, 17.5),
    ISNA("Islamic Society of North America", 15.0, 15.0)
}

data class PrayerItem(
    val name: PrayerName,
    val azanTime: String,
    val iqamahTime: String,
    val azanEpochMillis: Long,
    val iqamahEpochMillis: Long,
    val isNext: Boolean = false,
    val isCurrent: Boolean = false,
    val isNotificationEnabled: Boolean = true
)

data class DayPrayerSchedule(
    val mosque: MosqueLocation,
    val prayers: List<PrayerItem>,
    val currentPrayer: PrayerItem?,
    val nextPrayer: PrayerItem?,
    val timeRemainingMillis: Long,
    val progress: Float, // 0.0 to 1.0 progress through current prayer window
    val hijriDate: String,
    val hijriDateArabic: String,
    val gregorianDate: String,
    val qiblaBearing: Double,
    val distanceToMakkahKm: Double
)
