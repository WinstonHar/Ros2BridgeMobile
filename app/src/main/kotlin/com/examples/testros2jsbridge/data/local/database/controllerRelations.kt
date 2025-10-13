package com.examples.testros2jsbridge.data.local.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.examples.testros2jsbridge.data.local.database.entities.ButtonMap
import com.examples.testros2jsbridge.data.local.database.entities.ButtonPresets
import com.examples.testros2jsbridge.data.local.database.entities.Controller
import com.examples.testros2jsbridge.data.local.database.entities.PresetButtonMapCrossRef

/**
 * Represents a single ButtonPreset and its associated list of ButtonMaps.
 * This is used to fetch a complete preset configuration.
 */
data class PresetWithButtonMaps(
    @Embedded val preset: ButtonPresets,
    @Relation(
        parentColumn = "configId",
        entityColumn = "mappingId",
        associateBy = Junction(PresetButtonMapCrossRef::class)
    )
    val buttonMaps: List<ButtonMap>
)

/**
 * Represents a complete Controller configuration.
 * It includes the controller itself, all its presets (each with their button maps),
 * and all of its fixed button maps.
 */
data class ControllerWithDetails(
    @Embedded val controller: Controller,

    // Fetches all presets owned by the controller, each populated with its button maps
    @Relation(
        entity = ButtonPresets::class,
        parentColumn = "controllerId",
        entityColumn = "controllerId"
    )
    val presets: List<PresetWithButtonMaps>,

    // Fetches only the button maps directly assigned to the controller (fixed maps)
    @Relation(
        parentColumn = "controllerId",
        entityColumn = "controllerId"
    )
    val fixedButtonMaps: List<ButtonMap>
)