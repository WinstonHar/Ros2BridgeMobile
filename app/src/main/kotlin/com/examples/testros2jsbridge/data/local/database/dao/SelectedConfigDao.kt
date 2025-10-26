package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.examples.testros2jsbridge.data.local.database.entities.SelectedConfig

@Dao
interface SelectedConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSelectedConfig(selectedConfig: SelectedConfig)

    @Query("SELECT * FROM selected_config WHERE id = 1")
    suspend fun getSelectedConfig(): SelectedConfig?
}
