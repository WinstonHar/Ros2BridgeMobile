package com.examples.testros2jsbridge.presentation.state

import com.examples.testros2jsbridge.domain.model.Publisher

/**
 * UI state for custom publisher management and message publishing.
 */
data class PublisherUiState(
    val publishers: List<Publisher> = emptyList(),         // List of saved custom publishers/buttons
    val selectedPublisher: Publisher? = null,              // Currently selected publisher for editing/sending
    val topicInput: String = "",                           // Topic input field
    val typeInput: String = "",                            // Message type input field
    val messageContentInput: String = "",                  // Message content (JSON) input field
    val isSaving: Boolean = false,                         // Show loading indicator when saving
    val showErrorDialog: Boolean = false,                  // Show error dialog
    val errorMessage: String? = null,                      // Error message to display
    val showAddPublisherDialog: Boolean = false,           // Show/hide add publisher dialog
    val showEditPublisherDialog: Boolean = false           // Show/hide edit publisher dialog
)