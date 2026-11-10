package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.MosqueLocation
import com.example.ui.components.AddEditMosqueDialog
import com.example.ui.components.MosqueQuickSwitchDialog
import com.example.ui.screens.HijriScreen
import com.example.ui.screens.MosquesScreen
import com.example.ui.screens.PrayerTimesScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.theme.KaabaGold
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PrayerViewModel
import kotlinx.coroutines.launch

enum class AppScreen(val title: String, val icon: ImageVector) {
    PRAYER("Prayer Times", Icons.Default.AccessTime),
    MOSQUES("Mosques", Icons.Default.Mosque),
    QIBLA("Qibla", Icons.Default.Explore),
    HIJRI("Hijri & Fiqh", Icons.Default.CalendarMonth)
}

class MainActivity : ComponentActivity() {

    private val viewModel: PrayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: PrayerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf(AppScreen.PRAYER) }
    var showQuickSwitchDialog by remember { mutableStateOf(false) }
    var showAddMosqueDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Request permissions on launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            viewModel.tryDetectLocation()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needsRequest = permissionsToRequest.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsRequest) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Handle back button on sub-screens
    BackHandler(enabled = currentScreen != AppScreen.PRAYER) {
        currentScreen = AppScreen.PRAYER
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(KaabaGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🕌", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Noor Salah",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = "Ahl al-Hadith • Salafi",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = KaabaGold,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        // Top right quick location chip
                        uiState.activeMosque?.let { mosque ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showQuickSwitchDialog = true }
                                    .testTag("top_bar_mosque_switcher")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = KaabaGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = mosque.name.replace(" Mosque", ""),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Switch",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                AppScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.PRAYER -> {
                    PrayerTimesScreen(
                        uiState = uiState,
                        onSwitchMosqueClick = { showQuickSwitchDialog = true },
                        onTestNotificationClick = { prayer ->
                            viewModel.triggerTestNotification(prayer)
                            scope.launch {
                                snackbarHostState.showSnackbar("Alert triggered for ${prayer.name.title} (${uiState.activeMosque?.name})")
                            }
                        }
                    )
                }
                AppScreen.MOSQUES -> {
                    MosquesScreen(
                        uiState = uiState,
                        onSelectMosque = { id ->
                            viewModel.selectActiveMosque(id)
                            val m = uiState.allMosques.find { it.id == id }
                            scope.launch {
                                snackbarHostState.showSnackbar("Switched to ${m?.name}")
                            }
                        },
                        onSaveMosque = { mosque ->
                            viewModel.saveMosque(mosque)
                            scope.launch {
                                snackbarHostState.showSnackbar("Saved ${mosque.name}")
                            }
                        },
                        onDeleteMosque = { id ->
                            viewModel.deleteMosque(id)
                            scope.launch {
                                snackbarHostState.showSnackbar("Mosque removed")
                            }
                        },
                        onToggleAutoLocation = { enabled ->
                            viewModel.toggleAutoLocation(enabled)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (enabled) "Auto location-based mosque switching enabled"
                                    else "Manual mosque selection active"
                                )
                            }
                        },
                        onRefreshGps = {
                            viewModel.tryDetectLocation()
                            scope.launch {
                                snackbarHostState.showSnackbar("Refreshing GPS coordinates...")
                            }
                        }
                    )
                }
                AppScreen.QIBLA -> {
                    QiblaScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }
                AppScreen.HIJRI -> {
                    HijriScreen(
                        uiState = uiState,
                        onSetHijriAdjustment = { days ->
                            viewModel.setHijriAdjustment(days)
                            scope.launch {
                                snackbarHostState.showSnackbar("Hijri calendar adjustment set to $days days")
                            }
                        },
                        onSetFiqhSchool = { school ->
                            viewModel.setFiqhSchool(school)
                            scope.launch {
                                snackbarHostState.showSnackbar("Fiqh updated to ${school.title}")
                            }
                        },
                        onSetCalculationMethod = { method ->
                            viewModel.setCalculationMethod(method)
                            scope.launch {
                                snackbarHostState.showSnackbar("Calculation method: ${method.title}")
                            }
                        }
                    )
                }
            }
        }
    }

    // Quick switch dialog
    if (showQuickSwitchDialog) {
        MosqueQuickSwitchDialog(
            mosques = uiState.allMosques,
            activeMosqueId = uiState.activeMosque?.id,
            onSelectMosque = { id ->
                viewModel.selectActiveMosque(id)
                val m = uiState.allMosques.find { it.id == id }
                scope.launch {
                    snackbarHostState.showSnackbar("Switched to ${m?.name}")
                }
            },
            onAddNewMosqueClick = {
                showAddMosqueDialog = true
            },
            onDismiss = { showQuickSwitchDialog = false }
        )
    }

    // Add mosque dialog
    if (showAddMosqueDialog) {
        AddEditMosqueDialog(
            currentGpsCoords = uiState.userCoordinates,
            onDismiss = { showAddMosqueDialog = false },
            onSave = { newMosque ->
                viewModel.saveMosque(newMosque)
                showAddMosqueDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Added ${newMosque.name}")
                }
            }
        )
    }
}
