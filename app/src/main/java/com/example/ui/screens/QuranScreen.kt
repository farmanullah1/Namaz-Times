package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.AppColors
import com.example.core.design.DualProgressRing
import com.example.core.design.FrostedCard
import com.example.core.design.InfoBanner
import com.example.core.design.LabeledValue
import com.example.core.design.Pill
import com.example.core.design.PrimaryButton
import com.example.core.design.SecondaryButton
import com.example.core.design.SectionTitle
import com.example.core.design.SettingSwitchRow
import com.example.core.design.TimeEntryField
import com.example.domain.model.QuranResetRule
import com.example.domain.model.QuranStructureData
import com.example.domain.model.SurahInfo
import com.example.domain.model.WallClock
import com.example.ui.AppUiState
import com.example.ui.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    var showKhatamCalculator by remember { mutableStateOf(false) }
    var selectedJuzNumber by remember { mutableIntStateOf(1) }
    val surahsInSelectedJuz = remember(selectedJuzNumber) {
        QuranStructureData.surahs.filter { it.juzNumber == selectedJuzNumber }
    }

    var goalText by remember(settings.dailyGoal) { mutableStateOf(settings.dailyGoal.toString()) }
    var editionTotalText by remember(settings.editionTotalRukus) { mutableStateOf(settings.editionTotalRukus?.toString() ?: "") }
    var editionLabel by remember(settings.editionLabel) { mutableStateOf(settings.editionLabel) }
    var customResetText by remember(settings.customResetTime) { mutableStateOf(WallClock.format(settings.customResetTime) ?: "00:00") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Quran", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(
                    "Daily Ruku ledger & Khatam reading companion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Hero: Dual Progress Ring with Live Stats
        item {
            FrostedCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Pill(snapshot.todayDateKey.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())))
                        Pill("${snapshot.streak} day streak", color = AppColors.Gold)
                    }

                    DualProgressRing(
                        todayCompleted = snapshot.completedToday,
                        todayTarget = snapshot.target,
                        totalCompleted = snapshot.totalCompleted,
                        totalRukus = QuranStructureData.TOTAL_RUKUS,
                        sizeDp = 220.dp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LabeledValue("Remaining Today", "${snapshot.remainingToday}")
                        LabeledValue("Backlog", "${snapshot.backlog}")
                        LabeledValue("Surplus Credit", "${snapshot.surplusCredit}")
                        LabeledValue("Total Read", "${snapshot.totalCompleted} / ${QuranStructureData.TOTAL_RUKUS}")
                    }

                    // Quick Action Stepper Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SecondaryButton(
                            text = "-1",
                            onClick = { viewModel.undoLastRuku() },
                            modifier = Modifier.weight(0.7f)
                        )
                        SecondaryButton(
                            text = "+1 Ruku",
                            onClick = { viewModel.quickAddRukus(1) },
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "+4 (1/Salah)",
                            onClick = { viewModel.quickAddRukus(4) },
                            modifier = Modifier.weight(1.2f)
                        )
                        SecondaryButton(
                            text = "+ Custom",
                            onClick = { showAddDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SecondaryButton(
                            text = "🎯 Khatam Target Pace",
                            onClick = { showKhatamCalculator = true },
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "Correct Total",
                            onClick = { showCorrectDialog = snapshot.todayDateKey },
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    if (snapshot.resetExplanation.isNotBlank()) {
                        Text(
                            snapshot.resetExplanation,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section: Interactive Ruku Checklist
        item {
            SectionTitle("Ruku Checklist (${snapshot.totalCompleted} of ${QuranStructureData.TOTAL_RUKUS} completed)")
        }

        // Horizontal Juz Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuranStructureData.juzList.forEach { juz ->
                    val isSelected = juz.number == selectedJuzNumber
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedJuzNumber = juz.number },
                        label = {
                            Text("Juz ${juz.number} · ${juz.nameArabic}")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Gold.copy(alpha = 0.2f),
                            selectedLabelColor = AppColors.Gold
                        )
                    )
                }
            }
        }

        // Display Surahs and Rukus for Selected Juz
        items(surahsInSelectedJuz, key = { it.number }) { surah ->
            SurahRukuCard(
                surah = surah,
                markedKeys = state.markedRukuKeys,
                onToggleRuku = { rukuNumber, isChecked ->
                    viewModel.toggleRukuCheckoff(surah.nameEnglish, rukuNumber, isChecked)
                }
            )
        }

        // Today's reading entries
        if (state.quranSessionsToday.isNotEmpty()) {
            item { SectionTitle("Today's Recorded Sessions") }
            items(state.quranSessionsToday, key = { it.id }) { session ->
                FrostedCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${session.rukus} Ruku" + (session.surah?.let { " · $it" } ?: ""),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                buildList {
                                    session.rukuNumber?.let { add("Ruku $it") }
                                    if (session.note.isNotBlank()) add(session.note)
                                    session.correctionReason?.let { add("Correction: $it") }
                                }.joinToString(" · ").ifBlank { "Recorded reading" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (session.isCorrection) Pill("Correction")
                    }
                }
            }
        }

        // Ledger Settings
        item { SectionTitle("Ledger Settings") }
        item {
            FrostedCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Daily Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                        "Goal updates apply from the next reading day, ensuring today's active target stays consistent.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SettingSwitchRow(
                        title = "Carry surplus credit",
                        subtitle = "Reading above target reduces future targets. Backlog still accrues for missed days.",
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
                }
            }
        }

        // Recent reading history
        if (snapshot.recentDays.isNotEmpty()) {
            item { SectionTitle("Recent Reading Days") }
            items(snapshot.recentDays, key = { it.dateKey }) { day ->
                FrostedCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                day.dateKey.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${day.completedRukus} read · target ${day.requiredTarget}" +
                                    if (day.backlogAfterClose > 0) " · backlog ${day.backlogAfterClose}" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (day.targetSatisfied) Pill("Met", color = AppColors.Gold) else Pill("Missed")
                        TextButton(onClick = { showCorrectDialog = day.dateKey }) { Text("Correct") }
                    }
                }
            }
        }

        item {
            InfoBanner(
                "Your Ruku ledger tracks every reading session with full audit fidelity. Missed days add backlog, and surplus credit carries forward when enabled."
            )
        }
    }

    // Khatam Target Pace Calculator Dialog
    if (showKhatamCalculator) {
        KhatamPaceDialog(
            totalCompleted = snapshot.totalCompleted,
            currentGoal = settings.dailyGoal,
            onDismiss = { showKhatamCalculator = false },
            onApplyTarget = { targetDate ->
                viewModel.setKhatamTargetDate(targetDate)
                showKhatamCalculator = false
            }
        )
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
private fun SurahRukuCard(
    surah: SurahInfo,
    markedKeys: Set<String>,
    onToggleRuku: (Int, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val completedCount = remember(markedKeys, surah) {
        (1..surah.rukusCount).count { rukuNum ->
            markedKeys.contains("${surah.nameEnglish}:$rukuNum")
        }
    }

    FrostedCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${surah.number}. ${surah.nameEnglish}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            surah.nameArabic,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Gold
                        )
                    }
                    Text(
                        "$completedCount / ${surah.rukusCount} Rukus completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (rukuNum in 1..surah.rukusCount) {
                        val key = "${surah.nameEnglish}:$rukuNum"
                        val isChecked = markedKeys.contains(key)
                        val globalIndex = surah.startGlobalRuku + rukuNum - 1

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChecked) AppColors.Gold.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onToggleRuku(rukuNum, !isChecked) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onToggleRuku(rukuNum, !isChecked) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = if (isChecked) "Uncheck Ruku $rukuNum" else "Check Ruku $rukuNum",
                                        tint = if (isChecked) AppColors.Gold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Ruku $rukuNum",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        "Global Ruku #$globalIndex",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isChecked) {
                                    Pill("Read", color = AppColors.Gold, background = AppColors.Gold.copy(alpha = 0.15f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KhatamPaceDialog(
    totalCompleted: Int,
    currentGoal: Int,
    onDismiss: () -> Unit,
    onApplyTarget: (LocalDate) -> Unit
) {
    val remaining = (QuranStructureData.TOTAL_RUKUS - totalCompleted).coerceAtLeast(1)
    var selectedDays by remember { mutableIntStateOf(30) }
    val targetDate = remember(selectedDays) { LocalDate.now().plusDays(selectedDays.toLong()) }
    val requiredPace = remember(selectedDays, remaining) {
        QuranStructureData.calculateRequiredPace(remaining, targetDate = targetDate)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎯 Khatam Target Calculator") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "You have $remaining Rukus remaining to complete the whole Quran (558 Rukus). Select your completion timeframe to compute the required daily pace.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text("Target Duration", style = MaterialTheme.typography.labelMedium)
                val presets = listOf(
                    30 to "30 Days (Ramadan)",
                    60 to "60 Days (2 Months)",
                    90 to "90 Days (Quarter)",
                    180 to "180 Days (Half Year)",
                    365 to "365 Days (1 Year)"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { (days, label) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedDays == days) AppColors.Gold.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selectedDays == days) androidx.compose.foundation.BorderStroke(1.dp, AppColors.Gold) else null,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { selectedDays = days }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Pill("${QuranStructureData.calculateRequiredPace(remaining, targetDate = LocalDate.now().plusDays(days.toLong()))} / day")
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AppColors.EmeraldDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Gold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Required Daily Pace",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.GoldSoft
                        )
                        Text(
                            "$requiredPace Rukus / day",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = AppColors.Gold)
                        )
                        Text(
                            "Khatam date: ${targetDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApplyTarget(targetDate) }) {
                Text("Set Daily Goal to $requiredPace")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
