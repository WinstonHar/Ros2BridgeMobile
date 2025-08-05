package com.examples.testros2jsbridge.data.local.database.dao

import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.domain.model.RosId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory DAO for Publisher management.
 */
class PublisherDao {
    private val _publishers = MutableStateFlow<List<Publisher>>(emptyList())
    val publishers: StateFlow<List<Publisher>> get() = _publishers

    private val publisherMap = mutableMapOf<RosId, Publisher>()

    fun savePublisher(publisher: Publisher) {
        publisher.id?.let { publisherMap[it] = publisher }
        _publishers.value = publisherMap.values.toList()
    }

    fun getPublisher(publisherId: RosId): Publisher? {
        return publisherMap[publisherId]
    }

    fun deletePublisher(publisherId: RosId) {
        publisherMap.remove(publisherId)
        _publishers.value = publisherMap.values.toList()
    }

    fun clearPublishers() {
        publisherMap.clear()
        _publishers.value = emptyList()
    }
}