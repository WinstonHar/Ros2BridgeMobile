package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.local.database.dao.ControllerDao
import com.examples.testros2jsbridge.data.local.database.entities.ButtonMapEntity
import com.examples.testros2jsbridge.data.local.database.entities.ButtonPresetsEntity
import com.examples.testros2jsbridge.data.local.database.entities.ControllerButtonFixedMapJunction
import com.examples.testros2jsbridge.data.local.database.entities.ControllerEntity
import com.examples.testros2jsbridge.data.local.database.entities.PresetButtonMapJunction
import com.examples.testros2jsbridge.domain.model.ButtonMap
import com.examples.testros2jsbridge.domain.model.ButtonPreset
import com.examples.testros2jsbridge.domain.model.Controller
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.repository.ConfigurationRepository
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ControllerRepositoryImpl @Inject constructor(
    private val controllerDao: ControllerDao,
    private val configurationRepository: ConfigurationRepository
) : ControllerRepository {

    override fun getControllers(): Flow<List<Controller>> {
        TODO("Not yet implemented")
    }

    override suspend fun getController(controllerId: Int): Flow<Controller> {
        return controllerDao.getControllerWithDetails(controllerId).filterNotNull().map {
            Controller(
                controllerId = it.controller.controllerId,
                name = it.controller.name,
                presets = it.presets.map {
                    ButtonPreset(
                        presetId = it.preset.presetId,
                        name = it.preset.name,
                        buttonMaps = it.buttonMaps.map {
                            ButtonMap(
                                buttonMapId = it.buttonMapId,
                                inputType = it.inputType,
                                mappedActionId = it.mappedActionId,
                                joystickDeadzone = it.joystickDeadzone,
                                joystickSensitivity = it.joystickSensitivity
                            )
                        }
                    )
                },
                fixedButtonMaps = it.fixedButtonMaps.map {
                    ButtonMap(
                        buttonMapId = it.buttonMapId,
                        inputType = it.inputType,
                        mappedActionId = it.mappedActionId,
                        joystickDeadzone = it.joystickDeadzone,
                        joystickSensitivity = it.joystickSensitivity
                    )
                }
            )
        }
    }

    override suspend fun addController(name: String): Long {
        return controllerDao.insertController(ControllerEntity(name = name))
    }

    override suspend fun addButtonMapToAction(actionId: String, inputType: String, deadzone: Float?, sensitivity: Float?): Long {
        return controllerDao.insertButtonMap(ButtonMapEntity(
            mappedActionId = actionId,
            inputType = inputType,
            joystickDeadzone = deadzone,
            joystickSensitivity = sensitivity
        ))
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

    override suspend fun getAllControllerConfigs(): List<ControllerConfig> {
        return configurationRepository.getAllControllerConfigs()
    }

    override suspend fun getSelectedConfigName(id: String): String? {
        return configurationRepository.getSelectedConfigName(id)
    }

    override suspend fun saveSelectedConfigName(name: String) {
        configurationRepository.saveSelectedConfigName(name)
    }

    override suspend fun deletePresetsForController(controllerId: Int) {
        // This is now handled by cascading deletes in the database
    }
}
