package com.examples.testros2jsbridge.domain.usecase.messaging

import com.examples.testros2jsbridge.domain.repository.RosTopicRepository

class UnsubscribeFromTopicUseCase(private val rosTopicRepository: RosTopicRepository) {

    suspend fun unsubscribe(topic: String) {
        rosTopicRepository.unsubscribe(topic)
    }
}