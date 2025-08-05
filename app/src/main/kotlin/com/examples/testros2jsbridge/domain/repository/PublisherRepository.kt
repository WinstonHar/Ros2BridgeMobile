package com.examples.testros2jsbridge.domain.repository

import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.domain.model.RosId
import kotlinx.coroutines.flow.Flow

interface PublisherRepository : com.examples.testros2jsbridge.core.base.RosRepository {
    val publishers: Flow<Publisher>

    suspend fun savePublisher(publisher: Publisher)
    suspend fun getPublisher(publisherId: RosId): Publisher?
    suspend fun deletePublisher(publisherId: RosId)
    suspend fun createPublisher(publisher: Publisher): Publisher
}