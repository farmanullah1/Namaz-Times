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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import com.example.ui.components.FiqhGuideDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.core.design.PrayerProgressRing
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
import com.example.domain.model.HadithRepository
import com.example.domain.model.DailyHadith
import com.example.domain.model.ResolvedPrayerTime
import com.example.navigation.PermissionActions
import com.example.prayer.TodayContext
import com.example.ui.AppUiState
import com.example.ui.AppViewModel
import com.example.ui.components.SoundSelectorSheet
import java.time.DayOfWeek
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
    var showSoundSheet by remember { mutableStateOf(false) }
    var showFiqhGuide by remember { mutableStateOf(false) }
    var detailPrayer by remember { mutableStateOf<PrayerName?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TodayHeader(
                state = state,
                today = today,
                onOpenPlaces = onOpenPlaces,
                onOpenFiqhGuide = { showFiqhGuide = true }
            )
        }
        item {
            val nextPrayer = today?.next?.prayer
            NextPrayerCard(
                state = state,
                today = today,
                isCompleted = nextPrayer != null && completed[nextPrayer] != null,
                onQuickMark = {
                    nextPrayer?.let { viewModel.markPrayed(it, "Mosque", "", "") }
                },
                onOpenSoundSheet = { showSoundSheet = true },
                onOpenMosques = onOpenMosques,
                onOpenQibla = onOpenQibla
            )
        }
        item {
            DailyHadithCard(date = today?.today ?: LocalDate.now())
        }
        item { ReminderWarnings(state, viewModel, permissions, onOpenMosques) }

        item { SectionTitle("Today's prayer timeline") }
        items(PrayerName.obligatories) { prayer ->
            PrayerTimelineRow(
                prayer = prayer,
                resolved = today?.resolved?.get(prayer),
                record = completed[prayer],
                zone = today?.zone ?: java.time.ZoneId.systemDefault(),
                onClick = { detailPrayer = prayer },
                onQuickMark = {
                    if (completed[prayer] != null) {
                        viewModel.undoPrayed(completed[prayer]!!.id)
                    } else {
                        viewModel.markPrayed(prayer, "Mosque", "", "")
                    }
                }
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

    if (showSoundSheet) {
        SoundSelectorSheet(
            settings = state.settings,
            onSetSoundMode = { mode, uri -> viewModel.setReminderSound(mode, uri) },
            onSetSpokenAnnouncement = { enabled -> viewModel.setSpokenAnnouncements(enabled) },
            onTestSound = { viewModel.testReminderSound() },
            onDismiss = { showSoundSheet = false }
        )
    }

    if (showFiqhGuide) {
        FiqhGuideDialog(onDismiss = { showFiqhGuide = false })
    }
}

@Composable
private fun TodayHeader(
    state: AppUiState,
    today: TodayContext?,
    onOpenPlaces: () -> Unit,
    onOpenFiqhGuide: () -> Unit
) {
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
        val isMondayOrThursday = date.dayOfWeek == DayOfWeek.MONDAY || date.dayOfWeek == DayOfWeek.THURSDAY
        val isAyyamAlBeed = state.hijri?.day in 13..15
        if (isMondayOrThursday || isAyyamAlBeed) {
            val fastLabel = if (isAyyamAlBeed) "🌙 Sunnah Fast: Ayyam al-Beed (White Days)"
                else "🌙 Sunnah Fast: ${date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}"
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppColors.Gold.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Gold.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AppColors.Gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        fastLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = AppColors.Gold)
                    )
                }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            today?.place?.displayName ?: (if (today?.travelMode == true) "Travel mode" else "Automatic place switching"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.settings.locationMode == LocationMode.AUTOMATIC) {
                            Pill("Auto Geofenced", color = AppColors.Emerald)
                        }
                    }
                }
                Text("Change", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                onClick = onOpenFiqhGuide
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = AppColors.Gold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Ahl al-Hadith Fiqh Guide",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.Gold
                    )
                }
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
    isCompleted: Boolean,
    onQuickMark: () -> Unit,
    onOpenSoundSheet: () -> Unit,
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

        val currentPrayer = today.currentPrayer
        val start = currentPrayer?.let { today.resolved[it]?.adhan?.toEpochMilli() } ?: (next.instant.toEpochMilli() - 3 * 3600 * 1000L)
        val end = next.instant.toEpochMilli()
        val total = (end - start).coerceAtLeast(1L)
        val elapsed = (state.nowMillis - start).coerceIn(0L, total)
        val fraction = (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Location and source status header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = AppColors.Gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = next.mosqueName ?: "Calculated times",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
                SourceBadge(next.source)
            }

            // Animated Circular Progress Ring with live countdown
            PrayerProgressRing(
                progress = fraction,
                countdownText = formatDuration(state.countdownMillis),
                prayerTitle = next.prayer.title,
                prayerArabicTitle = next.prayer.arabicTitle,
                timeUntilLabel = if (next.isTomorrow) "Tomorrow" else "Next Prayer"
            )

            // Adhan and Iqamah times with gold highlight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LabeledValue("Adhan", formatClock(next.instant, today.zone))
                LabeledValue(
                    "Iqamah",
                    next.iqamah?.let { formatClock(it, today.zone) } ?: "Not entered",
                    valueColor = if (next.iqamah == null) AppColors.Warning else AppColors.Gold
                )
            }

            // One-tap Quick Mark button
            if (isCompleted) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${next.prayer.title} marked as prayed",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            } else {
                PrimaryButton(
                    text = "Mark ${next.prayer.title} as Prayed",
                    onClick = onQuickMark,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Quick Actions: Sound, Qibla, Timetable
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    text = "Adhan Audio",
                    onClick = onOpenSoundSheet,
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = "Qibla",
                    onClick = onOpenQibla,
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = "Timetable",
                    onClick = onOpenMosques,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DailyHadithCard(date: LocalDate) {
    val hadith = remember(date) { HadithRepository.forDate(date) }
    FrostedCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AppColors.Gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Daily Hadith",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Pill(hadith.topic)
            }
            Text(
                hadith.arabicText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Gold,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "“${hadith.englishTranslation}”",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            )
            Text(
                "— ${hadith.narrator} · ${hadith.source}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    onClick: () -> Unit,
    onQuickMark: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        resolved?.adhan?.let { formatClock(it, zone) } ?: "Not entered",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        resolved?.iqamah?.let { "Iqamah " + formatClock(it, zone) } ?: "Iqamah not entered",
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
            IconButton(
                onClick = onQuickMark,
                modifier = Modifier.size(48.dp)
            ) {
                if (record != null) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Marked - tap to undo",
                        tint = AppColors.Gold,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Tap to mark as prayed",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
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
