package com.examples.testros2jsbridge.domain.model

/*
Topic business rules and validation
 */

data class RosTopic(
    val name: String,                // The topic name, e.g., "/cmd_vel"
    val type: String,                // The ROS message type, e.g., "geometry_msgs/msg/Twist"
    val isSubscribed: Boolean = false, // True if currently subscribed
    val isPublished: Boolean = false,  // True if currently published to
    val lastMessage: String? = null,   // Last message received/published (optional, for UI/history)
    val description: String? = null    // Optional: for UI display or documentation
)