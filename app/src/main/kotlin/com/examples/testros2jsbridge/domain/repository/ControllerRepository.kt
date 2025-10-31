package com.examples.testros2jsbridge.domain.repository

import com.examples.testros2jsbridge.domain.model.Controller
import kotlinx.coroutines.flow.Flow

interface ControllerRepository {

    fun getControllers(): Flow<List<Controller>>

    suspend fun getController(controllerId: Int): Flow<Controller>

    suspend fun addController(name: String): Long

    suspend fun addButtonMapToAction(actionId: String, inputType: String, deadzone: Float?, sensitivity: Float?): Long

    suspend fun addPresetToController(controllerId: Int, presetName: String): Long

    suspend fun addButtonMapToPreset(presetId: Int, buttonMapId: Int)

    suspend fun addFixedButtonMapToController(controllerId: Int, buttonMapId: Int)

    suspend fun getSelectedConfigName(id: String): String?

    suspend fun saveSelectedConfigName(name: String)

    suspend fun deletePresetsForController(controllerId: Int)
}
