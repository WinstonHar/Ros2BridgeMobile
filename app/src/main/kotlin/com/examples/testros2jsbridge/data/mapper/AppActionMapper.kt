package com.examples.testros2jsbridge.data.mapper

import com.examples.testros2jsbridge.data.local.database.RosProtocolType
import com.examples.testros2jsbridge.data.local.database.entities.AppActionEntity
import com.examples.testros2jsbridge.domain.model.AppAction

fun AppAction.toEntity(): AppActionEntity {
    val (protocolPackageName, protocolName) = this.type.split("/").let {
        val protocolName = it.getOrNull(2) ?: ""
        val protocolPackageName = it.getOrNull(0) ?: ""
        Pair(protocolPackageName, protocolName)
    }

    val finalRosMessageType = when (RosProtocolType.valueOf(this.rosMessageType)) {
        RosProtocolType.SUBSCRIBER, RosProtocolType.PUBLISHER -> {
            "${protocolPackageName}/${protocolName}"
        }
        RosProtocolType.ACTION_CLIENT -> {
            "${protocolPackageName}/action/${protocolName}"
        }
        RosProtocolType.SERVICE_CLIENT -> {
            "${protocolPackageName}/srv/${protocolName}"
        }
        RosProtocolType.INTERNAL -> {
             this.type
        }
        else -> this.type
    }

    return AppActionEntity(
        appActionId = this.id,
        displayName = this.displayName,
        rosTopic = this.topic,
        rosMessageType = finalRosMessageType,
        messageJsonTemplate = this.msg,
        rosProtocolType = RosProtocolType.valueOf(this.rosMessageType),
        protocolPackageName = protocolPackageName,
        protocolName = protocolName
    )
}

fun AppActionEntity.toDomain(): AppAction {
    return AppAction(
        id = this.appActionId,
        displayName = this.displayName,
        topic = this.rosTopic,
        type = this.rosMessageType,
        source = "database",
        msg = this.messageJsonTemplate,
        rosMessageType = this.rosProtocolType.name
    )
}
