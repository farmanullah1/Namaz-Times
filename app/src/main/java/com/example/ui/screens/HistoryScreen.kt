package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import com.example.core.design.EmptyState
import com.example.core.design.FrostedCard
import com.example.core.design.InfoBanner
import com.example.core.design.LabeledValue
import com.example.core.design.Pill
import com.example.core.design.ScreenHeader
import com.example.core.design.SecondaryButton
import com.example.core.design.SectionTitle
import com.example.core.design.SettingSwitchRow
import com.example.core.design.TimeEntryField
import com.example.core.time.formatClock
import com.example.data.local.PrayerHistoryRecord
import com.example.domain.model.PrayerName
import com.example.domain.model.WallClock
import com.example.ui.AppUiState
import com.example.ui.AppViewModel
import com.example.ui.components.PrayerHistoryHeatmap
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
    var query by remember { mutableStateOf("") }
    var prayerFilter by remember { mutableStateOf<PrayerName?>(null) }
    var editingRecord by remember { mutableStateOf<PrayerHistoryRecord?>(null) }

    val filtered = remember(state.recentHistory, query, prayerFilter) {
        val needle = query.trim()
        state.recentHistory.filter { record ->
            val prayerMatches = prayerFilter == null || record.prayer == prayerFilter
            val textMatches = needle.isEmpty() || listOf(
                record.prayer.title,
                record.context,
                record.tags,
                record.note
            ).any { it.contains(needle, ignoreCase = true) }
            prayerMatches && textMatches
        }
    }
    val allGrouped = remember(state.recentHistory) {
        state.recentHistory
            .groupBy { it.trackingDate }
            .toSortedMap(compareByDescending { it })
    }
    val grouped = remember(filtered) {
        filtered
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
                PrayerHistoryHeatmap(
                    history = state.history,
                    zone = state.today?.zone ?: ZoneId.systemDefault()
                )
            }

            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Search records") },
                            placeholder = { Text("Note, tag or context") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = prayerFilter == null,
                                onClick = { prayerFilter = null },
                                label = { Text("All", style = MaterialTheme.typography.labelSmall) }
                            )
                            PrayerName.obligatories.forEach { prayer ->
                                FilterChip(
                                    selected = prayerFilter == prayer,
                                    onClick = { prayerFilter = if (prayerFilter == prayer) null else prayer },
                                    label = { Text(prayer.title, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        if (query.isNotBlank() || prayerFilter != null) {
                            Text(
                                "${filtered.size} of ${state.recentHistory.size} records match. " +
                                    "Search covers the last 30 days.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Last 30 days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        LabeledValue("Prayers marked", "${state.recentHistory.size}")
                        LabeledValue("Days with at least one record", "${allGrouped.size}")
                        Text(
                            PrayerName.obligatories.joinToString("  ·  ") { prayer ->
                                "${prayer.title} ${state.recentHistory.count { it.prayer == prayer }}"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Counts describe what you recorded. They are not a score, a judgement or a claim about " +
                                "prayers you did not mark.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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

            item {
                SectionTitle(
                    if (query.isBlank() && prayerFilter == null) "Last 30 days" else "Matching records"
                )
            }

            if (grouped.isEmpty()) {
                item {
                    if (state.recentHistory.isEmpty()) {
                        EmptyState(
                            title = "No prayer records yet",
                            body = "Mark a prayer from Today. It will appear here with its context and note."
                        )
                    } else {
                        EmptyState(
                            title = "No matching records",
                            body = "No marked prayer in the last 30 days matches this search and prayer filter.",
                            actionLabel = "Clear filters",
                            onAction = {
                                query = ""
                                prayerFilter = null
                            }
                        )
                    }
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
                                    TextButton(onClick = { editingRecord = record }) { Text("Edit") }
                                    TextButton(onClick = { viewModel.undoPrayed(record.id) }) { Text("Undo") }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "History is stored locally and included in your backups. Editing keeps the record; undo removes it permanently.",
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

    editingRecord?.let { record ->
        EditHistoryDialog(
            record = record,
            onDismiss = { editingRecord = null },
            onSave = { updated ->
                viewModel.updateHistoryRecord(updated)
                editingRecord = null
            }
        )
    }
}

@Composable
private fun EditHistoryDialog(
    record: PrayerHistoryRecord,
    onDismiss: () -> Unit,
    onSave: (PrayerHistoryRecord) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    var timeText by remember(record.id) {
        mutableStateOf(WallClock.format(record.completedAt.atZone(zone).toLocalTime()).orEmpty())
    }
    var context by remember(record.id) { mutableStateOf(record.context) }
    var tags by remember(record.id) { mutableStateOf(record.tags) }
    var note by remember(record.id) { mutableStateOf(record.note) }
    var error by remember(record.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit prayer record") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${record.prayer.title} · ${record.trackingDate}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TimeEntryField(
                    label = "Completed at",
                    value = timeText,
                    onValueChange = { timeText = it.orEmpty() }
                )
                OutlinedTextField(
                    value = context,
                    onValueChange = { context = it },
                    label = { Text("Context") },
                    placeholder = { Text("Mosque, home, travel…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags") },
                    placeholder = { Text("Comma-separated") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Prayer and date cannot be changed here. Undo and mark again to move a record.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                error?.let { InfoBanner(it, isWarning = true) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val time = WallClock.parse(timeText)
                if (time == null) {
                    error = "Enter the completion time as HH:mm."
                } else {
                    onSave(
                        record.copy(
                            completedAt = record.trackingDate.atTime(time).atZone(zone).toInstant(),
                            context = context.trim(),
                            tags = tags.trim(),
                            note = note.trim()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
