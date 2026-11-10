package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.design.FrostedCard
import com.example.core.design.InfoBanner
import com.example.core.design.LabeledValue
import com.example.core.design.NumberEntryField
import com.example.core.design.Pill
import com.example.core.design.PrimaryButton
import com.example.core.design.ScreenHeader
import com.example.core.design.SecondaryButton
import com.example.core.design.SectionTitle
import com.example.core.design.SettingSwitchRow
import com.example.data.repository.BackupPreview
import com.example.domain.model.AsrShadowFactor
import com.example.domain.model.CalculationPreset
import com.example.domain.model.CalculationSettings
import com.example.domain.model.HighLatitudeRuleOption
import com.example.domain.model.PerPrayerAdjustments
import com.example.domain.model.PrayerName
import com.example.domain.model.ReminderPreference
import com.example.domain.model.ReminderSoundMode
import com.example.navigation.PermissionActions
import com.example.ui.AppUiState
import com.example.ui.AppViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalculationSettingsScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val calculation = state.settings.calculation
    var preset by remember(calculation) { mutableStateOf(calculation.preset) }
    var fajrAngle by remember(calculation) { mutableStateOf(calculation.fajrAngle.toString()) }
    var ishaAngle by remember(calculation) { mutableStateOf(calculation.ishaAngle.toString()) }
    var ishaInterval by remember(calculation) { mutableStateOf(calculation.ishaIntervalMinutes?.toString() ?: "") }
    var asr by remember(calculation) { mutableStateOf(calculation.asr) }
    var highLatitude by remember(calculation) { mutableStateOf(calculation.highLatitude) }
    var fajrAdjust by remember(calculation) { mutableStateOf(calculation.adjustments.fajr.toString()) }
    var dhuhrAdjust by remember(calculation) { mutableStateOf(calculation.adjustments.dhuhr.toString()) }
    var asrAdjust by remember(calculation) { mutableStateOf(calculation.adjustments.asr.toString()) }
    var maghribAdjust by remember(calculation) { mutableStateOf(calculation.adjustments.maghrib.toString()) }
    var ishaAdjust by remember(calculation) { mutableStateOf(calculation.adjustments.isha.toString()) }
    var rounding by remember(calculation) { mutableStateOf(calculation.roundingToNearestMinute) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Calculation settings",
            subtitle = "Fallback estimates only — mosque times always win",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoBanner(CalculationSettings.described())
            }
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CalculationPreset.entries.forEach { option ->
                                FilterChip(
                                    selected = preset == option,
                                    onClick = {
                                        preset = option
                                        fajrAngle = option.defaultFajrAngle.toString()
                                        ishaAngle = option.defaultIshaAngle.toString()
                                        ishaInterval = option.defaultIshaIntervalMinutes?.toString() ?: ""
                                    },
                                    label = { Text(option.title, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Text(
                            "Current selection: ${state.today?.calculationLabel ?: preset.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = fajrAngle,
                                onValueChange = { fajrAngle = it },
                                label = { Text("Fajr angle") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = ishaAngle,
                                onValueChange = { ishaAngle = it },
                                label = { Text("Isha angle") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = ishaInterval,
                            onValueChange = { ishaInterval = it },
                            label = { Text("Isha interval (minutes, blank = use angle)") },
                            singleLine = true
                        )
                        Text(
                            "A non-empty interval replaces the Isha angle, for example 90 minutes after Maghrib in the Umm al-Qura preset.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Asr", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        AsrShadowFactor.entries.forEach { option ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(option.title, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        option.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(checked = asr == option, onCheckedChange = { if (it) asr = option })
                            }
                        }
                        Text("High-latitude rule", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HighLatitudeRuleOption.entries.forEach { option ->
                                FilterChip(
                                    selected = highLatitude == option,
                                    onClick = { highLatitude = option },
                                    label = { Text(option.title, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Minute adjustments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Positive values move a time later, negative earlier. Adjustments apply to calculated times only.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(value = fajrAdjust, onValueChange = { fajrAdjust = it }, label = { Text("Fajr") }, singleLine = true, modifier = Modifier.width(92.dp))
                            OutlinedTextField(value = dhuhrAdjust, onValueChange = { dhuhrAdjust = it }, label = { Text("Dhuhr") }, singleLine = true, modifier = Modifier.width(92.dp))
                            OutlinedTextField(value = asrAdjust, onValueChange = { asrAdjust = it }, label = { Text("Asr") }, singleLine = true, modifier = Modifier.width(92.dp))
                            OutlinedTextField(value = maghribAdjust, onValueChange = { maghribAdjust = it }, label = { Text("Maghrib") }, singleLine = true, modifier = Modifier.width(92.dp))
                            OutlinedTextField(value = ishaAdjust, onValueChange = { ishaAdjust = it }, label = { Text("Isha") }, singleLine = true, modifier = Modifier.width(92.dp))
                        }
                        SettingSwitchRow(
                            title = "Round to the nearest minute",
                            subtitle = "Disable to keep the calculator's seconds for display.",
                            checked = rounding,
                            onCheckedChange = { rounding = it }
                        )
                        error?.let { InfoBanner(it, isWarning = true) }
                        PrimaryButton("Save calculation settings", onClick = {
                            val fajr = fajrAngle.trim().toDoubleOrNull()
                            val isha = ishaAngle.trim().toDoubleOrNull()
                            if (fajr == null || fajr !in 0.0..30.0) {
                                error = "Fajr angle must be between 0 and 30 degrees."
                                return@PrimaryButton
                            }
                            if (isha == null || isha !in 0.0..30.0) {
                                error = "Isha angle must be between 0 and 30 degrees."
                                return@PrimaryButton
                            }
                            val interval = ishaInterval.trim().let { raw ->
                                if (raw.isEmpty()) null else raw.toIntOrNull()?.takeIf { it in 1..180 }
                            }
                            if (ishaInterval.trim().isNotEmpty() && interval == null) {
                                error = "Isha interval must be between 1 and 180 minutes, or blank."
                                return@PrimaryButton
                            }
                            val adjustments = listOf(fajrAdjust, dhuhrAdjust, asrAdjust, maghribAdjust, ishaAdjust)
                                .map { it.trim().toIntOrNull() }
                            if (adjustments.any { it == null || it !in -60..60 }) {
                                error = "Each adjustment must be between -60 and 60 minutes."
                                return@PrimaryButton
                            }
                            viewModel.setCalculation(
                                CalculationSettings(
                                    preset = preset,
                                    fajrAngle = fajr,
                                    ishaAngle = isha,
                                    ishaIntervalMinutes = interval,
                                    asr = asr,
                                    highLatitude = highLatitude,
                                    adjustments = PerPrayerAdjustments(
                                        fajr = adjustments[0] ?: 0,
                                        dhuhr = adjustments[1] ?: 0,
                                        asr = adjustments[2] ?: 0,
                                        maghrib = adjustments[3] ?: 0,
                                        isha = adjustments[4] ?: 0
                                    ),
                                    roundingToNearestMinute = rounding
                                )
                            )
                            error = null
                        })
                    }
                }
            }
            item {
                InfoBanner(
                    "Presets are calculation conventions, not religious rulings. Verify against your mosque or the " +
                        "scholar you follow; the app does not decide which practice is correct."
                )
            }
        }
    }
}

@Composable
fun NotificationSettingsScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    permissions: PermissionActions,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings = state.settings
    val activeMosque = state.today?.mosque
    var silenceMinutes by remember(settings.prayerSilenceMinutes) { mutableStateOf(settings.prayerSilenceMinutes.toString()) }
    var showCustomSoundError by remember { mutableStateOf<String?>(null) }
    var preferences by remember(activeMosque?.id, state.snapshot.reminderPreferences) {
        mutableStateOf(
            PrayerName.obligatories.map { prayer ->
                state.snapshot.reminderPreferences
                    .firstOrNull { it.mosqueId == activeMosque?.id && it.prayer == prayer }
                    ?: ReminderPreference(mosqueId = activeMosque?.id.orEmpty(), prayer = prayer)
            }
        )
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val persisted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            true
        }.getOrDefault(false)
        if (persisted) {
            viewModel.setReminderSound(ReminderSoundMode.CUSTOM_AUDIO, uri.toString())
            showCustomSoundError = null
        } else {
            showCustomSoundError = "Android did not grant persistent access to that audio file. Choose a local file again."
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Notifications & Adhan",
            subtitle = "Reminders stay local to this device",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingSwitchRow(
                            title = "Prayer reminders",
                            subtitle = "Adhan and Iqamah notifications for the selected mosque timetable.",
                            checked = settings.remindersEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setRemindersEnabled(enabled)
                                if (enabled) permissions.requestNotifications()
                            }
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill(if (state.notificationsAllowed) "Notifications allowed" else "Blocked by Android")
                            Spacer(Modifier.width(6.dp))
                            Pill(if (state.adhanChannelEnabled) "Prayer channel on" else "Prayer channel off")
                        }
                        SecondaryButton("Open app notification settings", onClick = permissions.openAppNotificationSettings)
                    }
                }
            }

            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Reminder sound", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReminderSoundMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.reminderSoundMode == mode,
                                    onClick = {
                                        if (mode == ReminderSoundMode.CUSTOM_AUDIO) {
                                            audioLauncher.launch(arrayOf("audio/*"))
                                        } else {
                                            viewModel.setReminderSound(mode, settings.customAdhanUri)
                                        }
                                    },
                                    label = { Text(mode.title, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        if (settings.reminderSoundMode == ReminderSoundMode.CUSTOM_AUDIO) {
                            Text(
                                if (settings.customAdhanUri == null) {
                                    "No audio file selected yet."
                                } else {
                                    "Imported Adhan audio selected. It is read from the file you chose."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { audioLauncher.launch(arrayOf("audio/*")) }) { Text("Choose audio file") }
                        }
                        showCustomSoundError?.let { InfoBanner(it, isWarning = true) }
                        SecondaryButton("Test reminder sound", onClick = { viewModel.testReminderSound() })
                        SettingSwitchRow(
                            title = "Spoken announcement",
                            subtitle = "Android may read the prayer name over the reminder when Text-to-Speech is available.",
                            checked = settings.spokenAnnouncementEnabled,
                            onCheckedChange = { viewModel.setSpokenAnnouncements(it) }
                        )
                        SettingSwitchRow(
                            title = "Friday reminders",
                            subtitle = "Adds Khutbah and Jumuah reminders when those times are entered in the timetable.",
                            checked = settings.fridayRemindersEnabled,
                            onCheckedChange = { viewModel.setFridayReminders(it) }
                        )
                    }
                }
            }

            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Alarm reliability", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        LabeledValue(
                            "Exact alarms",
                            if (state.exactAlarmAvailable) "Allowed" else "Not allowed — Android may delay reminders"
                        )
                        LabeledValue(
                            "Battery optimisation",
                            if (state.batteryOptimizationIgnored) "Unrestricted for this app" else "Android may restrict background work"
                        )
                        SecondaryButton("Allow exact alarms", onClick = permissions.openExactAlarmSettings)
                        SecondaryButton("Battery optimisation settings", onClick = permissions.openBatterySettings)
                        InfoBanner(
                            "Some manufacturers add extra background restrictions. Reliability shows what the app can detect " +
                                "and what remains outside its control."
                        )
                    }
                }
            }

            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Silence during prayer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SettingSwitchRow(
                            title = "Silence for a short window",
                            subtitle = "Uses Do Not Disturb when Android grants access. Attempts to restore the previous state afterwards.",
                            checked = settings.prayerSilenceEnabled,
                            onCheckedChange = { enabled ->
                                val minutes = silenceMinutes.toIntOrNull() ?: 10
                                viewModel.setPrayerSilence(enabled, minutes)
                            }
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberEntryField(
                                label = "Minutes",
                                value = silenceMinutes,
                                onValueChange = { silenceMinutes = it.filter { char -> char.isDigit() }.take(2) },
                                modifier = Modifier.weight(1f)
                            )
                            SecondaryButton("Save minutes", onClick = {
                                val minutes = silenceMinutes.toIntOrNull()?.coerceIn(1, 60) ?: 10
                                viewModel.setPrayerSilence(settings.prayerSilenceEnabled, minutes)
                            }, modifier = Modifier.weight(1f))
                        }
                        SecondaryButton("Open Do Not Disturb access", onClick = permissions.openDndSettings)
                        SecondaryButton("Restore silence state now", onClick = { viewModel.restoreSilenceNow() })
                    }
                }
            }

            item { SectionTitle("Per-prayer reminders") }
            item {
                FrostedCard {
                    if (activeMosque == null) {
                        Text(
                            "Select a mosque to configure its per-prayer reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                activeMosque.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            preferences.forEach { preference ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        preference.prayer.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Adhan", style = MaterialTheme.typography.labelSmall)
                                        Switch(
                                            checked = preference.adhanEnabled,
                                            onCheckedChange = { enabled ->
                                                preferences = preferences.map {
                                                    if (it.prayer == preference.prayer) it.copy(adhanEnabled = enabled) else it
                                                }
                                            }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Iqamah", style = MaterialTheme.typography.labelSmall)
                                        Switch(
                                            checked = preference.iqamahEnabled,
                                            onCheckedChange = { enabled ->
                                                preferences = preferences.map {
                                                    if (it.prayer == preference.prayer) it.copy(iqamahEnabled = enabled) else it
                                                }
                                            }
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = preference.preAdhanMinutes?.toString() ?: "",
                                            onValueChange = { raw ->
                                                val value = raw.filter { char -> char.isDigit() }.take(3).toIntOrNull()
                                                preferences = preferences.map {
                                                    if (it.prayer == preference.prayer) it.copy(preAdhanMinutes = value) else it
                                                }
                                            },
                                            label = { Text("Before Adhan (min)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = preference.preIqamahMinutes?.toString() ?: "",
                                            onValueChange = { raw ->
                                                val value = raw.filter { char -> char.isDigit() }.take(3).toIntOrNull()
                                                preferences = preferences.map {
                                                    if (it.prayer == preference.prayer) it.copy(preIqamahMinutes = value) else it
                                                }
                                            },
                                            label = { Text("Before Iqamah (min)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            SecondaryButton("Save per-prayer reminders", onClick = {
                                viewModel.saveReminderPreferences(preferences)
                            })
                        }
                    }
                }
            }

            item {
                SecondaryButton("Rebuild all reminders", onClick = { viewModel.rebuildReminders() })
            }
        }
    }
}

@Composable
fun HijriSettingsScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val settings = state.settings
    var adjustment by remember(settings.hijriAdjustmentDays) { mutableStateOf(settings.hijriAdjustmentDays) }
    var sunset by remember(settings.hijriSunsetBoundary) { mutableStateOf(settings.hijriSunsetBoundary) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Hijri calendar",
            subtitle = "Estimated conversion, with an explicit adjustment",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            state.hijri?.formatted ?: "Hijri date unavailable",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        state.hijri?.let { hijri ->
                            Text(
                                "${hijri.formattedArabic} · ${hijri.sourceLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LabeledValue("Gregorian", hijri.originalGregorianDate.toString())
                            if (hijri.usedSunsetBoundary) LabeledValue("Boundary", "After Maghrib the next Hijri date is shown")
                        }
                    }
                }
            }
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Adjustment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (-2..2).forEach { days ->
                                FilterChip(
                                    selected = adjustment == days,
                                    onClick = { adjustment = days },
                                    label = { Text(if (days > 0) "+$days" else "$days") }
                                )
                            }
                        }
                        Text(
                            "Adjustment shifts the displayed Hijri date only. It does not change prayer times or records.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SettingSwitchRow(
                            title = "Start the Hijri day after Maghrib",
                            subtitle = "Matches local practice where the date changes at sunset rather than midnight.",
                            checked = sunset,
                            onCheckedChange = { sunset = it }
                        )
                        PrimaryButton("Save Hijri settings", onClick = {
                            viewModel.setHijri(adjustment, sunset)
                        })
                    }
                }
            }
            item {
                InfoBanner(
                    "This is an Umm al-Qura tabular estimate, not a local moon-sighting announcement. Local Hilal " +
                        "committees can differ by a day; use the adjustment to match the calendar you follow."
                )
            }
        }
    }
}

@Composable
fun LocationSettingsScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    permissions: PermissionActions,
    onBack: () -> Unit
) {
    val settings = state.settings
    var maxDistance by remember(settings.maximumNearestDistanceMeters) { mutableStateOf(settings.maximumNearestDistanceMeters.toInt().toString()) }
    var accuracy by remember(settings.accuracyThresholdMeters) { mutableStateOf(settings.accuracyThresholdMeters.toInt().toString()) }
    var dwell by remember(settings.dwellSeconds) { mutableStateOf(settings.dwellSeconds.toString()) }
    var margin by remember(settings.boundaryMarginMeters) { mutableStateOf(settings.boundaryMarginMeters.toInt().toString()) }
    var fixAge by remember(settings.maxFixAgeSeconds) { mutableStateOf(settings.maxFixAgeSeconds.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Location & automation",
            subtitle = "Selection confidence and boundary thresholds",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        LabeledValue("Location", if (state.locationPermissionGranted) "Allowed" else "Not granted")
                        LabeledValue("Background location", if (state.backgroundLocationGranted) "Allowed" else "Not granted")
                        LabeledValue("Location services", if (state.locationServicesEnabled) "On" else "Off")
                        SecondaryButton("Request location permission", onClick = permissions.requestForegroundLocation)
                        SecondaryButton("Request background location", onClick = permissions.requestBackgroundLocation)
                        SecondaryButton("Re-register place boundaries", onClick = { viewModel.syncGeofences() })
                    }
                }
            }
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Selection thresholds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = maxDistance,
                                onValueChange = { maxDistance = it.filter { c -> c.isDigit() }.take(6) },
                                label = { Text("Max nearest distance (m)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = accuracy,
                                onValueChange = { accuracy = it.filter { c -> c.isDigit() }.take(5) },
                                label = { Text("GPS accuracy limit (m)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = dwell,
                                onValueChange = { dwell = it.filter { c -> c.isDigit() }.take(3) },
                                label = { Text("Dwell (seconds)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = margin,
                                onValueChange = { margin = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text("Boundary margin (m)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = fixAge,
                                onValueChange = { fixAge = it.filter { c -> c.isDigit() }.take(3) },
                                label = { Text("Max fix age (s)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            "A fix older than the max age is treated as stale and does not change the selected mosque. " +
                                "A boundary decision also requires the configured dwell time, so passing a mosque does not " +
                                "flip the selection immediately.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        error?.let { InfoBanner(it, isWarning = true) }
                        PrimaryButton("Save thresholds", onClick = {
                            val distance = maxDistance.toDoubleOrNull()
                            val accuracyValue = accuracy.toFloatOrNull()
                            val dwellValue = dwell.toLongOrNull()
                            val marginValue = margin.toDoubleOrNull()
                            val fixAgeValue = fixAge.toLongOrNull()
                            if (distance == null || accuracyValue == null || dwellValue == null ||
                                marginValue == null || fixAgeValue == null
                            ) {
                                error = "Fill every threshold with a number."
                                return@PrimaryButton
                            }
                            viewModel.setSelectionThresholds(
                                maxDistanceMeters = distance,
                                accuracyMeters = accuracyValue,
                                dwellSeconds = dwellValue,
                                marginMeters = marginValue,
                                maxFixAgeSeconds = fixAgeValue
                            )
                            error = null
                        })
                    }
                }
            }
            item {
                InfoBanner(
                    "Selection order: temporary override, then manual lock, then the active saved-place boundary, then a " +
                        "priority place, then the nearest saved mosque within the maximum distance. Otherwise the app uses " +
                        "calculated times instead of guessing a mosque."
                )
            }
        }
    }
}

@Composable
fun ReliabilityScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    permissions: PermissionActions,
    onBack: () -> Unit
) {
    val settings = state.settings
    val lastRebuild = settings.lastSuccessfulRebuildMillis?.let { millis ->
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault()))
    }
    val zone = state.today?.zone ?: ZoneId.systemDefault()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Reminder reliability",
            subtitle = "What Android allows and what it does not",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Checklist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        ChecklistRow("Notifications allowed", state.notificationsAllowed)
                        ChecklistRow("Prayer channel enabled", state.adhanChannelEnabled)
                        ChecklistRow("Exact alarms allowed", state.exactAlarmAvailable)
                        ChecklistRow("Battery unrestricted", state.batteryOptimizationIgnored)
                        ChecklistRow("Background location allowed", state.backgroundLocationGranted)
                        ChecklistRow("Reminders need repair", !settings.remindersNeedRepair)
                        ChecklistRow("Boot receiver registered", true)
                    }
                }
            }
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Last rebuild", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        LabeledValue("Result", if (settings.lastRebuildSucceeded) "Succeeded" else "Failed")
                        LabeledValue("Reason", settings.lastRebuildReason.ifBlank { "Not recorded" })
                        LabeledValue("When", lastRebuild ?: "Not yet")
                        LabeledValue("Timetable revision", "${state.today?.scheduleRevision ?: 0}")
                        LabeledValue("Active mosque", state.today?.mosque?.name ?: "Calculated fallback")
                        LabeledValue("Next prayer", state.today?.next?.prayer?.title ?: "None")
                        val next = state.today?.next
                        if (next != null) {
                            LabeledValue(
                                "Next instant",
                                com.example.core.time.formatClock(next.instant, zone)
                            )
                        }
                    }
                }
            }
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SecondaryButton("Rebuild reminders now", onClick = { viewModel.rebuildReminders() })
                        SecondaryButton("Allow exact alarms", onClick = permissions.openExactAlarmSettings)
                        SecondaryButton("Battery optimisation settings", onClick = permissions.openBatterySettings)
                        SecondaryButton("App notification settings", onClick = permissions.openAppNotificationSettings)
                        SecondaryButton("Request background location", onClick = permissions.requestBackgroundLocation)
                    }
                }
            }
            item {
                InfoBanner(
                    "The app cannot force delivery on a device whose firmware aggressively stops background apps. " +
                        "If reminders are late, allow exact alarms and remove battery restrictions. Reliability reports " +
                        "only what it can actually detect."
                )
            }
        }
    }
}

@Composable
private fun ChecklistRow(label: String, ok: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Pill(if (ok) "OK" else "Action needed")
    }
}

@Composable
fun BackupScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingRaw by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<BackupPreview?>(null) }
    var localMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = viewModel.exportBackup()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
            }.onSuccess {
                localMessage = "Backup written to the selected file."
            }.onFailure {
                localMessage = "The backup file could not be written."
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.onSuccess { raw ->
            if (raw.isNullOrBlank()) {
                localMessage = "The selected file was empty."
            } else {
                pendingRaw = raw
                preview = viewModel.previewBackup(raw)
            }
        }.onFailure {
            localMessage = "The selected file could not be read."
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Backup & restore",
            subtitle = "Your data stays in a file you choose",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Includes places, mosques, timetables, overrides, reminder preferences, prayer history, Qada " +
                                "records, Quran entries and settings. Audio files and the Android database are not copied.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SecondaryButton(
                            "Export backup file",
                            onClick = { exportLauncher.launch("namaz-times-backup.json") },
                            enabled = !state.busy
                        )
                    }
                }
            }
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Merge adds records and keeps what is already here. Replace wipes current records first, but " +
                                "writes an automatic safety backup to the app cache before changing anything.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SecondaryButton(
                            "Choose a backup file",
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                            enabled = !state.busy
                        )
                    }
                }
            }
            localMessage?.let { item { InfoBanner(it) } }
            item {
                InfoBanner(
                    "A backup is plain JSON. Anyone with the file can read it. Keep it wherever you keep private data; " +
                        "the app never uploads it."
                )
            }
        }
    }

    preview?.let { result ->
        AlertDialog(
            onDismissRequest = {
                if (!state.busy) {
                    preview = null
                    pendingRaw = null
                }
            },
            title = { Text(if (result.valid) "Import preview" else "This file cannot be imported") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (result.valid) {
                        LabeledValue("Places", "${result.places}")
                        LabeledValue("Mosques", "${result.mosques}")
                        LabeledValue("Timetables", "${result.schedules}")
                        LabeledValue("Timetable entries", "${result.entries}")
                        LabeledValue("Overrides", "${result.overrides}")
                        LabeledValue("Prayer history", "${result.history}")
                        LabeledValue("Qada records", "${result.qada}")
                        LabeledValue("Quran entries", "${result.quranSessions}")
                        LabeledValue("Settings included", if (result.hasSettings) "Yes" else "No")
                        Text(
                            "Merge keeps existing records. Replace removes them after writing a safety backup.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        result.errors.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            },
            confirmButton = {
                if (result.valid) {
                    TextButton(
                        enabled = !state.busy,
                        onClick = {
                            val raw = pendingRaw ?: return@TextButton
                            viewModel.importBackup(raw, replace = false) { imported ->
                                if (imported.isSuccess) {
                                    preview = null
                                    pendingRaw = null
                                    localMessage = "Backup merged."
                                }
                            }
                        }
                    ) { Text("Merge") }
                } else {
                    TextButton(onClick = { preview = null; pendingRaw = null }) { Text("Close") }
                }
            },
            dismissButton = {
                if (result.valid) {
                    TextButton(
                        enabled = !state.busy,
                        onClick = {
                            val raw = pendingRaw ?: return@TextButton
                            viewModel.importBackup(raw, replace = true) { imported ->
                                if (imported.isSuccess) {
                                    preview = null
                                    pendingRaw = null
                                    localMessage = "Backup replaced current records."
                                }
                            }
                        }
                    ) { Text("Replace") }
                } else {
                    TextButton(onClick = { preview = null; pendingRaw = null }) { Text("Close") }
                }
            }
        )
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "About Namaz Times", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Namaz Times", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Version ${versionName ?: "unknown"} · Android · Kotlin & Jetpack Compose",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "A local-first prayer companion. No account, no ads, no analytics, and no network requests at runtime.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Time accuracy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Mosque-entered times are displayed as the mosque's timetable and are authoritative for this app. " +
                                "Calculated times are fallback estimates based on astronomical calculation and are always labelled. " +
                                "The app does not fabricate mosque times, Iqamah times or congregation details.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Your places, timetables, history, Quran ledger, settings and backups are stored on this device. " +
                                "Location is used only to resolve the selected place and mosque; only the latest decision and " +
                                "last fix are kept, not a movement history. Notifications and alarms are scheduled with Android.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Sources & attribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Prayer calculations use the Batoulapps Adhan library (MIT). Hijri conversion uses an Umm al-Qura " +
                                "tabular estimate, adjustable by ±2 days. Quran tracking counts Rukus you record and makes no " +
                                "claim about reward or religious obligation.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                InfoBanner(
                    "Nothing here is a religious ruling. For times and practice, follow your mosque and the scholar you trust."
                )
            }
        }
    }
}
