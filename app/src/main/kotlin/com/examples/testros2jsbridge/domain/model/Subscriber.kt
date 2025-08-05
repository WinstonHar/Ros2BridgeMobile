package com.examples.testros2jsbridge.domain.model

/*
Subscription management
*/

data class Subscriber(
    val id: String? = null, // Unique ID for editing/deleting/history
    val topic: RosId, // Topic name, e.g., "/cmd_vel"
    val type: String, // Message type, e.g., "geometry_msgs/msg/Twist"
    val isActive: Boolean = true, // True if currently subscribed
    val lastMessage: String? = null, // Last message received (for UI/history)
    val label: String? = null, // For UI display
    val isEnabled: Boolean = true, // For UI toggling
    val group: String? = null, // For grouping subscriptions (optional)
    val timestamp: Long = System.currentTimeMillis() // Last subscription update
)