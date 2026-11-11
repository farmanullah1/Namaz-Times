package com.example.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SurahInfo(
    val number: Int,
    val nameEnglish: String,
    val nameArabic: String,
    val rukusCount: Int,
    val startGlobalRuku: Int,
    val juzNumber: Int
)

data class JuzInfo(
    val number: Int,
    val nameArabic: String,
    val startSurah: Int,
    val startRuku: Int,
    val totalRukus: Int
)

object QuranStructureData {
    const val TOTAL_RUKUS = 558
    const val TOTAL_SURAHS = 114
    const val TOTAL_JUZ = 30

    val surahs: List<SurahInfo> = buildList {
        val raw = listOf(
            Triple(1, "Al-Fatihah", "الفاتحة") to Pair(1, 1),
            Triple(2, "Al-Baqarah", "البقرة") to Pair(40, 1),
            Triple(3, "Ali 'Imran", "آل عمران") to Pair(20, 3),
            Triple(4, "An-Nisa", "النساء") to Pair(24, 4),
            Triple(5, "Al-Ma'idah", "المائدة") to Pair(16, 6),
            Triple(6, "Al-An'am", "الأنعام") to Pair(20, 7),
            Triple(7, "Al-A'raf", "الأعراف") to Pair(24, 8),
            Triple(8, "Al-Anfal", "الأنفال") to Pair(10, 9),
            Triple(9, "At-Tawbah", "التوبة") to Pair(16, 10),
            Triple(10, "Yunus", "يونس") to Pair(11, 11),
            Triple(11, "Hud", "هود") to Pair(10, 11),
            Triple(12, "Yusuf", "يوسف") to Pair(12, 12),
            Triple(13, "Ar-Ra'd", "الرعد") to Pair(6, 13),
            Triple(14, "Ibrahim", "إبراهيم") to Pair(7, 13),
            Triple(15, "Al-Hijr", "الحجر") to Pair(6, 14),
            Triple(16, "An-Nahl", "النحل") to Pair(16, 14),
            Triple(17, "Al-Isra", "الإسراء") to Pair(12, 15),
            Triple(18, "Al-Kahf", "الكهف") to Pair(12, 15),
            Triple(19, "Maryam", "مريم") to Pair(6, 16),
            Triple(20, "Taha", "طه") to Pair(8, 16),
            Triple(21, "Al-Anbiya", "الأنبياء") to Pair(7, 17),
            Triple(22, "Al-Hajj", "الحج") to Pair(10, 17),
            Triple(23, "Al-Mu'minun", "المؤمنون") to Pair(6, 18),
            Triple(24, "An-Nur", "النور") to Pair(9, 18),
            Triple(25, "Al-Furqan", "الفرقان") to Pair(6, 18),
            Triple(26, "Ash-Shu'ara", "الشعراء") to Pair(11, 19),
            Triple(27, "An-Naml", "النمل") to Pair(7, 19),
            Triple(28, "Al-Qasas", "القصص") to Pair(9, 20),
            Triple(29, "Al-'Ankabut", "العنكبوت") to Pair(7, 20),
            Triple(30, "Ar-Rum", "الروم") to Pair(6, 21),
            Triple(31, "Luqman", "لقمان") to Pair(3, 21),
            Triple(32, "As-Sajdah", "السجدة") to Pair(3, 21),
            Triple(33, "Al-Ahzab", "الأحزاب") to Pair(9, 21),
            Triple(34, "Saba", "سبأ") to Pair(6, 22),
            Triple(35, "Fatir", "فاطر") to Pair(5, 22),
            Triple(36, "Ya-Sin", "يس") to Pair(5, 22),
            Triple(37, "As-Saffat", "الصافات") to Pair(5, 23),
            Triple(38, "Sad", "ص") to Pair(5, 23),
            Triple(39, "Az-Zumar", "الزمر") to Pair(8, 23),
            Triple(40, "Ghafir", "غافر") to Pair(9, 24),
            Triple(41, "Fussilat", "فصلت") to Pair(6, 24),
            Triple(42, "Ash-Shura", "الشورى") to Pair(5, 25),
            Triple(43, "Az-Zukhruf", "الزخرف") to Pair(7, 25),
            Triple(44, "Ad-Dukhan", "الدخان") to Pair(3, 25),
            Triple(45, "Al-Jathiyah", "الجاثية") to Pair(4, 25),
            Triple(46, "Al-Ahqaf", "الأحقاف") to Pair(4, 26),
            Triple(47, "Muhammad", "محمد") to Pair(4, 26),
            Triple(48, "Al-Fath", "الفتح") to Pair(4, 26),
            Triple(49, "Al-Hujurat", "الحجرات") to Pair(2, 26),
            Triple(50, "Qaf", "ق") to Pair(3, 26),
            Triple(51, "Adh-Dhariyat", "الذاريات") to Pair(3, 26),
            Triple(52, "At-Tur", "الطور") to Pair(2, 27),
            Triple(53, "An-Najm", "النجم") to Pair(3, 27),
            Triple(54, "Al-Qamar", "القمر") to Pair(3, 27),
            Triple(55, "Ar-Rahman", "الرحمن") to Pair(3, 27),
            Triple(56, "Al-Waqi'ah", "الواقعة") to Pair(3, 27),
            Triple(57, "Al-Hadid", "الحديد") to Pair(4, 27),
            Triple(58, "Al-Mujadila", "المجادلة") to Pair(3, 28),
            Triple(59, "Al-Hashr", "الحشر") to Pair(3, 28),
            Triple(60, "Al-Mumtahanah", "الممتحنة") to Pair(2, 28),
            Triple(61, "As-Saff", "الصف") to Pair(2, 28),
            Triple(62, "Al-Jumu'ah", "الجمعة") to Pair(2, 28),
            Triple(63, "Al-Munafiqun", "المنافقون") to Pair(2, 28),
            Triple(64, "At-Taghabun", "التغابن") to Pair(2, 28),
            Triple(65, "At-Talaq", "الطلاق") to Pair(2, 28),
            Triple(66, "At-Tahrim", "التحريم") to Pair(2, 28),
            Triple(67, "Al-Mulk", "الملك") to Pair(2, 29),
            Triple(68, "Al-Qalam", "القلم") to Pair(2, 29),
            Triple(69, "Al-Haqqah", "الحاقة") to Pair(2, 29),
            Triple(70, "Al-Ma'arij", "المعارج") to Pair(2, 29),
            Triple(71, "Nuh", "نوح") to Pair(2, 29),
            Triple(72, "Al-Jinn", "الجن") to Pair(2, 29),
            Triple(73, "Al-Muzzammil", "المزمل") to Pair(2, 29),
            Triple(74, "Al-Muddaththir", "المدثر") to Pair(2, 29),
            Triple(75, "Al-Qiyamah", "القيامة") to Pair(2, 29),
            Triple(76, "Al-Insan", "الإنسان") to Pair(2, 29),
            Triple(77, "Al-Mursalat", "المرسلات") to Pair(2, 29),
            Triple(78, "An-Naba", "النبأ") to Pair(2, 30),
            Triple(79, "An-Nazi'at", "النازعات") to Pair(2, 30),
            Triple(80, "'Abasa", "عبس") to Pair(1, 30),
            Triple(81, "At-Takwir", "التكوير") to Pair(1, 30),
            Triple(82, "Al-Infitar", "الانفطار") to Pair(1, 30),
            Triple(83, "Al-Mutaffifin", "المطففين") to Pair(1, 30),
            Triple(84, "Al-Inshiqaq", "الانشقاق") to Pair(1, 30),
            Triple(85, "Al-Buruj", "البروج") to Pair(1, 30),
            Triple(86, "At-Tariq", "الطارق") to Pair(1, 30),
            Triple(87, "Al-A'la", "الأعلى") to Pair(1, 30),
            Triple(88, "Al-Ghashiyah", "الغاشية") to Pair(1, 30),
            Triple(89, "Al-Fajr", "الفجر") to Pair(1, 30),
            Triple(90, "Al-Balad", "البلد") to Pair(1, 30),
            Triple(91, "Ash-Shams", "الشمس") to Pair(1, 30),
            Triple(92, "Al-Layl", "الليل") to Pair(1, 30),
            Triple(93, "Ad-Duha", "الضحى") to Pair(1, 30),
            Triple(94, "Ash-Sharh", "الشرح") to Pair(1, 30),
            Triple(95, "At-Tin", "التين") to Pair(1, 30),
            Triple(96, "Al-'Alaq", "العلق") to Pair(2, 30),
            Triple(97, "Al-Qadr", "القدر") to Pair(1, 30),
            Triple(98, "Al-Bayyinah", "البينة") to Pair(1, 30),
            Triple(99, "Az-Zalzalah", "الزلزلة") to Pair(1, 30),
            Triple(100, "Al-'Adiyat", "العاديات") to Pair(1, 30),
            Triple(101, "Al-Qari'ah", "القارعة") to Pair(1, 30),
            Triple(102, "At-Takathur", "التكاثر") to Pair(1, 30),
            Triple(103, "Al-'Asr", "العصر") to Pair(1, 30),
            Triple(104, "Al-Humazah", "الهمزة") to Pair(1, 30),
            Triple(105, "Al-Fil", "الفيل") to Pair(1, 30),
            Triple(106, "Quraysh", "قريش") to Pair(1, 30),
            Triple(107, "Al-Ma'un", "الماعون") to Pair(1, 30),
            Triple(108, "Al-Kawthar", "الكوثر") to Pair(1, 30),
            Triple(109, "Al-Kafirun", "الكافرون") to Pair(1, 30),
            Triple(110, "An-Nasr", "النصر") to Pair(1, 30),
            Triple(111, "Al-Masad", "المسد") to Pair(1, 30),
            Triple(112, "Al-Ikhlas", "الإخلاص") to Pair(1, 30),
            Triple(113, "Al-Falaq", "الفلق") to Pair(1, 30),
            Triple(114, "An-Nas", "الناس") to Pair(1, 30)
        )

        var currentGlobalRuku = 1
        raw.forEach { (meta, countAndJuz) ->
            val (num, eng, arab) = meta
            val (count, juz) = countAndJuz
            add(
                SurahInfo(
                    number = num,
                    nameEnglish = eng,
                    nameArabic = arab,
                    rukusCount = count,
                    startGlobalRuku = currentGlobalRuku,
                    juzNumber = juz
                )
            )
            currentGlobalRuku += count
        }
    }

    val juzList: List<JuzInfo> = (1..30).map { juzNum ->
        val surahsInJuz = surahs.filter { it.juzNumber == juzNum }
        val startSurah = surahsInJuz.firstOrNull()?.number ?: 1
        val startRuku = surahsInJuz.firstOrNull()?.startGlobalRuku ?: 1
        val totalRukus = surahsInJuz.sumOf { it.rukusCount }
        val arabicNames = listOf(
            "الم", "سيقول", "تلك الرسل", "لن تنالوا", "والمحصنات", "لا يحب الله",
            "وإذا سمعوا", "ولو أننا", "قال الملأ", "واعلموا", "يعتذرون", "وما من دابة",
            "وما أبرئ", "ربما", "سبحان الذي", "قال ألم", "اقترب للناس", "قد أفلح",
            "وقال الذين", "أمن خلق", "اتل ما أوحي", "ومن يقنت", "وما لي", "فمن أظلم",
            "إليه يرد", "حم", "قال فما خطبكم", "قد سمع الله", "تبارك الذي", "عم يتساءلون"
        )
        JuzInfo(
            number = juzNum,
            nameArabic = arabicNames.getOrElse(juzNum - 1) { "الجزء $juzNum" },
            startSurah = startSurah,
            startRuku = startRuku,
            totalRukus = totalRukus
        )
    }

    /**
     * Maps a global ruku index (1..558) to its Surah and 1-indexed Ruku within that Surah.
     */
    fun findSurahForGlobalRuku(globalIndex: Int): Pair<SurahInfo, Int> {
        val bounded = globalIndex.coerceIn(1, TOTAL_RUKUS)
        val surah = surahs.lastOrNull { it.startGlobalRuku <= bounded } ?: surahs.first()
        val rukuInSurah = bounded - surah.startGlobalRuku + 1
        return Pair(surah, rukuInSurah)
    }

    /**
     * Calculates required daily pace given remaining Rukus and a target Khatam date.
     */
    fun calculateRequiredPace(
        remainingRukus: Int,
        fromDate: LocalDate = LocalDate.now(),
        targetDate: LocalDate
    ): Int {
        val daysUntil = ChronoUnit.DAYS.between(fromDate, targetDate).coerceAtLeast(1)
        return kotlin.math.ceil(remainingRukus.toDouble() / daysUntil.toDouble()).toInt().coerceAtLeast(1)
    }
}
