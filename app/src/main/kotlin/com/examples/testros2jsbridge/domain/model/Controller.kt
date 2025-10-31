package com.examples.testros2jsbridge.domain.model

data class Controller(
    val controllerId: Int,
    val name: String,
    val presets: List<ButtonPreset>,
    val fixedButtonMaps: List<ButtonMap>
)
