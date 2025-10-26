package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.examples.testros2jsbridge.data.local.database.entities.AppActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppActionDao {

    /**
     * Inserts a new AppAction. If an action with the same primary key already exists,
     * it will be replaced.
     * @param appAction The action to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppAction(appAction: AppActionEntity)

    /**
     * Updates an existing AppAction.
     * @param appAction The action to update.
     */
    @Update
    suspend fun updateAppAction(appAction: AppActionEntity)

    /**
     * Deletes an AppAction from the database.
     * @param appAction The action to delete.
     */
    @Delete
    suspend fun deleteAppAction(appAction: AppActionEntity)

    /**
     * Retrieves a single AppAction by its ID.
     * @param actionId The ID of the action to retrieve.
     * @return A Flow emitting the AppAction, or null if not found.
     */
    @Query("SELECT * FROM app_action WHERE appActionId = :actionId")
    fun getAppActionById(actionId: String): Flow<AppActionEntity?>

    /**
     * Retrieves all AppActions from the database, ordered by their display name.
     * @return A Flow emitting the list of all actions.
     */
    @Query("SELECT * FROM app_action ORDER BY displayName ASC")
    fun getAllAppActions(): Flow<List<AppActionEntity>>
}