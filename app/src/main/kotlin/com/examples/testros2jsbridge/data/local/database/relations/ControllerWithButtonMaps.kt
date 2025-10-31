package com.examples.testros2jsbridge.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.examples.testros2jsbridge.data.local.database.entities.ButtonMapEntity
import com.examples.testros2jsbridge.data.local.database.entities.ControllerButtonFixedMapJunction
import com.examples.testros2jsbridge.data.local.database.entities.ControllerEntity

data class ControllerWithButtonMaps(
    @Embedded val controller: ControllerEntity,
    @Relation(
        parentColumn = "controllerId",
        entityColumn = "buttonMapId",
        associateBy = Junction(ControllerButtonFixedMapJunction::class)
    )
    val buttonMaps: List<ButtonMapEntity>
)