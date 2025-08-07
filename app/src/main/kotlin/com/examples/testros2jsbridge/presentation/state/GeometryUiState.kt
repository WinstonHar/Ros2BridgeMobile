package com.examples.testros2jsbridge.presentation.state

import com.examples.testros2jsbridge.domain.model.RosMessage

/**
 * UI state for geometry message management.
 */
data class GeometryUiState(
    val messages: List<RosMessage> = emptyList(),           // List of reusable geometry messages
    val selectedMessage: RosMessage? = null,                // Currently selected message for editing/sending
    val nameInput: String = "",                            // Name input field for message label
    val topicInput: String = "",                            // Topic input field
    val typeInput: String = "",                             // Message type input field
    val messageContentInput: String = "",                   // Message content (JSON) input field
    val isSaving: Boolean = false,                          // Show loading indicator when saving
    val showErrorDialog: Boolean = false,                   // Show error dialog
    val errorMessage: String? = null,                       // Error message to display
    val showSavedButtons: Boolean = true                    // Show/hide reusable message buttons
)