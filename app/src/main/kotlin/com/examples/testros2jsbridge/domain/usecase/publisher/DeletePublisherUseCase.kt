package com.examples.testros2jsbridge.domain.usecase.publisher

import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.repository.PublisherRepository
import javax.inject.Inject

class DeletePublisherUseCase @Inject constructor(private val publisherRepository: PublisherRepository) {

    suspend fun delete(publisherId: RosId) {
        publisherRepository.deletePublisher(publisherId)
    }
}