package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.design.EmptyState
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
import com.example.core.design.TimeEntryField
import com.example.domain.model.ManualOverride
import com.example.domain.model.MosqueProfile
import com.example.domain.model.MosqueSchedule
import com.example.domain.model.PrayerName
import com.example.domain.model.ScheduleEntry
import com.example.domain.model.ScheduleKind
import com.example.domain.model.WallClock
import com.example.prayer.MosqueScheduleResolver
import com.example.prayer.validatePrayerEntries
import com.example.ui.AppUiState
import com.example.ui.AppViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Composable
fun MosqueListScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onEdit: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<MosqueProfile?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.onSuccess { raw ->
            if (raw.isNullOrBlank()) message = "The selected file was empty." else viewModel.importMosqueJson(raw)
        }.onFailure { message = "The selected file could not be read." }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Mosque profiles",
            subtitle = "Entered timetables are authoritative",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("Add mosque profile", onClick = { onEdit(null) })
                    SecondaryButton("Import mosque profile", onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) })
                    InfoBanner(
                        "The app never invents mosque times. Any prayer field you leave empty stays missing and is labelled " +
                            "«Not entered», even when calculated times are available as a fallback."
                    )
                }
            }

            if (state.snapshot.mosques.isEmpty()) {
                item {
                    EmptyState(
                        title = "No mosques yet",
                        body = "Add your mosque once, then enter its Adhan and Iqamah times. You can link saved places to it.",
                        actionLabel = "Add mosque",
                        onAction = { onEdit(null) }
                    )
                }
            } else {
                items(state.snapshot.mosques, key = { it.id }) { mosque ->
                    val schedules = state.snapshot.schedulesFor(mosque.id)
                    var menuExpanded by remember { mutableStateOf(false) }
                    FrostedCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(mosque.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(8.dp))
                                if (!mosque.isEnabled) Pill("Disabled")
                                if (mosque.isArchived) Pill("Archived")
                                Spacer(Modifier.weight(1f))
                                Text("v${mosque.scheduleRevision}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (mosque.addressLabel.isNotBlank()) {
                                Text(mosque.addressLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LabeledValue(
                                "Timetables",
                                if (schedules.isEmpty()) "None entered" else schedules.joinToString(", ") { it.kind.title }
                            )
                            LabeledValue(
                                "Linked places",
                                "${state.snapshot.places.count { it.mosqueProfileId == mosque.id }}"
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { onEdit(mosque.id) }) { Text("Timetable") }
                                TextButton(onClick = { viewModel.selectManual(mosque.id, null) }) { Text("Use") }
                                TextButton(onClick = {
                                    scope.launch {
                                        val json = viewModel.exportMosqueJson(mosque.id)
                                        if (json == null) {
                                            message = "That mosque could not be exported."
                                        } else {
                                            val intent = Intent(Intent.ACTION_SEND)
                                                .setType("text/plain")
                                                .putExtra(Intent.EXTRA_SUBJECT, "${mosque.name} timetable")
                                                .putExtra(Intent.EXTRA_TEXT, json)
                                            context.startActivity(Intent.createChooser(intent, "Export mosque profile"))
                                        }
                                    }
                                }) { Text("Export") }
                                TextButton(onClick = { deleteTarget = mosque }) { Text("Delete") }
                            }
                            SettingSwitchRow(
                                title = "Enabled",
                                subtitle = "Disabled mosques stop sending reminders but keep their timetable.",
                                checked = mosque.isEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.saveMosque(mosque.copy(isEnabled = enabled))
                                }
                            )
                            SettingSwitchRow(
                                title = "Archived",
                                subtitle = "Archived mosques are hidden from selection but kept for history.",
                                checked = mosque.isArchived,
                                onCheckedChange = { archived ->
                                    viewModel.saveMosque(mosque.copy(isArchived = archived))
                                }
                            )
                        }
                    }
                }
            }

            message?.let { item { InfoBanner(it, isWarning = true) } }
            item {
                InfoBanner(
                    "Export writes this profile's timetables as JSON text that you can save or send. " +
                        "Imported profiles always arrive as a new mosque, never overwriting an existing one."
                )
            }
        }
    }

    deleteTarget?.let { mosque ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${mosque.name}?") },
            text = { Text("Its timetables and overrides are deleted. Saved places are unlinked, and prayer history is kept.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMosque(mosque.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun MosqueEditorScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    mosqueId: String?,
    onBack: () -> Unit
) {
    val existing = state.snapshot.mosque(mosqueId)
    val newId = remember { UUID.randomUUID().toString() }
    var savedId by remember(mosqueId) { mutableStateOf(mosqueId) }
    var profileError by remember { mutableStateOf<String?>(null) }

    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var address by remember(existing?.id) { mutableStateOf(existing?.addressLabel ?: "") }
    var latitude by remember(existing?.id) { mutableStateOf(existing?.latitude?.toString() ?: "") }
    var longitude by remember(existing?.id) { mutableStateOf(existing?.longitude?.toString() ?: "") }
    var timezone by remember(existing?.id) { mutableStateOf(existing?.timezoneId ?: ZoneId.systemDefault().id) }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes ?: "") }
    var priority by remember(existing?.id) { mutableStateOf((existing?.userPriority ?: 100).toString()) }
    var enabled by remember(existing?.id) { mutableStateOf(existing?.isEnabled ?: true) }
    var archived by remember(existing?.id) { mutableStateOf(existing?.isArchived ?: false) }

    val schedules = state.snapshot.schedulesFor(savedId.orEmpty())
    var selectedScheduleId by remember { mutableStateOf<String?>(null) }
    var draftKind by remember { mutableStateOf(ScheduleKind.FIXED) }
    var draftLabel by remember { mutableStateOf("") }
    var draftWeekday by remember { mutableStateOf<Int?>(null) }
    var draftFrom by remember { mutableStateOf("") }
    var draftUntil by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(blankEntries("")) }
    var showNewScheduleDialog by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var showIqamahWarning by remember { mutableStateOf(false) }
    var deleteScheduleTarget by remember { mutableStateOf<MosqueSchedule?>(null) }
    var previewSchedule by remember { mutableStateOf<MosqueSchedule?>(null) }

    fun loadSchedule(schedule: MosqueSchedule) {
        selectedScheduleId = schedule.id
        draftKind = schedule.kind
        draftLabel = schedule.label
        draftWeekday = schedule.weekday
        draftFrom = schedule.effectiveFrom?.toString().orEmpty()
        draftUntil = schedule.effectiveUntil?.toString().orEmpty()
        entries = state.snapshot.entriesFor(schedule.id).ifEmpty { blankEntries(schedule.id) }
        saveError = null
    }

    fun startNewSchedule() {
        selectedScheduleId = null
        draftKind = ScheduleKind.FIXED
        draftLabel = ""
        draftWeekday = null
        draftFrom = ""
        draftUntil = ""
        entries = blankEntries("")
        saveError = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = if (existing == null && savedId == null) "New mosque" else (name.ifBlank { "Mosque" }),
                subtitle = "Profile, timetables and today's overrides",
                onBack = onBack
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Mosque profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Mosque name") }, singleLine = true)
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address / area") }, singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = latitude,
                                onValueChange = { latitude = it },
                                label = { Text("Latitude (optional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = longitude,
                                onValueChange = { longitude = it },
                                label = { Text("Longitude (optional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(value = timezone, onValueChange = { timezone = it }, label = { Text("Timezone (IANA)") }, singleLine = true)
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, minLines = 2)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberEntryField("Priority", priority, { priority = it }, Modifier.weight(1f))
                        }
                        SettingSwitchRow("Enabled", "Disabled mosques stop reminders but keep their data.", enabled, { enabled = it })
                        SettingSwitchRow("Archived", "Hidden from selection, retained with history.", archived, { archived = it })
                        profileError?.let { InfoBanner(it, isWarning = true) }
                        PrimaryButton("Save mosque profile", onClick = {
                            val trimmed = name.trim()
                            if (trimmed.isEmpty()) {
                                profileError = "Enter the mosque name."
                                return@PrimaryButton
                            }
                            val lat = latitude.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                            val lon = longitude.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                            if ((lat == null) != (lon == null)) {
                                profileError = "Enter both coordinates or leave both empty."
                                return@PrimaryButton
                            }
                            val zone = runCatching { ZoneId.of(timezone.trim()) }.getOrNull() ?: run {
                                profileError = "Enter a valid timezone such as Asia/Karachi."
                                return@PrimaryButton
                            }
                            val id = savedId ?: newId
                            val profile = MosqueProfile(
                                id = id,
                                name = trimmed,
                                addressLabel = address.trim(),
                                latitude = lat,
                                longitude = lon,
                                timezoneId = zone.id,
                                isEnabled = enabled,
                                isArchived = archived,
                                userPriority = priority.toIntOrNull()?.coerceIn(0, 9999) ?: 100,
                                notes = notes.trim(),
                                scheduleRevision = existing?.scheduleRevision ?: 0L,
                                createdAt = existing?.createdAt ?: 0L,
                                updatedAt = System.currentTimeMillis()
                            )
                            viewModel.saveMosque(profile)
                            savedId = id
                            profileError = null
                        })
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionTitle("Timetables")
                if (savedId == null) {
                    InfoBanner("Save the mosque profile first, then enter its Adhan and Iqamah times.")
                } else {
                    FrostedCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (schedules.isEmpty()) {
                                Text(
                                    "No timetable yet. Fields you leave blank stay missing.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            schedules.forEach { schedule ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SecondaryButton(
                                        text = "${schedule.label.ifBlank { schedule.kind.title }}${if (schedule.id == selectedScheduleId) " · editing" else ""}",
                                        onClick = { loadSchedule(schedule) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(onClick = { previewSchedule = schedule }) { Text("Preview") }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { showNewScheduleDialog = true; startNewSchedule() }) { Text("New timetable") }
                                if (selectedScheduleId != null) {
                                    TextButton(onClick = {
                                        schedules.firstOrNull { it.id == selectedScheduleId }?.let { deleteScheduleTarget = it }
                                    }) { Text("Delete timetable") }
                                }
                            }
                            InfoBanner(
                                "Precedence on any date: one-date override → date range → weekly → fixed daily. " +
                                    "Adhan and Iqamah resolve independently; a blank field falls through without being invented."
                            )
                        }
                    }
                }
            }
        }

        if (savedId != null) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    FrostedCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                if (selectedScheduleId == null) "New timetable entries" else "Edit timetable",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ScheduleKind.entries.forEach { kind ->
                                    FilterChip(
                                        selected = draftKind == kind,
                                        onClick = { draftKind = kind },
                                        label = { Text(kind.title, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            if (draftKind == ScheduleKind.WEEKLY) {
                                Text("Weekday", style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    (1..7).forEach { day ->
                                        FilterChip(
                                            selected = draftWeekday == day,
                                            onClick = { draftWeekday = day },
                                            label = { Text(dayName(day), style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                            if (draftKind == ScheduleKind.DATE_RANGE || draftKind == ScheduleKind.DATE_OVERRIDE) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = draftFrom,
                                        onValueChange = { draftFrom = it },
                                        label = { Text(if (draftKind == ScheduleKind.DATE_OVERRIDE) "Date" else "From") },
                                        placeholder = { Text("yyyy-MM-dd") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (draftKind == ScheduleKind.DATE_RANGE) {
                                        OutlinedTextField(
                                            value = draftUntil,
                                            onValueChange = { draftUntil = it },
                                            label = { Text("Until") },
                                            placeholder = { Text("yyyy-MM-dd") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(value = draftLabel, onValueChange = { draftLabel = it }, label = { Text("Label (optional)") }, singleLine = true)

                            PrayerName.obligatories.forEach { prayer ->
                                val entry = entries.firstOrNull { it.prayer == prayer }
                                    ?: ScheduleEntry(scheduleId = selectedScheduleId.orEmpty(), prayer = prayer)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(prayer.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.width(6.dp))
                                        Text(prayer.arabicTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TimeEntryField(
                                            label = "Adhan",
                                            value = entry.adhan,
                                            onValueChange = { value -> entries = upsertEntry(entries, entry.copy(adhan = value)) },
                                            modifierWeight = true
                                        )
                                        TimeEntryField(
                                            label = "Iqamah",
                                            value = entry.iqamah,
                                            onValueChange = { value -> entries = upsertEntry(entries, entry.copy(iqamah = value)) },
                                            modifierWeight = true
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FilterChip(
                                            selected = entry.adhanReminder,
                                            onClick = { entries = upsertEntry(entries, entry.copy(adhanReminder = !entry.adhanReminder)) },
                                            label = { Text("Adhan alert", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = entry.iqamahReminder,
                                            onClick = { entries = upsertEntry(entries, entry.copy(iqamahReminder = !entry.iqamahReminder)) },
                                            label = { Text("Iqamah alert", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                    if (prayer == PrayerName.DHUHR) {
                                        Text("Friday / Jumuah fields", style = MaterialTheme.typography.labelMedium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TimeEntryField(
                                                label = "Khutbah",
                                                value = entry.khutbah,
                                                onValueChange = { value -> entries = upsertEntry(entries, entry.copy(khutbah = value)) },
                                                modifierWeight = true
                                            )
                                            TimeEntryField(
                                                label = "Jumuah",
                                                value = entry.jumuah,
                                                onValueChange = { value -> entries = upsertEntry(entries, entry.copy(jumuah = value)) },
                                                modifierWeight = true
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TimeEntryField(
                                                label = "Second Jumuah",
                                                value = entry.secondJumuah,
                                                onValueChange = { value -> entries = upsertEntry(entries, entry.copy(secondJumuah = value)) },
                                                modifierWeight = true
                                            )
                                            OutlinedTextField(
                                                value = entry.fridayNote,
                                                onValueChange = { value -> entries = upsertEntry(entries, entry.copy(fridayNote = value)) },
                                                label = { Text("Friday note") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            saveError?.let { InfoBanner(it, isWarning = true) }
                            PrimaryButton("Save timetable revision", onClick = {
                                val mosque = savedId?.let { state.snapshot.mosque(it) }
                                if (mosque == null) {
                                    saveError = "Save the mosque profile first."
                                    return@PrimaryButton
                                }
                                val scheduleId = selectedScheduleId ?: UUID.randomUUID().toString()
                                val from = draftFrom.trim().takeIf { it.isNotEmpty() }?.let { raw ->
                                    runCatching { LocalDate.parse(raw) }.getOrNull()
                                }
                                val until = draftUntil.trim().takeIf { it.isNotEmpty() }?.let { raw ->
                                    runCatching { LocalDate.parse(raw) }.getOrNull()
                                }
                                if ((draftKind == ScheduleKind.DATE_RANGE || draftKind == ScheduleKind.DATE_OVERRIDE) && from == null) {
                                    saveError = "Enter the start date as yyyy-MM-dd."
                                    return@PrimaryButton
                                }
                                if (draftKind == ScheduleKind.DATE_RANGE && until == null) {
                                    saveError = "Enter the end date as yyyy-MM-dd."
                                    return@PrimaryButton
                                }
                                val schedule = MosqueSchedule(
                                    id = scheduleId,
                                    mosqueId = mosque.id,
                                    kind = draftKind,
                                    label = draftLabel.trim(),
                                    weekday = if (draftKind == ScheduleKind.WEEKLY) draftWeekday else null,
                                    effectiveFrom = from,
                                    effectiveUntil = if (draftKind == ScheduleKind.DATE_RANGE) until else null,
                                    revision = schedules.firstOrNull { it.id == scheduleId }?.revision ?: 0L,
                                    isActive = true,
                                    createdAt = schedules.firstOrNull { it.id == scheduleId }?.createdAt ?: 0L,
                                    updatedAt = System.currentTimeMillis()
                                )
                                val normalized = entries.map { it.copy(scheduleId = scheduleId) }
                                val validation = validatePrayerEntries(normalized, confirmIqamahBeforeAdhan = true)
                                if (!validation.ok) {
                                    saveError = validation.errors.first()
                                    return@PrimaryButton
                                }
                                if (validation.warnings.isNotEmpty()) {
                                    showIqamahWarning = true
                                    return@PrimaryButton
                                }
                                viewModel.saveSchedule(mosque.id, schedule, normalized, confirmIqamahBeforeAdhan = true)
                                selectedScheduleId = scheduleId
                                saveError = null
                            })
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionTitle("Today's manual override")
                    FrostedCard {
                        var overridePrayer by remember { mutableStateOf(PrayerName.FAJR) }
                        var overrideAdhan by remember { mutableStateOf<String?>(null) }
                        var overrideIqamah by remember { mutableStateOf<String?>(null) }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "A one-date override wins over every timetable layer for that prayer. Clearing both fields removes it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                PrayerName.obligatories.forEach { prayer ->
                                    FilterChip(
                                        selected = overridePrayer == prayer,
                                        onClick = { overridePrayer = prayer },
                                        label = { Text(prayer.title, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TimeEntryField("Adhan", overrideAdhan, { overrideAdhan = it }, true)
                                TimeEntryField("Iqamah", overrideIqamah, { overrideIqamah = it }, true)
                            }
                            SecondaryButton("Save today's override", onClick = {
                                val id = savedId ?: return@SecondaryButton
                                viewModel.saveManualOverride(
                                    ManualOverride(
                                        mosqueId = id,
                                        date = LocalDate.now(),
                                        prayer = overridePrayer,
                                        adhan = overrideAdhan,
                                        iqamah = overrideIqamah
                                    )
                                )
                            })
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                InfoBanner(
                    "Fajr 05:30 → 05:20 replaces only the current and future occurrences from today and re-arms the reminders " +
                        "for the new revision. Earlier occurrences are left untouched."
                )
            }
        }
    }

    if (showNewScheduleDialog) {
        NewScheduleDialog(
            onDismiss = { showNewScheduleDialog = false },
            onCreate = { kind ->
                startNewSchedule()
                draftKind = kind
                showNewScheduleDialog = false
            }
        )
    }

    if (showIqamahWarning) {
        val warnings = validatePrayerEntries(entries, confirmIqamahBeforeAdhan = true).warnings
        AlertDialog(
            onDismissRequest = { showIqamahWarning = false },
            title = { Text("Confirm unusual times") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    Text(
                        "This is an explicit exception, not an automatic correction. If these times are intentional, save anyway.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showIqamahWarning = false
                    val mosque = savedId?.let { state.snapshot.mosque(it) }
                    if (mosque != null) {
                        val scheduleId = selectedScheduleId ?: UUID.randomUUID().toString()
                        val schedule = MosqueSchedule(
                            id = scheduleId,
                            mosqueId = mosque.id,
                            kind = draftKind,
                            label = draftLabel.trim(),
                            weekday = if (draftKind == ScheduleKind.WEEKLY) draftWeekday else null,
                            effectiveFrom = draftFrom.trim().takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                            effectiveUntil = draftUntil.trim().takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                            revision = schedules.firstOrNull { it.id == scheduleId }?.revision ?: 0L,
                            createdAt = schedules.firstOrNull { it.id == scheduleId }?.createdAt ?: 0L,
                            updatedAt = System.currentTimeMillis()
                        )
                        viewModel.saveSchedule(
                            mosque.id,
                            schedule,
                            entries.map { it.copy(scheduleId = scheduleId) },
                            confirmIqamahBeforeAdhan = true
                        )
                        selectedScheduleId = scheduleId
                    }
                }) { Text("Save anyway") }
            },
            dismissButton = { TextButton(onClick = { showIqamahWarning = false }) { Text("Review") } }
        )
    }

    deleteScheduleTarget?.let { schedule ->
        AlertDialog(
            onDismissRequest = { deleteScheduleTarget = null },
            title = { Text("Delete this timetable?") },
            text = { Text("The timetable entries are removed and the mosque's revision is bumped so stale alarms are cancelled.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSchedule(schedule)
                    if (selectedScheduleId == schedule.id) startNewSchedule()
                    deleteScheduleTarget = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteScheduleTarget = null }) { Text("Cancel") } }
        )
    }

    previewSchedule?.let { schedule ->
        val mosque = savedId?.let { state.snapshot.mosque(it) }
        if (mosque != null) {
            val resolved = remember(schedule.id, state.snapshot.entries, state.snapshot.overrides) {
                MosqueScheduleResolver().resolve(
                    LocalDate.now(),
                    mosque,
                    state.snapshot.schedulesFor(mosque.id),
                    state.snapshot.entries,
                    state.snapshot.overrides
                )
            }
            AlertDialog(
                onDismissRequest = { previewSchedule = null },
                title = { Text("Today · ${schedule.label.ifBlank { schedule.kind.title }}") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PrayerName.obligatories.forEach { prayer ->
                            val slot = resolved.slots[prayer]
                            LabeledValue(
                                prayer.title,
                                "Adhan ${WallClock.format(slot?.adhan) ?: "Not entered"} · Iqamah ${WallClock.format(slot?.iqamah) ?: "Not entered"}"
                            )
                        }
                        Text(
                            "Saved timetable only. Unsaved edits are not shown.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = { TextButton(onClick = { previewSchedule = null }) { Text("Close") } }
            )
        }
    }
}

@Composable
private fun NewScheduleDialog(onDismiss: () -> Unit, onCreate: (ScheduleKind) -> Unit) {
    var kind by remember { mutableStateOf(ScheduleKind.FIXED) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a timetable layer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScheduleKind.entries.forEach { option ->
                    FilterChip(
                        selected = kind == option,
                        onClick = { kind = option },
                        label = { Text(option.title) }
                    )
                }
                Text(
                    when (kind) {
                        ScheduleKind.FIXED -> "One normal daily timetable. A mosque can have only one fixed layer."
                        ScheduleKind.WEEKLY -> "Different times for a chosen weekday, such as Friday."
                        ScheduleKind.DATE_RANGE -> "Seasonal times between two dates, such as Ramadan."
                        ScheduleKind.DATE_OVERRIDE -> "One specific date that beats every other layer."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(kind) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun blankEntries(scheduleId: String): List<ScheduleEntry> =
    PrayerName.obligatories.map { ScheduleEntry(scheduleId = scheduleId, prayer = it) }

private fun upsertEntry(entries: List<ScheduleEntry>, entry: ScheduleEntry): List<ScheduleEntry> {
    val others = entries.filter { it.prayer != entry.prayer }
    return (others + entry).sortedBy { it.prayer.ordinal }
}

private fun dayName(isoDay: Int): String = when (isoDay) {
    1 -> "Mon"
    2 -> "Tue"
    3 -> "Wed"
    4 -> "Thu"
    5 -> "Fri"
    6 -> "Sat"
    else -> "Sun"
}
