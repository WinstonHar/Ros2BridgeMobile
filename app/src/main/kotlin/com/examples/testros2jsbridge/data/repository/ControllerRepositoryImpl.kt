package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.local.database.dao.ControllerDao
import com.examples.testros2jsbridge.data.local.database.entities.ButtonMapEntity
import com.examples.testros2jsbridge.data.local.database.entities.ControllerButtonFixedMapJunction
import com.examples.testros2jsbridge.data.local.database.entities.ControllerEntity
import com.examples.testros2jsbridge.data.local.database.entities.PresetButtonMapJunction
import com.examples.testros2jsbridge.domain.model.ButtonMap
import com.examples.testros2jsbridge.domain.model.ButtonPreset
import com.examples.testros2jsbridge.domain.model.Controller
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ControllerRepositoryImpl @Inject constructor(
    private val controllerDao: ControllerDao
) : ControllerRepository {

    override fun getControllers(): Flow<List<Controller>> {
        return controllerDao.getAllControllersWithDetails().map { controllersWithDetails ->
            controllersWithDetails.map { controllerWithDetails ->
                Controller(
                    controllerId = controllerWithDetails.controller.controllerId,
                    name = controllerWithDetails.controller.name,
                    presets = controllerWithDetails.presets.map { presetWithButtonMaps ->
                        ButtonPreset(
                            presetId = presetWithButtonMaps.preset.presetId,
                            name = presetWithButtonMaps.preset.name,
                            buttonMaps = presetWithButtonMaps.buttonMaps.map { buttonMapEntity ->
                                ButtonMap(
                                    buttonMapId = buttonMapEntity.buttonMapId,
                                    inputType = buttonMapEntity.inputType,
                                    mappedActionId = buttonMapEntity.mappedActionId,
                                    joystickDeadzone = buttonMapEntity.joystickDeadzone,
                                    joystickSensitivity = buttonMapEntity.joystickSensitivity
                                )
                            }
                        )
                    },
                    fixedButtonMaps = controllerWithDetails.fixedButtonMaps.map { buttonMapEntity ->
                        ButtonMap(
                            buttonMapId = buttonMapEntity.buttonMapId,
                            inputType = buttonMapEntity.inputType,
                            mappedActionId = buttonMapEntity.mappedActionId,
                            joystickDeadzone = buttonMapEntity.joystickDeadzone,
                            joystickSensitivity = buttonMapEntity.joystickSensitivity
                        )
                    }
                )
            }
        }
    }

    override suspend fun getController(controllerId: Int): Flow<Controller> {
        return controllerDao.getControllerWithDetails(controllerId).map { controllerWithDetails ->
            Controller(
                controllerId = controllerWithDetails.controller.controllerId,
                name = controllerWithDetails.controller.name,
                presets = controllerWithDetails.presets.map { presetWithButtonMaps ->
                    ButtonPreset(
                        presetId = presetWithButtonMaps.preset.presetId,
                        name = presetWithButtonMaps.preset.name,
                        buttonMaps = presetWithButtonMaps.buttonMaps.map { buttonMapEntity ->
                            ButtonMap(
                                buttonMapId = buttonMapEntity.buttonMapId,
                                inputType = buttonMapEntity.inputType,
                                mappedActionId = buttonMapEntity.mappedActionId,
                                joystickDeadzone = buttonMapEntity.joystickDeadzone,
                                joystickSensitivity = buttonMapEntity.joystickSensitivity
                            )
                        }
                    )
                },
                fixedButtonMaps = controllerWithDetails.fixedButtonMaps.map { buttonMapEntity ->
                    ButtonMap(
                        buttonMapId = buttonMapEntity.buttonMapId,
                        inputType = buttonMapEntity.inputType,
                        mappedActionId = buttonMapEntity.mappedActionId,
                        joystickDeadzone = buttonMapEntity.joystickDeadzone,
                        joystickSensitivity = buttonMapEntity.joystickSensitivity
                    )
                }
            )
        }
    }

    override suspend fun addController(name: String): Long {
        return controllerDao.insertController(ControllerEntity(name = name))
    }

    override suspend fun addButtonMapToAction(actionId: String, inputType: String, deadzone: Float?, sensitivity: Float?): Long {
        return controllerDao.insertButtonMap(
            ButtonMapEntity(
                mappedActionId = actionId,
                inputType = inputType,
                joystickDeadzone = deadzone,
                joystickSensitivity = sensitivity
            )
        )
    }

    override suspend fun addPresetToController(controllerId: Int, presetName: String): Long {
        return controllerDao.addPresetToControllerTransactional(controllerId, presetName)
    }

    override suspend fun addButtonMapToPreset(presetId: Int, buttonMapId: Int) {
        controllerDao.insertPresetButtonMapJunction(PresetButtonMapJunction(presetId, buttonMapId))
    }

    override suspend fun addFixedButtonMapToController(controllerId: Int, buttonMapId: Int) {
        controllerDao.insertControllerButtonFixedMapJunction(ControllerButtonFixedMapJunction(controllerId, buttonMapId))
    }

    override suspend fun getSelectedConfigName(id: String): String? {
        // TODO: This should probably be stored in a different way
        return null
    }

    override suspend fun saveSelectedConfigName(name: String) {
        // TODO: This should probably be stored in a different way
    }

    override suspend fun deletePresetsForController(controllerId: Int) {
        controllerDao.deletePresetsForController(controllerId)
    }
}
