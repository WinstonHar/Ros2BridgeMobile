package com.examples.testros2jsbridge.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.presentation.ui.screens.NavGraphs
import com.examples.testros2jsbridge.presentation.ui.screens.destinations.ControllerOverviewScreenDestination
import com.examples.testros2jsbridge.presentation.ui.screens.settings.SettingsViewModel
import com.examples.testros2jsbridge.presentation.ui.theme.Ros2BridgeTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.dependency
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
            val navController = rememberNavController()

            val controllerViewModel: com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerViewModel = hiltViewModel()

            val tabTitles = listOf(
                "Connection", "Controller", "Controller Overview", "Publisher", "Subscriber", "Settings"
            )
            val destinations = listOf(
                "connection_screen",
                "controller_screen",
                "controller_overview_screen", // Only base route
                "publisher_screen",
                "subscriber_screen",
                "setting_screen"
            )
            var selectedTab by rememberSaveable { mutableIntStateOf(0) }
            val tabHistory = androidx.compose.runtime.remember { mutableListOf<Int>() }
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val tabIndex = destinations.indexOf(
                if (currentRoute?.startsWith("controller_overview_screen") == true) "controller_overview_screen" else currentRoute
            )
            if (tabIndex != -1 && tabIndex != selectedTab) {
                selectedTab = tabIndex
            }

            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (tabHistory.isNotEmpty()) {
                        val previousTab = tabHistory.removeAt(tabHistory.lastIndex)
                        selectedTab = previousTab
                        navController.navigate(destinations[previousTab]) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        if (!navController.popBackStack()) {
                            finish()
                        }
                    }
                }
            })

            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val uiState by settingsViewModel.uiState.collectAsState()
            val useDarkTheme = when (uiState.theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            Ros2BridgeTheme(useDarkTheme = useDarkTheme) {
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
                                        if (selectedTab != idx) {
                                            if (idx == 2) {
                                                val selectedConfigName = controllerViewModel.uiState.value.selectedConfigName
                                                val configNames = controllerViewModel.uiState.value.controllerConfigs.map { it.name }
                                                Logger.d("MainActivity","selectedConfigName: ${selectedConfigName}, configNames: ${configNames}")
                                                if (selectedConfigName != "New Config" && configNames.contains(selectedConfigName)) {
                                                    tabHistory.add(selectedTab)
                                                    selectedTab = idx
                                                    navController.navigate("${destinations[idx]}/$selectedConfigName") {
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                } else {
                                                    android.widget.Toast.makeText(
                                                        this@MainActivity,
                                                        "Please select a valid config before opening Controller Overview.",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } else {
                                                tabHistory.add(selectedTab)
                                                selectedTab = idx
                                                navController.navigate(destinations[idx]) {
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    },
                                    text = { Text(title, maxLines = 1) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            DestinationsNavHost(
                                navController = navController,
                                navGraph = NavGraphs.root,
                                modifier = Modifier.fillMaxSize(),
                                dependenciesContainerBuilder = {
                                    dependency(ControllerOverviewScreenDestination) { controllerViewModel }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
