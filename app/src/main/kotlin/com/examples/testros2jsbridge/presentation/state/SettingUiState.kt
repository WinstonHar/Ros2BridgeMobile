package com.examples.testros2jsbridge.presentation.state

/**
 * UI state for app settings management.
 */
data class SettingUiState(
    val theme: String = "system",                // "light", "dark", "system"
    val language: String = "en",                 // Language code
    val notificationsEnabled: Boolean = true,     // App notifications toggle
    val autoConnectEnabled: Boolean = false,      // Auto-connect to last ROS server
    val lastConnectedIp: String = "",            // Last used IP address
    val lastConnectedPort: String = "",          // Last used port
    val isSaving: Boolean = false,                // Show loading indicator when saving
    val showErrorDialog: Boolean = false,         // Show error dialog
    val errorMessage: String? = null              // Error message to display
)