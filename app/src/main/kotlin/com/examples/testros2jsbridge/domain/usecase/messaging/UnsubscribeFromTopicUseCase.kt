package com.examples.testros2jsbridge.domain.usecase.messaging

import com.examples.testros2jsbridge.domain.repository.RosTopicRepository
import javax.inject.Inject

class UnsubscribeFromTopicUseCase @Inject constructor(private val rosTopicRepository: RosTopicRepository) {

    suspend fun unsubscribe(topic: String) {
        rosTopicRepository.unsubscribe(topic)
    }
}