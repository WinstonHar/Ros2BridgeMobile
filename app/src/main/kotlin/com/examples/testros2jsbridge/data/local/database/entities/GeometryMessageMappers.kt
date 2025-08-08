package com.examples.testros2jsbridge.data.local.database.entities

import com.examples.testros2jsbridge.data.local.database.entities.GeometryMessageEntity
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.domain.model.RosId

fun GeometryMessageEntity.toDto(): RosMessageDto = RosMessageDto(
    op = "publish", // or whatever is appropriate
    topic = RosId(this.topic),
    type = this.type,
    label = this.label,
    timestamp = this.timestamp,
    content = this.content
)

fun RosMessageDto.toEntity(): GeometryMessageEntity = GeometryMessageEntity(
    id = 0, // Let Room auto-generate
    label = this.label,
    topic = this.topic.value,
    type = this.type ?: "",
    content = this.content,
    timestamp = this.timestamp
)
