package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "publishers")
data class PublisherEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val topic: String,
    val type: String,
    val label: String,
    val isEnabled: Boolean = true,
    val group: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
