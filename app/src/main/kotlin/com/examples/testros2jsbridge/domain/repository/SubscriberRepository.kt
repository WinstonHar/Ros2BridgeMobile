package com.examples.testros2jsbridge.domain.repository

import com.examples.testros2jsbridge.domain.model.Subscriber
import kotlinx.coroutines.flow.Flow

interface SubscriberRepository {
    fun getSubscribers(): Flow<List<Subscriber>>
    suspend fun saveSubscriber(subscriber: Subscriber)
    suspend fun deleteSubscriber(topic: String)
    suspend fun subscribeToTopic(topic: String, type: String, label: String?, onMessage: (String) -> Unit)
    suspend fun unsubscribeFromTopic(topic: String)
}
