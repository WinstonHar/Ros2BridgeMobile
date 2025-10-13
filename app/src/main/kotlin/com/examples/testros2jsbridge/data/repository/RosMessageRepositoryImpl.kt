package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RosMessageRepositoryImpl @Inject constructor() : RosMessageRepository {
    override val messages = MutableStateFlow<List<RosMessageDto>>(emptyList())

    override suspend fun deleteMessage(message: RosMessageDto) {
        messages.value = messages.value.filterNot { it == message }
    }

    override suspend fun saveMessage(message: RosMessageDto) {
        messages.value = messages.value + message
    }

    override suspend fun getMessage(messageId: String): RosMessageDto? {
        return messages.value.find { it.id == messageId }
    }

    override suspend fun getMessagesByTopic(topic: RosId): List<RosMessageDto> {
        return messages.value.filter { it.topic.value == topic.value }
    }

    override fun publishMessage(message: RosMessage) {
        // TODO: Implement publishing logic
    }

    override fun clearCustomMessage() {
        // TODO: Implement custom message clearing
    }

    override fun onCustomMessageChange(newMessage: String) {
        // TODO: Implement custom message change handling
    }

    override fun updateConnectionStatus(status: String) {
        // TODO: Implement connection status update
    }
}
