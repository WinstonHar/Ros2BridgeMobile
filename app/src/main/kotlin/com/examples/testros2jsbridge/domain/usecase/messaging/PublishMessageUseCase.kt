package com.examples.testros2jsbridge.domain.usecase.messaging

import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository

class PublishMessageUseCase(private val rosMessageRepository: RosMessageRepository) {

    suspend fun publish(message: RosMessage) {
        rosMessageRepository.publishMessage(message)
    }
}