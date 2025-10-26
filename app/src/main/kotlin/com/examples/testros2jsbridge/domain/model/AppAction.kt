package com.examples.testros2jsbridge.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppAction(
    val id: String, // Unique identifier for persistence
    val displayName: String,
    val topic: String,
    val type: String, // This will now be consistently used for the full ROS message type
    val source: String,
    val msg: String = "",
    val rosMessageType: String // To store the simple type, e.g., "MSG", "SRV"
)
