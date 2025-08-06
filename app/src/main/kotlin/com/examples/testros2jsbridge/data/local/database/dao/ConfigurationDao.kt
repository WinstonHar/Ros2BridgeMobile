package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.examples.testros2jsbridge.data.local.database.entities.ConfigurationEntity

/**
 * Centralized config DAO for managing AppConfiguration.
 */
@Dao
abstract class ConfigurationDao {
    @Insert
    abstract suspend fun insertConfiguration(config: ConfigurationEntity)

    @Update
    abstract suspend fun updateConfiguration(config: ConfigurationEntity)

    @Query("SELECT * FROM configuration LIMIT 1")
    abstract suspend fun getConfiguration(): ConfigurationEntity?

    @Query("DELETE FROM configuration")
    abstract suspend fun clearConfiguration()
}