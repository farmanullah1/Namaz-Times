package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.core.design.EmptyState
import com.example.core.design.FrostedCard
import com.example.core.design.InfoBanner
import com.example.core.design.Pill
import com.example.core.design.ScreenHeader
import com.example.core.design.SecondaryButton
import com.example.core.design.SectionTitle
import com.example.core.design.SettingSwitchRow
import com.example.core.time.formatClock
import com.example.domain.model.PrayerName
import com.example.ui.AppUiState
import com.example.ui.AppViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    var showQadaDialog by remember { mutableStateOf(false) }
    var qadaError by remember { mutableStateOf<String?>(null) }
    val grouped = remember(state.recentHistory) {
        state.recentHistory
            .groupBy { it.trackingDate }
            .toSortedMap(compareByDescending { it })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Prayer history",
            subtitle = "Only prayers you marked yourself",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoBanner(
                    "A prayer is recorded only when you mark it. If a prayer is missed, leave it unmarked; " +
                        "nothing is inferred from time passing."
                )
            }

            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingSwitchRow(
                            title = "Track Qada separately",
                            subtitle = "Optional. Adds a separate Qada list; it never changes today's prayer records.",
                            checked = state.settings.qadaEnabled,
                            onCheckedChange = { viewModel.setQadaEnabled(it) }
                        )
                    }
                }
            }

            if (state.settings.qadaEnabled) {
                item {
                    FrostedCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Qada records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(8.dp))
                                Pill("${state.qada.size}")
                            }
                            if (state.qada.isEmpty()) {
                                Text(
                                    "No Qada records. Nothing is added automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            state.qada.forEach { record ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "${record.prayer.title} · ${record.performedDate}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (record.note.isNotBlank()) {
                                            Text(
                                                record.note,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    TextButton(onClick = { viewModel.deleteQada(record.id) }) { Text("Remove") }
                                }
                            }
                            qadaError?.let { InfoBanner(it, isWarning = true) }
                            SecondaryButton("Add Qada record", onClick = { showQadaDialog = true })
                        }
                    }
                }
            }

            item { SectionTitle("Last 30 days") }

            if (grouped.isEmpty()) {
                item {
                    EmptyState(
                        title = "No prayer records yet",
                        body = "Mark a prayer from Today. It will appear here with its context and note."
                    )
                }
            } else {
                items(grouped.keys.toList(), key = { it.toString() }) { date ->
                    val records = grouped[date].orEmpty()
                    FrostedCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            records.sortedBy { it.prayer.ordinal }.forEach { record ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(record.prayer.title, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            buildList {
                                                add(
                                                    formatClock(
                                                        record.completedAt.atZone(ZoneId.systemDefault()).toLocalTime()
                                                    )
                                                )
                                                if (record.context.isNotBlank()) add(record.context)
                                                if (record.tags.isNotBlank()) add(record.tags)
                                                if (record.note.isNotBlank()) add(record.note)
                                            }.joinToString(" · "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { viewModel.undoPrayed(record.id) }) { Text("Undo") }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "History is stored locally and included in your backups. Undo removes a record permanently.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showQadaDialog) {
        QadaDialog(
            onDismiss = { showQadaDialog = false },
            onSave = { prayer, date, note ->
                if (date == null) {
                    qadaError = "Enter the date as yyyy-MM-dd."
                } else {
                    viewModel.addQada(prayer, date, note)
                    qadaError = null
                    showQadaDialog = false
                }
            }
        )
    }
}

@Composable
private fun QadaDialog(
    onDismiss: () -> Unit,
    onSave: (PrayerName, LocalDate?, String) -> Unit
) {
    var prayer by remember { mutableStateOf(PrayerName.FAJR) }
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a Qada record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Prayer", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PrayerName.obligatories.forEach { option ->
                        FilterChip(
                            selected = prayer == option,
                            onClick = { prayer = option },
                            label = { Text(option.title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date performed") },
                    placeholder = { Text("yyyy-MM-dd") },
                    singleLine = true
                )
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                onSave(prayer, date, note.trim())
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
