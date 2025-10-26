package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.examples.testros2jsbridge.data.local.database.entities.ControllerConfigEntity

@Dao
interface ControllerConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ControllerConfigEntity)

    @Query("SELECT * FROM controller_configs WHERE name = :name")
    suspend fun getControllerConfig(name: String): ControllerConfigEntity?

    @Query("SELECT * FROM controller_configs")
    suspend fun getAllControllerConfigs(): List<ControllerConfigEntity>
}
