package com.examples.testros2jsbridge.domain.repository

import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.domain.model.RosId
import kotlinx.coroutines.flow.StateFlow

interface PublisherRepository {
    val publishers: StateFlow<List<Publisher>>

    suspend fun savePublisher(publisher: Publisher)
    suspend fun getPublisher(publisherId: RosId): Publisher?
    suspend fun deletePublisher(publisherId: RosId)
    suspend fun createPublisher(publisher: Publisher): Publisher
}