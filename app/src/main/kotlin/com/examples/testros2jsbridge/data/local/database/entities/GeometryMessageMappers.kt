package com.examples.testros2jsbridge.data.local.database.entities

import com.examples.testros2jsbridge.data.local.database.entities.GeometryMessageEntity
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.domain.model.RosId


import java.util.UUID

fun GeometryMessageEntity.toDto(): RosMessageDto = RosMessageDto(
    op = "publish",
    topic = RosId(this.topic),
    type = this.type,
    label = this.label,
    timestamp = this.timestamp,
    content = this.content,
    id = this.uuid // Map uuid to id
)

fun RosMessageDto.toEntity(): GeometryMessageEntity = GeometryMessageEntity(
    id = 0, // Let Room auto-generate
    uuid = this.id ?: UUID.randomUUID().toString(),
    label = this.label,
    topic = this.topic.value,
    type = this.type ?: "",
    content = this.content,
    timestamp = this.timestamp
)
