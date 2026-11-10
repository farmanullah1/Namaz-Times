package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.AppColors
import com.example.core.design.FrostedCard
import com.example.core.design.InfoBanner
import com.example.core.design.LabeledValue
import com.example.core.design.Pill
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
    val haptic = LocalHapticFeedback.current
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
    val isAligned = difference != null && difference <= 4.0 && sensorAvailable

    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasAligned = isAligned
    }

    val locationSource = when {
        manualLatitude != null -> "Manual coordinates"
        mosque?.latitude != null -> "Mosque: ${mosque.name}"
        place?.latitude != null -> "Place: ${place.displayName}"
        lastKnown != null -> "Last known location"
        else -> "Not set"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Qibla", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(
                    "Great-circle bearing to the Kaaba in Makkah",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            FrostedCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        // Bearing header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${bearing.roundToInt()}°",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAligned) AppColors.Gold else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Qibla bearing (True North)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Pill(QiblaCalculator.cardinalDirection(bearing))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    distanceKm?.let { "%.0f km to Makkah".format(it) } ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Alignment status indicator banner
                        if (sensorAvailable) {
                            val bannerColor by animateColorAsState(
                                targetValue = if (isAligned) AppColors.EmeraldDark else MaterialTheme.colorScheme.surfaceVariant,
                                animationSpec = tween(300),
                                label = "bannerColor"
                            )
                            val borderColor = if (isAligned) AppColors.Gold else Color.Transparent

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = bannerColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isAligned) Icons.Default.CheckCircle else Icons.Default.RotateRight,
                                        contentDescription = null,
                                        tint = if (isAligned) AppColors.Gold else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            if (isAligned) "Aligned with the Kaaba! 🕋"
                                            else {
                                                val turnDirection = if (((heading - bearing + 360) % 360) > 180) "Turn right ↻" else "Turn left ↺"
                                                "$turnDirection · ${difference?.roundToInt()}° off"
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isAligned) AppColors.Gold else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            if (isAligned) "Face this direction for prayer"
                                            else "Heading: ${heading.roundToInt()}° · Qibla: ${bearing.roundToInt()}°",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isAligned) AppColors.GoldSoft else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Compass Canvas
                        QiblaCompassCanvas(
                            bearing = bearing,
                            heading = heading.toDouble(),
                            hasSensor = sensorAvailable,
                            isAligned = isAligned
                        )

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LabeledValue("Device Heading", "${heading.roundToInt()}°")
                            LabeledValue("Offset", "${difference?.roundToInt() ?: 0}°")
                            LabeledValue("Accuracy", accuracy.label)
                        }
                    }
                }
            }
        }

        // Tilt & Calibration guidance card
        if (sensorAvailable && (accuracy == CompassAccuracy.LOW || accuracy == CompassAccuracy.MODERATE)) {
            item {
                CalibrationGuidanceCard(accuracy = accuracy)
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
                SecondaryButton("Open the Kaaba in Google Maps", onClick = {
                    val uri = Uri.parse(
                        "geo:${QiblaCalculator.KAABA_LATITUDE},${QiblaCalculator.KAABA_LONGITUDE}" +
                            "?q=${QiblaCalculator.KAABA_LATITUDE},${QiblaCalculator.KAABA_LONGITUDE}(Holy+Kaaba)"
                    )
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                })
            }
        }

        item { SectionTitle("How to use the compass") }
        item {
            InfoBanner(
                "Hold phone flat parallel to ground, away from magnets, metal tables and laptop speakers. " +
                    "The golden Kaaba beacon points to Makkah. The compass automatically applies magnetic declination for your coordinates."
            )
        }
    }
}

@Composable
private fun CalibrationGuidanceCard(accuracy: CompassAccuracy) {
    FrostedCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CompassCalibration,
                    contentDescription = null,
                    tint = AppColors.Gold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Calibrate Compass",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                "Sensor confidence is ${accuracy.label.lowercase()}. Follow these steps to calibrate the magnetometer:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "1. Wave phone gently in a figure-8 motion in the air 3–5 times.",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    "2. Hold phone flat parallel to the ground (level with the horizon).",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    "3. Step away from magnetic phone cases, metal frames, and laptop speakers.",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun QiblaCompassCanvas(
    bearing: Double,
    heading: Double,
    hasSensor: Boolean,
    isAligned: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val northColor = AppColors.Gold
    val centerColor = MaterialTheme.colorScheme.surfaceVariant
    val alignedGlow = AppColors.KaabaGold

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 20.dp.toPx()

        // Outer glow when aligned
        if (isAligned) {
            drawCircle(
                color = alignedGlow.copy(alpha = 0.20f),
                radius = radius + 12.dp.toPx(),
                center = center
            )
            drawCircle(
                color = AppColors.Emerald.copy(alpha = 0.15f),
                radius = radius + 6.dp.toPx(),
                center = center
            )
        }

        // Outer bezel ring
        drawCircle(
            color = if (isAligned) AppColors.Gold else outline.copy(alpha = 0.40f),
            radius = radius,
            center = center,
            style = Stroke(width = if (isAligned) 3.dp.toPx() else 2.dp.toPx())
        )

        // Inner decorative ring
        drawCircle(
            color = outline.copy(alpha = 0.15f),
            radius = radius * 0.72f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw degree ticks (rotated by -heading so North stays oriented to device heading)
        rotate(degrees = -heading.toFloat(), pivot = center) {
            for (deg in 0 until 360 step 5) {
                val isMajor = deg % 30 == 0
                val isCardinal = deg % 90 == 0
                val tickLength = when {
                    isCardinal -> 14.dp.toPx()
                    isMajor -> 9.dp.toPx()
                    else -> 4.dp.toPx()
                }
                val tickColor = when {
                    deg == 0 -> northColor
                    isCardinal -> outline.copy(alpha = 0.85f)
                    isMajor -> outline.copy(alpha = 0.50f)
                    else -> outline.copy(alpha = 0.25f)
                }
                val strokeWidth = when {
                    deg == 0 -> 3.dp.toPx()
                    isCardinal -> 2.dp.toPx()
                    else -> 1.dp.toPx()
                }

                rotate(degrees = deg.toFloat(), pivot = center) {
                    drawLine(
                        color = tickColor,
                        start = Offset(center.x, center.y - radius),
                        end = Offset(center.x, center.y - radius + tickLength),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            // North Arrow indicator
            drawLine(
                color = northColor,
                start = Offset(center.x, center.y - radius * 0.4f),
                end = Offset(center.x, center.y - radius * 0.86f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = northColor,
                radius = 4.dp.toPx(),
                center = Offset(center.x, center.y - radius * 0.86f)
            )
        }

        // Qibla Pointer Needle
        if (hasSensor) {
            val qiblaAngle = (bearing - heading).toFloat()
            rotate(degrees = qiblaAngle, pivot = center) {
                val needleLength = radius * 0.80f
                val needleTip = Offset(center.x, center.y - needleLength)

                // Needle shaft
                drawLine(
                    color = if (isAligned) AppColors.Gold else primary.copy(alpha = 0.45f),
                    start = center,
                    end = needleTip,
                    strokeWidth = if (isAligned) 14.dp.toPx() else 10.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = if (isAligned) AppColors.KaabaGold else primary,
                    start = center,
                    end = needleTip,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Kaaba Beacon (Stylized Cube at Tip)
                val cubeSize = 22.dp.toPx()
                val cubeLeft = needleTip.x - cubeSize / 2f
                val cubeTop = needleTip.y - cubeSize / 2f

                // Outer beacon glow
                drawCircle(
                    color = if (isAligned) AppColors.Gold else primary.copy(alpha = 0.4f),
                    radius = cubeSize * 0.9f,
                    center = needleTip
                )

                // Kaaba Black Cube body
                drawRoundRect(
                    color = Color(0xFF1E1E1E),
                    topLeft = Offset(cubeLeft, cubeTop),
                    size = Size(cubeSize, cubeSize),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                // Golden Kiswa band across upper portion
                drawRect(
                    color = AppColors.Gold,
                    topLeft = Offset(cubeLeft, cubeTop + cubeSize * 0.22f),
                    size = Size(cubeSize, cubeSize * 0.20f)
                )
            }
        }

        // Center Pivot
        drawCircle(color = centerColor, radius = 14.dp.toPx(), center = center)
        drawCircle(
            color = if (isAligned) AppColors.Gold else primary,
            radius = 6.dp.toPx(),
            center = center
        )
    }
}
