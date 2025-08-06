package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.local.database.dao.PublisherDao
import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.repository.PublisherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class PublisherRepositoryImpl @Inject constructor(
    private val publisherDao: PublisherDao
) : PublisherRepository {
    private val _publishers = MutableStateFlow<List<Publisher>>(emptyList())
    override val publishers: StateFlow<List<Publisher>> get() = _publishers


    override suspend fun savePublisher(publisher: Publisher) {
        publisherDao.savePublisher(publisher)
        _publishers.value = publisherDao.publishers.value
    }


    override suspend fun getPublisher(publisherId: RosId): Publisher? {
        return publisherDao.getPublisher(publisherId)
    }


    override suspend fun deletePublisher(publisherId: RosId) {
        publisherDao.deletePublisher(publisherId)
        _publishers.value = publisherDao.publishers.value
    }


    override suspend fun createPublisher(publisher: Publisher): Publisher {
        publisherDao.savePublisher(publisher)
        _publishers.value = publisherDao.publishers.value
        return publisher
    }

}
