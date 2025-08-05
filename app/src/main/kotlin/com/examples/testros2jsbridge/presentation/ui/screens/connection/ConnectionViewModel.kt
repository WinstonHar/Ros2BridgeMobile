package com.examples.testros2jsbridge.presentation.ui.screens.connection

import com.examples.testros2jsbridge.presentation.state.ConnectionUiState

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ConnectionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState

    private val connectionRepository = com.examples.testros2jsbridge.data.repository.RosConnectionRepositoryImpl()
    private val disconnectUseCase = com.examples.testros2jsbridge.domain.usecase.connection.DisconnectFromRosUseCase(connectionRepository)

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
                            isDisconnecting = false,
                            isConnectButtonEnabled = true,
                            isDisconnectButtonEnabled = false,
                            connection = it.connection.copy(isConnected = false)
                        )
                    }
                } else {
                    _uiState.update { it.copy(showErrorDialog = true, errorMessage = result.exceptionOrNull()?.message ?: "Unknown error") }
                }
            } else {
                _uiState.update {
                    it.copy(
                        connection = it.connection.copy(isConnected = false),
                        isDisconnecting = false,
                        isConnectButtonEnabled = true,
                        isDisconnectButtonEnabled = false
                    )
                }
            }
        }
    }
}