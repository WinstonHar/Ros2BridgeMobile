package com.examples.testros2jsbridge.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.examples.testros2jsbridge.core.ros.RosBridgeViewModel
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.presentation.ui.MainActivity
import com.examples.testros2jsbridge.presentation.ui.screens.connection.ConnectionScreen
import com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerConfigScreen
import com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerOverviewScreen
import com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerScreen
import com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerViewModel
import com.examples.testros2jsbridge.presentation.ui.screens.geometry.GeometryMessageScreen
import com.examples.testros2jsbridge.presentation.ui.screens.geometry.GeometryViewModel
import com.examples.testros2jsbridge.presentation.ui.screens.protocol.CustomProtocolScreen
import com.examples.testros2jsbridge.presentation.ui.screens.protocol.ProtocolViewModel
import com.examples.testros2jsbridge.presentation.ui.screens.publisher.PublisherListScreen
import com.examples.testros2jsbridge.presentation.ui.screens.publisher.PublisherScreen
import com.examples.testros2jsbridge.presentation.ui.screens.publisher.PublisherViewModel
import com.examples.testros2jsbridge.presentation.ui.screens.settings.SettingScreen
import com.examples.testros2jsbridge.presentation.ui.screens.settings.SettingsViewModel
import com.examples.testros2jsbridge.presentation.ui.screens.subscriber.SubscriberScreen
import com.examples.testros2jsbridge.presentation.ui.screens.subscriber.SubscriberViewModel
import com.examples.testros2jsbridge.presentation.ui.screens.subscriber.TopicListScreen

object Destinations {
    const val MAIN_ACTIVITY = "main_activity"
    const val CONNECTION_SCREEN = "connection_screen"
    const val CONTROLLER_CONFIG_SCREEN = "controller_config_screen"
    const val CONTROLLER_OVERVIEW_SCREEN = "controller_overview_screen"
    const val CONTROLLER_SCREEN = "controller_screen"
    const val CUSTOM_PROTOCOL_SCREEN = "custom_protocol_screen"
    const val CREATE_PUBLISHER_SCREEN = "create_publisher_screen"
    const val PUBLISHER_LIST_SCREEN = "publisher_list_screen"
    const val PUBLISHER_SCREEN = "publisher_screen"
    const val SETTINGS_SCREEN = "settings_screen"
    const val SUBSCRIBER_SCREEN = "subscriber_screen"
    const val TOPIC_LIST_SCREEN = "topic_list_screen"
}

fun NavGraphBuilder.setupNavigation(
    navController: NavHostController,
    onRestoreTab: (() -> Unit)? = null
) {
    fun defaultOnBack() { BackNavigationHandler.handleBack(navController) }

    composable(route = Destinations.MAIN_ACTIVITY) { MainActivity() }
    composable(route = Destinations.CONNECTION_SCREEN) {
        ConnectionScreen(
            viewModel = hiltViewModel(),
            onBack = { BackNavigationHandler.handleBack(navController) }
        )
    }
    composable(
        route = "${Destinations.CONTROLLER_CONFIG_SCREEN}/{configName}",
        arguments = listOf(navArgument("configName") { type = NavType.StringType })
    ) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(Destinations.CONTROLLER_SCREEN)
        }
        val controllerViewModel: ControllerViewModel = hiltViewModel(parentEntry)
        val configName = backStackEntry.arguments?.getString("configName") ?: ""
        ControllerConfigScreen(
            configName = configName,
            viewModel = controllerViewModel,
            onBack = { BackNavigationHandler.handleBack(navController) },
        )
    }
    composable(
        route = "${Destinations.CONTROLLER_OVERVIEW_SCREEN}/{configName}",
        arguments = listOf(navArgument("configName") { type = NavType.StringType })
    ) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(Destinations.CONTROLLER_SCREEN)
        }
        val controllerViewModel: ControllerViewModel = hiltViewModel(parentEntry)
        val context = LocalContext.current
        val configName = backStackEntry.arguments?.getString("configName")
        if (configName.isNullOrBlank()) {
            LaunchedEffect(Unit) {
                navController.navigate(Destinations.CONTROLLER_SCREEN) {
                    popUpTo(Destinations.CONTROLLER_OVERVIEW_SCREEN) { inclusive = true }
                }
            }
        } else {
            ControllerOverviewScreen(
                viewModel = controllerViewModel,
                backgroundImageRes = null,
                onAbxyButtonClick = { btn ->
                    controllerViewModel.assignAbxyButton(
                        btn,
                        "",
                        context = context
                    )
                },
                onPresetSwap = { preset -> controllerViewModel.selectPreset(preset.name) },
                selectedConfigName = configName,
            )
        }
    }
    composable(route = Destinations.CONTROLLER_SCREEN) {
        val controllerViewModel: ControllerViewModel = hiltViewModel()
        ControllerScreen(
            viewModel = controllerViewModel,
            navController = navController,
            onBack = { BackNavigationHandler.handleBack(navController) }
        )
    }
    composable(route = Destinations.PUBLISHER_LIST_SCREEN) {
        val viewModel: PublisherViewModel = hiltViewModel()
        PublisherListScreen(
            viewModel = viewModel,
            onPublisherSelected = { navController.navigate(Destinations.PUBLISHER_SCREEN) },
            onEditPublisher = { /* REMOVED now custom protocol */ },
            onDeletePublisher = { /* Implement delete logic in ViewModel or pass lambda */ },
        )
    }
    composable(route = Destinations.PUBLISHER_SCREEN) {
        val viewModel: PublisherViewModel = hiltViewModel()
        val rosBridgeViewModel: RosBridgeViewModel = hiltViewModel()
        PublisherScreen(
            viewModel = viewModel,
            rosBridgeViewModel = rosBridgeViewModel,
            onBack = { BackNavigationHandler.handleBack(navController) }
        )
    }
    composable(route = Destinations.SETTINGS_SCREEN) {
        val viewModel: SettingsViewModel = hiltViewModel()
        SettingScreen(
            viewModel = viewModel,
            onBack = { BackNavigationHandler.handleBack(navController) }
        )
    }
    composable(route = Destinations.SUBSCRIBER_SCREEN) {
        val viewModel: SubscriberViewModel = hiltViewModel()
        SubscriberScreen(
            viewModel = viewModel,
            onBack = { BackNavigationHandler.handleBack(navController) },
            onRestoreTab = onRestoreTab
        )
    }
    composable(route = Destinations.TOPIC_LIST_SCREEN) {
        val viewModel: SubscriberViewModel = hiltViewModel()
        TopicListScreen(
            viewModel = viewModel,
            onTopicSelected = { topic: String, type: String ->
                viewModel.updateTopicInput(topic)
                viewModel.updateTypeInput(type)
                viewModel.showAddSubscriberDialog(true)
            },
            onSubscribe = { topic: String, type: String ->
                viewModel.updateTopicInput(topic)
                viewModel.updateTypeInput(type)
                viewModel.subscribeToTopic()
            }
        )
    }
}

