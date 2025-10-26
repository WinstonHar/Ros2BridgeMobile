package com.examples.testros2jsbridge.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.examples.testros2jsbridge.domain.model.JoystickMapping

@Entity(tableName = "controller_configs")
data class ControllerConfigEntity(
    @PrimaryKey
    val name: String,
    val addressingMode: String,
    val sensitivity: Float,
    val invertYAxis: Boolean,
    val deadZone: Float,
    val joystickPublishRate: Int,
    val buttonAssignments: Map<String, String> = emptyMap(),
    val joystickMappings: List<JoystickMapping> = emptyList()
)
