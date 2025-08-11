package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geometry_messages")
data class GeometryMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String, // Unique string ID for matching edits
    val label: String?,
    val topic: String,
    val type: String,
    val content: String?,
    val timestamp: Long?
)
