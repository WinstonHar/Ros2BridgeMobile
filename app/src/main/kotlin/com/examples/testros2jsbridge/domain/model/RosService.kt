package com.examples.testros2jsbridge.domain.model

/*
Service call abstraction
 */

data class RosService(
    val id: String? = null, // Unique ID for tracking/history
    val serviceName: String, // e.g., "/add_two_ints"
    val requestType: String, // e.g., "example_interfaces/srv/AddTwoInts"
    val responseType: String, // e.g., "example_interfaces/srv/AddTwoInts"
    val request: String, // JSON string or serialized request
    val response: String? = null, // JSON string or serialized response
    val status: String = "pending", // "pending", "success", "error"
    val errorMessage: String? = null,
    val caller: String? = null, // Optional: who initiated the call
    val timestamp: Long = System.currentTimeMillis()
)