package com.examples.testros2jsbridge.domain.model

data class JoystickMapping(
    val displayName: String = "",
    val topic: RosId? = null,
    val type: String? = "",
    val axisX: Int = 0,
    val axisY: Int = 1,
    val max: Float? = 1.0f,
    val step: Float? = 0.2f,
    val deadzone: Float? = 0.1f
)
