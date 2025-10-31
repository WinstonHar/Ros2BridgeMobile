package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "controllers")
data class ControllerEntity(
    @PrimaryKey(autoGenerate = true)
    val controllerId: Int = 0,
    val name: String
)