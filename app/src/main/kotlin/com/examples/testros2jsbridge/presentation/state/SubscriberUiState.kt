package com.examples.testros2jsbridge.presentation.state

import com.examples.testros2jsbridge.domain.model.Subscriber

/**
 * UI state for subscriber management and topic subscription.
 */
data class SubscriberUiState(
    val subscribers: List<Subscriber> = emptyList(),      // List of active subscribers
    val selectedSubscriber: Subscriber? = null,           // Currently selected subscriber for editing/viewing
    val topicInput: String = "",                         // Topic input field
    val typeInput: String = "",                          // Message type input field
    val labelInput: String = "",                         // Label input field
    val isSubscribing: Boolean = false,                   // Show loading indicator when subscribing
    val showErrorDialog: Boolean = false,                 // Show error dialog
    val errorMessage: String? = null,                     // Error message to display
    val showAddSubscriberDialog: Boolean = false,         // Show/hide add subscriber dialog
    val showSubscriberHistory: Boolean = false            // Show/hide message history for selected subscriber
)