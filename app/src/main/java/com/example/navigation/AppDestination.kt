package com.example.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector

sealed interface AppDestination {
    data object Today : AppDestination
    data object Places : AppDestination
    data object Qibla : AppDestination
    data object Quran : AppDestination
    data object More : AppDestination

    data object MosqueList : AppDestination
    data class MosqueEditor(val mosqueId: String?) : AppDestination
    data object History : AppDestination
    data object CalculationSettings : AppDestination
    data object NotificationSettings : AppDestination
    data object HijriSettings : AppDestination
    data object LocationSettings : AppDestination
    data object Backup : AppDestination
    data object Reliability : AppDestination
    data object About : AppDestination
    data object Onboarding : AppDestination
}

enum class MainTab(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector
) {
    TODAY(AppDestination.Today, "Today", Icons.Default.AccessTime),
    PLACES(AppDestination.Places, "Places", Icons.Default.Place),
    QIBLA(AppDestination.Qibla, "Qibla", Icons.Default.Explore),
    QURAN(AppDestination.Quran, "Quran", Icons.Default.MenuBook),
    MORE(AppDestination.More, "More", Icons.Default.MoreHoriz)
}

fun AppDestination.tab(): MainTab = when (this) {
    is AppDestination.Today -> MainTab.TODAY
    is AppDestination.Places -> MainTab.PLACES
    is AppDestination.Qibla -> MainTab.QIBLA
    is AppDestination.Quran -> MainTab.QURAN
    is AppDestination.More -> MainTab.MORE
    else -> MainTab.MORE
}
