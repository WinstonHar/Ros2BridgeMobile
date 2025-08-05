package com.examples.testros2jsbridge.domain.model

/*
Controller configuration business rules
*/

data class ControllerConfig(
    val addressingMode: RosId = RosId("DIRECT"), // "DIRECT", "INVERTED_ROTATED"
    val sensitivity: Float = 1.0f,
    val buttonPresets: Map<String, String> = emptyMap(), // e.g., yxba mappings
    val invertYAxis: Boolean = false,
    val deadZone: Float = 0.05f,
    val customProfileName: String? = null,
    val joystickMappings: List<JoystickMapping> = emptyList(),
    val controllerPresets: List<ControllerPreset> = emptyList(),
    val buttonAssignments: Map<String, AppAction> = emptyMap(),
    val joystickPublishRate: Int = 5
    // Add other controller-specific settings as needed
)

// These should be in their own files, but for reference:
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

data class ControllerPreset(
    val name: String = "Preset",
    val topic: RosId? = null,
    val abxy: Map<String, String> = mapOf("A" to "", "B" to "", "X" to "", "Y" to "")
)

data class AppAction(
    val displayName: String,
    val topic: RosId,
    val type: String,
    val source: String,
    val msg: String = ""
)