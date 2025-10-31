package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "preset_button_map_junction",
    primaryKeys = ["presetId", "buttonMapId"],
    foreignKeys = [
        ForeignKey(
            entity = ButtonPresetsEntity::class,
            parentColumns = ["presetId"],
            childColumns = ["presetId"],
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
data class PresetButtonMapJunction(
    val presetId: Int,
    val buttonMapId: Int
)