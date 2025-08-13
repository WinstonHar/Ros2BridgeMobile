package com.examples.testros2jsbridge.presentation.state

import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.model.ControllerPreset
import com.examples.testros2jsbridge.domain.model.AppAction

/**
 * UI state for controller configuration, wrapping ControllerConfig and adding UI flags/fields.
 */
data class ControllerUiState(
    val config: ControllerConfig = ControllerConfig(),
    val controllerButtons: List<String> = emptyList(),
    val appActions: List<AppAction> = emptyList(),
    val presets: List<ControllerPreset> = emptyList(),
    val selectedPreset: ControllerPreset? = null,
    val buttonAssignments: Map<String, AppAction> = emptyMap(),
    val isLoading: Boolean = false,
    val showErrorDialog: Boolean = false,
    val errorMessage: String? = null,
    val showPresetDialog: Boolean = false,
    val presetNameInput: String = "",
    val abxyAssignments: Map<String, String> = mapOf("A" to "", "B" to "", "X" to "", "Y" to ""),
    val controllerConfigs: List<ControllerConfig> = emptyList(),
    val selectedConfigName: String = "New Config"
)