package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geometry_messages")
data class GeometryMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val topic: String,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
