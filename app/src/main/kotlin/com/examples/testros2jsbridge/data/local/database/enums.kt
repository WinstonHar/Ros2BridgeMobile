package com.examples.testros2jsbridge.data.local.database

enum class InputType {
    BUTTON,
    JOYSTICK
}

enum class RosProtocolType {
    PUBLISHER,
    SUBSCRIBER,
    ACTION_CLIENT,
    SERVICE_CLIENT,
    INTERNAL
}