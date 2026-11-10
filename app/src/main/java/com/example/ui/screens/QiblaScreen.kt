package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.design.FrostedCard
import com.example.core.design.InfoBanner
import com.example.core.design.LabeledValue
import com.example.core.design.Pill
import com.example.core.design.ScreenHeader
import com.example.core.design.SecondaryButton
import com.example.core.design.SectionTitle
import com.example.navigation.PermissionActions
import com.example.qibla.CompassAccuracy
import com.example.qibla.CompassSensorManager
import com.example.qibla.QiblaCalculator
import com.example.ui.AppUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun QiblaScreen(
    state: AppUiState,
    permissions: PermissionActions
) {
    val context = LocalContext.current
    val compass = remember { CompassSensorManager(context) }
    val heading by compass.headingDegrees.collectAsState()
    val accuracy by compass.accuracy.collectAsState()
    val sensorAvailable by compass.sensorAvailable.collectAsState()

    DisposableEffect(compass) {
        compass.start()
        onDispose { compass.stop() }
    }

    val mosque = state.today?.mosque
    val place = state.today?.place
    val lastKnown = state.settings.lastLocation

    var manualLatitudeText by remember { mutableStateOf("") }
    var manualLongitudeText by remember { mutableStateOf("") }
    var manualLatitude by remember { mutableStateOf<Double?>(null) }
    var manualLongitude by remember { mutableStateOf<Double?>(null) }
    var manualError by remember { mutableStateOf<String?>(null) }

    val latitude = manualLatitude ?: mosque?.latitude ?: place?.latitude ?: lastKnown?.latitude
    val longitude = manualLongitude ?: mosque?.longitude ?: place?.longitude ?: lastKnown?.longitude

    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null) {
            compass.updateLocation(latitude, longitude)
        }
    }

    val bearing = if (latitude != null && longitude != null) {
        runCatching { QiblaCalculator.bearingFromTrueNorth(latitude, longitude) }.getOrNull()
    } else {
        null
    }
    val distanceKm = if (latitude != null && longitude != null) {
        runCatching { QiblaCalculator.distanceToKaabaKm(latitude, longitude) }.getOrNull()
    } else {
        null
    }
    val difference = bearing?.let { QiblaCalculator.angularDifference(it, heading.toDouble()) }
    val locationSource = when {
        manualLatitude != null -> "Manual coordinates"
        mosque?.latitude != null -> "Mosque: ${mosque.name}"
        place?.latitude != null -> "Place: ${place.displayName}"
        lastKnown != null -> "Last known location"
        else -> "Not set"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Qibla", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Bearing to the Kaaba from true north",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            FrostedCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bearing == null) {
                        Text("Location needed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "The Qibla direction needs your coordinates. Select a mosque with coordinates, " +
                                "allow location, or enter coordinates manually below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SecondaryButton("Use current location", onClick = { permissions.requestForegroundLocation() })
                    } else {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(
                                "${bearing.roundToInt()}°",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                Pill(QiblaCalculator.cardinalDirection(bearing))
                                androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                                Text(
                                    "from true north",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        QiblaCompassCanvas(
                            bearing = bearing,
                            heading = heading.toDouble(),
                            hasSensor = sensorAvailable
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            LabeledValue("Device heading", "${heading.roundToInt()}°")
                            LabeledValue("To Qibla", "${difference?.roundToInt() ?: 0}° off")
                            LabeledValue("Distance", distanceKm?.let { "%.0f km".format(it) } ?: "—")
                        }
                        Text(
                            if (!sensorAvailable) {
                                "No compass sensor detected. Use the bearing number and a map; the needle is hidden."
                            } else {
                                when (accuracy) {
                                    CompassAccuracy.HIGH -> "High compass confidence."
                                    CompassAccuracy.MODERATE -> "Moderate compass confidence. Move away from metal and magnets."
                                    CompassAccuracy.LOW -> "Low compass confidence — calibrate by moving the phone in a figure-8."
                                    CompassAccuracy.UNAVAILABLE -> "Compass unavailable."
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            FrostedCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Coordinates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    LabeledValue("Source", locationSource)
                    if (latitude != null && longitude != null) {
                        LabeledValue("Point", String.format("%.5f, %.5f", latitude, longitude))
                    }
                    if (mosque == null && place == null) {
                        Text(
                            "Choose a mosque or saved place to make this automatic.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (lastKnown != null) {
                        val time = Instant.ofEpochMilli(lastKnown.timestampMillis)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("d MMM, HH:mm"))
                        Text(
                            "Last fix ${time} · ±${lastKnown.accuracyMeters.roundToInt()} m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SecondaryButton("Refresh location", onClick = { permissions.requestForegroundLocation() })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualLatitudeText,
                            onValueChange = { manualLatitudeText = it },
                            label = { Text("Latitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = manualLongitudeText,
                            onValueChange = { manualLongitudeText = it },
                            label = { Text("Longitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    manualError?.let { InfoBanner(it, isWarning = true) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("Apply coordinates", onClick = {
                            val lat = manualLatitudeText.trim().toDoubleOrNull()
                            val lon = manualLongitudeText.trim().toDoubleOrNull()
                            if (lat == null || lon == null) {
                                manualError = "Enter both latitude and longitude as decimal degrees."
                                return@SecondaryButton
                            }
                            if (!com.example.domain.model.GeoMath.isValidLatitude(lat) ||
                                !com.example.domain.model.GeoMath.isValidLongitude(lon)
                            ) {
                                manualError = "Latitude must be -90..90 and longitude -180..180."
                                return@SecondaryButton
                            }
                            manualLatitude = lat
                            manualLongitude = lon
                            manualError = null
                        }, modifier = Modifier.weight(1f))
                        SecondaryButton("Clear manual", onClick = {
                            manualLatitude = null
                            manualLongitude = null
                            manualLatitudeText = ""
                            manualLongitudeText = ""
                        }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (bearing != null) {
            item {
                SecondaryButton("Open the Kaaba in a map app", onClick = {
                    val uri = Uri.parse(
                        "geo:${QiblaCalculator.KAABA_LATITUDE},${QiblaCalculator.KAABA_LONGITUDE}" +
                            "?q=${QiblaCalculator.KAABA_LATITUDE},${QiblaCalculator.KAABA_LONGITUDE}(Kaaba)"
                    )
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                })
            }
        }

        item { SectionTitle("How to use the compass") }
        item {
            InfoBanner(
                "Place the phone flat, away from magnets, metal tables and laptop speakers. The needle points to the " +
                    "Qibla when the phone is level. The displayed bearing is measured from true north; the compass adds " +
                    "magnetic declination for your coordinates, so it differs from a raw magnetic compass."
            )
        }
        item {
            InfoBanner(
                "Qibla is a direction, not a guaranteed device measurement. If accuracy is low, use the bearing " +
                    "number with a map instead of trusting the needle."
            )
        }
    }
}

@Composable
private fun QiblaCompassCanvas(
    bearing: Double,
    heading: Double,
    hasSensor: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val ring = MaterialTheme.colorScheme.outline
    val north = MaterialTheme.colorScheme.onSurfaceVariant
    val centerColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 18.dp.toPx()
        drawCircle(color = ring.copy(alpha = 0.35f), radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))
        drawCircle(color = ring.copy(alpha = 0.18f), radius = radius * 0.7f, center = center, style = Stroke(width = 1.dp.toPx()))

        rotate(degrees = -heading.toFloat(), pivot = center) {
            drawLine(
                color = north,
                start = center,
                end = Offset(center.x, center.y - radius * 0.84f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = north,
                radius = 5.dp.toPx(),
                center = Offset(center.x, center.y - radius * 0.84f)
            )
        }

        if (hasSensor) {
            rotate(degrees = (bearing - heading).toFloat(), pivot = center) {
                drawLine(
                    color = primary.copy(alpha = 0.35f),
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.78f),
                    strokeWidth = 14.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = primary,
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.78f),
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = primary,
                    radius = 10.dp.toPx(),
                    center = Offset(center.x, center.y - radius * 0.78f)
                )
            }
        }

        drawCircle(color = centerColor, radius = 12.dp.toPx(), center = center)
        drawCircle(color = primary, radius = 5.dp.toPx(), center = center)
    }
}
