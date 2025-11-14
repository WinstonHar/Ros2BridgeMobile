package com.examples.testros2jsbridge.presentation.mapper

import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.presentation.state.ControllerUiState

object ControllerUiMapper {


    fun toUiState(config: ControllerConfig): ControllerUiState {
        val abxyPresets = config.controllerPresets.filter { it.name != "Default" }
        return ControllerUiState(
            config = config,
            controllerButtons = config.buttonPresets.keys.toList(),
            presets = abxyPresets // Only ABXY presets
        )
    }

    fun toDomainConfig(uiState: ControllerUiState): ControllerConfig =
        uiState.config.copy(
            buttonPresets = uiState.controllerButtons.associateWith { "" },
            controllerPresets = uiState.presets
        )
}
