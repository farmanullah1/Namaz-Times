package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.design.FrostedCard
import com.example.core.design.InfoBanner
import com.example.core.design.LabeledValue
import com.example.core.design.Pill
import com.example.core.design.PrimaryButton
import com.example.core.design.SecondaryButton
import com.example.core.design.SectionTitle
import com.example.core.design.SettingSwitchRow
import com.example.core.design.SimpleProgressBar
import com.example.core.design.TimeEntryField
import com.example.domain.model.QuranResetRule
import com.example.domain.model.WallClock
import com.example.ui.AppUiState
import com.example.ui.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun QuranScreen(
    state: AppUiState,
    viewModel: AppViewModel
) {
    val snapshot = state.quran
    val settings = state.settings.quran
    var showAddDialog by remember { mutableStateOf(false) }
    var showCorrectDialog by remember { mutableStateOf<LocalDate?>(null) }
    var showSurplusConfirm by remember { mutableStateOf(false) }
    var goalText by remember(settings.dailyGoal) { mutableStateOf(settings.dailyGoal.toString()) }
    var editionTotalText by remember(settings.editionTotalRukus) { mutableStateOf(settings.editionTotalRukus?.toString() ?: "") }
    var editionLabel by remember(settings.editionLabel) { mutableStateOf(settings.editionLabel) }
    var customResetText by remember(settings.customResetTime) { mutableStateOf(WallClock.format(settings.customResetTime) ?: "00:00") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Quran", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Your Ruku ledger — tracked only when you record it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            FrostedCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Pill(snapshot.todayDateKey.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())))
                        Spacer(Modifier.weight(1f))
                        Pill("${snapshot.streak} day streak")
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${snapshot.completedToday}",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            " / ${snapshot.target} Ruku",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    SimpleProgressBar(
                        progress = if (snapshot.target > 0) {
                            snapshot.completedToday.toFloat() / snapshot.target.toFloat()
                        } else {
                            if (snapshot.completedToday > 0) 1f else 0f
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        LabeledValue("Remaining today", "${snapshot.remainingToday}")
                        LabeledValue("Backlog", "${snapshot.backlog}")
                        LabeledValue("Surplus credit", "${snapshot.surplusCredit}")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        LabeledValue("Total Rukus", "${snapshot.totalCompleted}")
                        LabeledValue("Overall", snapshot.completionPercent?.let { "$it%" } ?: "Not set")
                    }
                    Text(
                        snapshot.resetExplanation,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            FrostedCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Record reading", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3).forEach { count ->
                            SecondaryButton("+$count", onClick = {
                                viewModel.addQuranRukus(count, null, null, "")
                            }, modifier = Modifier.weight(1f))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("Add with details", onClick = { showAddDialog = true }, modifier = Modifier.weight(1f))
                        SecondaryButton("Undo last", onClick = { viewModel.undoLastRuku() }, modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = { showCorrectDialog = snapshot.todayDateKey }) {
                        Text("Correct today's total")
                    }
                    InfoBanner(
                        "Entries are append-only. A correction is saved as its own audited entry, so a day is never double-counted."
                    )
                }
            }
        }

        if (state.quranSessionsToday.isNotEmpty()) {
            item { SectionTitle("Today's entries") }
            items(state.quranSessionsToday, key = { it.id }) { session ->
                FrostedCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${session.rukus} Ruku" + (session.surah?.let { " · $it" } ?: ""),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                buildList {
                                    session.rukuNumber?.let { add("Ruku $it") }
                                    if (session.note.isNotBlank()) add(session.note)
                                    session.correctionReason?.let { add("Correction: $it") }
                                }.joinToString(" · ").ifBlank { "No details" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (session.isCorrection) Pill("Correction")
                    }
                }
            }
        }

        item { SectionTitle("Settings") }
        item {
            FrostedCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Daily goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = goalText,
                            onValueChange = { goalText = it.filter { char -> char.isDigit() }.take(2) },
                            label = { Text("Rukus per day") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton("Save", onClick = {
                            val goal = goalText.toIntOrNull()
                            if (goal == null || goal !in 0..50) {
                                goalText = settings.dailyGoal.toString()
                                return@SecondaryButton
                            }
                            viewModel.updateQuranSettings(settings.copy(dailyGoal = goal))
                        }, modifier = Modifier.weight(0.6f))
                    }
                    Text(
                        "A goal change applies from the next reading day, so today's target does not move under you.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SettingSwitchRow(
                        title = "Carry surplus credit",
                        subtitle = "When enabled, reading above the target reduces future days' targets. Backlog still accrues for missed days.",
                        checked = settings.surplusCreditEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !settings.surplusCreditEnabled) showSurplusConfirm = true
                            else viewModel.setQuranSurplus(false)
                        }
                    )

                    Text("Reading day starts at", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        QuranResetRule.entries.forEach { rule ->
                            FilterChip(
                                selected = settings.resetRule == rule,
                                onClick = { viewModel.updateQuranSettings(settings.copy(resetRule = rule)) },
                                label = { Text(rule.title, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    if (settings.resetRule == QuranResetRule.CUSTOM_TIME) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeEntryField("Reset time", customResetText, { customResetText = it ?: "00:00" }, modifier = Modifier.weight(1f))
                            SecondaryButton("Apply", onClick = {
                                val parsed = WallClock.parse(customResetText) ?: return@SecondaryButton
                                viewModel.setQuranCustomResetTime(parsed)
                            }, modifier = Modifier.weight(0.5f))
                        }
                    }

                    Text("Edition progress", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editionTotalText,
                            onValueChange = { editionTotalText = it.filter { char -> char.isDigit() }.take(4) },
                            label = { Text("Total Rukus") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editionLabel,
                            onValueChange = { editionLabel = it },
                            label = { Text("Edition label") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    SecondaryButton("Save edition", onClick = {
                        val total = editionTotalText.toIntOrNull()?.takeIf { it > 0 }
                        viewModel.setQuranEditionTotal(total, editionLabel.trim())
                    })
                }
            }
        }

        if (snapshot.recentDays.isNotEmpty()) {
            item { SectionTitle("Recent reading days") }
            items(snapshot.recentDays, key = { it.dateKey }) { day ->
                FrostedCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                day.dateKey.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${day.completedRukus} read · target ${day.requiredTarget}" +
                                    if (day.backlogAfterClose > 0) " · backlog ${day.backlogAfterClose}" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (day.targetSatisfied) Pill("Met") else Pill("Missed")
                        TextButton(onClick = { showCorrectDialog = day.dateKey }) { Text("Correct") }
                    }
                }
            }
        }

        item {
            InfoBanner(
                "Backlog is every missed Ruku since tracking began. It is shown honestly and is never silently cleared " +
                    "or deducted from another day's reading."
            )
        }
    }

    if (showAddDialog) {
        AddReadingDialog(
            onDismiss = { showAddDialog = false },
            onSave = { count, surah, rukuNumber, note ->
                viewModel.addQuranRukus(count, surah, rukuNumber, note)
                showAddDialog = false
            }
        )
    }

    showCorrectDialog?.let { date ->
        CorrectDayDialog(
            date = date,
            currentTotal = if (date == snapshot.todayDateKey) {
                snapshot.completedToday
            } else {
                snapshot.recentDays.firstOrNull { it.dateKey == date }?.completedRukus ?: 0
            },
            onDismiss = { showCorrectDialog = null },
            onSave = { total, reason ->
                viewModel.correctQuranDay(date, total, reason)
                showCorrectDialog = null
            }
        )
    }

    if (showSurplusConfirm) {
        AlertDialog(
            onDismissRequest = { showSurplusConfirm = false },
            title = { Text("Enable surplus credit?") },
            text = {
                Text(
                    "Reading above your daily target will reduce the target on later days until the credit is used. " +
                        "Missed days still add backlog. This does not delete any recorded Ruku."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setQuranSurplus(true)
                    showSurplusConfirm = false
                }) { Text("Enable") }
            },
            dismissButton = { TextButton(onClick = { showSurplusConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AddReadingDialog(
    onDismiss: () -> Unit,
    onSave: (Int, String?, Int?, String) -> Unit
) {
    var countText by remember { mutableStateOf("1") }
    var surah by remember { mutableStateOf("") }
    var rukuText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Quran reading") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { char -> char.isDigit() }.take(2) },
                    label = { Text("Rukus read") },
                    singleLine = true
                )
                OutlinedTextField(value = surah, onValueChange = { surah = it }, label = { Text("Surah (optional)") }, singleLine = true)
                OutlinedTextField(
                    value = rukuText,
                    onValueChange = { rukuText = it.filter { char -> char.isDigit() }.take(3) },
                    label = { Text("Ruku number (optional)") },
                    singleLine = true
                )
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val count = countText.toIntOrNull() ?: return@TextButton
                onSave(count, surah.trim().takeIf { it.isNotEmpty() }, rukuText.toIntOrNull(), note.trim())
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CorrectDayDialog(
    date: LocalDate,
    currentTotal: Int,
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit
) {
    var totalText by remember { mutableStateOf(currentTotal.toString()) }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Correct ${date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Recorded total: $currentTotal Ruku. Enter the corrected total for this day.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { totalText = it.filter { char -> char.isDigit() }.take(3) },
                    label = { Text("Corrected total") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (required)") },
                    minLines = 2
                )
                error?.let { InfoBanner(it, isWarning = true) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val total = totalText.toIntOrNull()
                if (total == null || total !in 0..100) {
                    error = "Enter a total between 0 and 100."
                    return@TextButton
                }
                if (reason.isBlank()) {
                    error = "A correction needs a short reason."
                    return@TextButton
                }
                onSave(total, reason.trim())
            }) { Text("Save correction") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
