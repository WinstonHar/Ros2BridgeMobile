package com.examples.testros2jsbridge.presentation.ui.screens.connection


import com.examples.testros2jsbridge.presentation.state.ConnectionUiState
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.examples.testros2jsbridge.core.network.ConnectionManager


@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionManager: ConnectionManager
) : ViewModel(), ConnectionManager.Listener {
    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState

    fun onIpAddressChange(ip: String) {
        _uiState.update { it.copy(ipInput = ip) }
    }

    fun onPortChange(port: String) {
        _uiState.update { it.copy(portInput = port) }
    }

    fun connect(ip: String, port: String) {
        if (ip.isBlank() || port.isBlank()) {
            _uiState.update { it.copy(showErrorDialog = true, errorMessage = "IP and Port must not be empty") }
            return
        }
        _uiState.update { it.copy(isConnecting = true, connectionStatus = "Connecting...") }
        connectionManager.connect(ip, port.toIntOrNull() ?: 9090)
    }

    fun disconnect() {
        _uiState.update { it.copy(isDisconnecting = true, connectionStatus = "Disconnecting...") }
        // Use a dummy connectionId for now, as it's not used in ConnectionManager.disconnect
        connectionManager.disconnect("")
    }

    // ConnectionManager.Listener implementation
    override fun onConnected() {
        _uiState.update {
            it.copy(
                isConnected = true,
                connectionStatus = "Connected",
                isConnecting = false,
                isConnectButtonEnabled = false,
                isDisconnectButtonEnabled = true,
                showErrorDialog = false,
                errorMessage = null
            )
        }
    }

    override fun onDisconnected() {
        _uiState.update {
            it.copy(
                isConnected = false,
                connectionStatus = "Disconnected",
                isDisconnecting = false,
                isConnectButtonEnabled = true,
                isDisconnectButtonEnabled = false
            )
        }
    }

    override fun onMessage(text: String) {
        // Optionally handle messages if needed
    }

    override fun onError(error: String) {
        _uiState.update { it.copy(showErrorDialog = true, errorMessage = error, isConnecting = false, isDisconnecting = false) }
    }

    init {
        connectionManager.addListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        connectionManager.removeListener(this)
    }
}