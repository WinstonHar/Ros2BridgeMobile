package com.examples.testros2jsbridge.domain.usecase.controller

import android.content.Context
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.repository.AppActionRepository
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SaveControllerConfigUseCase @Inject constructor(
    private val controllerRepository: ControllerRepository,
    private val appActionRepository: AppActionRepository
) {
    suspend fun save(config: ControllerConfig, context: Context) {
        withContext(Dispatchers.IO) {
            val controllerId = 1 // Assuming a single controller for now

            // Ensure the controller exists
            val controller = controllerRepository.getController(controllerId).firstOrNull()
            if (controller == null) {
                controllerRepository.addController("Default Controller")
            }

            // Save all actions first to avoid foreign key constraints
            val allActions = (config.controllerPresets.flatMap { it.buttonAssignments.values } + config.buttonAssignments.values).distinct()
            allActions.forEach { action ->
                appActionRepository.saveCustomAppAction(action, context)
            }

            // Save presets and button maps
            config.controllerPresets.forEach { preset ->
                val presetId = controllerRepository.addPresetToController(controllerId, preset.name)
                preset.buttonAssignments.forEach { (button, action) ->
                    val buttonMapId = controllerRepository.addButtonMapToAction(
                        actionId = action.id,
                        inputType = button,
                        deadzone = null,
                        sensitivity = null
                    )
                    controllerRepository.addButtonMapToPreset(presetId.toInt(), buttonMapId.toInt())
                }
            }

            // Save fixed button maps
            config.buttonAssignments.forEach { (button, action) ->
                val buttonMapId = controllerRepository.addButtonMapToAction(
                    actionId = action.id,
                    inputType = button,
                    deadzone = null,
                    sensitivity = null
                )
                controllerRepository.addFixedButtonMapToController(controllerId, buttonMapId.toInt())
            }
        }
    }
}
