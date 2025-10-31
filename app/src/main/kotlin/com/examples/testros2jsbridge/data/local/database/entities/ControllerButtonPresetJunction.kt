package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "controller_button_preset_junction",
    primaryKeys = ["controllerId", "presetId"],
    foreignKeys = [
        ForeignKey(
            entity = ControllerEntity::class,
            parentColumns = ["controllerId"],
            childColumns = ["controllerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ButtonPresetsEntity::class,
            parentColumns = ["presetId"],
            childColumns = ["presetId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ControllerButtonPresetJunction(
    val controllerId: Int,
    val presetId: Int
)