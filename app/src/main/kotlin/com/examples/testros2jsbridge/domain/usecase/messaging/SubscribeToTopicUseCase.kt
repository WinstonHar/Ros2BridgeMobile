package com.examples.testros2jsbridge.domain.usecase.messaging

import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.model.RosTopic
import com.examples.testros2jsbridge.domain.repository.RosTopicRepository

class SubscribeToTopicUseCase(
    private val rosTopicRepository: RosTopicRepository
) {
    suspend fun subscribe(topic: RosTopic) {
        rosTopicRepository.subscribe(topic)
    }
}