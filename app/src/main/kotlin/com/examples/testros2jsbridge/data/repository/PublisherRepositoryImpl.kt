package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.local.database.dao.PublisherDao
import com.examples.testros2jsbridge.data.local.database.entities.PublisherEntity
import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.repository.PublisherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PublisherRepositoryImpl @Inject constructor(
    private val publisherDao: PublisherDao
) : PublisherRepository {

    private val _publishers = MutableStateFlow<List<Publisher>>(emptyList())
    override val publishers: StateFlow<List<Publisher>> = _publishers.asStateFlow()

    init {
        // Initialize by loading from database
        // This should be called from a coroutine scope
    }

    suspend fun initialize() {
        val entities = publisherDao.getAllPublishers()
        _publishers.value = entities.map { it.toPublisher() }
    }

    override suspend fun savePublisher(publisher: Publisher) {
        val entity = publisher.toEntity()
        publisherDao.insertPublisher(entity)
        refreshPublishers()
    }

    override suspend fun getPublisher(publisherId: RosId): Publisher? {
        val id = publisherId.value.toIntOrNull() ?: return null
        val entity = publisherDao.getPublisherById(id)
        return entity?.toPublisher()
    }

    override suspend fun deletePublisher(publisherId: RosId) {
        val id = publisherId.value.toIntOrNull() ?: return
        publisherDao.deletePublisherById(id)
        refreshPublishers()
    }

    override suspend fun createPublisher(publisher: Publisher): Publisher {
        val entity = publisher.toEntity()
        val newId = publisherDao.insertPublisher(entity)
        refreshPublishers()
        return publisher.copy(id = RosId(newId.toString()))
    }

    private suspend fun refreshPublishers() {
        val entities = publisherDao.getAllPublishers()
        _publishers.value = entities.map { it.toPublisher() }
    }

    private fun PublisherEntity.toPublisher(): Publisher {
        return Publisher(
            id = RosId(this.id.toString()),
            topic = RosId(this.topic),
            messageType = this.type,
            msgType = this.type,
            message = "",
            label = this.label,
            isEnabled = this.isEnabled,
            lastPublishedTimestamp = this.timestamp,
            presetGroup = this.group
        )
    }

    private fun Publisher.toEntity(): PublisherEntity {
        return PublisherEntity(
            id = this.id?.value?.toIntOrNull() ?: 0,
            topic = this.topic.value,
            type = this.messageType,
            label = this.label ?: "",
            isEnabled = this.isEnabled,
            group = this.presetGroup,
            timestamp = this.lastPublishedTimestamp ?: System.currentTimeMillis()
        )
    }
}
