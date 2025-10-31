package com.examples.testros2jsbridge.domain.model

data class ButtonMap(
    val buttonMapId: Int,
    val inputType: String,
    val mappedActionId: String,
    val joystickDeadzone: Float? = null,
    val joystickSensitivity: Float? = null
)
