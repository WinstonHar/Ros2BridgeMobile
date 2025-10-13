package com.examples.testros2jsbridge.data.remote.rosbridge.dto

/**
 * Data Transfer Object for ROS services
 * Used for network communication with rosbridge
 */
data class RosServiceDto(
    val name: String,
    val type: String,
    val request: Map<String, Any>? = null,
    val response: Map<String, Any>? = null
)
