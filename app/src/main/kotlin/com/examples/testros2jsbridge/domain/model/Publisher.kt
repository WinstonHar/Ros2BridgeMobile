package com.examples.testros2jsbridge.domain.model

/*
Publisher business logic
*/

data class Publisher(
    val id: RosId? = null, // Unique ID for editing/deleting/history
    val topic: RosId,
    val messageType: String,
    val msgType: String,
    val message: String,
    val label: String? = null, // For UI display
    val isEnabled: Boolean = true, // For UI toggling
    val lastPublishedTimestamp: Long? = null, // For history/feedback
    val presetGroup: String? = null // For grouping publishers (optional)
)