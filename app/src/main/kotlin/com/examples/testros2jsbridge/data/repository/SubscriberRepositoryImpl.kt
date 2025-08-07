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

    // Map SubscriberEntity to Subscriber
    private fun entityToDomain(entity: com.examples.testros2jsbridge.data.local.database.entities.SubscriberEntity): Subscriber = Subscriber(
        id = entity.id,
        topic = RosId(entity.topic),
        type = entity.type,
        isActive = entity.isActive,
        lastMessage = entity.lastMessage,
        label = entity.label,
        isEnabled = entity.isEnabled,
        group = entity.group,
        timestamp = entity.timestamp,
        messageHistory = emptyList<String>()
    )

    // Map Subscriber to SubscriberEntity
    private fun domainToEntity(subscriber: Subscriber): com.examples.testros2jsbridge.data.local.database.entities.SubscriberEntity =
        com.examples.testros2jsbridge.data.local.database.entities.SubscriberEntity(
            id = subscriber.id ?: java.util.UUID.randomUUID().toString(),
            topic = subscriber.topic.value,
            type = subscriber.type,
            isActive = subscriber.isActive,
            lastMessage = subscriber.lastMessage,
            label = subscriber.label,
            isEnabled = subscriber.isEnabled,
            group = subscriber.group,
            timestamp = subscriber.timestamp
        )

    override fun getSubscribers(): Flow<List<Subscriber>> =
        subscriberDao.getAllSubscribersFlow().map { list -> list.map { entityToDomain(it) } }

    override suspend fun saveSubscriber(subscriber: Subscriber) {
        val entity = domainToEntity(subscriber)
        val existing = subscriberDao.getSubscriberByTopic(entity.topic)
        if (existing == null) {
            subscriberDao.insertSubscriber(entity)
        } else {
            subscriberDao.updateSubscriber(entity)
        }
    }

    override suspend fun deleteSubscriber(topic: String) {
        subscriberDao.deleteSubscriberByTopic(topic)
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
        saveSubscriber(subscriber)
    }

    override suspend fun unsubscribeFromTopic(topic: String) {
        rosbridgeClient.unsubscribe(topic)
        deleteSubscriber(topic)
    }
}
