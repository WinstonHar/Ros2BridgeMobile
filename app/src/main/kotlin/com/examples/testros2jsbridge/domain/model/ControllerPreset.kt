package com.examples.testros2jsbridge.domain.model

data class ControllerPreset(
    val name: String = "Preset",
    val topic: RosId? = null,
    val buttonAssignments: Map<String, AppAction> = emptyMap(),
    val joystickMappings: List<JoystickMapping> = emptyList()
)
