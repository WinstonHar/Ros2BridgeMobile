package com.examples.testros2jsbridge.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.examples.testros2jsbridge.presentation.ui.navigation.Destinations
import com.examples.testros2jsbridge.presentation.ui.theme.Ros2BridgeTheme
import dagger.hilt.android.AndroidEntryPoint
import com.examples.testros2jsbridge.presentation.ui.navigation.setupNavigation

/**
 * MainActivity provides the main entry point for the app UI, handling connection to rosbridge, navigation, and message history display.
 * All business logic is delegated to modular ViewModels and Compose screens.
 */

@AndroidEntryPoint

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Ros2BridgeTheme {
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
                var selectedTab by rememberSaveable { mutableIntStateOf(0) } // Default to Connection

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
                                        // Avoid building up a huge back stack
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
                    // Navigation host for all screens
                    NavHost(
                        navController = navController,
                        startDestination = Destinations.CONTROLLER_SCREEN,
                        modifier = Modifier.weight(1f)
                    ) {
                        setupNavigation(navController)
                    }
                }
            }
        }
    }
}

@Composable
fun MainActivityContent() {
    // Navigation state: which module is active
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "Connection", "Controller", "Controller Overview", "Publisher", "Subscriber", "Geometry", "Protocol", "Settings"
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

            // Make the main content area scrollable
            androidx.compose.foundation.rememberScrollState().let { scrollState ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    // Main content area: swap in modular Compose screens
                    when (selectedTab) {
                        0 -> ConnectionScreen(viewModel = hiltViewModel(), onBack = {})
                        1 -> ControllerScreen(
                            viewModel = hiltViewModel<com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerViewModel>(),
                            navController = navController,
                            onBack = {}
                        )
                        2 -> com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerOverviewScreen(
                            viewModel = hiltViewModel(),
                            backgroundImageRes = null,
                            onAbxyButtonClick = {},
                            onPresetSwap = {}
                        )
                        3 -> PublisherScreen(viewModel = hiltViewModel(), onBack = {})
                        4 -> SubscriberScreen(viewModel = hiltViewModel(), onBack = {})
                        5 -> com.examples.testros2jsbridge.presentation.ui.screens.geometry.GeometryMessageScreen(viewModel = hiltViewModel())
                        6 -> CustomProtocolScreen(viewModel = hiltViewModel(), onBack = {})
                        7 -> SettingScreen(viewModel = hiltViewModel(), onBack = {})
                    }

                    Spacer(Modifier.height(16.dp))

                    // Message history log (Compose)
                    val publisherViewModel: PublisherViewModel = hiltViewModel()
                    val uiState by publisherViewModel.uiState.collectAsState()
                    CollapsibleMessageHistoryList(messageHistory = uiState.messageHistory)
                }
            }
        }
    }
}