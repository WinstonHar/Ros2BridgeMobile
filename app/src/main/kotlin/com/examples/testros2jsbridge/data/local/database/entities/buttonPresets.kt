package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single preset configuration that belongs to a controller.
 */
@Entity(
    tableName = "button_presets",
    foreignKeys = [
        ForeignKey(
            entity = Controller::class,
            parentColumns = ["controllerId"],
            childColumns = ["controllerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("controllerId")]
)
data class ButtonPresets(
    @PrimaryKey(autoGenerate = true)
    val configId: Long = 0,
    val configName: String,
    val isActiveConfig: Boolean = false,

    // Foreign key to the owning Controller
    val controllerId: Long
)

/**
 * Cross-reference table to create the many-to-many relationship
 * between a ButtonPreset and its assigned ButtonMaps.
 */
@Entity(
    tableName = "preset_buttonmap_cross_ref",
    primaryKeys = ["configId", "mappingId"],
    foreignKeys = [
        ForeignKey(
            entity = ButtonPresets::class,
            parentColumns = ["configId"],
            childColumns = ["configId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ButtonMap::class,
            parentColumns = ["mappingId"],
            childColumns = ["mappingId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PresetButtonMapCrossRef(
    val configId: Long,
    val mappingId: Long
)