package com.examples.testros2jsbridge.presentation.ui.navigation

import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.examples.testros2jsbridge.domain.model.ControllerPreset
import com.examples.testros2jsbridge.presentation.ui.MainActivity
import com.examples.testros2jsbridge.presentation.ui.screens.connection.ConnectionScreen
import com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerConfigScreen
import com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerOverviewScreen
import com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerScreen
import com.examples.testros2jsbridge.presentation.ui.screens.geometry.GeometryMessageScreen
import com.examples.testros2jsbridge.presentation.ui.screens.protocol.CustomProtocolScreen
import com.examples.testros2jsbridge.presentation.ui.screens.publisher.CreatePublisherScreen
import com.examples.testros2jsbridge.presentation.ui.screens.publisher.PublisherListScreen
import com.examples.testros2jsbridge.presentation.ui.screens.publisher.PublisherScreen
import com.examples.testros2jsbridge.presentation.ui.screens.settings.SettingScreen
import com.examples.testros2jsbridge.presentation.ui.screens.subscriber.SubscriberScreen
import com.examples.testros2jsbridge.presentation.ui.screens.subscriber.TopicListScreen
import androidx.navigation.NavHostController
import com.examples.testros2jsbridge.presentation.ui.screens.controller.ControllerViewModel
import com.examples.testros2jsbridge.presentation.ui.screens.geometry.GeometryViewModel
import com.examples.testros2jsbridge.presentation.ui.screens.protocol.ProtocolViewModel
import com.examples.testros2jsbridge.presentation.ui.screens.publisher.PublisherViewModel
import com.examples.testros2jsbridge.presentation.ui.screens.settings.SettingsViewModel
import com.examples.testros2jsbridge.presentation.ui.screens.subscriber.SubscriberViewModel
import com.examples.testros2jsbridge.presentation.ui.screens.connection.ConnectionViewModel

@Suppress("unused")
object Destinations {
    const val SPLASH_SCREEN = "splash_screen"
    const val MAIN_ACTIVITY = "main_activity"
    const val CONNECTION_SCREEN = "connection_screen"
    const val CONTROLLER_CONFIG_SCREEN = "controller_config_screen"
    const val CONTROLLER_OVERVIEW_SCREEN = "controller_overview_screen"
    const val CONTROLLER_SCREEN = "controller_screen"
    const val GEOMETRY_MESSAGE_SCREEN = "geometry_message_screen"
    const val CUSTOM_PROTOCOL_SCREEN = "custom_protocol_screen"
    const val CREATE_PUBLISHER_SCREEN = "create_publisher_screen"
    const val PUBLISHER_LIST_SCREEN = "publisher_list_screen"
    const val PUBLISHER_SCREEN = "publisher_screen"
    const val SETTINGS_SCREEN = "settings_screen"
    const val SUBSCRIBER_SCREEN = "subscriber_screen"
    const val TOPIC_LIST_SCREEN = "topic_list_screen"
}

fun NavGraphBuilder.setupNavigation(navController: NavHostController) {
    composable(route = Destinations.MAIN_ACTIVITY) { MainActivity() }
    composable(route = Destinations.CONNECTION_SCREEN) {
        ConnectionScreen(
            viewModel = hiltViewModel(),
            onBack = { navController.popBackStack() }
        )
    }
    composable(route = Destinations.CONTROLLER_CONFIG_SCREEN) {
        val viewModel: ControllerViewModel = hiltViewModel()
        val uiState = viewModel.uiState.collectAsState()
        ControllerConfigScreen(
            controllerButtons = uiState.value.controllerButtons,
            appActions = uiState.value.appActions,
            presets = uiState.value.presets,
            selectedPreset = uiState.value.selectedPreset?.name,
            joystickMappings = uiState.value.config.joystickMappings,
            onPresetSelected = viewModel::selectPreset,
            onAddPreset = viewModel::addPreset,
            onRemovePreset = viewModel::removePreset,
            onSavePreset = viewModel::savePreset,
            onControllerButtonAssign = viewModel::assignButton,
            onJoystickMappingsChanged = viewModel::updateJoystickMappings,
            onBack = { navController.popBackStack() }
        )
    }
    composable(
        route = "${Destinations.CONTROLLER_CONFIG_SCREEN}/{presetName}",
        arguments = listOf(androidx.navigation.navArgument("presetName") { type = androidx.navigation.NavType.StringType })
    ) { backStackEntry ->
        val viewModel: ControllerViewModel = hiltViewModel()
        val uiState = viewModel.uiState.collectAsState()
        val presetName = backStackEntry.arguments?.getString("presetName")
        ControllerConfigScreen(
            controllerButtons = uiState.value.controllerButtons,
            appActions = uiState.value.appActions,
            presets = uiState.value.presets,
            selectedPreset = presetName ?: uiState.value.selectedPreset?.name,
            joystickMappings = uiState.value.config.joystickMappings,
            onPresetSelected = viewModel::selectPreset,
            onAddPreset = viewModel::addPreset,
            onRemovePreset = viewModel::removePreset,
            onSavePreset = viewModel::savePreset,
            onControllerButtonAssign = viewModel::assignButton,
            onJoystickMappingsChanged = viewModel::updateJoystickMappings,
            onBack = { navController.popBackStack() }
        )
    }
    composable(route = Destinations.CONTROLLER_OVERVIEW_SCREEN) {
        val viewModel: ControllerViewModel = hiltViewModel()
        ControllerOverviewScreen(
            viewModel = viewModel,
            backgroundImageRes = null,
            onAbxyButtonClick = { btn -> viewModel.assignAbxyButton(btn, "") },
            onPresetSwap = { preset -> viewModel.selectPreset(preset.name) }
        )
    }
    composable(route = Destinations.CONTROLLER_SCREEN) {
        val viewModel: ControllerViewModel = hiltViewModel()
        ControllerScreen(
            viewModel = viewModel,
            navController = navController,
            onBack = { navController.popBackStack() }
        )
    }
    composable(route = Destinations.GEOMETRY_MESSAGE_SCREEN) {
        val viewModel: GeometryViewModel = hiltViewModel()
        GeometryMessageScreen(viewModel = viewModel)
    }
    composable(route = Destinations.CUSTOM_PROTOCOL_SCREEN) {
        val viewModel: ProtocolViewModel = hiltViewModel()
        CustomProtocolScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }
    composable(route = Destinations.CREATE_PUBLISHER_SCREEN) {
        val viewModel: PublisherViewModel = hiltViewModel()
        CreatePublisherScreen(
            viewModel = viewModel,
            onPublisherCreated = { navController.navigate(Destinations.PUBLISHER_LIST_SCREEN) },
            onCancel = { navController.popBackStack() }
        )
    }
    composable(route = Destinations.PUBLISHER_LIST_SCREEN) {
        val viewModel: PublisherViewModel = hiltViewModel()
        PublisherListScreen(
            viewModel = viewModel,
            onPublisherSelected = { navController.navigate(Destinations.PUBLISHER_SCREEN) },
            onEditPublisher = { navController.navigate(Destinations.CREATE_PUBLISHER_SCREEN) },
            onDeletePublisher = { /* Implement delete logic in ViewModel or pass lambda */ },
        )
    }
    composable(route = Destinations.PUBLISHER_SCREEN) {
        val viewModel: PublisherViewModel = hiltViewModel()
        PublisherScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }
    composable(route = Destinations.SETTINGS_SCREEN) {
        val viewModel: SettingsViewModel = hiltViewModel()
        SettingScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }
    composable(route = Destinations.SUBSCRIBER_SCREEN) {
        val viewModel: SubscriberViewModel = hiltViewModel()
        SubscriberScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
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

