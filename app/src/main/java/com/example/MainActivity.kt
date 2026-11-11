package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.design.NamazTimesTheme
import com.example.ui.AppViewModel
import com.example.navigation.AppNavHost
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private val destinationRequests = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = when (state.settings.themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            NamazTimesTheme(darkTheme = darkTheme) {
                AppNavHost(viewModel = viewModel, destinationRequests = destinationRequests)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val destination = intent?.getStringExtra(EXTRA_DESTINATION) ?: return
        destinationRequests.value = "$destination#${System.currentTimeMillis()}"
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val DESTINATION_TODAY = "today"
        const val DESTINATION_QIBLA = "qibla"
        const val DESTINATION_QURAN = "quran"
    }
}
