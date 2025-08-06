package com.examples.testros2jsbridge.domain.usecase.messaging

import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository
import javax.inject.Inject

class PublishMessageUseCase @Inject constructor(private val rosMessageRepository: RosMessageRepository) {

    suspend fun publish(message: RosMessage) {
        rosMessageRepository.publishMessage(message)
    }
}