package com.examples.testros2jsbridge.data.remote.rosbridge.dto

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for ROS messages sent/received via rosbridge.
 * Used for network serialization instead of domain representation.
 */
@Serializable
data class RosMessageDto(
    val op: String,           // Operation type, e.g. "publish", "subscribe"
    val topic: String,        // Topic name
    val type: String? = null, // Message type (optional for some ops)
    val msg: Map<String, Any>? = null, // Message payload as a map (for generic JSON)
    val id: String? = null,   // Optional message ID for tracking
    val latch: Boolean? = null,
    val queue_size: Int? = null
)