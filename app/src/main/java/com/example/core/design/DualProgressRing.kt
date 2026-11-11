package com.example.core.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DualProgressRing(
    todayCompleted: Int,
    todayTarget: Int,
    totalCompleted: Int,
    totalRukus: Int = 558,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 230.dp
) {
    val dailyProgress = if (todayTarget > 0) {
        (todayCompleted.toFloat() / todayTarget.toFloat()).coerceIn(0f, 1f)
    } else {
        if (todayCompleted > 0) 1f else 0f
    }
    val khatamProgress = if (totalRukus > 0) {
        (totalCompleted.toFloat() / totalRukus.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedDailyProgress by animateFloatAsState(
        targetValue = dailyProgress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "dailyProgressAnim"
    )
    val animatedKhatamProgress by animateFloatAsState(
        targetValue = khatamProgress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "khatamProgressAnim"
    )

    val goldColor = AppColors.Gold
    val emeraldColor = AppColors.Emerald
    val trackGold = goldColor.copy(alpha = 0.18f)
    val trackEmerald = emeraldColor.copy(alpha = 0.18f)

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer ring: Total Khatam (Gold)
            val outerStroke = 11.dp.toPx()
            val outerRadius = size.minDimension / 2f - outerStroke / 2f - 4.dp.toPx()
            val outerTopLeft = Offset(center.x - outerRadius, center.y - outerRadius)
            val outerArcSize = Size(outerRadius * 2, outerRadius * 2)

            // Outer track
            drawArc(
                color = trackGold,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = outerTopLeft,
                size = outerArcSize,
                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
            )
            // Outer progress
            if (animatedKhatamProgress > 0f) {
                drawArc(
                    color = goldColor,
                    startAngle = -90f,
                    sweepAngle = animatedKhatamProgress * 360f,
                    useCenter = false,
                    topLeft = outerTopLeft,
                    size = outerArcSize,
                    style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                )
            }

            // Inner ring: Today's Target (Emerald)
            val innerStroke = 9.dp.toPx()
            val innerRadius = outerRadius - outerStroke - 6.dp.toPx()
            val innerTopLeft = Offset(center.x - innerRadius, center.y - innerRadius)
            val innerArcSize = Size(innerRadius * 2, innerRadius * 2)

            // Inner track
            drawArc(
                color = trackEmerald,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerArcSize,
                style = Stroke(width = innerStroke, cap = StrokeCap.Round)
            )
            // Inner progress
            if (animatedDailyProgress > 0f) {
                drawArc(
                    color = emeraldColor,
                    startAngle = -90f,
                    sweepAngle = animatedDailyProgress * 360f,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerArcSize,
                    style = Stroke(width = innerStroke, cap = StrokeCap.Round)
                )
            }
        }

        // Center textual readout
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "القرآن الكريم",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = goldColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "$todayCompleted",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                "/ $todayTarget Ruku today",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(Modifier.height(4.dp))
            Pill(
                text = "${(khatamProgress * 100).toInt()}% Khatam",
                color = goldColor,
                background = goldColor.copy(alpha = 0.15f)
            )
        }
    }
}
