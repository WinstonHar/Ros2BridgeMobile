package com.examples.testros2jsbridge.presentation.ui.screens.controller

import android.content.Context
import android.view.InputDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.core.ros.RosBridgeViewModel
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.data.local.database.RosProtocolType
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.model.ControllerPreset
import com.examples.testros2jsbridge.domain.model.JoystickMapping
import com.examples.testros2jsbridge.domain.repository.AppActionRepository
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import com.examples.testros2jsbridge.domain.usecase.controller.HandleControllerInputUseCase
import com.examples.testros2jsbridge.domain.usecase.controller.LoadAllControllerConfigsUseCase
import com.examples.testros2jsbridge.domain.usecase.controller.LoadControllerConfigUseCase
import com.examples.testros2jsbridge.domain.usecase.controller.SaveControllerConfigUseCase
import com.examples.testros2jsbridge.presentation.state.ControllerUiState
import com.examples.testros2jsbridge.util.sanitizeConfigName
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class ControllerViewModel @Inject constructor(
    val handleControllerInputUseCase: HandleControllerInputUseCase,
    private val loadControllerConfigUseCase: LoadControllerConfigUseCase,
    private val saveControllerConfigUseCase: SaveControllerConfigUseCase,
    private val loadAllControllerConfigsUseCase: LoadAllControllerConfigsUseCase,
    private val appActionRepository: AppActionRepository,
    private val controllerRepository: ControllerRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    private var lastSavedConfig: ControllerConfig? = null

    private val _detectedControllerButtons = MutableStateFlow<List<String>>(emptyList())
    val detectedControllerButtons: StateFlow<List<String>> = _detectedControllerButtons.asStateFlow()

    private val _selectedConfigName = MutableStateFlow<String?>(null)
    val selectedConfigName: StateFlow<String?> = _selectedConfigName.asStateFlow()

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

    fun triggerAppAction(action: AppAction, rosBridgeViewModel: RosBridgeViewModel) {
        when (action.rosMessageType) {
            RosProtocolType.PUBLISHER.name -> {
                rosBridgeViewModel.publishMessage(action.topic, action.type, action.msg)
            }
            RosProtocolType.SERVICE_CLIENT.name -> {
                rosBridgeViewModel.sendOrQueueServiceRequest(action.topic, action.type, action.msg) { result ->
                    Logger.d("ControllerViewModel", "Service result for '${action.topic}': $result")
                }
            }
            RosProtocolType.ACTION_CLIENT.name -> {
                rosBridgeViewModel.sendOrQueueActionGoal(
                    actionName = action.topic,
                    actionType = action.type,
                    goalFields = Json.parseToJsonElement(action.msg).jsonObject
                ) { result ->
                    Logger.d("ControllerViewModel", "Action result for '${action.topic}': $result")
                }
            }
            "internal" -> {
                when (action.id) {
                    "__cycle_preset_forward__" -> cyclePresetForward()
                    "__cycle_preset_backward__" -> cyclePresetBackward()
                }
            }
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
        msg = "Cycle to next preset",
        rosMessageType = "internal"
    )

    private val cyclePresetBackwardAction = AppAction(
        id = "__cycle_preset_backward__",
        displayName = "Cycle Preset Backward",
        topic = "Controller",
        type = "internal",
        source = "internal",
        msg = "Cycle to previous preset",
        rosMessageType = "internal"
    )

    private var geometryActions: List<AppAction> = emptyList()
    private val _selectedPreset = MutableStateFlow<ControllerPreset?>(null)
    val selectedPreset: StateFlow<ControllerPreset?> = _selectedPreset
    private val _presets = MutableStateFlow<List<ControllerPreset>>(emptyList())
    val presets: StateFlow<List<ControllerPreset>> = _presets
    private val _uiState = MutableStateFlow(ControllerUiState())
    val uiState: StateFlow<ControllerUiState> = _uiState
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
            if (config != null) {
                _presets.value = config.controllerPresets
                _uiState.update { it.copy(config = config, presets = config.controllerPresets) }
                _selectedPreset.value = config.controllerPresets.firstOrNull()
                lastSavedConfig = config.copy()
                updateHasUnsavedChanges()
            }
        }

        viewModelScope.launch {
            val allConfigs = loadAllControllerConfigsUseCase.load()
            _uiState.update { it.copy(controllerConfigs = allConfigs) }
        }

        viewModelScope.launch {
            appActionRepository.getCustomAppActions(appContext).collect { customActions ->
                mergeAndEmitAppActions(customActions)
            }
        }

        refreshControllerButtons()
    }

    private fun mergeAndEmitAppActions(customActions: List<AppAction>) {
        val merged = (geometryActions + customActions + listOf(cyclePresetForwardAction, cyclePresetBackwardAction)).distinctBy { it.id }
        _appActions.value = merged
        _uiState.update { it.copy(appActions = merged) }
    }

    private fun RosMessageDto.toAppAction(): AppAction? {
        return AppAction(
            id = this.id ?: (this.label ?: this.topic.value),
            displayName = this.label ?: this.topic.value,
            topic = this.topic.value,
            type = this.type ?: "",
            source = "geometry",
            msg = this.content ?: "",
            rosMessageType = this.type ?: ""
        )
    }

    fun selectPreset(presetName: String) {
        val preset = _presets.value.find { it.name == presetName }
        _selectedPreset.value = preset
        val newButtonAssignments = if (presetName == "New Preset" || preset == null) {
            emptyMap()
        } else {
            preset.buttonAssignments
        }
        _uiState.update { it.copy(config = it.config.copy(buttonAssignments = newButtonAssignments)) }
        updateHasUnsavedChanges()
    }

    fun addPreset(name: String, context: Context) {
        val newPreset = ControllerPreset(
            name = name,
            topic = null,
            buttonAssignments = _uiState.value.config.buttonAssignments
        )
        val updatedPresets = _uiState.value.config.controllerPresets + newPreset
        val updatedConfig = _uiState.value.config.copy(controllerPresets = updatedPresets)
        updateAndSaveConfig(updatedConfig)
        _selectedPreset.value = newPreset
    }

    fun removePreset(context: Context) {
        val selected = _selectedPreset.value ?: return
        val updatedPresets = _uiState.value.config.controllerPresets.filter { it.name != selected.name }
        val updatedConfig = _uiState.value.config.copy(controllerPresets = updatedPresets)
        updateAndSaveConfig(updatedConfig)
        selectPreset("New Preset")
    }

    fun savePreset(name: String) {
        val selected = _selectedPreset.value ?: return
        val updatedPreset = selected.copy(name = name, buttonAssignments = _uiState.value.config.buttonAssignments)
        val updatedPresets = _uiState.value.config.controllerPresets.map { if (it.name == selected.name) updatedPreset else it }
        _selectedPreset.value = updatedPreset
        val updatedConfig = _uiState.value.config.copy(controllerPresets = updatedPresets)
        updateAndSaveConfig(updatedConfig)
    }

    fun assignButton(button: String, action: AppAction?, context: Context) {
        val updated = _uiState.value.config.buttonAssignments.toMutableMap()
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
        _uiState.update { it.copy(config = it.config.copy(joystickMappings = newMappings)) }
        updateHasUnsavedChanges()
    }

    fun updateButtonAssignments(newAssignments: Map<String, AppAction>) {
        _uiState.update { it.copy(config = it.config.copy(buttonAssignments = newAssignments)) }
        updateHasUnsavedChanges()
    }

    fun saveConfig() {
        val configToSave = _uiState.value.config.copy(
            name = _selectedConfigName.value ?: _uiState.value.config.name
        )
        updateAndSaveConfig(configToSave)
    }

    private fun updateHasUnsavedChanges() {
        _hasUnsavedChanges.value = lastSavedConfig != _uiState.value.config
    }

    private fun updateAndSaveConfig(configToSave: ControllerConfig) {
        viewModelScope.launch {
            saveControllerConfigUseCase.save(configToSave, appContext)

            val configIndex = _uiState.value.controllerConfigs.indexOfFirst { it.name == configToSave.name }
            val updatedConfigs = if (configIndex != -1) {
                _uiState.value.controllerConfigs.toMutableList().apply { set(configIndex, configToSave) }
            } else {
                _uiState.value.controllerConfigs + configToSave
            }

            _uiState.update {
                it.copy(
                    controllerConfigs = updatedConfigs,
                    config = configToSave,
                    presets = configToSave.controllerPresets
                )
            }
            _presets.value = configToSave.controllerPresets
            lastSavedConfig = configToSave.copy()
            updateHasUnsavedChanges()
        }
    }

    fun selectControllerConfig(name: String, persist: Boolean = true) {
        _selectedConfigName.value = name
        val sanitizedName = sanitizeConfigName(name)
        val config = _uiState.value.controllerConfigs.find { sanitizeConfigName(it.name) == sanitizedName }
        if (config != null) {
            if (persist) {
                viewModelScope.launch {
                    controllerRepository.saveSelectedConfigName(name)
                }
            }
            val buttonNames = config.buttonAssignments.keys.toList()
            _presets.value = config.controllerPresets
            _uiState.update { currentState ->
                currentState.copy(
                    config = config,
                    presets = config.controllerPresets,
                    selectedConfigName = config.name,
                    controllerButtons = buttonNames
                )
            }
            _selectedPreset.value = getPresetForConfigName(_uiState.value.config.name)
            lastSavedConfig = config.copy()
            updateHasUnsavedChanges()
        } else {
            _uiState.update { it.copy(selectedConfigName = name) }
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
        _uiState.update { it.copy(controllerConfigs = updatedConfigs, config = newConfig) }
        viewModelScope.launch {
            saveControllerConfigUseCase.save(newConfig, appContext)
            lastSavedConfig = newConfig.copy()
            updateHasUnsavedChanges()
        }
    }

    fun removeControllerConfig(name: String, context: Context) {
        val sanitizedName = sanitizeConfigName(name)
        val updatedConfigs = _uiState.value.controllerConfigs.filter { sanitizeConfigName(it.name) != sanitizedName }
        _uiState.value = _uiState.value.copy(controllerConfigs = updatedConfigs)
        if (_uiState.value.config.name == sanitizedName) {
            _uiState.value = _uiState.value.copy(config = ControllerConfig())
        }
        updateHasUnsavedChanges()
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
