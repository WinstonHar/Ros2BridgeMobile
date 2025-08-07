package com.examples.testros2jsbridge.domain.model

import org.yaml.snakeyaml.Yaml

/*
App-wide settings management
 */

data class AppConfiguration(
    val rosServerUrl: RosId = RosId("10.0.0.0"),
    val defaultPublishTopic: RosId = RosId("/default_topic"),
    val defaultSubscribeTopic: RosId = RosId("/default_subscribe"),
    val userName: String = "",
    val enableLogging: Boolean = false,
    val reconnectOnFailure: Boolean = false,
    val theme: String = "system", // "light", "dark", "system"
    val connectionTimeoutMs: Int = 10000,
    val maxRetryCount: Int = 3,
    val lastUsedProtocolType: RosId = RosId("msg"), // "msg", "srv", "action"
    val messageHistorySize: Int = 25,
    val exportProfilePath: String? = null,
    val importProfilePath: String? = null,
    val joystickAddressingMode: RosId = RosId("DIRECT"), // e.g., "DIRECT", "INDIRECT"
    val customButtonPresets: Map<String, String> = emptyMap(), // e.g., yxba mappings
    val language: String = "en",
    val notificationsEnabled: Boolean = true
    // Add other global settings as needed
) {
    companion object
}

fun AppConfiguration.toMap(): Map<String, Any?> = mapOf(
    "rosServerUrl" to rosServerUrl.value,
    "defaultPublishTopic" to defaultPublishTopic.value,
    "defaultSubscribeTopic" to defaultSubscribeTopic.value,
    "userName" to userName,
    "enableLogging" to enableLogging,
    "reconnectOnFailure" to reconnectOnFailure,
    "theme" to theme,
    "connectionTimeoutMs" to connectionTimeoutMs,
    "maxRetryCount" to maxRetryCount,
    "lastUsedProtocolType" to lastUsedProtocolType.value,
    "messageHistorySize" to messageHistorySize,
    "exportProfilePath" to exportProfilePath,
    "importProfilePath" to importProfilePath,
    "joystickAddressingMode" to joystickAddressingMode.value,
    "customButtonPresets" to customButtonPresets,
    "language" to language,
    "notificationsEnabled" to notificationsEnabled
)

fun AppConfiguration.toYaml(): String {
    val yaml = Yaml()
    return yaml.dump(this.toMap())
}

@Suppress("UNCHECKED_CAST")
fun AppConfiguration.Companion.fromMap(map: Map<String, Any?>): AppConfiguration {
    return AppConfiguration(
        rosServerUrl = RosId(map["rosServerUrl"] as? String ?: ""),
        defaultPublishTopic = RosId(map["defaultPublishTopic"] as? String ?: ""),
        defaultSubscribeTopic = RosId(map["defaultSubscribeTopic"] as? String ?: ""),
        userName = map["userName"] as? String ?: "",
        enableLogging = map["enableLogging"] as? Boolean ?: false,
        reconnectOnFailure = map["reconnectOnFailure"] as? Boolean ?: false,
        theme = map["theme"] as? String ?: "system",
        connectionTimeoutMs = (map["connectionTimeoutMs"] as? Int) ?: 10000,
        maxRetryCount = (map["maxRetryCount"] as? Int) ?: 3,
        lastUsedProtocolType = RosId(map["lastUsedProtocolType"] as? String ?: "msg"),
        messageHistorySize = (map["messageHistorySize"] as? Int) ?: 25,
        exportProfilePath = map["exportProfilePath"] as? String,
        importProfilePath = map["importProfilePath"] as? String,
        joystickAddressingMode = RosId(map["joystickAddressingMode"] as? String ?: "DIRECT"),
        customButtonPresets = map["customButtonPresets"] as? Map<String, String> ?: emptyMap(),
        language = map["language"] as? String ?: "en",
        notificationsEnabled = map["notificationsEnabled"] as? Boolean ?: true
    )
}

fun AppConfiguration.Companion.fromYaml(yamlString: String): AppConfiguration {
    val yaml = Yaml()
    val map = yaml.load<Map<String, Any?>>(yamlString)
    return fromMap(map)
}