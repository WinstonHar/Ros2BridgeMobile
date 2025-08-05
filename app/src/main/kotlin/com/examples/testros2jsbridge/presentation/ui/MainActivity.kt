package com.examples.testros2jsbridge.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.examples.testros2jsbridge.presentation.ui.screens.connection.ConnectionScreen
import com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerScreen
import com.examples.testros2jsbridge.presentation.ui.screens.publisher.PublisherScreen
import com.examples.testros2jsbridge.presentation.ui.screens.subscriber.SubscriberScreen
import com.examples.testros2jsbridge.presentation.ui.screens.protocol.ProtocolScreen
import com.examples.testros2jsbridge.presentation.ui.screens.settings.SettingScreen
import com.examples.testros2jsbridge.presentation.ui.components.CollapsibleMessageHistoryList

/**
 * MainActivity provides the main entry point for the app UI, handling connection to rosbridge, navigation, and message history display.
 * All business logic is delegated to modular ViewModels and Compose screens.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainActivityContent()
            }
        }
    }
}

@Composable
fun MainActivityContent() {
    // Navigation state: which module is active
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf(
        "Connection", "Controller", "Publisher", "Subscriber", "Protocol", "Settings"
    )

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Top tab bar for navigation
        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Main content area: swap in modular Compose screens
        when (selectedTab) {
            0 -> ConnectionScreen()
            1 -> ControllerScreen(onBack = {})
            2 -> PublisherScreen(viewModel = viewModel(), onBack = {})
            3 -> SubscriberScreen(viewModel = viewModel(), onBack = {})
            4 -> ProtocolScreen(viewModel = viewModel(), onBack = {})
            5 -> SettingScreen(viewModel = viewModel(), onBack = {})
        }

        Spacer(Modifier.height(16.dp))

        // Message history log (Compose)
        val publisherViewModel: com.examples.testros2jsbridge.presentation.ui.screens.publisher.PublisherViewModel = viewModel()
        val messageHistory by publisherViewModel.messageHistory.collectAsState()
        CollapsibleMessageHistoryList(messageHistory = messageHistory)
    }
}