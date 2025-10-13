package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.domain.model.Subscriber
import com.examples.testros2jsbridge.domain.repository.SubscriberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriberRepositoryImpl @Inject constructor() : SubscriberRepository {
    private val _subscribers = MutableStateFlow<List<Subscriber>>(emptyList())

    override fun getSubscribers(): Flow<List<Subscriber>> = _subscribers

    override suspend fun saveSubscriber(subscriber: Subscriber) {
        val currentList = _subscribers.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == subscriber.id }
        if (index >= 0) {
            currentList[index] = subscriber
        } else {
            currentList.add(subscriber)
        }
        _subscribers.value = currentList
    }

    override suspend fun deleteSubscriber(topic: String) {
        _subscribers.value = _subscribers.value.filterNot { it.topic.value == topic }
    }

    override suspend fun subscribeToTopic(topic: String, type: String, label: String?, onMessage: (String) -> Unit) {
        // TODO: Implement subscription logic
    }

    override suspend fun unsubscribeFromTopic(topic: String) {
        deleteSubscriber(topic)
    }
}
