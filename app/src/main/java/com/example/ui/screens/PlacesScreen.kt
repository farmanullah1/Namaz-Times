package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.core.design.Pill
import com.example.core.design.PrimaryButton
import com.example.core.design.SecondaryButton
import com.example.core.design.SectionTitle
import com.example.core.design.SettingSwitchRow
import com.example.domain.model.GeoMath
import com.example.domain.model.LocationMode
import com.example.domain.model.SavedPlace
import com.example.navigation.PermissionActions
import com.example.ui.AppUiState
import com.example.ui.AppViewModel
import java.time.ZoneId
import java.util.UUID

@Composable
fun PlacesScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    permissions: PermissionActions,
    onOpenMosqueEditor: (String?) -> Unit,
    onOpenMosqueList: () -> Unit
) {
    var editorTarget by remember { mutableStateOf<SavedPlace?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showTemporaryDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<SavedPlace?>(null) }
    var showBackgroundExplanation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Places", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Saved areas, linked mosques and selection mode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            FrostedCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Active selection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        Pill(modeLabel(state.settings))
                    }
                    LabeledValue("Mosque", state.today?.mosque?.name ?: "Calculated times")
                    LabeledValue("Place", state.today?.place?.displayName ?: "No saved place matched")
                    if (state.settings.lastSelectionReason.isNotBlank()) {
                        LabeledValue("Reason", state.settings.lastSelectionReason)
                    }
                    state.settings.lastDecision?.let { decision ->
                        LabeledValue(
                            "Last location decision",
                            "${decision.reason} · ${decision.confidence.lowercase()} confidence"
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("Manual", onClick = { viewModel.setConvenienceMode(LocationMode.MANUAL) }, modifier = Modifier.weight(1f))
                        SecondaryButton("Temporary", onClick = { showTemporaryDialog = true }, modifier = Modifier.weight(1f))
                    }
                    SecondaryButton(
                        "Use automatic switching",
                        onClick = {
                            if (!state.locationPermissionGranted) {
                                permissions.requestForegroundLocation()
                            } else if (!state.backgroundLocationGranted) {
                                showBackgroundExplanation = true
                            } else {
                                viewModel.enableAutomaticSwitching()
                            }
                        }
                    )
                    SecondaryButton("Refresh current location", onClick = { viewModel.refreshLocation() })
                    Text(
                        if (!state.locationPermissionGranted) {
                            "Location permission is not granted. Manual selection and saved places still work."
                        } else if (!state.locationServicesEnabled) {
                            "Location services are turned off on this device."
                        } else {
                            "Accuracy ${state.settings.lastLocation?.accuracyMeters?.toInt() ?: 0} m · " +
                                "threshold ${state.settings.accuracyThresholdMeters.toInt()} m"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    "Add place",
                    onClick = { editorTarget = null; showEditor = true },
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    "Mosque profiles",
                    onClick = onOpenMosqueList,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SecondaryButton(
                "Use calculated times for this location",
                onClick = { viewModel.useCalculatedForCurrentLocation() }
            )
        }

        item { SectionTitle("Saved places") }

        if (state.snapshot.places.isEmpty()) {
            item {
                FrostedCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No saved places", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Add Defence, Clifton, Hala New City, Home, Office or any area you choose. " +
                                "A place is not a mosque: link it to a mosque profile with its own timetable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SecondaryButton("Add your first place", onClick = { showEditor = true })
                    }
                }
            }
        } else {
            items(state.snapshot.places, key = { it.id }) { place ->
                PlaceCard(
                    place = place,
                    mosqueName = state.snapshot.mosque(place.mosqueProfileId)?.name,
                    onEdit = { editorTarget = place; showEditor = true },
                    onDelete = { deleteTarget = place },
                    onSelect = { viewModel.selectManual(place.mosqueProfileId, place.id) },
                    onToggle = { enabled -> viewModel.savePlace(place.copy(isEnabled = enabled)) },
                    onEditMosque = { place.mosqueProfileId?.let(onOpenMosqueEditor) }
                )
            }
        }

        item {
            FrostedCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Automatic switching", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    SettingSwitchRow(
                        title = "Enable automatic place switching",
                        subtitle = "Uses saved boundaries and requires background location. Never runs a continuous GPS service.",
                        checked = state.settings.automaticLocationEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (!state.locationPermissionGranted) {
                                    permissions.requestForegroundLocation()
                                } else if (!state.backgroundLocationGranted) {
                                    showBackgroundExplanation = true
                                } else {
                                    viewModel.enableAutomaticSwitching()
                                }
                            } else {
                                viewModel.setConvenienceMode(LocationMode.MANUAL)
                            }
                        }
                    )
                    Text(
                        when {
                            state.backgroundLocationGranted -> "Background location granted. Up to 95 highest-priority boundaries are registered."
                            state.locationPermissionGranted -> "Foreground location granted. Background access is needed only for automatic switching."
                            else -> "Location permission not granted. Add places manually and select a mosque yourself."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.locationPermissionGranted && !state.backgroundLocationGranted) {
                        SecondaryButton("Grant background location", onClick = { permissions.requestBackgroundLocation() })
                    }
                    if (state.backgroundLocationGranted) {
                        SecondaryButton("Re-register boundaries", onClick = { viewModel.syncGeofences() })
                    }
                }
            }
        }

        item {
            InfoBanner(
                "When you are outside every saved place, the app shows calculated times instead of choosing a random nearby mosque. " +
                    "You can add the place or choose a saved mosque."
            )
        }
    }

    if (showEditor) {
        PlaceEditorDialog(
            initial = editorTarget,
            mosques = state.snapshot.mosques.filter { !it.isArchived },
            lastLocation = state.settings.lastLocation,
            onDismiss = { showEditor = false },
            onRequestLocation = { permissions.requestForegroundLocation() },
            onSave = { place ->
                viewModel.savePlace(place)
                showEditor = false
            }
        )
    }

    if (showTemporaryDialog) {
        TemporarySelectionDialog(
            mosques = state.snapshot.mosques.filter { it.isEnabled && !it.isArchived },
            places = state.snapshot.places,
            onDismiss = { showTemporaryDialog = false },
            onConfirm = { mosqueId, placeId, hours ->
                viewModel.selectTemporary(mosqueId, placeId, hours)
                showTemporaryDialog = false
            }
        )
    }

    deleteTarget?.let { place ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${place.displayName}?") },
            text = { Text("This removes the saved place. Prayer history and mosque timetables are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlace(place.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }

    if (showBackgroundExplanation) {
        AlertDialog(
            onDismissRequest = { showBackgroundExplanation = false },
            title = { Text("Background location") },
            text = {
                Text(
                    "Automatic mosque switching needs background location so Android can tell the app when you enter or leave a saved place. " +
                        "The app stores only the latest location decision, not a trail of your movement. You can keep using manual selection without it."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundExplanation = false
                    permissions.requestBackgroundLocation()
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showBackgroundExplanation = false }) { Text("Not now") } }
        )
    }
}

@Composable
private fun PlaceCard(
    place: SavedPlace,
    mosqueName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEditMosque: () -> Unit
) {
    FrostedCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(place.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                if (place.isPriorityLocation) Pill("Priority")
                if (!place.isEnabled) Pill("Disabled")
                Spacer(Modifier.weight(1f))
                Text("Rank ${place.priorityRank}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LabeledValue(
                "Coordinates",
                if (place.latitude != null && place.longitude != null) {
                    String.format("%.5f, %.5f", place.latitude, place.longitude)
                } else {
                    "Not set"
                }
            )
            LabeledValue("Boundary", "${place.geofenceRadiusMeters} m · ${place.timezoneId}")
            LabeledValue("Mosque", mosqueName ?: "Not linked — calculated fallback")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onSelect, enabled = place.mosqueProfileId != null) { Text("Select") }
                if (mosqueName != null) TextButton(onClick = onEditMosque) { Text("Timetable") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
            SettingSwitchRow(
                title = "Enabled",
                subtitle = "Disabled places never participate in automatic selection.",
                checked = place.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun PlaceEditorDialog(
    initial: SavedPlace?,
    mosques: List<com.example.domain.model.MosqueProfile>,
    lastLocation: com.example.domain.model.LastLocation?,
    onDismiss: () -> Unit,
    onRequestLocation: () -> Unit,
    onSave: (SavedPlace) -> Unit
) {
    var name by remember { mutableStateOf(initial?.displayName ?: "") }
    var latitude by remember { mutableStateOf(initial?.latitude?.toString() ?: "") }
    var longitude by remember { mutableStateOf(initial?.longitude?.toString() ?: "") }
    var radius by remember { mutableStateOf((initial?.geofenceRadiusMeters ?: 250).toString()) }
    var timezone by remember { mutableStateOf(initial?.timezoneId ?: ZoneId.systemDefault().id) }
    var priority by remember { mutableStateOf((initial?.priorityRank ?: 100).toString()) }
    var isPriority by remember { mutableStateOf(initial?.isPriorityLocation ?: false) }
    var enabled by remember { mutableStateOf(initial?.isEnabled ?: true) }
    var linkedMosqueId by remember { mutableStateOf(initial?.mosqueProfileId) }
    var mosqueMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add a saved place" else "Edit saved place") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Place name") },
                    placeholder = { Text("Defence, Karachi") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                SecondaryButton("Use last known location", onClick = onRequestLocation)
                lastLocation?.let {
                    Text(
                        "Last fix: ${String.format("%.5f, %.5f", it.latitude, it.longitude)} (±${it.accuracyMeters.toInt()} m)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = {
                        latitude = it.latitude.toString()
                        longitude = it.longitude.toString()
                    }) { Text("Copy last fix into this place") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = radius,
                        onValueChange = { radius = it },
                        label = { Text("Boundary (m)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priority,
                        onValueChange = { priority = it },
                        label = { Text("Priority rank") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = timezone,
                    onValueChange = { timezone = it },
                    label = { Text("Timezone (IANA)") },
                    placeholder = { Text("Asia/Karachi") },
                    singleLine = true
                )
                Box {
                    SecondaryButton(
                        text = "Mosque: ${mosques.firstOrNull { it.id == linkedMosqueId }?.name ?: "Calculated fallback"}",
                        onClick = { mosqueMenu = true }
                    )
                    DropdownMenu(expanded = mosqueMenu, onDismissRequest = { mosqueMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Calculated fallback") },
                            onClick = { linkedMosqueId = null; mosqueMenu = false }
                        )
                        mosques.forEach { mosque ->
                            DropdownMenuItem(
                                text = { Text(mosque.name) },
                                onClick = { linkedMosqueId = mosque.id; mosqueMenu = false }
                            )
                        }
                    }
                }
                SettingSwitchRow(
                    title = "Priority place",
                    subtitle = "Priority places beat nearest-mosque matching when you are outside boundaries.",
                    checked = isPriority,
                    onCheckedChange = { isPriority = it }
                )
                SettingSwitchRow(
                    title = "Enabled",
                    subtitle = "Disabled places are excluded from automatic selection.",
                    checked = enabled,
                    onCheckedChange = { enabled = it }
                )
                error?.let { InfoBanner(it, isWarning = true) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmedName = name.trim()
                if (trimmedName.isEmpty()) {
                    error = "Enter a name for this place."
                    return@TextButton
                }
                val lat = latitude.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                val lon = longitude.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                if ((lat == null) != (lon == null)) {
                    error = "Enter both latitude and longitude, or leave both empty."
                    return@TextButton
                }
                if (lat != null && !GeoMath.isValidLatitude(lat)) {
                    error = "Latitude must be between -90 and 90."
                    return@TextButton
                }
                if (lon != null && !GeoMath.isValidLongitude(lon)) {
                    error = "Longitude must be between -180 and 180."
                    return@TextButton
                }
                val radiusValue = radius.toIntOrNull()?.coerceAtLeast(100) ?: 250
                val priorityValue = priority.toIntOrNull()?.coerceIn(0, 9999) ?: 100
                val resolvedZone = runCatching { ZoneId.of(timezone.trim()) }.getOrElse {
                    error = "Timezone must be a valid IANA zone such as Asia/Karachi."
                    return@TextButton
                }
                onSave(
                    SavedPlace(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        displayName = trimmedName,
                        latitude = lat,
                        longitude = lon,
                        geofenceRadiusMeters = radiusValue,
                        timezoneId = resolvedZone.id,
                        mosqueProfileId = linkedMosqueId,
                        priorityRank = priorityValue,
                        isEnabled = enabled,
                        isPriorityLocation = isPriority,
                        createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TemporarySelectionDialog(
    mosques: List<com.example.domain.model.MosqueProfile>,
    places: List<SavedPlace>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Int) -> Unit
) {
    var selectedMosqueId by remember { mutableStateOf(mosques.firstOrNull()?.id) }
    var hours by remember { mutableStateOf(2) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Temporary mosque") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Hold this mosque for a chosen time, then return to your normal mode.")
                if (mosques.isEmpty()) {
                    Text("Add a mosque profile first.", style = MaterialTheme.typography.bodySmall)
                }
                mosques.forEach { mosque ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedMosqueId == mosque.id) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        },
                        onClick = { selectedMosqueId = mosque.id }
                    ) {
                        Text(
                            mosque.name,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Text("Duration", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 2, 4, 8).forEach { option ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (hours == option) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            onClick = { hours = option }
                        ) {
                            Text(
                                "${option}h",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedMosqueId != null,
                onClick = {
                    val mosqueId = selectedMosqueId ?: return@TextButton
                    val linkedPlace = places.firstOrNull { it.mosqueProfileId == mosqueId }
                    onConfirm(mosqueId, linkedPlace?.id, hours)
                }
            ) { Text("Start override") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
