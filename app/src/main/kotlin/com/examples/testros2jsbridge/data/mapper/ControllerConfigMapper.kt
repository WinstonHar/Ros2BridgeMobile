package com.examples.testros2jsbridge.data.mapper

import com.examples.testros2jsbridge.data.local.database.entities.ControllerConfigEntity
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.model.JoystickMapping
import com.examples.testros2jsbridge.domain.model.RosId

fun ControllerConfig.toEntity(): ControllerConfigEntity {
    return ControllerConfigEntity(
        name = this.name,
        addressingMode = this.addressingMode.value,
        sensitivity = this.sensitivity,
        invertYAxis = this.invertYAxis,
        deadZone = this.deadZone,
        joystickPublishRate = this.joystickPublishRate,
        buttonAssignments = this.buttonAssignments.mapValues { it.value.id },
        joystickMappings = this.joystickMappings
    )
}

fun ControllerConfigEntity.toDomain(appActions: List<AppAction>): ControllerConfig {
    val buttonAssignments = this.buttonAssignments.mapValues { entry ->
        appActions.find { it.id == entry.value }
    }.filterValues { it != null } as Map<String, AppAction>

    return ControllerConfig(
        name = this.name,
        addressingMode = RosId(this.addressingMode),
        sensitivity = this.sensitivity,
        invertYAxis = this.invertYAxis,
        deadZone = this.deadZone,
        joystickPublishRate = this.joystickPublishRate,
        buttonAssignments = buttonAssignments,
        joystickMappings = this.joystickMappings
    )
}
