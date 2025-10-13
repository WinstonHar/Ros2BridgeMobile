package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.examples.testros2jsbridge.data.local.database.InputType

@Entity(
    tableName = "button_map",
    foreignKeys = [
        //AppAction forigen key cannot be used as in different DB, this is a limitation with Room DB SQlite interfacing
        /*
        ForeignKey(
            entity = AppAction::class,
            parentColumns = ["appActionId"],
            childColumns = ["mappedActionId"],
            onDelete = ForeignKey.CASCADE
        ), */
        ForeignKey(
            entity = Controller::class,
            parentColumns = ["controllerId"],
            childColumns = ["controllerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("mappedActionId"),
        Index("controllerId")
    ]
)
data class ButtonMap(
    @PrimaryKey(autoGenerate = true)
    val mappingId: Long = 0,

    // App action ID, not enforced, make sure is legal
    val mappedActionId: Long,

    // Foreign key to the Controller (for fixed maps)
    // This can be null if the map only exists inside a preset and is not a "fixed" map.
    val controllerId: Long?,

    val inputType: InputType,
    val joystickDeadzone: Float?,
    val joystickSensitivity: Float?
)