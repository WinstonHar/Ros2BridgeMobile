package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "button_presets",
    foreignKeys = [
        ForeignKey(
            entity = ControllerEntity::class,
            parentColumns = ["controllerId"],
            childColumns = ["controllerOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ButtonPresetsEntity(
    @PrimaryKey(autoGenerate = true)
    val presetId: Int = 0,
    val controllerOwnerId: Int,
    val name: String
)
