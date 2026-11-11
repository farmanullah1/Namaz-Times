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
    val LightBackground = Color(0xFFF7F5EF)
    val LightBackgroundAlt = Color(0xFFFFF6DA)
    val LightSurface = Color(0x9EFFFFFF)
    val LightRim = Color(0xCCFFFFFF)
    val Ink = Color(0xFF17140A)
    val InkSecondary = Color(0xFF5E5842)
    val Teal = Color(0xFFF5B700)
    val TealSoft = Color(0x2EF5B700)
    val Gold = Color(0xFFC97A00)
    val GoldSoft = Color(0xFFFFEFC2)
    val Emerald = Color(0xFF2E9E5B)
    val EmeraldSoft = Color(0xFFE2F3EC)
    val Warning = Color(0xFFC97A00)

    val DarkBackground = Color(0xFF0B0B0D)
    val DarkBackgroundAlt = Color(0xFF141416)
    val DarkSurface = Color(0x941A1A1F)
    val DarkRim = Color(0x24FFFFFF)
    val OffWhite = Color(0xFFF5F2E8)
    val Muted = Color(0xFFB9B49F)
    val Mint = Color(0xFFFFCB2E)
    val EmeraldDark = Color(0xFF123B29)
    val KaabaGold = Color(0xFFFFCB2E)
    val KaabaGoldGlow = Color(0xFFFFE08A)
}

private val OnAccent = Color(0xFF1C1500)

private val LightScheme = lightColorScheme(
    primary = AppColors.Teal,
    onPrimary = OnAccent,
    primaryContainer = AppColors.GoldSoft,
    onPrimaryContainer = AppColors.Ink,
    secondary = AppColors.Gold,
    onSecondary = Color.White,
    background = AppColors.LightBackground,
    onBackground = AppColors.Ink,
    surface = Color(0xFFFDFBF5),
    onSurface = AppColors.Ink,
    surfaceVariant = Color(0xFFEDE7D8),
    onSurfaceVariant = AppColors.InkSecondary,
    outline = Color(0xFFD8CFB8),
    error = Color(0xFFC93A2E)
)

private val DarkScheme = darkColorScheme(
    primary = AppColors.Mint,
    onPrimary = OnAccent,
    primaryContainer = Color(0xFF3A2C00),
    onPrimaryContainer = AppColors.Mint,
    secondary = AppColors.Muted,
    onSecondary = AppColors.DarkBackground,
    background = AppColors.DarkBackground,
    onBackground = AppColors.OffWhite,
    surface = Color(0xFF1A1A1F),
    onSurface = AppColors.OffWhite,
    surfaceVariant = Color(0xFF26262C),
    onSurfaceVariant = AppColors.Muted,
    outline = Color(0xFF3A3A42),
    error = Color(0xFFFF6B5E)
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
