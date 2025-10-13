package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val host: String,
    val port: Int,
    val isActive: Boolean = false,
    val lastConnected: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)
