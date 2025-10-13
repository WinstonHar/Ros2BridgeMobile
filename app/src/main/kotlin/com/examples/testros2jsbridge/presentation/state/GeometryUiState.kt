package com.examples.testros2jsbridge.presentation.state

import com.examples.testros2jsbridge.domain.model.RosMessage

/**
 * UI state for geometry message management and visualization.
 */
data class GeometryUiState(
    val messages: List<RosMessage> = emptyList(),          // List of received geometry messages
    val selectedMessage: RosMessage? = null,               // Currently selected message for viewing details
    val isLoading: Boolean = false,                        // Show loading indicator
    val showErrorDialog: Boolean = false,                  // Show error dialog
    val errorMessage: String? = null,                      // Error message to display
    val showMessageDetails: Boolean = false,               // Show/hide message details dialog
    val filterTopic: String? = null,                       // Filter messages by topic
    val sortBy: MessageSortOrder = MessageSortOrder.TIMESTAMP_DESC  // Sort order for messages
)

enum class MessageSortOrder {
    TIMESTAMP_ASC,
    TIMESTAMP_DESC,
    TOPIC_ASC,
    TOPIC_DESC,
    TYPE_ASC,
    TYPE_DESC
}
