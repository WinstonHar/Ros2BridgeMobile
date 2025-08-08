@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.examples.testros2jsbridge.data.remote.rosbridge.dto

import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.RosMessage
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for ROS messages sent/received via rosbridge.
 * Used for network serialization instead of domain representation.
 */
@Serializable
data class RosMessageDto(
    val op: String,           // Operation type, e.g. "publish", "subscribe"
    @Contextual val topic: RosId,        // Topic name
    val type: String? = null, // Message type (optional for some ops)
    val msg: Map<String, String>? = null, // Message payload as a map (for generic JSON)
    val id: String? = null,   // Optional message ID for tracking
    val latch: Boolean? = null,
    val queue_size: Int? = null,
    val label: String? = null, // For saved/reusable messages
    val timestamp: Long? = null, // For message history
    val sender: String? = null,
    val isPublished: Boolean? = null, // True if sent, false if received
    val content: String? = null // JSON string for message content
)

// Extension function to convert from domain RosMessage to RosMessageDto
fun RosMessage.toDto(): RosMessageDto = RosMessageDto(
    op = this.op,
    topic = this.topic,
    type = this.type,
    msg = null, // Not used for geometry messages, use content field
    id = this.id,
    latch = this.latch,
    queue_size = this.queue_size,
    label = this.label,
    timestamp = this.timestamp,
    sender = this.sender,
    isPublished = this.isPublished,
    content = this.content
)