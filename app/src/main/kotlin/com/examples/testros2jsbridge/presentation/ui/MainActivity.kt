package com.examples.testros2jsbridge.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.examples.testros2jsbridge.presentation.ui.screens.settings.SettingsViewModel
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.examples.testros2jsbridge.presentation.ui.navigation.Destinations
import com.examples.testros2jsbridge.presentation.ui.navigation.setupNavigation
import com.examples.testros2jsbridge.presentation.ui.theme.Ros2BridgeTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll

/**
 * MainActivity provides the main entry point for the app UI, handling connection to rosbridge, navigation, and message history display.
 * All business logic is delegated to modular ViewModels and Compose screens.
 */


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val uiState by settingsViewModel.uiState.collectAsState()
            val useDarkTheme = when (uiState.theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            Ros2BridgeTheme(useDarkTheme = useDarkTheme) {
                val navController = rememberNavController()
                val tabTitles = listOf(
                    "Connection", "Controller", "Controller Overview", "Publisher", "Subscriber", "Geometry", "Protocol", "Settings"
                )
                val destinations = listOf(
                    Destinations.CONNECTION_SCREEN,
                    Destinations.CONTROLLER_SCREEN,
                    Destinations.CONTROLLER_OVERVIEW_SCREEN,
                    Destinations.PUBLISHER_SCREEN,
                    Destinations.SUBSCRIBER_SCREEN,
                    Destinations.GEOMETRY_MESSAGE_SCREEN,
                    Destinations.CUSTOM_PROTOCOL_SCREEN,
                    Destinations.SETTINGS_SCREEN
                )
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }

                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            edgePadding = 0.dp
                        ) {
                            tabTitles.forEachIndexed { idx, title ->
                                Tab(
                                    selected = selectedTab == idx,
                                    onClick = {
                                        selectedTab = idx
                                        navController.navigate(destinations[idx]) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    text = { Text(title, maxLines = 1) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // Make the main content scrollable if needed
                        androidx.compose.foundation.rememberScrollState().let { scrollState ->
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState)
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = Destinations.CONNECTION_SCREEN,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    setupNavigation(navController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

