package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "button_maps",
    foreignKeys = [
        ForeignKey(
            entity = AppActionEntity::class,
            parentColumns = ["appActionId"],
            childColumns = ["mappedActionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ButtonMapEntity(
    @PrimaryKey(autoGenerate = true)
    val buttonMapId: Int = 0,
    val inputType: String, // e.g., "Button A", "Joystick Left"
    val mappedActionId: String,
    val joystickDeadzone: Float? = null,
    val joystickSensitivity: Float? = null
)
