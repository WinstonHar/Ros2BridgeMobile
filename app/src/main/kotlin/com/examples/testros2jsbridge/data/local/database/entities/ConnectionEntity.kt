package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connection")
data class ConnectionEntity(
    @PrimaryKey val id: String,
    val ipAddress: String,
    val port: Int,
    val protocol: String = "ws",
    val isConnected: Boolean = false,
    val lastError: String? = null,
    val authToken: String? = null,
    val lastReconnectAttempt: Long? = null,
    val messageCount: Int = 0,
    val serverName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
