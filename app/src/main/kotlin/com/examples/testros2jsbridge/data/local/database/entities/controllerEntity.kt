package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "controller")
data class Controller(
    @PrimaryKey(autoGenerate = true)
    val controllerId: Long = 0,
    val controllerName: String // e.g., "Logitech F310"
)