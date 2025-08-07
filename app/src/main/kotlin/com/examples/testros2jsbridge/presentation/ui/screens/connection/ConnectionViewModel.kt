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


@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionRepository: com.examples.testros2jsbridge.domain.repository.RosConnectionRepository,
    private val disconnectUseCase: com.examples.testros2jsbridge.domain.usecase.connection.DisconnectFromRosUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState

    fun onIpAddressChange(ip: String) {
        _uiState.update { it.copy(ipInput = ip) }
    }

    fun onPortChange(port: String) {
        _uiState.update { it.copy(portInput = port) }
    }

    fun connect(ip: String, port: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (ip.isBlank() || port.isBlank()) {
                _uiState.update { it.copy(showErrorDialog = true, errorMessage = "IP and Port must not be empty") }
                return@launch
            }
            val connection = com.examples.testros2jsbridge.domain.model.RosConnection(
                ipAddress = ip,
                port = port.toIntOrNull() ?: 9090,
                isConnected = true
            )
            connectionRepository.saveConnection(connection)
            _uiState.update {
                it.copy(
                    connection = connection,
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
    }

    fun disconnect() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentConnection = connectionRepository.connections.value.firstOrNull { it.isConnected }
            if (currentConnection?.connectionId != null) {
                val result = disconnectUseCase.disconnect(currentConnection.connectionId)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            connection = it.connection.copy(isConnected = false),
                            isConnected = false,
                            connectionStatus = "Disconnected",
                            isDisconnecting = false,
                            isConnectButtonEnabled = true,
                            isDisconnectButtonEnabled = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(showErrorDialog = true, errorMessage = result.exceptionOrNull()?.message ?: "Unknown error") }
                }
            } else {
                _uiState.update {
                    it.copy(
                        connection = it.connection.copy(isConnected = false),
                        isConnected = false,
                        connectionStatus = "Disconnected",
                        isDisconnecting = false,
                        isConnectButtonEnabled = true,
                        isDisconnectButtonEnabled = false
                    )
                }
            }
        }
    }
}