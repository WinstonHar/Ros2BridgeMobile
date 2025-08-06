@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.examples.testros2jsbridge.data.remote.rosbridge.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * DTO for rosbridge service calls (request/response).
 * Used for sending service call requests via rosbridge.
 */
@Serializable
data class RosBridgeServiceCall(
    val op: String,                // "call_service" or "service_response"
    val id: String? = null,        // Optional message ID for tracking
    val service: String,           // Service name, e.g. "/move_base/_action/send_goal"
    val type: String? = null,      // Service type, e.g. "move_base_msgs/action/MoveBase_SendGoal"
    val args: JsonObject? = null   // Arguments for the service call (payload)
)
