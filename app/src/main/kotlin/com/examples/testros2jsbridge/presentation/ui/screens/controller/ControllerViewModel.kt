package com.examples.testros2jsbridge.presentation.ui.screens.controller

import android.content.Context
import android.view.InputDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.model.ControllerPreset
import com.examples.testros2jsbridge.domain.model.JoystickMapping
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import com.examples.testros2jsbridge.domain.repository.ProtocolRepository
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository
import com.examples.testros2jsbridge.domain.usecase.controller.HandleControllerInputUseCase
import com.examples.testros2jsbridge.domain.usecase.controller.LoadControllerConfigUseCase
import com.examples.testros2jsbridge.domain.usecase.controller.SaveControllerConfigUseCase
import com.examples.testros2jsbridge.presentation.mapper.ControllerUiMapper
import com.examples.testros2jsbridge.presentation.state.ControllerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import com.examples.testros2jsbridge.util.sanitizeConfigName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.flow.update


@HiltViewModel
class ControllerViewModel @Inject constructor(
    val handleControllerInputUseCase: HandleControllerInputUseCase,
    private val loadControllerConfigUseCase: LoadControllerConfigUseCase,
    private val saveControllerConfigUseCase: SaveControllerConfigUseCase,
    val controllerRepository: ControllerRepository,
    private val rosMessageRepository: RosMessageRepository,
    private val protocolRepository: ProtocolRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    private var lastSavedConfig: ControllerConfig? = null

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
    private var pendingConfigName: String? = null
    private val _showPresetsOverlay = MutableStateFlow(false)
    val showPresetsOverlay: StateFlow<Boolean> = _showPresetsOverlay
    fun triggerPresetsOverlay() {
        _showPresetsOverlay.value = true
    }
    fun hidePresetsOverlay() {
        _showPresetsOverlay.value = false
    }
    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    init {
        viewModelScope.launch {
            val config = loadControllerConfigUseCase.load()
            _uiState.value = ControllerUiMapper.toUiState(config).copy(appActions = _appActions.value)
            _presets.value = config.controllerPresets
            _buttonAssignments.value = config.buttonAssignments
            _selectedPreset.value = config.controllerPresets.firstOrNull()
            lastSavedConfig = config.copy()
            updateHasUnsavedChanges()
        }
        refreshControllerButtons()

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
                updateHasUnsavedChanges()
            }
        }

        loadCustomAppActions(appContext)
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
        saveConfigWithPresets(updatedPresets)
    }

    fun removePreset(context: Context) {
        val selected = _selectedPreset.value ?: return
        val updated = _presets.value.filter { it.name != selected.name }
        _presets.value = updated
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
        updateButtonAssignments(updated)
    }

    fun assignAbxyButton(btn: String, actionName: String, context: Context) {
        val action = _appActions.value.find { it.displayName == actionName }
        assignButton(
            btn, action,
            context = context
        )
    }

    fun exportConfig(outputStream: OutputStream) {
        // TODO: Implement export config
    }

    fun importConfig(inputStream: InputStream) {
        // TODO: Implement import config
    }

    fun updateJoystickMappings(newMappings: List<JoystickMapping>) {
        val uiState = _uiState.value.copy(config = _uiState.value.config.copy(joystickMappings = newMappings))
        _uiState.value = uiState
        updateHasUnsavedChanges()
    }

    fun updateButtonAssignments(newAssignments: Map<String, AppAction>) {
        _buttonAssignments.value = newAssignments
        _uiState.value = _uiState.value.copy(buttonAssignments = newAssignments)
        updateHasUnsavedChanges()
    }

    fun saveConfig() {
        val mergedConfig = _uiState.value.config.copy(
            buttonAssignments = _buttonAssignments.value,
            joystickMappings = _uiState.value.config.joystickMappings
        )
        val mergedUiState = _uiState.value.copy(
            config = mergedConfig,
            buttonAssignments = _buttonAssignments.value
        )
        val config = ControllerUiMapper.toDomainConfig(mergedUiState)
        viewModelScope.launch {
            saveControllerConfigUseCase.save(config)
            Logger.d("ControllerViewModel", "saveConfig() - Saved config: $config")
            _uiState.value = ControllerUiMapper.toUiState(config).copy(appActions = _appActions.value)
            lastSavedConfig = config.copy()
            updateHasUnsavedChanges()
        }
    }

    private fun updateHasUnsavedChanges() {
        // Compare current config to lastSavedConfig
        val currentConfig = _uiState.value.config.copy(
            buttonAssignments = _buttonAssignments.value,
            joystickMappings = _uiState.value.config.joystickMappings
        )
        val dirty = lastSavedConfig?.let { it != currentConfig } ?: false
        _hasUnsavedChanges.value = dirty
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

    fun selectControllerConfig(name: String, persist: Boolean = true) {
        pendingConfigName = name
        val sanitizedName = sanitizeConfigName(name)
        val config = _uiState.value.controllerConfigs.find { sanitizeConfigName(it.name) == sanitizedName }
        if (config != null) {
            val buttonNames = config.buttonAssignments.keys.toList()
            _uiState.value = _uiState.value.copy(
                config = config,
                buttonAssignments = config.buttonAssignments,
                selectedConfigName = config.name,
                controllerButtons = buttonNames
            )
            _buttonAssignments.value = config.buttonAssignments
            _selectedPreset.value = getPresetForConfigName(_uiState.value.config.name)
        } else {
            _uiState.value = _uiState.value.copy(selectedConfigName = name)
        }
    }

    private fun ControllerConfig.toPreset(): ControllerPreset {
        return ControllerPreset(
            name = this.name,
            buttonAssignments = this.buttonAssignments,
            joystickMappings = this.joystickMappings
        )
    }

    fun getPresetForConfigName(configName: String): ControllerPreset? {
        val config = _uiState.value.controllerConfigs.find { it.name == configName }
        return config?.toPreset()
    }

    fun addControllerConfig(name: String, context: Context) {
        val sanitizedName = sanitizeConfigName(name)
        val newConfig = ControllerConfig(name = sanitizedName)
        val updatedConfigs = _uiState.value.controllerConfigs + newConfig
        _uiState.value = _uiState.value.copy(controllerConfigs = updatedConfigs, config = newConfig, buttonAssignments = newConfig.buttonAssignments)
        pendingConfigName = sanitizedName
    }

    fun removeControllerConfig(name: String, context: Context) {
        val sanitizedName = sanitizeConfigName(name)
        val updatedConfigs = _uiState.value.controllerConfigs.filter { sanitizeConfigName(it.name) != sanitizedName }
        _uiState.value = _uiState.value.copy(controllerConfigs = updatedConfigs)
        if (_uiState.value.config.name == sanitizedName) {
            _uiState.value = _uiState.value.copy(config = ControllerConfig())
        }
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
