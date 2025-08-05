package com.examples.testros2jsbridge.domain.model

/*
Business representation instead vs network dtos
 */

data class RosConnection(
    val ipAddress: String,                // e.g., "192.168.1.100"
    val port: Int,                        // e.g., 9090
    val protocol: String = "ws",          // "ws" or "wss"
    val isConnected: Boolean = false,     // Current connection state
    val connectionId: String? = null,     // Unique ID if supporting multiple connections
    val lastError: String? = null,        // Last error message, if any
    val authToken: String? = null,        // For future authentication support
    val lastReconnectAttempt: Long? = null, // Timestamp of last reconnect attempt
    val messageCount: Int = 0,            // Number of messages sent/received
    val serverName: String? = null,       // Optional: user-friendly name for the server
    val timestamp: Long = System.currentTimeMillis() // When connection was established/updated
)