@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.examples.testros2jsbridge.data.remote.rosbridge.dto

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for ROS service calls and responses via rosbridge.
 * Used for network serialization instead of domain representation.
 */
@Serializable
data class RosServiceDto(
    val op: String,                // "call_service" or "service_response"
    val service: String,           // Service name, e.g. "/add_two_ints"
    val args: Map<String, @Polymorphic ActionFieldValue>? = null, // Arguments for the service call
    val id: String? = null,        // Optional message ID for tracking
    val result: Boolean? = null,   // True if service call succeeded (response)
    val values: Map<String, @Polymorphic ActionFieldValue>? = null, // Returned values from service (response)
    val error: String? = null,     // Error message if service call failed
    val isSystem: Boolean = false, // True if system service, false if user-defined
    val dataType: String? = null,         // Overall service data type
    val requestDataType: String? = null,  // Request data type
    val responseDataType: String? = null  // Response data type
)