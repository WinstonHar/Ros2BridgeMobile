package com.examples.testros2jsbridge.presentation.state

/**
 * UI state for custom protocol management (.msg, .srv, .action files).
 */
data class ProtocolUiState(
    val availableMessages: List<ProtocolFile> = emptyList(),   // .msg files
    val availableServices: List<ProtocolFile> = emptyList(),   // .srv files
    val availableActions: List<ProtocolFile> = emptyList(),    // .action files
    val selectedProtocols: Set<String> = emptySet(),           // Selected import paths
    val isImporting: Boolean = false,                          // Show loading indicator during import
    val showErrorDialog: Boolean = false,                      // Show error dialog
    val errorMessage: String? = null                           // Error message to display
) {
    data class ProtocolFile(
        val name: String,
        val importPath: String,
        val type: ProtocolType
    )
    enum class ProtocolType { MSG, SRV, ACTION }
}