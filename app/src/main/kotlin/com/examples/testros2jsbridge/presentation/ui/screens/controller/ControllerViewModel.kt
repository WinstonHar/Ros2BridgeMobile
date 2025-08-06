package com.examples.testros2jsbridge.presentation.ui.screens.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.ControllerPreset
import com.examples.testros2jsbridge.presentation.state.ControllerUiState
import com.examples.testros2jsbridge.presentation.mapper.ControllerUiMapper
import com.examples.testros2jsbridge.domain.usecase.controller.HandleControllerInputUseCase
import com.examples.testros2jsbridge.domain.usecase.controller.LoadControllerConfigUseCase
import com.examples.testros2jsbridge.domain.usecase.controller.SaveControllerConfigUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ControllerViewModel @Inject constructor(
    private val handleControllerInputUseCase: HandleControllerInputUseCase,
    private val loadControllerConfigUseCase: LoadControllerConfigUseCase,
    private val saveControllerConfigUseCase: SaveControllerConfigUseCase,
    val controllerRepository: com.examples.testros2jsbridge.domain.repository.ControllerRepository
) : ViewModel() {
    var showPresetsOverlay: (() -> Unit)? = null
    private val _appActions = MutableStateFlow<List<com.examples.testros2jsbridge.domain.model.AppAction>>(emptyList())
    val appActions: StateFlow<List<com.examples.testros2jsbridge.domain.model.AppAction>> = _appActions
    private val _selectedPreset = MutableStateFlow<ControllerPreset?>(null)
    val selectedPreset: StateFlow<ControllerPreset?> = _selectedPreset
    private val _presets = MutableStateFlow<List<ControllerPreset>>(emptyList())
    val presets: StateFlow<List<ControllerPreset>> = _presets
    private val _buttonAssignments = MutableStateFlow<Map<String, com.examples.testros2jsbridge.domain.model.AppAction>>(emptyMap())
    val buttonAssignments: StateFlow<Map<String, com.examples.testros2jsbridge.domain.model.AppAction>> = _buttonAssignments
    private val _uiState = MutableStateFlow(ControllerUiState())
    val uiState: StateFlow<ControllerUiState> = _uiState

    init {
        viewModelScope.launch {
            val config = loadControllerConfigUseCase.load()
            _uiState.value = ControllerUiMapper.toUiState(config)
        }
    }

    fun selectPreset(presetName: String) {
        val preset = _presets.value.find { it.name == presetName }
        _selectedPreset.value = preset
    }

    fun addPreset() {
        val newPreset = ControllerPreset(
            name = "New Preset",
            topic = null,
            abxy = mapOf("A" to "", "B" to "", "X" to "", "Y" to "")
        )
        val updated = _presets.value + newPreset
        _presets.value = updated
        saveConfigWithPresets(updated)
    }

    fun removePreset() {
        val selected = _selectedPreset.value ?: return
        val updated = _presets.value.filter { it.name != selected.name }
        _presets.value = updated
        saveConfigWithPresets(updated)
        _selectedPreset.value = null
    }

    fun savePreset(name: String) {
        val selected = _selectedPreset.value ?: return
        val updatedPreset = selected.copy(name = name)
        val updated = _presets.value.map { if (it.name == selected.name) updatedPreset else it }
        _presets.value = updated
        saveConfigWithPresets(updated)
        _selectedPreset.value = updatedPreset
    }

    fun assignButton(button: String, action: com.examples.testros2jsbridge.domain.model.AppAction?) {
        if (action == null) return
        val updated = _buttonAssignments.value.toMutableMap().apply { put(button, action) }
        _buttonAssignments.value = updated
        saveConfigWithAssignments(updated)
    }

    fun assignAbxyButton(btn: String, actionName: String) {
        val selected = _selectedPreset.value ?: return
        val updatedAbxy = selected.abxy.toMutableMap().apply { put(btn, actionName) }
        val updatedPreset = selected.copy(abxy = updatedAbxy)
        val updated = _presets.value.map { if (it.name == selected.name) updatedPreset else it }
        _presets.value = updated
        saveConfigWithPresets(updated)
        _selectedPreset.value = updatedPreset
    }

    fun handleKeyEvent(keyCode: Int): AppAction? {
        return handleControllerInputUseCase.handleKeyEvent(keyCode, _buttonAssignments.value)
    }

    fun exportConfig(outputStream: java.io.OutputStream) {
        // Export the full config using repository logic
        (controllerRepository as? com.examples.testros2jsbridge.data.repository.ControllerRepositoryImpl)?.exportConfigToStream(outputStream)
    }

    fun importConfig(inputStream: java.io.InputStream) {
        // Import config using repository logic
        (controllerRepository as? com.examples.testros2jsbridge.data.repository.ControllerRepositoryImpl)?.importConfigFromStream(inputStream)
        // After import, reload config
        viewModelScope.launch {
            val config = loadControllerConfigUseCase.load()
            // Update all state flows as needed
            _uiState.value = ControllerUiMapper.toUiState(config)
            _presets.value = config.controllerPresets
            _buttonAssignments.value = config.buttonAssignments
            _selectedPreset.value = config.controllerPresets.firstOrNull()
        }
    }

    private fun saveConfigWithPresets(presets: List<ControllerPreset>) {
        val uiState = _uiState.value.copy(presets = presets)
        val config = ControllerUiMapper.toDomainConfig(uiState)
        viewModelScope.launch {
            saveControllerConfigUseCase.save(config)
            _uiState.value = ControllerUiMapper.toUiState(config)
        }
    }

    private fun saveConfigWithAssignments(assignments: Map<String, AppAction>) {
        val uiState = _uiState.value.copy(buttonAssignments = assignments)
        val config = ControllerUiMapper.toDomainConfig(uiState)
        viewModelScope.launch {
            saveControllerConfigUseCase.save(config)
            _uiState.value = ControllerUiMapper.toUiState(config)
        }
    }
}