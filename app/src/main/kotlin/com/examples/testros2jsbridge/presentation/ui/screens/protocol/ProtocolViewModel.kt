package com.examples.testros2jsbridge.presentation.ui.screens.protocol

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.domain.model.CustomProtocol
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.repository.ProtocolRepository
import com.examples.testros2jsbridge.presentation.state.ProtocolUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProtocolViewModel @Inject constructor(
    private val protocolRepository: ProtocolRepository
) : ViewModel() {
    // State for available protocols
    private val _availableMsgProtocols = MutableStateFlow<List<CustomProtocol>>(emptyList())
    val availableMsgProtocols: StateFlow<List<CustomProtocol>> = _availableMsgProtocols.asStateFlow()

    private val _availableSrvProtocols = MutableStateFlow<List<CustomProtocol>>(emptyList())
    val availableSrvProtocols: StateFlow<List<CustomProtocol>> = _availableSrvProtocols.asStateFlow()

    private val _availableActionProtocols = MutableStateFlow<List<CustomProtocol>>(emptyList())
    val availableActionProtocols: StateFlow<List<CustomProtocol>> = _availableActionProtocols.asStateFlow()

    // State for selected protocol import paths
    private val _selectedProtocols = MutableStateFlow<Set<String>>(emptySet())
    val selectedProtocols: StateFlow<Set<String>> = _selectedProtocols.asStateFlow()

    // State for custom app actions
    private val _customAppActions = MutableStateFlow<List<AppAction>>(emptyList())
    val customAppActions: StateFlow<List<AppAction>> = _customAppActions.asStateFlow()

    // Load all available protocols from assets
    fun loadAvailableProtocols(context: Context) {
        viewModelScope.launch {
            _availableMsgProtocols.value = protocolRepository.getMessageFiles(context)
            _availableSrvProtocols.value = protocolRepository.getServiceFiles(context)
            _availableActionProtocols.value = protocolRepository.getActionFiles(context)
        }
    }

    // Select or deselect a protocol by importPath
    fun toggleProtocolSelection(importPath: String) {
        _selectedProtocols.value = _selectedProtocols.value.toMutableSet().apply {
            if (contains(importPath)) remove(importPath) else add(importPath)
        }
    }

    // Import selected protocols as CustomProtocol objects
    fun importSelectedProtocols(context: Context, onResult: (List<CustomProtocol>) -> Unit = {}) {
        viewModelScope.launch {
            val imported = protocolRepository.importProtocols(context, _selectedProtocols.value)
            onResult(imported)
        }
    }

    // Load all custom app actions
    fun loadCustomAppActions(context: Context) {
        viewModelScope.launch {
            _customAppActions.value = protocolRepository.getCustomAppActions(context)
        }
    }

    // Save a new or edited custom app action
    fun saveCustomAppAction(context: Context, action: AppAction) {
        viewModelScope.launch {
            protocolRepository.saveCustomAppAction(action, context)
            loadCustomAppActions(context)
        }
    }

    // Delete a custom app action by id
    fun deleteCustomAppAction(context: Context, actionId: String) {
        viewModelScope.launch {
            protocolRepository.deleteCustomAppAction(actionId, context)
            loadCustomAppActions(context)
        }
    }

    // Utility: clear selected protocols
    fun clearSelectedProtocols() {
        _selectedProtocols.value = emptySet()
    }
    // --- UI State for Compose screens (single source of truth) ---
    private val _uiState = MutableStateFlow(ProtocolUiState())
    val uiState: StateFlow<ProtocolUiState> = _uiState

    // Load all available protocols and update UI state
    fun loadProtocols(context: Context) {
        viewModelScope.launch {
            val messages = protocolRepository.getMessageFiles(context)
            val services = protocolRepository.getServiceFiles(context)
            val actions = protocolRepository.getActionFiles(context)
            _uiState.value = _uiState.value.copy(
                availableMessages = messages.map { ProtocolUiState.ProtocolFile(it.name, it.importPath, ProtocolUiState.ProtocolType.valueOf(it.type.name)) },
                availableServices = services.map { ProtocolUiState.ProtocolFile(it.name, it.importPath, ProtocolUiState.ProtocolType.valueOf(it.type.name)) },
                availableActions = actions.map { ProtocolUiState.ProtocolFile(it.name, it.importPath, ProtocolUiState.ProtocolType.valueOf(it.type.name)) }
            )
        }
    }

    // Import selected protocols and update UI state
    fun importProtocols(context: Context, selected: Set<String>) {
        _uiState.value = _uiState.value.copy(isImporting = true)
        viewModelScope.launch {
            try {
                protocolRepository.importProtocols(context, selected)
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