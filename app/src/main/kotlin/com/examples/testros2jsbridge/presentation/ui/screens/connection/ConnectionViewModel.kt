package com.examples.testros2jsbridge.presentation.ui.screens.connection


import com.examples.testros2jsbridge.presentation.state.ConnectionUiState
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
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
    application: Application,
    private val connectionManager: ConnectionManager
) : AndroidViewModel(application), ConnectionManager.Listener {
    private val prefs: SharedPreferences = application.getSharedPreferences("ros2bridge_prefs", Application.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState


    init {
        // Load saved IP and port
        val savedIp = prefs.getString("ipInput", "") ?: ""
        val savedPort = prefs.getString("portInput", "") ?: ""
        _uiState.update { it.copy(ipInput = savedIp, portInput = savedPort) }
    }

    fun onIpAddressChange(ip: String) {
        _uiState.update { it.copy(ipInput = ip) }
        prefs.edit().putString("ipInput", ip).apply()
    }

    fun onPortChange(port: String) {
        _uiState.update { it.copy(portInput = port) }
        prefs.edit().putString("portInput", port).apply()
    }

    fun clearIpPort() {
        _uiState.update { it.copy(ipInput = "", portInput = "") }
        prefs.edit().remove("ipInput").remove("portInput").apply()
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
        connectionManager.disconnect("")
    }

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
        // no-op
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