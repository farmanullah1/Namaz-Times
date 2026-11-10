package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.MosqueLocation
import com.example.ui.theme.KaabaGold

@Composable
fun AddEditMosqueDialog(
    mosqueToEdit: MosqueLocation? = null,
    currentGpsCoords: Pair<Double, Double>? = null,
    onDismiss: () -> Unit,
    onSave: (MosqueLocation) -> Unit
) {
    var name by remember { mutableStateOf(mosqueToEdit?.name ?: "") }
    var area by remember { mutableStateOf(mosqueToEdit?.area ?: "") }
    var latStr by remember { mutableStateOf(mosqueToEdit?.latitude?.toString() ?: currentGpsCoords?.first?.toString() ?: "24.8016") }
    var lngStr by remember { mutableStateOf(mosqueToEdit?.longitude?.toString() ?: currentGpsCoords?.second?.toString() ?: "67.0673") }

    var isCustomTimes by remember { mutableStateOf(mosqueToEdit?.isCustomTimes ?: true) }

    // Prayer Azan & Iqamah times
    var fajrAzan by remember { mutableStateOf(mosqueToEdit?.fajrAzan ?: "05:15") }
    var fajrIqamah by remember { mutableStateOf(mosqueToEdit?.fajrIqamah ?: "05:40") }

    var dhuhrAzan by remember { mutableStateOf(mosqueToEdit?.dhuhrAzan ?: "12:20") }
    var dhuhrIqamah by remember { mutableStateOf(mosqueToEdit?.dhuhrIqamah ?: "12:45") }

    var asrAzan by remember { mutableStateOf(mosqueToEdit?.asrAzan ?: "15:45") }
    var asrIqamah by remember { mutableStateOf(mosqueToEdit?.asrIqamah ?: "16:05") }

    var maghribAzan by remember { mutableStateOf(mosqueToEdit?.maghribAzan ?: "17:45") }
    var maghribIqamah by remember { mutableStateOf(mosqueToEdit?.maghribIqamah ?: "17:55") }

    var ishaAzan by remember { mutableStateOf(mosqueToEdit?.ishaAzan ?: "19:00") }
    var ishaIqamah by remember { mutableStateOf(mosqueToEdit?.ishaIqamah ?: "19:20") }

    var jummahIqamah by remember { mutableStateOf(mosqueToEdit?.jummahIqamah ?: "13:15") }
    var notes by remember { mutableStateOf(mosqueToEdit?.note ?: "") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (mosqueToEdit == null) "Add Mosque / Location" else "Edit Mosque Times",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Enter the specific prayer schedule for this mosque to be reminded when in this location.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Mosque / Location Name") },
                    placeholder = { Text("e.g. Defence Mosque, Karachi") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_mosque_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Area / City") },
                    placeholder = { Text("e.g. Defence Phase 5, Karachi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // GPS Coordinates row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latStr,
                        onValueChange = { latStr = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lngStr,
                        onValueChange = { lngStr = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                if (currentGpsCoords != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            latStr = String.format("%.4f", currentGpsCoords.first)
                            lngStr = String.format("%.4f", currentGpsCoords.second)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Use My Current GPS Position")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Mosque Prayer Schedule (Azan & Iqamah)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "Times in 24-hour format (HH:mm) as announced by the mosque committee.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                PrayerInputRow(name = "Fajr", azan = fajrAzan, iqamah = fajrIqamah, onAzanChange = { fajrAzan = it }, onIqamahChange = { fajrIqamah = it })
                PrayerInputRow(name = "Dhuhr", azan = dhuhrAzan, iqamah = dhuhrIqamah, onAzanChange = { dhuhrAzan = it }, onIqamahChange = { dhuhrIqamah = it })
                PrayerInputRow(name = "Asr (Ahl al-Hadith)", azan = asrAzan, iqamah = asrIqamah, onAzanChange = { asrAzan = it }, onIqamahChange = { asrIqamah = it })
                PrayerInputRow(name = "Maghrib", azan = maghribAzan, iqamah = maghribIqamah, onAzanChange = { maghribAzan = it }, onIqamahChange = { maghribIqamah = it })
                PrayerInputRow(name = "Isha", azan = ishaAzan, iqamah = ishaIqamah, onAzanChange = { ishaAzan = it }, onIqamahChange = { ishaIqamah = it })

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = jummahIqamah,
                    onValueChange = { jummahIqamah = it },
                    label = { Text("Friday Jumu'ah Prayer Time") },
                    placeholder = { Text("13:15") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Imam / Landmark / Gates)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latStr.toDoubleOrNull() ?: 24.8016
                    val lng = lngStr.toDoubleOrNull() ?: 67.0673
                    val validName = if (name.isBlank()) "Mosque" else name
                    val updated = (mosqueToEdit ?: MosqueLocation(
                        name = validName,
                        area = area,
                        latitude = lat,
                        longitude = lng
                    )).copy(
                        name = validName,
                        area = area,
                        latitude = lat,
                        longitude = lng,
                        isCustomTimes = isCustomTimes,
                        fajrAzan = fajrAzan,
                        fajrIqamah = fajrIqamah,
                        dhuhrAzan = dhuhrAzan,
                        dhuhrIqamah = dhuhrIqamah,
                        asrAzan = asrAzan,
                        asrIqamah = asrIqamah,
                        maghribAzan = maghribAzan,
                        maghribIqamah = maghribIqamah,
                        ishaAzan = ishaAzan,
                        ishaIqamah = ishaIqamah,
                        jummahIqamah = jummahIqamah,
                        note = notes
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("save_mosque_button")
            ) {
                Text("Save Mosque")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PrayerInputRow(
    name: String,
    azan: String,
    iqamah: String,
    onAzanChange: (String) -> Unit,
    onIqamahChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.width(90.dp)
        )
        OutlinedTextField(
            value = azan,
            onValueChange = onAzanChange,
            label = { Text("Azan", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = iqamah,
            onValueChange = onIqamahChange,
            label = { Text("Iqamah", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
}
