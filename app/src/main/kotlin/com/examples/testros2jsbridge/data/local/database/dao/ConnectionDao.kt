package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.examples.testros2jsbridge.data.local.database.entities.ConnectionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory DAO for RosConnection objects. Replace with Room/DB implementation as needed.
 */
import com.examples.testros2jsbridge.data.local.database.entities.ConfigurationEntity

@Dao
abstract class ConnectionDao {
    // Room-compatible methods
    @Insert
    abstract suspend fun insertConnection(connection: ConnectionEntity)

    @Update
    abstract suspend fun updateConnection(connection: ConnectionEntity)

    @Query("SELECT * FROM connection WHERE id = :connectionId LIMIT 1")
    abstract suspend fun getConnectionById(connectionId: String): ConnectionEntity?

    @Query("DELETE FROM connection WHERE id = :connectionId")
    abstract suspend fun deleteConnectionById(connectionId: String)

    @Query("DELETE FROM connection")
    abstract suspend fun clearConnections()

    // In-memory fallback for non-persistent logic (optional, for testing/migration)
    private val _connections = MutableStateFlow<List<ConnectionEntity>>(emptyList())
    val connections: StateFlow<List<ConnectionEntity>> get() = _connections

    fun saveConnection(connection: ConnectionEntity) {
        _connections.value = _connections.value.filter { it.id != connection.id } + connection
    }

    fun getConnection(connectionId: String): ConnectionEntity? {
        return _connections.value.find { it.id == connectionId }
    }

    fun deleteConnection(connectionId: String) {
        _connections.value = _connections.value.filterNot { it.id == connectionId }
    }

    fun updateStatus(connectionId: String, isConnected: Boolean) {
        _connections.value = _connections.value.map {
            if (it.id == connectionId) it.copy(isConnected = isConnected) else it
        }
    }
}

