package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.qibla.QiblaHelper
import com.example.ui.theme.KaabaGold
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaCompassView(
    compassHeading: Float,
    qiblaBearing: Double,
    distanceToMakkahKm: Double,
    hasHardwareSensor: Boolean,
    onManualHeadingChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var prevAligned by remember { mutableStateOf(false) }

    // Smooth compass needle angle pointing towards Kaaba
    // Relative angle: when device heading matches qibla bearing, difference is 0 (straight up)
    val relativeAngle = ((qiblaBearing.toFloat() - compassHeading) % 360f + 360f) % 360f
    val isAligned = relativeAngle in 0f..4.5f || relativeAngle in 355.5f..360f

    // Trigger haptic vibration on entering alignment
    LaunchedEffect(isAligned) {
        if (isAligned && !prevAligned) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        }
        prevAligned = isAligned
    }

    val animatedDialRotation by animateFloatAsState(
        targetValue = -compassHeading,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "compass_dial"
    )

    val animatedNeedleRotation by animateFloatAsState(
        targetValue = relativeAngle,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "compass_needle"
    )

    val alignmentColor by animateColorAsState(
        targetValue = if (isAligned) Color(0xFF10B981) else KaabaGold,
        label = "alignment_glow"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("qibla_compass_view"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Alignment Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isAligned) Color(0xFF0F5A40) else MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isAligned) Color(0xFF34D399) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = if (isAligned) Icons.Default.CheckCircle else Icons.Default.LocationSearching,
                    contentDescription = null,
                    tint = if (isAligned) Color(0xFF34D399) else KaabaGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAligned) "ALIGNED WITH HOLY KAABA (قبلة)" else "Rotate device until needle points up",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isAligned) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Compass Dial Container
        Box(
            modifier = Modifier
                .size(310.dp)
                .aspectRatio(1f)
                .clip(CircleShape)
                .shadow(elevation = 12.dp, shape = CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0B291E),
                            Color(0xFF061811),
                            Color(0xFF030D09)
                        )
                    )
                )
                .border(
                    width = if (isAligned) 3.dp else 2.dp,
                    color = alignmentColor.copy(alpha = if (isAligned) 0.9f else 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Compass Rose & Degrees
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .rotate(animatedDialRotation)
            ) {
                drawCompassDial(this, qiblaBearing)
            }

            // Fixed Top Heading Marker
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFEF4444))
            )

            // Dynamic Qibla Pointer Needle
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(26.dp)
                    .rotate(animatedNeedleRotation)
            ) {
                drawQiblaNeedle(this, isAligned)
            }

            // Center Golden Kaaba Emblem
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                KaabaGold,
                                Color(0xFF8B6508)
                            )
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🕋",
                    fontSize = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Coordinates & Direction telemetry cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Qibla Angle",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "${String.format("%.1f", qiblaBearing)}°",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = KaabaGold
                        )
                    )
                    Text(
                        text = QiblaHelper.getCompassDirectionText(qiblaBearing),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Distance to Makkah",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "${String.format("%,d", distanceToMakkahKm.toInt())} km",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "Kaaba Al-Mukarramah",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Manual simulator slider if running in emulator or sensor uncalibrated
        if (!hasHardwareSensor) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Manual Heading Simulator (Device sensor not detected)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Drag slider to test Qibla rotation & alignment feedback: ${compassHeading.toInt()}°",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = compassHeading,
                        onValueChange = onManualHeadingChange,
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = KaabaGold,
                            activeTrackColor = KaabaGold
                        )
                    )
                }
            }
        }
    }
}

private fun drawCompassDial(scope: DrawScope, qiblaBearing: Double) {
    val center = scope.center
    val radius = scope.size.minDimension / 2f

    // Draw degree tick marks every 5 and 30 degrees
    for (deg in 0 until 360 step 5) {
        val angleRad = Math.toRadians((deg - 90).toDouble())
        val isMajor = deg % 30 == 0
        val isCardinal = deg % 90 == 0

        val tickLen = when {
            isCardinal -> 18f
            isMajor -> 12f
            else -> 6f
        }

        val strokeW = if (isCardinal) 2.5f else if (isMajor) 1.5f else 1f
        val color = if (isCardinal) Color.White else if (isMajor) KaabaGold.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.25f)

        val startX = (center.x + (radius - tickLen) * cos(angleRad)).toFloat()
        val startY = (center.y + (radius - tickLen) * sin(angleRad)).toFloat()
        val endX = (center.x + radius * cos(angleRad)).toFloat()
        val endY = (center.y + radius * sin(angleRad)).toFloat()

        scope.drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeW
        )
    }

    // Outer subtle circle
    scope.drawCircle(
        color = Color.White.copy(alpha = 0.1f),
        radius = radius,
        center = center,
        style = Stroke(width = 1f)
    )
}

private fun drawQiblaNeedle(scope: DrawScope, isAligned: Boolean) {
    val center = scope.center
    val radius = scope.size.minDimension / 2f
    val needleColor = if (isAligned) Color(0xFF10B981) else KaabaGold

    // Arrow pointing straight UP (which is aligned with Kaaba after Canvas rotation)
    val path = Path().apply {
        moveTo(center.x, center.y - radius * 0.88f) // Tip pointing up
        lineTo(center.x - 14f, center.y - 20f)
        lineTo(center.x + 14f, center.y - 20f)
        close()
    }
    scope.drawPath(
        path = path,
        color = needleColor
    )

    // Needle spine
    scope.drawLine(
        color = needleColor,
        start = Offset(center.x, center.y - 20f),
        end = Offset(center.x, center.y - radius * 0.88f),
        strokeWidth = 3f
    )

    // South counterweight needle (dim red)
    val southPath = Path().apply {
        moveTo(center.x, center.y + radius * 0.65f)
        lineTo(center.x - 10f, center.y + 20f)
        lineTo(center.x + 10f, center.y + 20f)
        close()
    }
    scope.drawPath(
        path = southPath,
        color = Color(0xFFEF4444).copy(alpha = 0.7f)
    )
}
