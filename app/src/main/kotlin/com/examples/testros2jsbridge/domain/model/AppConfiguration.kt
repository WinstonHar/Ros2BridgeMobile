package com.examples.testros2jsbridge.domain.model

/*
App-wide settings management
 */

data class AppConfiguration(
    val rosServerUrl: RosId,
    val defaultPublishTopic: RosId,
    val defaultSubscribeTopic: RosId,
    val userName: String,
    val enableLogging: Boolean,
    val reconnectOnFailure: Boolean,
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
)