package com.examples.testros2jsbridge.data.remote.protocol

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.model.RosId

class MessageSerializer : Serializer<RosMessage>() {
    override fun write(kryo: Kryo, output: Output, message: RosMessage) {
        output.writeString(message.id ?: "")
        output.writeString(message.topic.id)
        output.writeString(message.type)
        output.writeString(message.content)
        output.writeLong(message.timestamp)
        output.writeString(message.label ?: "")
        output.writeString(message.sender ?: "")
        output.writeBoolean(message.isPublished)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<RosMessage>): RosMessage {
        val id = input.readString().ifEmpty { null }
        val topicId = input.readString()
        val typeStr = input.readString()
        val contentStr = input.readString()
        val timestamp = input.readLong()
        val label = input.readString().ifEmpty { null }
        val sender = input.readString().ifEmpty { null }
        val isPublished = input.readBoolean()
        return RosMessage(
            id = id,
            topic = RosId(topicId),
            type = typeStr,
            content = contentStr,
            timestamp = timestamp,
            label = label,
            sender = sender,
            isPublished = isPublished
        )
    }
}