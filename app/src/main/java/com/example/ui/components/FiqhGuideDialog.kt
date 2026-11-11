package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.AppColors
import com.example.core.design.Pill

@Composable
fun FiqhGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = AppColors.Gold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Ahl al-Hadith Fiqh Guide",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "This application is tuned to the Ahl al-Hadith methodology (strict adherence to authentic Quran and Sahih Sunnah). Below are foundational proofs for prayer timings and rulings:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Item 1: Asr Time
                FiqhProofCard(
                    topic = "Asr Timing (Mithl-1)",
                    arabic = "وَصَلَّى بِيَ الْعَصْرَ حِينَ كَانَ ظِلِّ كُلِّ شَيْءٍ مِثْلَهُ",
                    explanation = "The Asr prayer begins when an object's shadow equals its length (Mithl-1, standard Shafi'i/Maliki/Hanbali/Ahl al-Hadith calculation), in accordance with the Hadith of Angel Jibreel leading the Prophet (ﷺ).",
                    reference = "Sunan Abi Dawud 393 · Jami' at-Tirmidhi 149 (Sahih)"
                )

                // Item 2: Raf' al-Yadayn
                FiqhProofCard(
                    topic = "Raf' al-Yadayn (Raising the Hands)",
                    arabic = "كَانَ إِذَا قَامَ إِلَى الصَّلاَةِ رَفَعَ يَدَيْهِ حَتَّى يَكُونَا حَذْوَ مَنْكِبَيْهِ ثُمَّ يُكَبِّرُ",
                    explanation = "Raising both hands to shoulder or ear level at Takbeeratul Ihram, before going into Ruku, and upon standing up from Ruku saying 'Sami' Allahu liman hamidah'.",
                    reference = "Sahih al-Bukhari 735 · Sahih Muslim 390 (Mutawatir)"
                )

                // Item 3: Ameen bil-Jahr
                FiqhProofCard(
                    topic = "Ameen bil-Jahr (Reciting Ameen Aloud)",
                    arabic = "إِذَا أَمَّنَ الإِمَامُ فَأَمِّنُوا فَإِنَّهُ مَنْ وَافَقَ تَأْمِينُهُ تَأْمِينَ الْمَلاَئِكَةِ غُفِرَ لَهُ مَا تَقَدَّمَ مِنْ ذَنْبِهِ",
                    explanation = "Saying Ameen aloud with the Imam in audible prayers (Fajr, Maghrib, Isha, Jumu'ah) when the Imam completes Surah Al-Fatihah.",
                    reference = "Sahih al-Bukhari 780 · Sahih Muslim 410"
                )

                // Item 4: Combining Prayers (Jam')
                FiqhProofCard(
                    topic = "Combining Prayers during Travel or Rain",
                    arabic = "جَمَعَ رَسُولُ اللَّهِ صلى الله عليه وسلم بَيْنَ الظُّهْرِ وَالْعَصْرِ وَالْمَغْرِبِ وَالْعِشَاءِ",
                    explanation = "Combining Dhuhr with Asr, and Maghrib with Isha (as Jam' Taqdim or Ta'khir) is a Sunnah concession during travel, torrential rain, or severe illness.",
                    reference = "Sahih Muslim 705 · Sunan Abi Dawud 1210"
                )

                // Item 5: Sunnah Fasting
                FiqhProofCard(
                    topic = "Sunnah Fasting Days",
                    arabic = "تُعْرَضُ الأَعْمَالُ يَوْمَ الاِثْنَيْنِ وَالْخَمِيسِ فَأُحِبُّ أَنْ يُعْرَضَ عَمَلِي وَأَنَا صَائِمٌ",
                    explanation = "Fasting Mondays and Thursdays when deeds are presented, and Ayyam al-Beed (13th, 14th, 15th of the Hijri month). The app displays reminders for these dates.",
                    reference = "Jami' at-Tirmidhi 747 · Sunan an-Nasa'i 2420 (Sahih)"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun FiqhProofCard(
    topic: String,
    arabic: String,
    explanation: String,
    reference: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Gold.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    topic,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Pill("Sunnah", color = AppColors.Gold)
            }
            Text(
                arabic,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Gold,
                    lineHeight = 22.sp
                )
            )
            Text(
                explanation,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "— $reference",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic
            )
        }
    }
}
