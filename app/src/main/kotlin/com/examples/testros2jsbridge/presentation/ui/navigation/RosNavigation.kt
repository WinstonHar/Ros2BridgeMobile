package com.examples.testros2jsbridge.presentation.ui.navigation

import androidx.navigation.NavHostController

// Centralized navigation logic
class RosNavigation {
    fun start(navController: NavHostController) = navController.navigate(Destinations.MAIN_ACTIVITY)

    fun toConnection(navController: NavHostController, connection: com.examples.testros2jsbridge.domain.model.RosConnection? = null) {
        // Pass connection as argument if needed
        navController.navigate(Destinations.CONNECTION_SCREEN)
    }

    fun toControllerConfig(navController: NavHostController, config: com.examples.testros2jsbridge.domain.model.ControllerConfig? = null) {
        navController.navigate(Destinations.CONTROLLER_CONFIG_SCREEN)
    }

    fun toControllerOverview(navController: NavHostController, configName: String) = 
        navController.navigate("${Destinations.CONTROLLER_OVERVIEW_SCREEN}/$configName") 

    fun toController(navController: NavHostController, preset: com.examples.testros2jsbridge.domain.model.ControllerPreset? = null) {
        // Pass preset as argument if needed
        navController.navigate(Destinations.CONTROLLER_SCREEN)
    }

    fun toCustomProtocol(navController: NavHostController, protocol: com.examples.testros2jsbridge.presentation.state.ProtocolUiState.ProtocolFile? = null) {
        // Pass protocol as argument if needed
        navController.navigate(Destinations.CUSTOM_PROTOCOL_SCREEN)
    }

    fun toPublisherList(navController: NavHostController) = navController.navigate(Destinations.PUBLISHER_LIST_SCREEN)

    fun toPublisher(navController: NavHostController, publisher: com.examples.testros2jsbridge.domain.model.Publisher? = null) {
        navController.navigate(Destinations.PUBLISHER_SCREEN)
    }

    fun toSettings(navController: NavHostController, config: com.examples.testros2jsbridge.domain.model.AppConfiguration? = null) {
        // Pass config as argument if needed
        navController.navigate(Destinations.SETTINGS_SCREEN)
    }

    fun toSubscriber(navController: NavHostController, subscriber: com.examples.testros2jsbridge.domain.model.Subscriber? = null) {
        navController.navigate(Destinations.SUBSCRIBER_SCREEN)
    }

    fun toTopicList(navController: NavHostController, topic: com.examples.testros2jsbridge.domain.model.RosTopic? = null) {
        // Pass topic as argument if needed
        navController.navigate(Destinations.TOPIC_LIST_SCREEN)
    }

    /* not implemented yet
    fun toMessages(navController: NavHostController, message: com.examples.testros2jsbridge.domain.model.RosMessage? = null) {
        navController.navigate(Destinations.MESSAGES_SCREEN)
    }

    fun toService(navController: NavHostController, service: com.examples.testros2jsbridge.domain.model.RosService? = null) {
        // Pass service as argument if needed
        navController.navigate(Destinations.ROS_SERVICE_SCREEN)
    }

    fun toAction(navController: NavHostController, action: com.examples.testros2jsbridge.domain.model.RosAction? = null) {
        // Pass action as argument if needed
        navController.navigate(Destinations.ROS_ACTION_SCREEN)
    }

    fun toSliderButtonConfig(navController: NavHostController, config: com.examples.testros2jsbridge.domain.model.SliderButtonConfig? = null) {
        // Pass config as argument if needed
        navController.navigate(Destinations.SLIDER_BUTTON_CONFIG_SCREEN)
    }

    fun toRosImage(navController: NavHostController, image: com.examples.testros2jsbridge.domain.model.RosImage? = null) {
        // Pass image as argument if needed
        navController.navigate(Destinations.ROS_IMAGE_SCREEN)
    }

    fun toAppConfiguration(navController: NavHostController, config: com.examples.testros2jsbridge.domain.model.AppConfiguration? = null) {
        navController.navigate(Destinations.APP_CONFIGURATION_SCREEN)
    } */

    // Add more navigation helpers as needed for other screens and argument types
}