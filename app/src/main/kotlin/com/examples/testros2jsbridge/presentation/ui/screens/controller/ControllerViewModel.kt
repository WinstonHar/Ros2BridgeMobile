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
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.view.InputDevice
import kotlinx.coroutines.flow.asStateFlow
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.domain.model.ControllerConfig

@HiltViewModel
class ControllerViewModel @Inject constructor(
    private val handleControllerInputUseCase: HandleControllerInputUseCase,
    private val loadControllerConfigUseCase: LoadControllerConfigUseCase,
    private val saveControllerConfigUseCase: SaveControllerConfigUseCase,
    val controllerRepository: com.examples.testros2jsbridge.domain.repository.ControllerRepository,
    private val rosMessageRepository: RosMessageRepository
) : ViewModel() {

    private val _detectedControllerButtons = MutableStateFlow<List<String>>(emptyList())
    val detectedControllerButtons: StateFlow<List<String>> = _detectedControllerButtons.asStateFlow()

    fun refreshControllerButtons() {
        val buttonNames = mutableSetOf<String>()
        val keyCodeToName = mapOf(
            96 to "A", 97 to "B", 99 to "X", 100 to "Y",
            102 to "L1", 103 to "R1", 104 to "L2", 105 to "R2",
            108 to "Start", 82 to "Select"
        )
        for (deviceId in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(deviceId) ?: continue
            val sources = device.sources
            if ((sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
                (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)) {
                for ((keyCode, name) in keyCodeToName) {
                    if (device.hasKeys(keyCode)[0]) {
                        buttonNames.add(name)
                    }
                }
            }
        }
        _detectedControllerButtons.value = buttonNames.toList().sorted()
    }

    fun triggerAppAction(action: AppAction) {
        handleControllerInputUseCase.triggerAppAction(action)
    }

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
        refreshControllerButtons()

        // Collect all saved geometry messages and expose as AppActions
        viewModelScope.launch {
            Logger.d("ControllerViewModel", "Collecting messages from rosMessageRepository")
            rosMessageRepository.messages.collect { messageList ->
                Logger.d("ControllerViewModel", "Messages collected: $messageList")
                val geometryActions = messageList.mapNotNull { it.toAppAction() }
                _appActions.value = geometryActions
                _uiState.value = _uiState.value.copy(appActions = geometryActions)
                Logger.d("ControllerViewModel", "AppActions updated: $geometryActions")
            }
        }

        viewModelScope.launch {
            _appActions.collect { actions ->
                _uiState.value = _uiState.value.copy(appActions = actions)
            }
        }
    }

    // Extension function to map RosMessageDto to AppAction
    private fun RosMessageDto.toAppAction(): com.examples.testros2jsbridge.domain.model.AppAction? {
        // Use label or topic as displayName, and content as msg
        return com.examples.testros2jsbridge.domain.model.AppAction(
            id = this.id ?: (this.label ?: this.topic.value),
            displayName = this.label ?: this.topic.value,
            topic = this.topic.value,
            type = this.type ?: "",
            source = "geometry",
            msg = this.content ?: ""
        )
    }

    fun selectPreset(presetName: String) {
        val preset = _presets.value.find { it.name == presetName }
        _selectedPreset.value = preset
    }

    fun addPreset(name: String) {
        val newPreset = ControllerPreset(
            name = name,
            topic = null,
            buttonAssignments = mapOf(
                "A" to AppAction(
                    id = "A",
                    displayName = "",
                    topic = "",
                    type = "",
                    source = "",
                    msg = ""
                ),
                "B" to AppAction(
                    id = "B",
                    displayName = "",
                    topic = "",
                    type = "",
                    source = "",
                    msg = ""
                ),
                "X" to AppAction(
                    id = "X",
                    displayName = "",
                    topic = "",
                    type = "",
                    source = "",
                    msg = ""
                ),
                "Y" to AppAction(
                    id = "Y",
                    displayName = "",
                    topic = "",
                    type = "",
                    source = "",
                    msg = ""
                )
            )
        )
        val updatedPresets = _presets.value + newPreset
        _presets.value = updatedPresets
        saveConfigWithPresets(updatedPresets)
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
        val updated = _buttonAssignments.value.toMutableMap()
        if (action != null) {
            updated[button] = action
        } else {
            updated.remove(button)
        }
        _buttonAssignments.value = updated
        saveConfigWithAssignments(updated)
    }

    fun assignAbxyButton(btn: String, actionName: String) {
        // Removed: abxy logic is obsolete. Use buttonAssignments instead.
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

    fun updateJoystickMappings(newMappings: List<com.examples.testros2jsbridge.domain.model.JoystickMapping>) {
        val uiState = _uiState.value.copy(config = _uiState.value.config.copy(joystickMappings = newMappings))
        val config = ControllerUiMapper.toDomainConfig(uiState)
        viewModelScope.launch {
            saveControllerConfigUseCase.save(config)
            _uiState.value = ControllerUiMapper.toUiState(config)
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
        val prevAppActions = _uiState.value.appActions
        val uiState = _uiState.value.copy(buttonAssignments = assignments)
        val config = ControllerUiMapper.toDomainConfig(uiState)
        viewModelScope.launch {
            saveControllerConfigUseCase.save(config)
            val newUiState = ControllerUiMapper.toUiState(config).copy(appActions = prevAppActions)
            _uiState.value = newUiState
        }
    }

    fun selectControllerConfig(name: String) {
        val config = _uiState.value.controllerConfigs.find { it.name == name }
        _uiState.value = _uiState.value.copy(selectedPreset = config?.toPreset())
    }

    private fun ControllerConfig.toPreset(): ControllerPreset {
        return ControllerPreset(
            name = this.name,
            buttonAssignments = this.buttonAssignments,
            joystickMappings = this.joystickMappings
        )
    }

    fun addControllerConfig(name: String) {
        val newConfig = ControllerConfig(name = name)
        val updatedConfigs = _uiState.value.controllerConfigs + newConfig
        _uiState.value = _uiState.value.copy(controllerConfigs = updatedConfigs)
    }

    fun removeControllerConfig(name: String) {
        val updatedConfigs = _uiState.value.controllerConfigs.filter { it.name != name }
        _uiState.value = _uiState.value.copy(controllerConfigs = updatedConfigs)
    }
}