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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.AppColors
import com.example.core.design.FrostedCard
import com.example.core.design.InfoBanner
import com.example.core.design.LabeledValue
import com.example.core.design.Pill
import com.example.core.design.PrimaryButton
import com.example.core.design.SecondaryButton
import com.example.core.design.SectionTitle
import com.example.core.design.SimpleProgressBar
import com.example.core.design.SourceBadge
import com.example.core.time.formatClock
import com.example.core.time.formatDuration
import com.example.data.local.PrayerHistoryRecord
import com.example.domain.model.AppSettings
import com.example.domain.model.LocationMode
import com.example.domain.model.PrayerEventType
import com.example.domain.model.PrayerName
import com.example.domain.model.ResolvedPrayerTime
import com.example.navigation.PermissionActions
import com.example.prayer.TodayContext
import com.example.ui.AppUiState
import com.example.ui.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    permissions: PermissionActions,
    onOpenPlaces: () -> Unit,
    onOpenMosques: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenQuran: () -> Unit
) {
    val today = state.today
    val completed = remember(state.history) { state.history.associateBy { it.prayer } }
    var showMarkDialog by remember { mutableStateOf(false) }
    var detailPrayer by remember { mutableStateOf<PrayerName?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { TodayHeader(state, today, onOpenPlaces) }
        item { NextPrayerCard(state, today, onOpenMosques, onOpenQibla) }
        item { ReminderWarnings(state, viewModel, permissions, onOpenMosques) }

        item { SectionTitle("Today's prayer timeline") }
        items(PrayerName.obligatories) { prayer ->
            PrayerTimelineRow(
                prayer = prayer,
                resolved = today?.resolved?.get(prayer),
                record = completed[prayer],
                zone = today?.zone ?: java.time.ZoneId.systemDefault(),
                onClick = { detailPrayer = prayer }
            )
        }
        if (today?.sunrise != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Sunrise (informational only)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatClock(today.sunrise, today.zone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Mark a prayer as prayed", onClick = { showMarkDialog = true })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        "Qibla",
                        onClick = onOpenQibla,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        "Quran",
                        onClick = onOpenQuran,
                        modifier = Modifier.weight(1f)
                    )
                }
                SecondaryButton("Switch place or mosque", onClick = onOpenPlaces)
            }
        }
    }

    if (showMarkDialog) {
        MarkPrayedDialog(
            defaultPrayer = today?.next?.prayer ?: PrayerName.FAJR,
            onDismiss = { showMarkDialog = false },
            onSave = { prayer, context, tags, note ->
                viewModel.markPrayed(prayer, context, tags, note)
                showMarkDialog = false
            }
        )
    }

    detailPrayer?.let { prayer ->
        val resolved = today?.resolved?.get(prayer)
        PrayerDetailDialog(
            prayer = prayer,
            resolved = resolved,
            record = completed[prayer],
            zone = today?.zone ?: java.time.ZoneId.systemDefault(),
            mosqueName = today?.mosque?.name,
            onDismiss = { detailPrayer = null },
            onMarkPrayed = {
                viewModel.markPrayed(prayer, "Mosque", "", "")
                detailPrayer = null
            },
            onUndo = { record -> record?.let { viewModel.undoPrayed(it.id) }; detailPrayer = null }
        )
    }
}

@Composable
private fun TodayHeader(state: AppUiState, today: TodayContext?, onOpenPlaces: () -> Unit) {
    val date = today?.today ?: LocalDate.now()
    val dateLabel = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Today",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Pill(modeLabel(state.settings))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                state.hijri?.formatted ?: "Hijri date unavailable",
                style = MaterialTheme.typography.bodyMedium
            )
            if (state.hijri?.isAdjusted == true) {
                Pill("Adjusted")
            }
            if (state.hijri?.usedSunsetBoundary == true) {
                Pill("After Maghrib")
            }
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            onClick = onOpenPlaces
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        today?.mosque?.name ?: "Calculated times — no mosque selected",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        today?.place?.displayName ?: (if (today?.travelMode == true) "Travel mode" else "Automatic place switching"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("Change", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

fun modeLabel(settings: AppSettings): String {
    val temporary = settings.temporaryMosqueId != null &&
        (settings.temporaryExpiresAtMillis ?: 0L) > System.currentTimeMillis()
    return if (temporary) {
        LocationMode.TEMPORARY.title
    } else {
        settings.locationMode.title
    }
}

@Composable
private fun NextPrayerCard(
    state: AppUiState,
    today: TodayContext?,
    onOpenMosques: () -> Unit,
    onOpenQibla: () -> Unit
) {
    val next = today?.next
    FrostedCard {
        if (today == null || next == null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("No upcoming prayer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Add a mosque timetable or allow a location so times can be resolved. Nothing is invented.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SecondaryButton("Set up a mosque", onClick = onOpenMosques)
            }
            return@FrostedCard
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    next.prayer.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    next.prayer.arabicTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                SourceBadge(next.source)
            }
            Text(
                if (next.isTomorrow) "Tomorrow · ${formatClock(next.instant, today.zone)}" else formatClock(next.instant, today.zone),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                formatDuration(state.countdownMillis) + " remaining",
                style = MaterialTheme.typography.bodyLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                LabeledValue("Adhan", formatClock(next.instant, today.zone))
                LabeledValue(
                    "Iqamah",
                    next.iqamah?.let { formatClock(it, today.zone) } ?: "Not entered",
                    valueColor = if (next.iqamah == null) AppColors.Warning else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                next.mosqueName ?: "Calculated — Travel mode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PrayerWindowProgress(today, state.nowMillis)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("View timetable", onClick = onOpenMosques, modifier = Modifier.weight(1f))
                SecondaryButton("Qibla", onClick = onOpenQibla, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PrayerWindowProgress(today: TodayContext, nowMillis: Long) {
    val currentPrayer = today.currentPrayer ?: return
    val next = today.next ?: return
    val start = today.resolved[currentPrayer]?.adhan?.toEpochMilli() ?: return
    val end = next.instant.toEpochMilli()
    if (end <= start) return
    val fraction = ((nowMillis - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SimpleProgressBar(progress = fraction, modifier = Modifier.padding(top = 6.dp))
        Text(
            "After ${currentPrayer.title} · ${today.occurrences.size} reminder event(s) today",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReminderWarnings(
    state: AppUiState,
    viewModel: AppViewModel,
    permissions: PermissionActions,
    onOpenMosques: () -> Unit
) {
    val today = state.today
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!state.settings.remindersEnabled) {
            RecommendationCard(
                title = "Reminders are off",
                body = "Adhan and Iqamah notifications will not appear. Times and history still work.",
                actionLabel = "Enable reminders",
                onAction = {
                    viewModel.setRemindersEnabled(true)
                    permissions.requestNotifications()
                }
            )
        } else {
            if (!state.notificationsAllowed) {
                RecommendationCard(
                    title = "Notifications are blocked",
                    body = "Android is blocking Namaz Times notifications. Reminders cannot be shown.",
                    actionLabel = "Open settings",
                    onAction = permissions.openAppNotificationSettings
                )
            } else if (!state.adhanChannelEnabled) {
                RecommendationCard(
                    title = "Prayer channel disabled",
                    body = "The Prayer Adhan notification channel is turned off in Android settings.",
                    actionLabel = "Open settings",
                    onAction = permissions.openAppNotificationSettings
                )
            }
            if (!state.exactAlarmAvailable) {
                RecommendationCard(
                    title = "Exact alarms unavailable",
                    body = "Reminders still work but Android may delay them. Grant exact-alarm access for time-critical alerts.",
                    actionLabel = "Allow exact alarms",
                    onAction = permissions.openExactAlarmSettings
                )
            }
            if (state.settings.remindersNeedRepair) {
                RecommendationCard(
                    title = "Reminders need repair",
                    body = "The timetable was saved, but some alarms could not be armed. Retry the rebuild.",
                    actionLabel = "Retry",
                    onAction = { viewModel.rebuildReminders() }
                )
            }
        }

        if (today == null || today.mosque == null) {
            RecommendationCard(
                title = "Calculated times",
                body = "No mosque timetable is active. Times are calculated from your settings and location.",
                actionLabel = "Choose a mosque",
                onAction = onOpenMosques
            )
        } else if (!today.travelMode && today.timetable?.hasAnyTime == false) {
            RecommendationCard(
                title = "No mosque times entered",
                body = "${today.mosque.name} has no entered Adhan or Iqamah times. Calculated times are shown as the fallback.",
                actionLabel = "Enter timetable",
                onAction = onOpenMosques
            )
        }
    }
}

@Composable
private fun RecommendationCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    FrostedCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun PrayerTimelineRow(
    prayer: PrayerName,
    resolved: ResolvedPrayerTime?,
    record: PrayerHistoryRecord?,
    zone: java.time.ZoneId,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(prayer.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        prayer.arabicTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        resolved?.adhan?.let { formatClock(it, zone) } ?: "Not entered",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        resolved?.iqamah?.let { formatClock(it, zone) } ?: "Iqamah not entered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                resolved?.adhanSource?.let {
                    Text(
                        it.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (record != null) {
                Icon(Icons.Default.Check, contentDescription = "Marked as prayed", tint = MaterialTheme.colorScheme.primary)
            } else {
                Text("Not marked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PrayerDetailDialog(
    prayer: PrayerName,
    resolved: ResolvedPrayerTime?,
    record: PrayerHistoryRecord?,
    zone: java.time.ZoneId,
    mosqueName: String?,
    onDismiss: () -> Unit,
    onMarkPrayed: () -> Unit,
    onUndo: (PrayerHistoryRecord?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${prayer.title} · ${prayer.arabicTitle}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LabeledValue(
                    "Adhan",
                    resolved?.adhan?.let { formatClock(it, zone) } ?: "Not entered"
                )
                LabeledValue(
                    "Iqamah",
                    resolved?.iqamah?.let { formatClock(it, zone) } ?: "Iqamah not entered"
                )
                LabeledValue("Source", resolved?.adhanSource?.label ?: "Not available")
                LabeledValue("Mosque", mosqueName ?: "Calculated fallback")
                if (resolved?.note?.isNotBlank() == true) {
                    LabeledValue("Note", resolved.note)
                }
                if (record != null) {
                    LabeledValue(
                        "Marked",
                        formatClock(record.completedAt.atZone(zone).toLocalTime()) +
                            " · ${record.context.ifBlank { "No context" }}"
                    )
                    if (record.note.isNotBlank()) LabeledValue("Record note", record.note)
                }
                Text(
                    "Prayer completion is only recorded when you mark it. The app never infers it.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (record == null) {
                TextButton(onClick = onMarkPrayed) { Text("Mark as prayed") }
            } else {
                TextButton(onClick = { onUndo(record) }) { Text("Undo") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun MarkPrayedDialog(
    defaultPrayer: PrayerName,
    onDismiss: () -> Unit,
    onSave: (PrayerName, String, String, String) -> Unit
) {
    var prayer by remember { mutableStateOf(defaultPrayer) }
    var context by remember { mutableStateOf("Mosque") }
    var tags by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val contexts = listOf("Mosque", "Congregation", "Alone", "Travelling", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark a prayer as prayed") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Prayer", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrayerName.obligatories.forEach { option ->
                        FilterChip(
                            selected = prayer == option,
                            onClick = { prayer = option },
                            label = { Text(option.title, fontSize = 11.sp) }
                        )
                    }
                }
                Text("How did you pray?", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    contexts.take(3).forEach { option ->
                        FilterChip(
                            selected = context == option,
                            onClick = { context = option },
                            label = { Text(option, fontSize = 11.sp) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    contexts.drop(3).forEach { option ->
                        FilterChip(
                            selected = context == option,
                            onClick = { context = option },
                            label = { Text(option, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (optional)") },
                    placeholder = { Text("Home, Work, Travel") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(prayer, context, tags, note) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
