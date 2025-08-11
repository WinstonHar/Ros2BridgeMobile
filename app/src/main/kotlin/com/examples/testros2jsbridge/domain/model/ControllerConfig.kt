package com.examples.testros2jsbridge.domain.model

import kotlinx.serialization.Serializable

import org.yaml.snakeyaml.Yaml
import java.io.StringWriter

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
    val controllerPresets: List<ControllerPreset> = listOf(
        ControllerPreset(
            name = "Default",
            buttonAssignments = emptyMap(),
            joystickMappings = emptyList()
        )
    ),
    val buttonAssignments: Map<String, AppAction> = emptyMap(),
    val joystickPublishRate: Int = 5,
    val name: String = "Unnamed Config" // Unique identifier for each configuration
    // Add other controller-specific settings as needed
) {
    companion object
}
// --- Extension functions for YAML and Map conversion ---

fun ControllerConfig.toYaml(): String {
    val yaml = Yaml()
    val writer = StringWriter()
    yaml.dump(this.toMap(), writer)
    return writer.toString()
}

fun ControllerConfig.Companion.fromYaml(yamlString: String): ControllerConfig {
    val yaml = Yaml()
    val map = yaml.load<Map<String, Any>>(yamlString)
    return fromMap(map)
}

fun ControllerConfig.toMap(): Map<String, Any?> {
    return mapOf(
        "addressingMode" to addressingMode,
        "sensitivity" to sensitivity,
        "buttonPresets" to buttonPresets,
        "invertYAxis" to invertYAxis,
        "deadZone" to deadZone,
        "customProfileName" to customProfileName,
        "joystickMappings" to joystickMappings,
        "controllerPresets" to controllerPresets,
        "buttonAssignments" to buttonAssignments,
        "joystickPublishRate" to joystickPublishRate,
        "name" to name
    )
}

fun ControllerConfig.Companion.fromMap(map: Map<String, Any?>): ControllerConfig {
        return ControllerConfig(
            addressingMode = map["addressingMode"] as? RosId ?: RosId("DIRECT"),
            sensitivity = (map["sensitivity"] as? Number)?.toFloat() ?: 1.0f,
            buttonPresets = (map["buttonPresets"] as? Map<*, *>)?.filterKeys { it is String }?.mapKeys { it.key as String }?.filterValues { it is String }?.mapValues { it.value as String } ?: emptyMap(),
            invertYAxis = map["invertYAxis"] as? Boolean ?: false,
            deadZone = (map["deadZone"] as? Number)?.toFloat() ?: 0.05f,
            customProfileName = map["customProfileName"] as? String,
            joystickMappings = (map["joystickMappings"] as? List<*>)?.filterIsInstance<Map<String, Any?>>()?.map { jm ->
                JoystickMapping(
                    displayName = jm["displayName"] as? String ?: "",
                    topic = jm["topic"] as? RosId,
                    type = jm["type"] as? String ?: ""
                )
            } ?: emptyList(),
            controllerPresets = (map["controllerPresets"] as? List<*>)?.filterIsInstance<Map<String, Any?>>()?.map { cp ->
                ControllerPreset(
                    name = cp["name"] as? String ?: "",
                    buttonAssignments = (cp["buttonAssignments"] as? Map<*, *>)?.filterKeys { it is String }?.mapKeys { it.key as String }?.filterValues { it is AppAction }?.mapValues { it.value as AppAction } ?: emptyMap(),
                    joystickMappings = (cp["joystickMappings"] as? List<*>)?.filterIsInstance<Map<String, Any?>>()?.map { jm ->
                        JoystickMapping(
                            displayName = jm["displayName"] as? String ?: "",
                            topic = jm["topic"] as? RosId,
                            type = jm["type"] as? String ?: ""
                        )
                    } ?: emptyList()
                )
            } ?: emptyList(),
            buttonAssignments = (map["buttonAssignments"] as? Map<*, *>)?.filterKeys { it is String }?.mapKeys { it.key as String }?.filterValues { it is AppAction }?.mapValues { it.value as AppAction } ?: emptyMap(),
            joystickPublishRate = (map["joystickPublishRate"] as? Number)?.toInt() ?: 5,
            name = map["name"] as? String ?: "Unnamed Config"
        )
    }

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
    val buttonAssignments: Map<String, AppAction> = emptyMap(),
    val joystickMappings: List<JoystickMapping> = emptyList()
)

@Serializable
data class AppAction(
    val id: String, // Unique identifier for persistence
    val displayName: String,
    val topic: String,
    val type: String,
    val source: String,
    val msg: String = ""
)