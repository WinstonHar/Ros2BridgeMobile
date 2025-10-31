package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "controller_button_fixed_map_junction",
    primaryKeys = ["controllerId", "buttonMapId"],
    foreignKeys = [
        ForeignKey(
            entity = ControllerEntity::class,
            parentColumns = ["controllerId"],
            childColumns = ["controllerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ButtonMapEntity::class,
            parentColumns = ["buttonMapId"],
            childColumns = ["buttonMapId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ControllerButtonFixedMapJunction(
    val controllerId: Int,
    val buttonMapId: Int
)