package com.examples.testros2jsbridge.domain.model

/*
Domain message model vs json strings
 */

data class RosMessage(
    val id: String? = null, // For history/templates
    val topic: RosId,
    val type: String,
    val content: String, // JSON string
    val timestamp: Long = System.currentTimeMillis(),
    val label: String? = null, // For saved/reusable messages
    val sender: String? = null,
    val isPublished: Boolean = true, // True if sent, false if received
    val op: String,
    val latch: Boolean? = null,
    val queue_size: Int? = null
)