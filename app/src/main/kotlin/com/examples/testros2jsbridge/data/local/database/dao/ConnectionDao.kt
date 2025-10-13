package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.*
import com.examples.testros2jsbridge.data.local.database.entities.ConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {
    @Query("SELECT * FROM connections")
    fun getAllConnectionsFlow(): Flow<List<ConnectionEntity>>

    @Query("SELECT * FROM connections")
    suspend fun getAllConnections(): List<ConnectionEntity>

    @Query("SELECT * FROM connections WHERE id = :id")
    suspend fun getConnectionById(id: Int): ConnectionEntity?

    @Query("SELECT * FROM connections WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveConnection(): ConnectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: ConnectionEntity): Long

    @Update
    suspend fun updateConnection(connection: ConnectionEntity)

    @Delete
    suspend fun deleteConnection(connection: ConnectionEntity)

    @Query("DELETE FROM connections WHERE id = :id")
    suspend fun deleteConnectionById(id: Int)

    @Query("DELETE FROM connections")
    suspend fun deleteAllConnections()
}
