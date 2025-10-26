package com.examples.testros2jsbridge.data.mapper

import com.examples.testros2jsbridge.data.local.database.RosProtocolType
import com.examples.testros2jsbridge.data.local.database.entities.AppActionEntity
import com.examples.testros2jsbridge.domain.model.AppAction

fun AppAction.toEntity(): AppActionEntity {
    return AppActionEntity(
        appActionId = this.id,
        displayName = this.displayName,
        rosTopic = this.topic,
        rosMessageType = this.type,
        messageJsonTemplate = this.msg,
        rosProtocolType = RosProtocolType.ACTION_CLIENT,
        protocolPackageName = null,
        protocolName = null
    )
}

fun AppActionEntity.toDomain(): AppAction {
    return AppAction(
        id = this.appActionId,
        displayName = this.displayName,
        topic = this.rosTopic,
        type = this.rosMessageType,
        source = "database",
        msg = this.messageJsonTemplate
    )
}
