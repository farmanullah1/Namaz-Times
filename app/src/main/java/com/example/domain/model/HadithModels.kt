package com.example.domain.model

import java.time.LocalDate

data class DailyHadith(
    val id: Int,
    val arabicText: String,
    val englishTranslation: String,
    val narrator: String,
    val source: String,
    val topic: String
)

object HadithRepository {

    private val AHADITH = listOf(
        DailyHadith(
            id = 1,
            arabicText = "إِنَّ أَوَّلَ مَا يُحَاسَبُ بِهِ الْعَبْدُ يَوْمَ الْقِيَامَةِ مِنْ عَمَلِهِ صَلَاتُهُ",
            englishTranslation = "The first thing for which a person will be brought to account on the Day of Resurrection will be their prayer.",
            narrator = "Abu Hurairah (may Allah be pleased with him)",
            source = "Sunan an-Nasa'i, Sahih by al-Albani",
            topic = "The Virtue of Salah"
        ),
        DailyHadith(
            id = 2,
            arabicText = "مَنْ تَوَضَّأَ نَحْوَ وُضُوئِي هَذَا ثُمَّ صَلَّى رَكْعَتَيْنِ لاَ يُحَدِّثُ فِيهِمَا نَفْسَهُ غُفِرَ لَهُ مَا تَقَدَّمَ مِنْ ذَنْبِهِ",
            englishTranslation = "Whoever performs ablution like this ablution of mine and then offers two rak'ahs without thinking of worldly matters, all his previous sins will be forgiven.",
            narrator = "Uthman ibn Affan (may Allah be pleased with him)",
            source = "Sahih al-Bukhari 159",
            topic = "Purification and Khushu"
        ),
        DailyHadith(
            id = 3,
            arabicText = "صَلَاةُ الْجَمَاعَةِ تَفْضُلُ صَلَاةَ الْفَذِّ بِسَبْعٍ وَعِشْرِينَ دَرَجَةً",
            englishTranslation = "Prayer in congregation is twenty-seven times more rewarding than prayer performed alone.",
            narrator = "Abdullah ibn Umar (may Allah be pleased with him)",
            source = "Sahih al-Bukhari 645, Sahih Muslim 650",
            topic = "Congregational Prayer"
        ),
        DailyHadith(
            id = 4,
            arabicText = "مَنْ صَلَّى الْبَرْدَيْنِ دَخَلَ الْجَنَّةَ",
            englishTranslation = "Whoever prays the two cool prayers (Fajr and Asr) will enter Paradise.",
            narrator = "Abu Musa al-Ash'ari (may Allah be pleased with him)",
            source = "Sahih al-Bukhari 574, Sahih Muslim 635",
            topic = "Fajr and Asr Prayers"
        ),
        DailyHadith(
            id = 5,
            arabicText = "مَنْ صَلَّى الصُّبْحَ فَهُوَ فِي ذِمَّةِ اللَّهِ",
            englishTranslation = "Whoever prays the dawn prayer (Fajr) is under the covenant and protection of Allah.",
            narrator = "Jundub ibn Abdullah (may Allah be pleased with him)",
            source = "Sahih Muslim 657",
            topic = "Protection through Fajr"
        ),
        DailyHadith(
            id = 6,
            arabicText = "أَحَبُّ الْأَعْمَالِ إِلَى اللَّهِ الصَّلَاةُ عَلَى وَقْتِهَا",
            englishTranslation = "The deeds most beloved to Allah is prayer offered at its proper appointed time.",
            narrator = "Abdullah ibn Mas'ud (may Allah be pleased with him)",
            source = "Sahih al-Bukhari 527, Sahih Muslim 85",
            topic = "Timeliness in Prayer"
        ),
        DailyHadith(
            id = 7,
            arabicText = "أَقْرَبُ مَا يَكُونُ الْعَبْدُ مِنْ رَبِّهِ وَهُوَ سَاجِدٌ فَأَكْثِرُوا الدُّعَاءَ",
            englishTranslation = "The nearest a servant comes to his Lord is while he is prostrating, so supplicate much therein.",
            narrator = "Abu Hurairah (may Allah be pleased with him)",
            source = "Sahih Muslim 482",
            topic = "Prostration and Supplication"
        ),
        DailyHadith(
            id = 8,
            arabicText = "مَنْ صَامَ ثَلَاثَةَ أَيَّامٍ مِنْ كُلِّ شَهْرٍ فَقَدْ صَامَ الدَّهْرَ كُلَّهُ",
            englishTranslation = "Whoever fasts three days of every month (Ayyam al-Beed: 13th, 14th, 15th), it is like fasting an entire lifetime.",
            narrator = "Abu Dharr (may Allah be pleased with him)",
            source = "Sunan an-Nasa'i, Sahih",
            topic = "Sunnah Fasting"
        )
    )

    fun forDate(date: LocalDate = LocalDate.now()): DailyHadith {
        val index = (date.dayOfYear - 1).coerceAtLeast(0) % AHADITH.size
        return AHADITH[index]
    }
}
