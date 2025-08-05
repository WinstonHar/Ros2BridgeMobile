package com.examples.testros2jsbridge.presentation.ui.screens.protocol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.presentation.state.ProtocolUiState
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.repository.ProtocolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProtocolViewModel(
    private val protocolRepository: ProtocolRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProtocolUiState())
    val uiState: StateFlow<ProtocolUiState> = _uiState

    fun loadProtocols() {
        viewModelScope.launch {
            val messages = protocolRepository.getMessageFiles()
            val services = protocolRepository.getServiceFiles()
            val actions = protocolRepository.getActionFiles()
            _uiState.value = _uiState.value.copy(
                availableMessages = messages,
                availableServices = services,
                availableActions = actions
            )
        }
    }

    fun importProtocols(selected: Set<String>) {
        _uiState.value = _uiState.value.copy(isImporting = true)
        viewModelScope.launch {
            try {
                protocolRepository.importProtocols(selected)
                _uiState.value = _uiState.value.copy(isImporting = false, selectedProtocols = selected)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isImporting = false, showErrorDialog = true, errorMessage = e.message)
            }
        }
    }

    fun dismissErrorDialog() {
        _uiState.value = _uiState.value.copy(showErrorDialog = false, errorMessage = null)
    }
}