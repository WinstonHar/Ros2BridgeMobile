package com.examples.testros2jsbridge.data.local.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.examples.testros2jsbridge.data.local.database.entities.ButtonMapEntity
import com.examples.testros2jsbridge.data.local.database.entities.ButtonPresetsEntity
import com.examples.testros2jsbridge.data.local.database.entities.ControllerButtonFixedMapJunction
import com.examples.testros2jsbridge.data.local.database.entities.ControllerEntity
import com.examples.testros2jsbridge.data.local.database.entities.PresetButtonMapJunction

data class PresetWithButtonMaps(
    @Embedded val preset: ButtonPresetsEntity,
    @Relation(
        parentColumn = "presetId",
        entityColumn = "buttonMapId",
        associateBy = Junction(PresetButtonMapJunction::class)
    )
    val buttonMaps: List<ButtonMapEntity>
)

data class ControllerWithDetails(
    @Embedded val controller: ControllerEntity,
    @Relation(
        entity = ButtonPresetsEntity::class,
        parentColumn = "controllerId",
        entityColumn = "controllerOwnerId"
    )
    val presets: List<PresetWithButtonMaps>,
    @Relation(
        parentColumn = "controllerId",
        entityColumn = "buttonMapId",
        associateBy = Junction(ControllerButtonFixedMapJunction::class)
    )
    val fixedButtonMaps: List<ButtonMapEntity>
)
