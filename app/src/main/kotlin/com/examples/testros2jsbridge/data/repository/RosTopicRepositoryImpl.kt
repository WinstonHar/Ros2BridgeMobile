package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RosTopicRepositoryImpl(
    private val rosbridgeClient: RosbridgeClient
) : RosTopicRepository {

    private val _subscribedTopics = MutableStateFlow<Set<Pair<String, String>>>(emptySet())
    override val subscribedTopics: StateFlow<Set<Pair<String, String>>> get() = _subscribedTopics

    private val subscriberDao = com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao()

    override fun subscribe(topic: String, type: String) {
        // Add subscription to state and update DAO if needed
        _subscribedTopics.value = _subscribedTopics.value + (topic to type)
        // Optionally, add a new Subscriber to the DAO
        val newSubscriber = com.examples.testros2jsbridge.domain.model.Subscriber(
            topic = com.examples.testros2jsbridge.domain.model.RosId(topic),
            type = type,
            messageHistory = emptyList(),
            topicInfo = com.examples.testros2jsbridge.domain.model.RosTopic(
                name = topic,
                type = type,
                isSubscribed = true
            )
        )
        subscriberDao.saveSubscriber(newSubscriber)
    }

    override suspend fun getTopics(): List<com.examples.testros2jsbridge.domain.model.RosTopic> {
        val allSubscribers = subscriberDao.subscribers.value
        val topics = mutableListOf<com.examples.testros2jsbridge.domain.model.RosTopic>()
        allSubscribers.forEach { sub ->
            if (sub.topicInfo != null) {
                topics.add(sub.topicInfo)
            } else {
                topics.add(
                    com.examples.testros2jsbridge.domain.model.RosTopic(
                        name = sub.topic.id,
                        type = sub.type,
                        isSubscribed = true,
                        description = sub.topic.id
                    )
                )
            }
        }
        return topics.distinctBy { it.name }
    }

    override fun unsubscribe(topic: String) {
        _subscribedTopics.value = _subscribedTopics.value.filterNot { it.first == topic }.toSet()
    }

    override fun clearSubscriptions() {
        _subscribedTopics.value.forEach { (topic, _) ->
            unsubscribe(topic)
        }
        _subscribedTopics.value = emptySet()
    }
}