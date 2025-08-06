package com.examples.testros2jsbridge.presentation.ui.navigation

import java.io.Serializable

// Type-safe navigation arguments for passing between destinations
sealed class NavigationArguments : Serializable {
    data class Message(val message: String) : NavigationArguments() {
        companion object {
            fun create(message: String): NavigationArguments = Message(message)
        }
    }
    data class RosAction(val action: com.examples.testros2jsbridge.domain.model.RosAction) : NavigationArguments()
    data class RosTopic(val topic: com.examples.testros2jsbridge.domain.model.RosTopic) : NavigationArguments()
    data class RosService(val service: com.examples.testros2jsbridge.domain.model.RosService) : NavigationArguments()
    data class ControllerConfigArg(val config: com.examples.testros2jsbridge.domain.model.ControllerConfig) : NavigationArguments()
    data class AppConfigurationArg(val config: com.examples.testros2jsbridge.domain.model.AppConfiguration) : NavigationArguments()
    data class RosConnectionArg(val connection: com.examples.testros2jsbridge.domain.model.RosConnection) : NavigationArguments()
    data class RosMessageArg(val message: com.examples.testros2jsbridge.domain.model.RosMessage) : NavigationArguments()
    data class PublisherArg(val publisher: com.examples.testros2jsbridge.domain.model.Publisher) : NavigationArguments()
    data class SubscriberArg(val subscriber: com.examples.testros2jsbridge.domain.model.Subscriber) : NavigationArguments()
    //data class SliderButtonConfigArg(val config: com.examples.testros2jsbridge.domain.model.SliderButtonConfig) : NavigationArguments()
    //data class RosImageArg(val image: com.examples.testros2jsbridge.domain.model.RosImage) : NavigationArguments()
    data class ControllerPresetArg(val preset: com.examples.testros2jsbridge.domain.model.ControllerPreset) : NavigationArguments()
}