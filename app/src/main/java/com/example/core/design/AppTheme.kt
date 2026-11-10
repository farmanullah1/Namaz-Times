package com.example.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppColors {
    val LightBackground = Color(0xFFF4F5F7)
    val LightBackgroundAlt = Color(0xFFE8EDF0)
    val LightSurface = Color(0xCCFFFFFF)
    val LightRim = Color(0x55FFFFFF)
    val Ink = Color(0xFF12161A)
    val InkSecondary = Color(0xFF5A646C)
    val Teal = Color(0xFF0D6B62)
    val TealSoft = Color(0xFFDCEEEB)
    val Gold = Color(0xFFD4AF37)
    val GoldSoft = Color(0xFFFDF0CC)
    val Emerald = Color(0xFF0D5A3D)
    val EmeraldSoft = Color(0xFFE2F3EC)
    val Warning = Color(0xFF9A5B12)

    val DarkBackground = Color(0xFF050607)
    val DarkBackgroundAlt = Color(0xFF0E1114)
    val DarkSurface = Color(0xB3161A1E)
    val DarkRim = Color(0x1FFFFFFF)
    val OffWhite = Color(0xFFF2F5F7)
    val Muted = Color(0xFFA8B2BA)
    val Mint = Color(0xFF5FD3C2)
    val EmeraldDark = Color(0xFF072B1F)
    val KaabaGold = Color(0xFFD4AF37)
    val KaabaGoldGlow = Color(0xFFFFDF7D)
}

private val LightScheme = lightColorScheme(
    primary = AppColors.Teal,
    onPrimary = Color.White,
    primaryContainer = AppColors.TealSoft,
    onPrimaryContainer = Color(0xFF04332E),
    secondary = AppColors.Gold,
    onSecondary = Color.White,
    background = AppColors.LightBackground,
    onBackground = AppColors.Ink,
    surface = Color(0xFFFDFDFE),
    onSurface = AppColors.Ink,
    surfaceVariant = Color(0xFFE3E8EC),
    onSurfaceVariant = AppColors.InkSecondary,
    outline = Color(0xFFCBD3D9),
    error = Color(0xFF8C2F2F)
)

private val DarkScheme = darkColorScheme(
    primary = AppColors.Mint,
    onPrimary = Color(0xFF032C27),
    primaryContainer = Color(0xFF12332F),
    onPrimaryContainer = AppColors.Mint,
    secondary = Color(0xFFC9A96E),
    onSecondary = Color(0xFF241A08),
    background = AppColors.DarkBackground,
    onBackground = AppColors.OffWhite,
    surface = Color(0xFF111417),
    onSurface = AppColors.OffWhite,
    surfaceVariant = Color(0xFF1C2126),
    onSurfaceVariant = AppColors.Muted,
    outline = Color(0xFF2C343B),
    error = Color(0xFFE8A3A3)
)

private val AppTypography = Typography().copy(
    displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    displayMedium = TextStyle(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1).sp),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp)
)

@Composable
fun NamazTimesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography
    ) {
        AppBackground(darkTheme = darkTheme) {
            content()
        }
    }
}

@Composable
fun AppBackground(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        listOf(AppColors.DarkBackground, AppColors.DarkBackgroundAlt, AppColors.DarkBackground)
    } else {
        listOf(AppColors.LightBackground, AppColors.LightBackgroundAlt, AppColors.LightBackground)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors))
    ) {
        content()
    }
}
