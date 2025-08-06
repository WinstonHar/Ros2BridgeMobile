@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.examples.testros2jsbridge.data.remote.rosbridge.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.json.Json


/**
 * Polymorphic value for ROS action fields (goal, feedback, result).
 */
@Polymorphic
@Serializable
sealed interface ActionFieldValue {
    @Serializable
    @SerialName("IntValue")
    data class IntValue(val value: Int) : ActionFieldValue

    @Serializable
    @SerialName("DoubleValue")
    data class DoubleValue(val value: Double) : ActionFieldValue

    @Serializable
    @SerialName("StringValue")
    data class StringValue(val value: String) : ActionFieldValue

    @Serializable
    @SerialName("BoolValue")
    data class BoolValue(val value: Boolean) : ActionFieldValue

    @Serializable
    @SerialName("ObjectValue")
    data class ObjectValue(val value: Map<String, @Polymorphic ActionFieldValue>) : ActionFieldValue

    @Serializable
    @SerialName("ListValue")
    data class ListValue(val value: List<@Polymorphic ActionFieldValue>) : ActionFieldValue
}

@Serializable
data class RosActionDto(
    val action: String,         // Action name, e.g. "/move_base"
    val type: String,           // Action type, e.g. "move_base_msgs/MoveBaseAction"
    val goal: Map<String, @Polymorphic ActionFieldValue>? = null,    // Goal payload
    val feedback: Map<String, @Polymorphic ActionFieldValue>? = null,// Feedback payload
    val result: Map<String, @Polymorphic ActionFieldValue>? = null,  // Result payload
    val id: String? = null,     // Optional message ID for tracking
    val status: String? = null, // Optional status string
    val goalId: String? = null,     // Goal ID for tracking action goals
    val resultCode: Int? = null,    // Result code for action result
    val resultText: String? = null  // Optional result description
)
