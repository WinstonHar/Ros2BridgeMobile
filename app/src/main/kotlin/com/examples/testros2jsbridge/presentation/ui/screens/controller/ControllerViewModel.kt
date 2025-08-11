package com.examples.testros2jsbridge.presentation.ui.screens.controller

import android.content.Context
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
import com.examples.testros2jsbridge.domain.repository.ProtocolRepository
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.view.InputDevice
import kotlinx.coroutines.flow.asStateFlow
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.data.repository.ControllerRepositoryImpl
import com.examples.testros2jsbridge.data.repository.RosMessageRepositoryImpl
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.model.JoystickMapping
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import java.io.InputStream
import java.io.OutputStream

@HiltViewModel
class ControllerViewModel @Inject constructor(
    private val handleControllerInputUseCase: HandleControllerInputUseCase,
    private val loadControllerConfigUseCase: LoadControllerConfigUseCase,
    private val saveControllerConfigUseCase: SaveControllerConfigUseCase,
    val controllerRepository: ControllerRepository,
    private val rosMessageRepository: RosMessageRepository,
    private val protocolRepository: ProtocolRepository
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
        when (action.id) {
            "__cycle_preset_forward__" -> cyclePresetForward()
            "__cycle_preset_backward__" -> cyclePresetBackward()
            else -> handleControllerInputUseCase.triggerAppAction(action)
        }
    }

    var showPresetsOverlay: (() -> Unit)? = null
    private val _appActions = MutableStateFlow<List<AppAction>>(emptyList())
    val appActions: StateFlow<List<AppAction>> = _appActions

    private val cyclePresetForwardAction = AppAction(
        id = "__cycle_preset_forward__",
        displayName = "Cycle Preset Forward",
        topic = "Controller",
        type = "internal",
        source = "internal",
        msg = "Cycle to next preset"
    )
    private val cyclePresetBackwardAction = AppAction(
        id = "__cycle_preset_backward__",
        displayName = "Cycle Preset Backward",
        topic = "Controller",
        type = "internal",
        source = "internal",
        msg = "Cycle to previous preset"
    )

    private var geometryActions: List<AppAction> = emptyList()
    private var customActions: List<AppAction> = emptyList()
    private val _selectedPreset = MutableStateFlow<ControllerPreset?>(null)
    val selectedPreset: StateFlow<ControllerPreset?> = _selectedPreset
    private val _presets = MutableStateFlow<List<ControllerPreset>>(emptyList())
    val presets: StateFlow<List<ControllerPreset>> = _presets
    private val _buttonAssignments = MutableStateFlow<Map<String, AppAction>>(emptyMap())
    val buttonAssignments: StateFlow<Map<String, AppAction>> = _buttonAssignments
    private val _uiState = MutableStateFlow(ControllerUiState())
    val uiState: StateFlow<ControllerUiState> = _uiState

    init {
        viewModelScope.launch {
            val repoImpl = controllerRepository as? ControllerRepositoryImpl
            val loadedPresets = repoImpl?.loadControllerPresets() ?: emptyList()
            _presets.value = loadedPresets
            if (loadedPresets.isNotEmpty()) {
                _selectedPreset.value = loadedPresets.first()
            }
            val loadedConfigs = repoImpl?.loadControllerConfigs() ?: emptyList()
            _uiState.value = _uiState.value.copy(controllerConfigs = loadedConfigs)
            val config = loadControllerConfigUseCase.load()
            _uiState.value = ControllerUiMapper.toUiState(config).copy(
                controllerConfigs = loadedConfigs,
                appActions = _appActions.value
            )
        }
        refreshControllerButtons()

        (rosMessageRepository as? RosMessageRepositoryImpl)?.initialize(viewModelScope)

        viewModelScope.launch {
            Logger.d("ControllerViewModel", "Collecting messages from rosMessageRepository")
            rosMessageRepository.messages.collect { messageList ->
                Logger.d("ControllerViewModel", "Messages collected: $messageList")
                geometryActions = messageList.mapNotNull { it.toAppAction() }
                mergeAndEmitAppActions()
            }
        }

        viewModelScope.launch {
            _appActions.collect { actions ->
                _uiState.value = _uiState.value.copy(appActions = actions)
            }
        }
    }

    fun loadCustomAppActions(context: Context) {
        viewModelScope.launch {
            customActions = protocolRepository.getCustomAppActions(context)
            mergeAndEmitAppActions()
        }
    }

    private fun mergeAndEmitAppActions() {
        val merged = (geometryActions + customActions + listOf(cyclePresetForwardAction, cyclePresetBackwardAction)).distinctBy { it.id }
        _appActions.value = merged
    }

    private fun RosMessageDto.toAppAction(): AppAction? {
        return AppAction(
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
        if (presetName == "New Preset" || preset == null) {
            _buttonAssignments.value = emptyMap()
            _uiState.value = _uiState.value.copy(buttonAssignments = emptyMap())
        } else {
            _buttonAssignments.value = preset.buttonAssignments
            _uiState.value = _uiState.value.copy(buttonAssignments = preset.buttonAssignments)
        }
    }

    fun addPreset(name: String, context: Context) {
        val newPreset = ControllerPreset(
            name = name,
            topic = null,
            buttonAssignments = _buttonAssignments.value
        )
        val updatedPresets = _presets.value + newPreset
        _presets.value = updatedPresets
        (controllerRepository as? ControllerRepositoryImpl)?.saveControllerPresets(updatedPresets)
        saveConfigWithPresets(updatedPresets)
    }

    fun removePreset(context: Context) {
        val selected = _selectedPreset.value ?: return
        val updated = _presets.value.filter { it.name != selected.name }
        _presets.value = updated
        (controllerRepository as? ControllerRepositoryImpl)?.saveControllerPresets(updated)
        saveConfigWithPresets(updated)
        selectPreset("New Preset")
    }

    fun savePreset(name: String) {
        val selected = _selectedPreset.value ?: return
        val updatedPreset = selected.copy(name = name, buttonAssignments = _buttonAssignments.value)
        val updated = _presets.value.map { if (it.name == selected.name) updatedPreset else it }
        _presets.value = updated
        saveConfigWithPresets(updated)
        _selectedPreset.value = updatedPreset
    }

    fun assignButton(button: String, action: AppAction?, context: Context) {
        val updated = _buttonAssignments.value.toMutableMap()
        if (action != null) {
            updated[button] = action
        } else {
            updated.remove(button)
        }
        _buttonAssignments.value = updated
        (controllerRepository as? ControllerRepositoryImpl)?.saveButtonAssignments(updated)
        saveConfigWithAssignments(updated)
    }

    fun assignAbxyButton(btn: String, actionName: String, context: android.content.Context) {
        val action = _appActions.value.find { it.displayName == actionName }
        assignButton(
            btn, action,
            context = context
        )
    }

    fun handleKeyEvent(keyCode: Int): AppAction? {
        return handleControllerInputUseCase.handleKeyEvent(keyCode, _buttonAssignments.value)
    }

    fun exportConfig(outputStream: OutputStream) {
        (controllerRepository as? ControllerRepositoryImpl)?.exportConfigToStream(outputStream)
    }

    fun importConfig(inputStream: InputStream) {
        (controllerRepository as? ControllerRepositoryImpl)?.importConfigFromStream(inputStream)
        viewModelScope.launch {
            val config = loadControllerConfigUseCase.load()
            _uiState.value = ControllerUiMapper.toUiState(config).copy(appActions = _appActions.value)
            _presets.value = config.controllerPresets
            _buttonAssignments.value = config.buttonAssignments
            _selectedPreset.value = config.controllerPresets.firstOrNull()
        }
    }

    fun updateJoystickMappings(newMappings: List<JoystickMapping>) {
        val uiState = _uiState.value.copy(config = _uiState.value.config.copy(joystickMappings = newMappings))
        val config = ControllerUiMapper.toDomainConfig(uiState)
        viewModelScope.launch {
            saveControllerConfigUseCase.save(config)
            _uiState.value = ControllerUiMapper.toUiState(config).copy(appActions = _appActions.value)
        }
    }

    private fun saveConfigWithPresets(presets: List<ControllerPreset>) {
        val uiState = _uiState.value.copy(presets = presets)
        val config = ControllerUiMapper.toDomainConfig(uiState)
        viewModelScope.launch {
            saveControllerConfigUseCase.save(config)
            _uiState.value = ControllerUiMapper.toUiState(config).copy(appActions = _appActions.value)
        }
    }

    private fun saveConfigWithAssignments(assignments: Map<String, AppAction>) {
        val uiState = _uiState.value.copy(buttonAssignments = assignments)
        val config = ControllerUiMapper.toDomainConfig(uiState)
        viewModelScope.launch {
            saveControllerConfigUseCase.save(config)
            _uiState.value = ControllerUiMapper.toUiState(config).copy(appActions = _appActions.value)
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

    fun addControllerConfig(name: String, context: Context) {
        val newConfig = ControllerConfig(name = name)
        val updatedConfigs = _uiState.value.controllerConfigs + newConfig
        _uiState.value = _uiState.value.copy(controllerConfigs = updatedConfigs)
        (controllerRepository as? ControllerRepositoryImpl)?.saveControllerConfigs(updatedConfigs)
    }

    fun removeControllerConfig(name: String, context: Context) {
        val updatedConfigs = _uiState.value.controllerConfigs.filter { it.name != name }
        _uiState.value = _uiState.value.copy(controllerConfigs = updatedConfigs)
        (controllerRepository as? ControllerRepositoryImpl)?.saveControllerConfigs(updatedConfigs)
    }

    fun cyclePresetForward() {
        val current = _selectedPreset.value
        val list = _presets.value
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.name == current?.name }
        val nextIdx = if (idx == -1 || idx == list.lastIndex) 0 else idx + 1
        _selectedPreset.value = list[nextIdx]
    }

    fun cyclePresetBackward() {
        val current = _selectedPreset.value
        val list = _presets.value
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.name == current?.name }
        val prevIdx = if (idx <= 0) list.lastIndex else idx - 1
        _selectedPreset.value = list[prevIdx]
    }
}