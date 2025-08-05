package com.examples.testros2jsbridge.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavGraphBuilder
import androidx.navigation.compose.composable
import com.examples.testros2jsbridge.presentation.ui.screens.*

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

fun NavGraphBuilder.setupNavigation() {
    composable(route = Destinations.MAIN_ACTIVITY) { MainActivity() }
    composable(route = Destinations.CONNECTION_SCREEN) { ConnectionScreen() }
    composable(route = Destinations.CONTROLLER_CONFIG_SCREEN) { ControllerConfigScreen() }
    composable(route = Destinations.CONTROLLER_OVERVIEW_SCREEN) { ControllerOverviewScreen() }
    composable(route = Destinations.CONTROLLER_SCREEN) { ControllerScreen() }
    composable(route = Destinations.GEOMETRY_MESSAGE_SCREEN) { GeometryMessageScreen() }
    composable(route = Destinations.CUSTOM_PROTOCOL_SCREEN) { CustomProtocolScreen() }
    composable(route = Destinations.CREATE_PUBLISHER_SCREEN) { CreatePublisherScreen() }
    composable(route = Destinations.PUBLISHER_LIST_SCREEN) { PublisherListScreen() }
    composable(route = Destinations.PUBLISHER_SCREEN) { PublisherScreen() }
    composable(route = Destinations.SETTINGS_SCREEN) { SettingsScreen() }
    composable(route = Destinations.SUBSCRIBER_SCREEN) { SubscriberScreen() }
    composable(route = Destinations.TOPIC_LIST_SCREEN) { TopicListScreen() }
}

