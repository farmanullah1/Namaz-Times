package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.AppColors
import com.example.core.design.FrostedCard
import com.example.core.design.Pill
import com.example.core.time.formatClock
import com.example.data.local.PrayerHistoryRecord
import com.example.domain.model.PrayerName
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PrayerHistoryHeatmap(
    history: List<PrayerHistoryRecord>,
    zone: ZoneId = ZoneId.systemDefault()
) {
    val today = remember { LocalDate.now() }
    val past30Days = remember(today) {
        (0 until 28).map { today.minusDays(it.toLong()) }
    }
    val groupedByDate = remember(history) {
        history.groupBy { it.trackingDate }
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val totalCompletedIn30Days = remember(history, today) {
        val thirtyDaysAgo = today.minusDays(28)
        history.count { !it.trackingDate.isBefore(thirtyDaysAgo) }
    }
    val completionPercentage = ((totalCompletedIn30Days.toFloat() / (28 * 5).toFloat()) * 100).toInt()

    FrostedCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Monthly Prayer Consistency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Pill("$completionPercentage% (Past 4 Weeks)", color = AppColors.Gold)
            }

            Text(
                "Tap any day to see prayer breakdown",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Day of Week Header: Mon Tue Wed Thu Fri Sat Sun
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4x7 Calendar Heatmap Grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                past30Days.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { date ->
                            val records = groupedByDate[date].orEmpty()
                            val count = records.size.coerceIn(0, 5)

                            val cellColor = when (count) {
                                5 -> AppColors.EmeraldDark
                                4 -> AppColors.Emerald.copy(alpha = 0.5f)
                                3 -> AppColors.Emerald.copy(alpha = 0.35f)
                                1, 2 -> AppColors.Emerald.copy(alpha = 0.18f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            }
                            val isFull = count == 5
                            val isToday = date == today

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = cellColor,
                                border = if (isToday) androidx.compose.foundation.BorderStroke(1.5.dp, AppColors.Gold)
                                else if (isFull) androidx.compose.foundation.BorderStroke(1.dp, AppColors.KaabaGold.copy(alpha = 0.6f))
                                else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .height(44.dp)
                                    .clickable { selectedDate = date }
                            ) {
                                Column(
                                    modifier = Modifier.padding(2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        "${date.dayOfMonth}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isToday || isFull) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isFull) AppColors.Gold else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    // 5 miniature dots for 5 prayers
                                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                        PrayerName.obligatories.forEach { p ->
                                            val prayed = records.any { it.prayer == p }
                                            Box(
                                                modifier = Modifier
                                                    .size(3.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (prayed) AppColors.Gold
                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                listOf(0, 2, 4, 5).forEach { sampleCount ->
                    val color = when (sampleCount) {
                        5 -> AppColors.EmeraldDark
                        4 -> AppColors.Emerald.copy(alpha = 0.5f)
                        2 -> AppColors.Emerald.copy(alpha = 0.25f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                    Spacer(Modifier.width(3.dp))
                }
                Text("5/5 Prayed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // Inspection dialog for a tapped date
    selectedDate?.let { date ->
        val records = groupedByDate[date].orEmpty()
        AlertDialog(
            onDismissRequest = { selectedDate = null },
            title = {
                Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${records.size} of 5 prayers marked",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (records.size == 5) AppColors.Gold else MaterialTheme.colorScheme.primary
                    )
                    PrayerName.obligatories.forEach { prayer ->
                        val record = records.firstOrNull { it.prayer == prayer }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (record != null) AppColors.Gold.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "${prayer.title} · ${prayer.arabicTitle}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (record != null) {
                                        Text(
                                            "Marked at ${formatClock(record.completedAt, zone)} · ${record.context}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (record != null) {
                                    Pill("Prayed", color = AppColors.Gold)
                                } else {
                                    Text(
                                        "Unmarked",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDate = null }) { Text("Close") }
            }
        )
    }
}
