package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.*
import com.examples.testros2jsbridge.data.local.database.entities.GeometryMessageEntity

@Dao
interface GeometryMessageDao {
    @Query("SELECT * FROM geometry_messages")
    suspend fun getAll(): List<GeometryMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: GeometryMessageEntity): Long

    @Delete
    suspend fun delete(message: GeometryMessageEntity)

    @Query("DELETE FROM geometry_messages")
    suspend fun deleteAll()
}
