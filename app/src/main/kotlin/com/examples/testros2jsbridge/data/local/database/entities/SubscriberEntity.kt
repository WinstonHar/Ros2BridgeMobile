package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriber")
data class SubscriberEntity(
    @PrimaryKey val id: String,
    val topic: String,
    val type: String,
    val isActive: Boolean,
    val lastMessage: String?,
    val label: String?,
    val isEnabled: Boolean,
    val group: String?,
    val timestamp: Long
)