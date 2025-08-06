package com.examples.testros2jsbridge.domain.usecase.publisher

import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.domain.repository.PublisherRepository
import javax.inject.Inject

class CreatePublisherUseCase @Inject constructor(private val publisherRepository: PublisherRepository) {

    suspend fun create(publisher: Publisher) {
        publisherRepository.createPublisher(publisher)
    }
}