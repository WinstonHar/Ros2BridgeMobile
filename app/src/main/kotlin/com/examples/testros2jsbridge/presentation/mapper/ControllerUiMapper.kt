package com.examples.testros2jsbridge.presentation.mapper

import com.examples.testros2jsbridge.domain.model.*
import com.examples.testros2jsbridge.presentation.state.ControllerUiState

object ControllerUiMapper {
    fun toUiState(config: ControllerConfig): ControllerUiState =
        ControllerUiState(
            config = config,
            controllerButtons = config.buttonPresets.keys.toList(),
            appActions = config.buttonAssignments.values.toList(),
            presets = config.controllerPresets,
            buttonAssignments = config.buttonAssignments
        )

    fun toDomainConfig(uiState: ControllerUiState): ControllerConfig =
        uiState.config.copy(
            buttonPresets = uiState.controllerButtons.associateWith { "" },
            controllerPresets = uiState.presets,
            buttonAssignments = uiState.buttonAssignments
        )
}