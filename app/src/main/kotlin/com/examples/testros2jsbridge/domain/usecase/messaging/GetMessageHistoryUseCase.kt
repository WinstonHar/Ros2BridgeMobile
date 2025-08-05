package com.examples.testros2jsbridge.domain.usecase.messaging

import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository

class GetMessageHistoryUseCase(private val rosMessageRepository: RosMessageRepository) {

    suspend fun getMessages(topicId: RosId): List<RosMessage> {
        return rosMessageRepository.getMessagesByTopic(topicId)
    }
}