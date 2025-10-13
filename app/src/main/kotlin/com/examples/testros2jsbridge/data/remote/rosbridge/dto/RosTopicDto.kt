package com.examples.testros2jsbridge.data.remote.rosbridge.dto

/**
 * Data Transfer Object for ROS topics
 * Used for network communication with rosbridge
 */
data class RosTopicDto(
    val name: String,
    val type: String
)
