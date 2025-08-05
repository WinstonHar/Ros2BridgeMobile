package com.examples.testros2jsbridge.domain.usecase.publisher

import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.domain.repository.PublisherRepository

class CreatePublisherUseCase(private val publisherRepository: PublisherRepository) {

    suspend fun create(publisher: Publisher) {
        publisherRepository.createPublisher(publisher)
    }
}