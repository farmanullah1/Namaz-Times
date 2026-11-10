package com.example.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppViewModel
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.CalculationSettingsScreen
import com.example.ui.screens.HijriSettingsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LocationSettingsScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.MosqueEditorScreen
import com.example.ui.screens.MosqueListScreen
import com.example.ui.screens.NotificationSettingsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlacesScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.screens.ReliabilityScreen
import com.example.ui.screens.TodayScreen
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AppNavHost(
    viewModel: AppViewModel,
    destinationRequests: StateFlow<String?>
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = rememberPermissionActions(viewModel)
    val snackbarHostState = remember { SnackbarHostState() }

    var stack by remember { mutableStateOf(listOf<AppDestination>(AppDestination.Today)) }
    val current = stack.last()

    val request by destinationRequests.collectAsStateWithLifecycle()
    LaunchedEffect(request) {
        val destination = request?.substringBefore('#') ?: return@LaunchedEffect
        when (destination) {
            "qibla" -> stack = listOf(AppDestination.Qibla)
            "today" -> stack = listOf(AppDestination.Today)
        }
    }

    LaunchedEffect(state.loading, state.settings.onboardingComplete) {
        if (!state.loading &&
            !state.settings.onboardingComplete &&
            stack.none { it is AppDestination.Onboarding }
        ) {
            stack = listOf(AppDestination.Today, AppDestination.Onboarding)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    BackHandler(enabled = stack.size > 1) {
        stack = stack.dropLast(1)
    }

    val showBottomBar = current !is AppDestination.Onboarding

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    tonalElevation = 6.dp()
                ) {
                    MainTab.entries.forEach { tab ->
                        val selected = stack.first().tab() == tab && current.tab() == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (current !is AppDestination.Today && tab == MainTab.TODAY) {
                                    stack = listOf(AppDestination.Today)
                                } else if (current.tab() == tab && stack.size == 1) {
                                    stack = listOf(tab.destination)
                                } else {
                                    stack = listOf(tab.destination)
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = {
                                Text(
                                    tab.label,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (current) {
                is AppDestination.Today -> TodayScreen(
                    state = state,
                    viewModel = viewModel,
                    permissions = permissions,
                    onOpenPlaces = { stack = listOf(AppDestination.Places) },
                    onOpenMosques = { stack = stack + AppDestination.MosqueList },
                    onOpenQibla = { stack = listOf(AppDestination.Qibla) },
                    onOpenQuran = { stack = listOf(AppDestination.Quran) }
                )

                is AppDestination.Places -> PlacesScreen(
                    state = state,
                    viewModel = viewModel,
                    permissions = permissions,
                    onOpenMosqueEditor = { mosqueId -> stack = stack + AppDestination.MosqueEditor(mosqueId) },
                    onOpenMosqueList = { stack = stack + AppDestination.MosqueList }
                )

                is AppDestination.Qibla -> QiblaScreen(
                    state = state,
                    permissions = permissions
                )

                is AppDestination.Quran -> QuranScreen(
                    state = state,
                    viewModel = viewModel
                )

                is AppDestination.More -> MoreScreen(
                    state = state,
                    viewModel = viewModel,
                    onNavigate = { destination -> stack = stack + destination }
                )

                is AppDestination.MosqueList -> MosqueListScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { stack = stack.dropLast(1) },
                    onEdit = { mosqueId -> stack = stack + AppDestination.MosqueEditor(mosqueId) }
                )

                is AppDestination.MosqueEditor -> MosqueEditorScreen(
                    state = state,
                    viewModel = viewModel,
                    mosqueId = current.mosqueId,
                    onBack = { stack = stack.dropLast(1) }
                )

                is AppDestination.History -> HistoryScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { stack = stack.dropLast(1) }
                )

                is AppDestination.CalculationSettings -> CalculationSettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { stack = stack.dropLast(1) }
                )

                is AppDestination.NotificationSettings -> NotificationSettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    permissions = permissions,
                    onBack = { stack = stack.dropLast(1) }
                )

                is AppDestination.HijriSettings -> HijriSettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { stack = stack.dropLast(1) }
                )

                is AppDestination.LocationSettings -> LocationSettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    permissions = permissions,
                    onBack = { stack = stack.dropLast(1) }
                )

                is AppDestination.Backup -> BackupScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { stack = stack.dropLast(1) }
                )

                is AppDestination.Reliability -> ReliabilityScreen(
                    state = state,
                    viewModel = viewModel,
                    permissions = permissions,
                    onBack = { stack = stack.dropLast(1) }
                )

                is AppDestination.About -> AboutScreen(onBack = { stack = stack.dropLast(1) })

                is AppDestination.Onboarding -> OnboardingScreen(
                    state = state,
                    viewModel = viewModel,
                    permissions = permissions,
                    onFinish = { stack = listOf(AppDestination.Today) }
                )
            }
        }
    }
}

private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())
