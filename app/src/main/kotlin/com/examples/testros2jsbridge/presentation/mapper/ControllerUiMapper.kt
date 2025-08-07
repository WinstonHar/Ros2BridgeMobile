package com.examples.testros2jsbridge.presentation.mapper

import com.examples.testros2jsbridge.domain.model.*
import com.examples.testros2jsbridge.presentation.state.ControllerUiState

object ControllerUiMapper {


    fun toUiState(config: ControllerConfig): ControllerUiState {
        val presets = if (config.controllerPresets.isEmpty()) {
            listOf(
                ControllerPreset(
                    name = "Default",
                    buttonAssignments = emptyMap(),
                    joystickMappings = emptyList()
                )
            )
        } else {
            config.controllerPresets
        }
        return ControllerUiState(
            config = config,
            controllerButtons = config.buttonPresets.keys.toList(),
            appActions = config.buttonAssignments.values.toList(),
            presets = presets,
            buttonAssignments = config.buttonAssignments
        )
    }

    fun toDomainConfig(uiState: ControllerUiState): ControllerConfig =
        uiState.config.copy(
            buttonPresets = uiState.controllerButtons.associateWith { "" },
            controllerPresets = uiState.presets,
            buttonAssignments = uiState.buttonAssignments
        )
}