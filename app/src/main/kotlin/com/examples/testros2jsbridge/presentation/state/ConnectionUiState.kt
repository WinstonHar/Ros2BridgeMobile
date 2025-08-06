package com.examples.testros2jsbridge.presentation.state

import com.examples.testros2jsbridge.domain.model.RosConnection

data class ConnectionUiState(
    val connection: RosConnection = RosConnection("", 9090),
    val isConnecting: Boolean = false,
    val isDisconnecting: Boolean = false,
    val showErrorDialog: Boolean = false,
    val errorMessage: String? = null,
    val ipInput: String = "",
    val portInput: String = "",
    val protocolInput: String = "ws",
    val isConnectButtonEnabled: Boolean = true,
    val isDisconnectButtonEnabled: Boolean = false,
    val showAuthDialog: Boolean = false,
    val authTokenInput: String = "",
    val serverNameInput: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val isConnected: Boolean = false,
    val connectionStatus: String = ""
)