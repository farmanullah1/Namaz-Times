package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.core.design.FrostedCard
import com.example.core.design.InfoBanner
import com.example.core.design.LabeledValue
import com.example.core.design.PrimaryButton
import com.example.core.design.SecondaryButton
import com.example.core.design.SettingSwitchRow
import com.example.core.design.SimpleProgressBar
import com.example.core.design.TimeEntryField
import com.example.domain.model.GeoMath
import com.example.domain.model.LocationMode
import com.example.domain.model.MosqueProfile
import com.example.domain.model.PrayerName
import com.example.domain.model.SavedPlace
import com.example.domain.model.ScheduleEntry
import com.example.navigation.PermissionActions
import com.example.ui.AppUiState
import com.example.ui.AppViewModel
import java.time.ZoneId
import java.util.UUID

private const val ONBOARDING_STEPS = 9

@Composable
fun OnboardingScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    permissions: PermissionActions,
    onFinish: () -> Unit
) {
    var step by remember { mutableStateOf(0) }

    var placeName by remember { mutableStateOf("") }
    var placeLatitude by remember { mutableStateOf("") }
    var placeLongitude by remember { mutableStateOf("") }

    var mosqueName by remember { mutableStateOf("") }
    var mosqueAddress by remember { mutableStateOf("") }
    var mosqueTimezone by remember { mutableStateOf(ZoneId.systemDefault().id) }
    var mosqueLatitude by remember { mutableStateOf("") }
    var mosqueLongitude by remember { mutableStateOf("") }

    var entries by remember {
        mutableStateOf(PrayerName.obligatories.map { ScheduleEntry(scheduleId = "", prayer = it) })
    }
    var modeChoice by remember { mutableStateOf("MOSQUE") }
    var reminders by remember { mutableStateOf(false) }
    var automation by remember { mutableStateOf(false) }
    var silence by remember { mutableStateOf(false) }
    var silenceMinutes by remember { mutableStateOf("10") }
    var finishError by remember { mutableStateOf<String?>(null) }

    fun finish() {
        val latitude = mosqueLatitude.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val longitude = mosqueLongitude.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        if ((latitude == null) != (longitude == null)) {
            finishError = "Enter both mosque coordinates, or leave both empty."
            return
        }
        if (latitude != null && (!GeoMath.isValidLatitude(latitude) || !GeoMath.isValidLongitude(longitude!!))) {
            finishError = "Coordinates must be valid latitude and longitude values."
            return
        }
        val placeLat = placeLatitude.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val placeLon = placeLongitude.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        if ((placeLat == null) != (placeLon == null)) {
            finishError = "Enter both place coordinates, or leave both empty."
            return
        }
        val zone = runCatching { ZoneId.of(mosqueTimezone.trim()) }.getOrNull()
        if (mosqueName.isNotBlank() && zone == null) {
            finishError = "Enter a valid IANA timezone such as Asia/Karachi."
            return
        }
        val resolvedZone = zone ?: ZoneId.systemDefault()
        val mosque = if (mosqueName.isNotBlank()) {
            MosqueProfile(
                id = UUID.randomUUID().toString(),
                name = mosqueName.trim(),
                addressLabel = mosqueAddress.trim(),
                latitude = latitude,
                longitude = longitude,
                timezoneId = resolvedZone.id,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        } else {
            null
        }
        val place = if (placeName.isNotBlank()) {
            SavedPlace(
                id = UUID.randomUUID().toString(),
                displayName = placeName.trim(),
                latitude = placeLat,
                longitude = placeLon,
                timezoneId = resolvedZone.id,
                mosqueProfileId = mosque?.id,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        } else {
            null
        }
        val mode = if (modeChoice == "AUTO") LocationMode.AUTOMATIC else LocationMode.MANUAL
        val minutes = silenceMinutes.toIntOrNull()?.coerceIn(1, 60) ?: 10
        viewModel.finishOnboarding(
            mosque = mosque,
            place = place,
            scheduleEntries = entries.map { it.copy(scheduleId = "") },
            mode = mode,
            remindersEnabled = reminders,
            automaticSwitching = automation,
            silenceEnabled = silence,
            silenceMinutes = minutes,
            useCalculatedFallback = modeChoice == "CALCULATED"
        )
        onFinish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                "Step ${step + 1} of $ONBOARDING_STEPS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        SimpleProgressBar(progress = (step + 1).toFloat() / ONBOARDING_STEPS)
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when (step) {
                0 -> {
                    Text("Namaz Times", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "A local-first prayer companion built around your mosque's own timetable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    FrostedCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("What it does", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("• Your mosque's entered timetable is authoritative.")
                            Text("• Calculated times are a fallback and are always labelled.")
                            Text("• Empty Iqamah fields stay empty — the app never invents them.")
                            Text("• Prayer completion, Quran reading and Qada are tracked only when you record them.")
                            Text("• No account, ads, analytics or network requests at runtime.")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    InfoBanner(
                        "You can skip any step. Every choice here is available later in More and Places."
                    )
                }

                1 -> {
                    Text("Your place", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "A saved place is an area, not a mosque. It can be linked to a mosque timetable later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = placeName,
                        onValueChange = { placeName = it },
                        label = { Text("Place name") },
                        placeholder = { Text("Home, Defence, Office") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = placeLatitude,
                            onValueChange = { placeLatitude = it },
                            label = { Text("Latitude (optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = placeLongitude,
                            onValueChange = { placeLongitude = it },
                            label = { Text("Longitude (optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    state.settings.lastLocation?.let { last ->
                        SecondaryButton("Use last known location", onClick = {
                            placeLatitude = last.latitude.toString()
                            placeLongitude = last.longitude.toString()
                        })
                    }
                    if (!state.locationPermissionGranted) {
                        SecondaryButton("Allow location", onClick = permissions.requestForegroundLocation)
                    }
                    Spacer(Modifier.height(8.dp))
                    InfoBanner("Coordinates are optional. Without them, this place is available for manual selection.")
                }

                2 -> {
                    Text("Your mosque", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Enter the mosque once. Its Adhan and Iqamah times come next.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = mosqueName,
                        onValueChange = { mosqueName = it },
                        label = { Text("Mosque name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mosqueAddress,
                        onValueChange = { mosqueAddress = it },
                        label = { Text("Address / area") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mosqueTimezone,
                        onValueChange = { mosqueTimezone = it },
                        label = { Text("Timezone (IANA)") },
                        placeholder = { Text("Asia/Karachi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = mosqueLatitude,
                            onValueChange = { mosqueLatitude = it },
                            label = { Text("Latitude (optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = mosqueLongitude,
                            onValueChange = { mosqueLongitude = it },
                            label = { Text("Longitude (optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.settings.lastLocation != null && mosqueLatitude.isBlank()) {
                        SecondaryButton("Use last known mosque coordinates", onClick = {
                            val last = state.settings.lastLocation ?: return@SecondaryButton
                            mosqueLatitude = last.latitude.toString()
                            mosqueLongitude = last.longitude.toString()
                        })
                    }
                    InfoBanner("Leave the name empty if you only want calculated times for now.")
                }

                3 -> {
                    Text("Mosque timetable", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Enter 24-hour times. Leave any field blank if the mosque has not published it — blanks stay missing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    PrayerName.obligatories.forEach { prayer ->
                        val entry = entries.firstOrNull { it.prayer == prayer }
                            ?: ScheduleEntry(scheduleId = "", prayer = prayer)
                        Spacer(Modifier.height(10.dp))
                        Text(prayer.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeEntryField(
                                label = "Adhan",
                                value = entry.adhan,
                                onValueChange = { value -> entries = entries.replace(entry.copy(adhan = value)) },
                                modifier = Modifier.weight(1f)
                            )
                            TimeEntryField(
                                label = "Iqamah",
                                value = entry.iqamah,
                                onValueChange = { value -> entries = entries.replace(entry.copy(iqamah = value)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (prayer == PrayerName.DHUHR) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TimeEntryField(
                                    label = "Khutbah",
                                    value = entry.khutbah,
                                    onValueChange = { value -> entries = entries.replace(entry.copy(khutbah = value)) },
                                    modifier = Modifier.weight(1f)
                                )
                                TimeEntryField(
                                    label = "Jumuah",
                                    value = entry.jumuah,
                                    onValueChange = { value -> entries = entries.replace(entry.copy(jumuah = value)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    InfoBanner("If both Adhan and Iqamah are blank for a prayer, no reminder is created for it.")
                }

                4 -> {
                    Text("Timing mode", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "This chooses which times are displayed first. You can change it at any time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    ModeChoiceCard(
                        title = "Mosque timetable",
                        body = "Use the entered mosque times. Missing fields fall back to calculations but stay labelled as not entered.",
                        selected = modeChoice == "MOSQUE",
                        onClick = { modeChoice = "MOSQUE" }
                    )
                    ModeChoiceCard(
                        title = "Automatic place switching",
                        body = "Switch mosques as you enter saved place boundaries. Requires background location.",
                        selected = modeChoice == "AUTO",
                        onClick = { modeChoice = "AUTO" }
                    )
                    ModeChoiceCard(
                        title = "Calculated times (travel)",
                        body = "Show labelled calculated times from your location. No mosque timetable is presented as current.",
                        selected = modeChoice == "CALCULATED",
                        onClick = { modeChoice = "CALCULATED" }
                    )
                    if (modeChoice == "MOSQUE" && mosqueName.isBlank()) {
                        InfoBanner("No mosque name entered. The app will use calculated times and stay clearly labelled.", isWarning = true)
                    }
                }

                5 -> {
                    Text("Prayer reminders", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Notifications are requested only when you turn them on. The app never sends marketing notifications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    FrostedCard {
                        SettingSwitchRow(
                            title = "Enable Adhan & Iqamah reminders",
                            subtitle = "Only for times present in the selected timetable. Android may ask for notification permission.",
                            checked = reminders,
                            onCheckedChange = { enabled ->
                                reminders = enabled
                                if (enabled) permissions.requestNotifications()
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    InfoBanner(
                        "You can disable reminders later. Exact alarms and battery settings are available in Reliability."
                    )
                }

                6 -> {
                    Text("Automatic switching", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Optional. Uses geofences and background location to select a mosque when you arrive at a saved place.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    FrostedCard {
                        SettingSwitchRow(
                            title = "Enable automatic place switching",
                            subtitle = "Without background location the app asks every time instead of switching silently.",
                            checked = automation,
                            onCheckedChange = { enabled ->
                                automation = enabled
                                if (enabled) {
                                    if (state.locationPermissionGranted) {
                                        permissions.requestBackgroundLocation()
                                    } else {
                                        permissions.requestForegroundLocation()
                                    }
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LabeledValue("Location permission", if (state.locationPermissionGranted) "Granted" else "Not granted")
                    LabeledValue("Background location", if (state.backgroundLocationGranted) "Granted" else "Not granted")
                    Spacer(Modifier.height(8.dp))
                    InfoBanner("Manual selection works fully without any location permission.")
                }

                7 -> {
                    Text("Silence during prayer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Optional and opt-in. If Do Not Disturb access is granted, the app can silence for a short window and then restore the previous state.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    FrostedCard {
                        SettingSwitchRow(
                            title = "Silence during prayer",
                            subtitle = "Android requires explicit Do Not Disturb access.",
                            checked = silence,
                            onCheckedChange = { silence = it }
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = silenceMinutes,
                                onValueChange = { silenceMinutes = it.filter { ch -> ch.isDigit() }.take(2) },
                                label = { Text("Minutes") },
                                singleLine = true,
                                modifier = Modifier.width(120.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            SecondaryButton("Open DND access", onClick = permissions.openDndSettings)
                        }
                    }
                }

                else -> {
                    Text("Ready", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    FrostedCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LabeledValue("Mosque", mosqueName.ifBlank { "Not added — calculated times" })
                            LabeledValue("Place", placeName.ifBlank { "Not added" })
                            LabeledValue(
                                "Mode",
                                when (modeChoice) {
                                    "AUTO" -> "Automatic place switching"
                                    "CALCULATED" -> "Calculated times (travel)"
                                    else -> "Mosque timetable"
                                }
                            )
                            LabeledValue("Reminders", if (reminders) "Enabled" else "Off")
                            LabeledValue("Automatic switching", if (automation) "Enabled" else "Off")
                            LabeledValue("Prayer silence", if (silence) "Enabled" else "Off")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    InfoBanner(
                        "The app starts with an empty prayer history and Quran ledger. Both fill only from what you record."
                    )
                    finishError?.let {
                        Spacer(Modifier.height(8.dp))
                        InfoBanner(it, isWarning = true)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.completeOnboarding(); onFinish() }) {
                        Text("Skip setup and finish")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) {
                SecondaryButton("Back", onClick = { step -= 1 }, modifier = Modifier.weight(1f))
            }
            PrimaryButton(
                text = if (step == ONBOARDING_STEPS - 1) "Finish setup" else "Continue",
                onClick = {
                    if (step == ONBOARDING_STEPS - 1) finish() else step += 1
                },
                modifier = Modifier.weight(1f)
            )
        }
        if (step < ONBOARDING_STEPS - 1) {
            TextButton(
                onClick = { viewModel.completeOnboarding(); onFinish() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Skip for now") }
        }
    }
}

@Composable
private fun ModeChoiceCard(
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        },
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun List<ScheduleEntry>.replace(entry: ScheduleEntry): List<ScheduleEntry> =
    (filter { it.prayer != entry.prayer } + entry).sortedBy { it.prayer.ordinal }
