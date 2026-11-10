package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.design.FrostedCard
import com.example.core.design.InfoBanner
import com.example.core.design.NavigationRow
import com.example.core.design.SectionTitle
import com.example.navigation.AppDestination
import com.example.ui.AppUiState
import com.example.ui.AppViewModel

@Composable
fun MoreScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onNavigate: (AppDestination) -> Unit
) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("More", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Settings, history and reliability" + (versionName?.let { " · v$it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            FrostedCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                            FilterChip(
                                selected = state.settings.themeMode == value,
                                onClick = { viewModel.setThemeMode(value) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        item {
            FrostedCard {
                Column {
                    Text("Prayer & timetables", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    NavigationRow(
                        title = "Prayer history",
                        subtitle = "Marked prayers, notes and Qada records",
                        onClick = { onNavigate(AppDestination.History) },
                        trailing = "${state.recentHistory.size}"
                    )
                    NavigationRow(
                        title = "Calculation settings",
                        subtitle = "Method, angles, Asr and minute adjustments",
                        onClick = { onNavigate(AppDestination.CalculationSettings) },
                        trailing = state.settings.calculation.preset.title
                    )
                    NavigationRow(
                        title = "Notifications & Adhan",
                        subtitle = "Reminders, sound, silence and exact alarms",
                        onClick = { onNavigate(AppDestination.NotificationSettings) },
                        trailing = if (state.settings.remindersEnabled) "On" else "Off"
                    )
                    NavigationRow(
                        title = "Hijri calendar",
                        subtitle = "Umm al-Qura estimate with adjustment",
                        onClick = { onNavigate(AppDestination.HijriSettings) }
                    )
                    NavigationRow(
                        title = "Location & automation",
                        subtitle = "Place switching, accuracy and dwell thresholds",
                        onClick = { onNavigate(AppDestination.LocationSettings) },
                        trailing = modeLabel(state.settings)
                    )
                    NavigationRow(
                        title = "Backup & restore",
                        subtitle = "Local JSON export, merge or replace",
                        onClick = { onNavigate(AppDestination.Backup) }
                    )
                    NavigationRow(
                        title = "Reminder reliability",
                        subtitle = "Permissions, battery and last rebuild",
                        onClick = { onNavigate(AppDestination.Reliability) },
                        trailing = if (state.settings.remindersNeedRepair) "Repair" else "OK"
                    )
                }
            }
        }

        item {
            FrostedCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Home screen widgets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Namaz Times adds two widgets from your launcher: Today (next prayer) and Combined (today and next). " +
                            "Long-press your home screen, choose Widgets, then Namaz Times. Widgets show only times already " +
                            "resolved by the app and refresh when the timetable or selection changes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            FrostedCard {
                Column {
                    Text("App", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    NavigationRow(
                        title = "Privacy",
                        subtitle = "No account, ads, analytics or runtime network",
                        onClick = { onNavigate(AppDestination.About) }
                    )
                    NavigationRow(
                        title = "About Namaz Times",
                        subtitle = "Sources, limitations and attribution",
                        onClick = { onNavigate(AppDestination.About) }
                    )
                }
            }
        }

        item {
            InfoBanner(
                "Mosque-entered times are authoritative. Calculated times are always labelled and never presented as " +
                    "the mosque's timetable. Empty Iqamah fields stay empty."
            )
        }
    }
}
