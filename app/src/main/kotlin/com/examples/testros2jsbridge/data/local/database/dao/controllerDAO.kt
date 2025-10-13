package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.examples.testros2jsbridge.data.local.database.entities.ButtonMap
import com.examples.testros2jsbridge.data.local.database.entities.ButtonPresets
import com.examples.testros2jsbridge.data.local.database.entities.Controller
import com.examples.testros2jsbridge.data.local.database.ControllerWithDetails
import com.examples.testros2jsbridge.data.local.database.PresetWithButtonMaps
import com.examples.testros2jsbridge.data.local.database.entities.PresetButtonMapCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ControllerDao {

    // --- Insert Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertController(controller: Controller): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButtonMap(buttonMap: ButtonMap): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButtonPreset(preset: ButtonPresets): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPresetButtonMapCrossRef(crossRef: PresetButtonMapCrossRef)

    // --- Delete Operations ---

    @Delete
    suspend fun deleteController(controller: Controller)

    @Delete
    suspend fun deleteButtonMap(buttonMap: ButtonMap)

    @Delete
    suspend fun deleteButtonPreset(preset: ButtonPresets)
    
    @Query("DELETE FROM preset_buttonmap_cross_ref WHERE configId = :presetId AND mappingId = :buttonMapId")
    suspend fun deleteLinkBetweenPresetAndButtonMap(presetId: Long, buttonMapId: Long)


    // --- Query (Accessor) Operations ---

    /**
     * Retrieves a single controller with all its presets and fixed button maps.
     * The @Transaction annotation ensures this complex query is performed atomically.
     * @param controllerId The ID of the controller to fetch.
     * @return A Flow emitting the complete controller configuration.
     */
    @Transaction
    @Query("SELECT * FROM controller WHERE controllerId = :controllerId")
    fun getControllerWithDetails(controllerId: Long): Flow<ControllerWithDetails?>

    /**
     * Retrieves all presets for a specific controller, each with its list of button maps.
     * @param controllerId The ID of the parent controller.
     * @return A Flow emitting a list of presets with their associated button maps.
     */
    @Transaction
    @Query("SELECT * FROM button_presets WHERE controllerId = :controllerId")
    fun getPresetsForController(controllerId: Long): Flow<List<PresetWithButtonMaps>>
    
    /**
     * Retrieves all controllers without their detailed relationships.
     * @return A Flow emitting the list of all controllers.
     */
    @Query("SELECT * FROM controller")
    fun getAllControllers(): Flow<List<Controller>>
}