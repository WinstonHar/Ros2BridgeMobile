package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.Subscriber
import com.examples.testros2jsbridge.domain.repository.SubscriberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SubscriberRepositoryImpl @Inject constructor(
    private val subscriberDao: SubscriberDao,
    private val rosbridgeClient: RosbridgeClient
) : SubscriberRepository {
    override fun getSubscribers(): Flow<List<Subscriber>> =
        subscriberDao.subscribers

    override suspend fun saveSubscriber(subscriber: Subscriber) {
        subscriberDao.saveSubscriber(subscriber)
    }

    override suspend fun deleteSubscriber(topic: String) {
        subscriberDao.deleteSubscriber(RosId(topic))
    }

    override suspend fun subscribeToTopic(topic: String, type: String, label: String?, onMessage: (String) -> Unit) {
        rosbridgeClient.subscribe(topic, type, onMessage)
        val subscriber = Subscriber(
            id = java.util.UUID.randomUUID().toString(),
            topic = RosId(topic),
            type = type,
            isActive = true,
            lastMessage = null,
            label = label,
            isEnabled = true,
            group = null,
            timestamp = System.currentTimeMillis(),
            messageHistory = emptyList<String>()
        )
        subscriberDao.saveSubscriber(subscriber)
    }

    override suspend fun unsubscribeFromTopic(topic: String) {
        rosbridgeClient.unsubscribe(topic)
        subscriberDao.deleteSubscriber(RosId(topic))
    }
}
