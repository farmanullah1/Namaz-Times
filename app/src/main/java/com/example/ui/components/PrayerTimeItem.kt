package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PrayerItem
import com.example.data.model.PrayerName
import com.example.ui.theme.KaabaGold

@Composable
fun PrayerTimeItem(
    prayer: PrayerItem,
    onTestNotificationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNext = prayer.isNext
    val isCurrent = prayer.isCurrent

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isNext -> KaabaGold
            isCurrent -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        label = "prayer_card_border"
    )

    val containerColor = when {
        isNext -> MaterialTheme.colorScheme.surfaceVariant
        isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isNext || isCurrent) 1.5.dp else 1.dp,
                color = if (isNext || isCurrent) cardBorderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("prayer_item_${prayer.name.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isNext -> KaabaGold.copy(alpha = 0.18f)
                            isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPrayerIcon(prayer.name),
                    contentDescription = prayer.name.title,
                    tint = when {
                        isNext -> KaabaGold
                        isCurrent -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Prayer Names & Badges
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = prayer.name.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isNext || isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    if (isNext) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = KaabaGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "NEXT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = KaabaGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "NOW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = prayer.name.arabicTitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Azan & Iqamah Times
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Azan Time
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Azan",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = prayer.azanTime,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                // Iqamah Time (for Salat)
                if (prayer.name.isSalat && prayer.iqamahTime != "-") {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isNext) KaabaGold.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Iqamah",
                                tint = if (isNext) KaabaGold else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Iqamah",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isNext) KaabaGold else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Text(
                            text = prayer.iqamahTime,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isNext) KaabaGold else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                // Bell / Notification preview
                IconButton(
                    onClick = onTestNotificationClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Test Notification for ${prayer.name.title}",
                        tint = if (isNext) KaabaGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun getPrayerIcon(name: PrayerName): ImageVector {
    return when (name) {
        PrayerName.FAJR -> Icons.Default.WbTwilight
        PrayerName.SUNRISE -> Icons.Default.Brightness5
        PrayerName.DHUHR -> Icons.Default.WbSunny
        PrayerName.ASR -> Icons.Default.Brightness7
        PrayerName.MAGHRIB -> Icons.Default.BrightnessMedium
        PrayerName.ISHA -> Icons.Default.NightlightRound
        PrayerName.TAHAJJUD -> Icons.Default.Bedtime
    }
}
