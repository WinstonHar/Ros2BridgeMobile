package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosTopicDto
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.RosTopic
import com.examples.testros2jsbridge.domain.repository.RosTopicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import javax.inject.Inject


class RosTopicRepositoryImpl @Inject constructor(
    private val rosbridgeClient: RosbridgeClient,
    private val subscriberDao: com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao
) : RosTopicRepository {

    private val _subscribedTopics = MutableStateFlow<Set<RosTopicDto>>(emptySet())
    override val subscribedTopics: StateFlow<Set<RosTopicDto>> get() = _subscribedTopics

    override fun subscribe(topic: RosTopic) {
        val topicDto = RosTopicDto(
            name = topic.name,
            type = topic.type
        )
        _subscribedTopics.value = (_subscribedTopics.value + topicDto)
        // Send subscribe message to rosbridge
        val subscribeMsg = kotlinx.serialization.json.Json.encodeToString(
            com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto(
                op = "subscribe",
                topic = RosId(topic.name),
                type = topic.type,
                id = "sub_${topic.name}_${System.currentTimeMillis()}"
            )
        )
        rosbridgeClient.send(subscribeMsg)
        // Persist a new SubscriberEntity in the database
        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            val entity = com.examples.testros2jsbridge.data.local.database.entities.SubscriberEntity(
                id = "sub_${topic.name}_$now",
                topic = topic.name,
                type = topic.type,
                isActive = true,
                lastMessage = null,
                label = null,
                isEnabled = true,
                group = null,
                timestamp = now
            )
            subscriberDao.insertSubscriber(entity)
        }
    }

    override suspend fun getTopics(): List<RosTopicDto> {
        return _subscribedTopics.value.toList()
    }

    override fun unsubscribe(topicName: String) {
        _subscribedTopics.value = _subscribedTopics.value.filterNot { it.name == topicName }.toSet()
        // Send unsubscribe message to rosbridge
        val unsubscribeMsg = kotlinx.serialization.json.Json.encodeToString(
            com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto(
                op = "unsubscribe",
                topic = RosId(topicName),
                type = null,
                id = "unsub_${topicName}_${System.currentTimeMillis()}"
            )
        )
        rosbridgeClient.send(unsubscribeMsg)
    }

    override fun clearSubscriptions() {
        _subscribedTopics.value.forEach { topicDto ->
            unsubscribe(topicDto.name)
        }
        _subscribedTopics.value = emptySet()
    }
}