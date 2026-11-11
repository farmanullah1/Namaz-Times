package com.example.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            "quran" -> stack = listOf(AppDestination.Quran)
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

    val showBottomBar = stack.size == 1

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
                        shadowElevation = 10.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MainTab.entries.forEach { tab ->
                                val selected = stack.first().tab() == tab
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { stack = listOf(tab.destination) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = tab.label,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
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

