package com.examples.testros2jsbridge.data.remote.rosbridge.dto

import com.examples.testros2jsbridge.domain.model.RosId

/**
 * Data Transfer Object for ROS messages
 * Used for network communication with rosbridge
 */
data class RosMessageDto(
    val id: String? = null,
    val topic: RosId,
    val type: String? = null, // Generic type
    val msgType: String? = null, // ROS message type
    val content: String? = null, // JSON string of the message content
    val label: String? = null,
    val op: String = "publish",
    val latch: Boolean? = null,
    val queue_size: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)
