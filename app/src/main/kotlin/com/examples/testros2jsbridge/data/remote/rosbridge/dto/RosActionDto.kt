package com.examples.testros2jsbridge.data.remote.rosbridge.dto

/**
 * Data Transfer Object for ROS actions
 * Used for network communication with rosbridge
 */
data class RosActionDto(
    val name: String,
    val type: String,
    val goal: Map<String, ActionFieldValue>? = null,
    val result: Map<String, ActionFieldValue>? = null,
    val feedback: Map<String, ActionFieldValue>? = null
)

/**
 * Represents a field value in an action goal/result/feedback
 */
sealed class ActionFieldValue {
    data class StringValue(val value: String) : ActionFieldValue()
    data class IntValue(val value: Int) : ActionFieldValue()
    data class DoubleValue(val value: Double) : ActionFieldValue()
    data class BoolValue(val value: Boolean) : ActionFieldValue()
    data class ObjectValue(val value: Map<String, ActionFieldValue>) : ActionFieldValue()
    data class ArrayValue(val value: List<ActionFieldValue>) : ActionFieldValue()
}
