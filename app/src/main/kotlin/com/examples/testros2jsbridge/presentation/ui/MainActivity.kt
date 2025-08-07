package com.examples.testros2jsbridge.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examples.testros2jsbridge.presentation.ui.screens.connection.ConnectionScreen
import com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerScreen
import com.examples.testros2jsbridge.presentation.ui.screens.publisher.PublisherScreen
import com.examples.testros2jsbridge.presentation.ui.screens.subscriber.SubscriberScreen
import com.examples.testros2jsbridge.presentation.ui.screens.protocol.CustomProtocolScreen
import com.examples.testros2jsbridge.presentation.ui.screens.settings.SettingScreen
import com.examples.testros2jsbridge.presentation.ui.components.CollapsibleMessageHistoryList
import com.examples.testros2jsbridge.presentation.ui.screens.publisher.PublisherViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity provides the main entry point for the app UI, handling connection to rosbridge, navigation, and message history display.
 * All business logic is delegated to modular ViewModels and Compose screens.
 */

@AndroidEntryPoint

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Get settings viewmodel and theme state
            val settingsViewModel: com.examples.testros2jsbridge.presentation.ui.screens.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val settingsUiState by settingsViewModel.uiState.collectAsState()
            // Determine theme
            val useDarkTheme = when (settingsUiState.theme) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            com.examples.testros2jsbridge.presentation.ui.theme.Ros2BridgeTheme(useDarkTheme = useDarkTheme) {
                MainActivityContent()
            }
        }
    }
}

@Composable
fun MainActivityContent() {
    // Navigation state: which module is active
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "Connection", "Controller", "Controller Overview", "Publisher", "Subscriber", "Protocol", "Settings"
    )

    // NavController for navigation
    val navController = androidx.navigation.compose.rememberNavController()
    val rosNavigation = remember { com.examples.testros2jsbridge.presentation.ui.navigation.RosNavigation() }

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top tab bar for navigation (scrollable, no edge padding)
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp
            ) {
                tabTitles.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Main content area: swap in modular Compose screens
            when (selectedTab) {
                0 -> ConnectionScreen(viewModel = hiltViewModel(), onBack = {})
                1 -> ControllerScreen(
                    onBack = {},
                    viewModel = hiltViewModel<com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerViewModel>(),
                    onNavigateToConfig = { rosNavigation.toControllerConfig(navController) }
                )
                2 -> com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerOverviewScreen(
                    viewModel = hiltViewModel(),
                    backgroundImageRes = null,
                    onAbxyButtonClick = {},
                    onPresetSwap = {}
                )
                3 -> PublisherScreen(viewModel = hiltViewModel(), onBack = {})
                4 -> SubscriberScreen(viewModel = hiltViewModel(), onBack = {})
                5 -> CustomProtocolScreen(viewModel = hiltViewModel(), onBack = {})
                6 -> SettingScreen(viewModel = hiltViewModel(), onBack = {})
            }

            Spacer(Modifier.height(16.dp))

            // Message history log (Compose)
            val publisherViewModel: PublisherViewModel = hiltViewModel()
            val uiState by publisherViewModel.uiState.collectAsState()
            CollapsibleMessageHistoryList(messageHistory = uiState.messageHistory)
        }
    }
}