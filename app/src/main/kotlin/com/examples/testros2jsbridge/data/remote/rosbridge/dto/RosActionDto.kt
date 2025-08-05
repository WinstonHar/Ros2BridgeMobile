package com.examples.testros2jsbridge.data.remote.rosbridge.dto

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for ROS actions (if your app supports ROS actionlib).
 * Used for sending action goals, feedback, and results via rosbridge.
 */
@Serializable
data class RosActionDto(
    val action: String,         // Action name, e.g. "/move_base"
    val type: String,           // Action type, e.g. "move_base_msgs/MoveBaseAction"
    val goal: Map<String, Any>? = null,    // Goal payload
    val feedback: Map<String, Any>? = null,// Feedback payload
    val result: Map<String, Any>? = null,  // Result payload
    val id: String? = null,     // Optional message ID for tracking
    val status: String? = null, // Optional status string
    val goalId: String? = null,     // Goal ID for tracking action goals
    val resultCode: Int? = null,    // Result code for action result
    val resultText: String? = null  // Optional result description
)
