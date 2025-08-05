package com.examples.testros2jsbridge.domain.usecase.publisher

import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.domain.repository.PublisherRepository

class GetPublisherUseCase(private val publisherRepository: PublisherRepository) {

    suspend fun get(publisherId: RosId): Publisher? {
        return publisherRepository.getPublisher(publisherId)
    }
}