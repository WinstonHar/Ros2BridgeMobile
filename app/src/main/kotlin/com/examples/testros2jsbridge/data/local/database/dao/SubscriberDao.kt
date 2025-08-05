package com.examples.testros2jsbridge.data.local.database.dao

import com.examples.testros2jsbridge.domain.model.Subscriber
import com.examples.testros2jsbridge.domain.model.RosId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory DAO for Subscriber management.
 */
class SubscriberDao {
    private val _subscribers = MutableStateFlow<List<Subscriber>>(emptyList())
    val subscribers: StateFlow<List<Subscriber>> get() = _subscribers

    private val subscriberMap = mutableMapOf<RosId, Subscriber>()

    fun saveSubscriber(subscriber: Subscriber) {
        subscriber.topic.let { subscriberMap[it] = subscriber }
        _subscribers.value = subscriberMap.values.toList()
    }

    fun getSubscriber(topicId: RosId): Subscriber? {
        return subscriberMap[topicId]
    }

    fun deleteSubscriber(topicId: RosId) {
        subscriberMap.remove(topicId)
        _subscribers.value = subscriberMap.values.toList()
    }

    fun clearSubscribers() {
        subscriberMap.clear()
        _subscribers.value = emptyList()
    }
}