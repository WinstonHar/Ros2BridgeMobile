@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.examples.testros2jsbridge.data.remote.rosbridge.dto

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for ROS topics used in rosbridge communication.
 * Provides type-safe contracts for topic metadata and subscription.
 */
@Serializable
data class RosTopicDto(
    val name: String,           // Topic name, e.g. "/cmd_vel"
    val type: String,           // Message type, e.g. "geometry_msgs/Twist"
    val description: String? = null, // Optional description
    val isLatched: Boolean? = null,  // Whether topic is latched
    val queueSize: Int? = null,      // Optional queue size for subscription
    val isSystem: Boolean = false    // True if system topic, false if user-defined
)