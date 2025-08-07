package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.local.database.dao.PublisherDao
import com.examples.testros2jsbridge.data.local.database.entities.PublisherEntity
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

    // Helper to map PublisherEntity to Publisher
    private fun entityToDomain(entity: PublisherEntity): Publisher = Publisher(
        id = RosId(entity.id),
        topic = RosId(entity.topic),
        messageType = entity.messageType,
        message = entity.message,
        label = entity.label,
        isEnabled = entity.isEnabled,
        lastPublishedTimestamp = entity.lastPublishedTimestamp,
        presetGroup = entity.presetGroup
    )

    // Helper to map Publisher to PublisherEntity
    private fun domainToEntity(publisher: Publisher): PublisherEntity = PublisherEntity(
        id = publisher.id?.value ?: publisher.topic.value, // fallback if id is null
        topic = publisher.topic.value,
        messageType = publisher.messageType,
        message = publisher.message,
        label = publisher.label,
        isEnabled = publisher.isEnabled,
        lastPublishedTimestamp = publisher.lastPublishedTimestamp,
        presetGroup = publisher.presetGroup
    )

    override suspend fun savePublisher(publisher: Publisher) {
        val entity = domainToEntity(publisher)
        // If exists, update; else, insert
        val existing = publisherDao.getPublisherById(entity.id)
        if (existing == null) {
            publisherDao.insertPublisher(entity)
        } else {
            publisherDao.updatePublisher(entity)
        }
        refreshPublishers()
    }

    override suspend fun getPublisher(publisherId: RosId): Publisher? {
        return publisherDao.getPublisherById(publisherId.value)?.let { entityToDomain(it) }
    }

    override suspend fun deletePublisher(publisherId: RosId) {
        publisherDao.deletePublisherById(publisherId.value)
        refreshPublishers()
    }

    override suspend fun createPublisher(publisher: Publisher): Publisher {
        val entity = domainToEntity(publisher)
        publisherDao.insertPublisher(entity)
        refreshPublishers()
        return publisher.copy(id = RosId(entity.id))
    }

    private suspend fun refreshPublishers() {
        _publishers.value = publisherDao.getAllPublishers().map { entityToDomain(it) }
    }
}
