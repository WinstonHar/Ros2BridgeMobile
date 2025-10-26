package com.examples.testros2jsbridge.presentation.state

data class ProtocolUiState(
    val packageNames: List<String> = emptyList(),
    val availableMessages: List<ProtocolFile> = emptyList(),
    val availableServices: List<ProtocolFile> = emptyList(),
    val availableActions: List<ProtocolFile> = emptyList(),
    val selectedProtocols: Set<String> = emptySet(),
    val isImporting: Boolean = false,
    val showErrorDialog: Boolean = false,
    val errorMessage: String? = null,
    val actionSaved: Boolean = false
) {
    data class ProtocolFile(
        val name: String,
        val importPath: String,
        val type: ProtocolType
    )

    enum class ProtocolType { MSG, SRV, ACTION }
}