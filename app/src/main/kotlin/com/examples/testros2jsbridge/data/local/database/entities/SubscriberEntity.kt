package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscribers")
data class SubscriberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val topic: String,
    val type: String,
    val isActive: Boolean = false,
    val lastMessage: String? = null,
    val label: String,
    val isEnabled: Boolean = true,
    val group: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
