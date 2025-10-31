package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.examples.testros2jsbridge.data.local.database.ControllerWithDetails
import com.examples.testros2jsbridge.data.local.database.entities.ButtonMapEntity
import com.examples.testros2jsbridge.data.local.database.entities.ButtonPresetsEntity
import com.examples.testros2jsbridge.data.local.database.entities.ControllerButtonFixedMapJunction
import com.examples.testros2jsbridge.data.local.database.entities.ControllerEntity
import com.examples.testros2jsbridge.data.local.database.entities.PresetButtonMapJunction
import com.examples.testros2jsbridge.data.local.database.relations.ControllerWithButtonMaps
import com.examples.testros2jsbridge.data.local.database.relations.PresetWithButtonMaps
import kotlinx.coroutines.flow.Flow

@Dao
interface ControllerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertController(controller: ControllerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: ButtonPresetsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButtonMap(buttonMap: ButtonMapEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPresetButtonMapJunction(junction: PresetButtonMapJunction)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertControllerButtonFixedMapJunction(junction: ControllerButtonFixedMapJunction)

    @Transaction
    @Query("SELECT * FROM button_presets WHERE presetId = :presetId")
    fun getPresetWithButtonMaps(presetId: Int): Flow<PresetWithButtonMaps>

    @Transaction
    @Query("SELECT * FROM controllers WHERE controllerId = :controllerId")
    fun getControllerWithFixedButtonMaps(controllerId: Int): Flow<ControllerWithButtonMaps>

    @Transaction
    @Query("SELECT * FROM controllers WHERE controllerId = :controllerId")
    fun getControllerWithDetails(controllerId: Int): Flow<ControllerWithDetails>

    @Transaction
    suspend fun addPresetToControllerTransactional(controllerId: Int, presetName: String): Long {
        val presetId = insertPreset(ButtonPresetsEntity(name = presetName, controllerOwnerId = controllerId))
        return presetId
    }
}
