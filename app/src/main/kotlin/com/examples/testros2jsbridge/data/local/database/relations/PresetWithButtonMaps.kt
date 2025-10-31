package com.examples.testros2jsbridge.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.examples.testros2jsbridge.data.local.database.entities.ButtonMapEntity
import com.examples.testros2jsbridge.data.local.database.entities.ButtonPresetsEntity
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