package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosTopicDto
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.RosTopic
import com.examples.testros2jsbridge.domain.repository.RosTopicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import javax.inject.Inject

class RosTopicRepositoryImpl @Inject constructor(
    private val rosbridgeClient: RosbridgeClient
) : RosTopicRepository {

    private val _subscribedTopics = MutableStateFlow<Set<RosTopicDto>>(emptySet())
    override val subscribedTopics: StateFlow<Set<RosTopicDto>> get() = _subscribedTopics

    private val subscriberDao = com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao()

    override fun subscribe(topicDto: RosTopic) {
        _subscribedTopics.value = (_subscribedTopics.value + topicDto) as Set<RosTopicDto>
        // Send subscribe message to rosbridge
        val subscribeMsg = kotlinx.serialization.json.Json.encodeToString(
            com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto(
                op = "subscribe",
                topic = RosId(topicDto.name),
                type = topicDto.type,
                id = "sub_${topicDto.name}_${System.currentTimeMillis()}",
                latch = topicDto.isLatched,
                queue_size = topicDto.queueSize
            )
        )
        rosbridgeClient.send(subscribeMsg)
        // Optionally, add a new Subscriber to the DAO
        val newSubscriber = com.examples.testros2jsbridge.domain.model.Subscriber(
            topic = com.examples.testros2jsbridge.domain.model.RosId(topicDto.name),
            type = topicDto.type,
            messageHistory = emptyList<com.examples.testros2jsbridge.domain.model.RosMessage>()
        )
        subscriberDao.saveSubscriber(newSubscriber)
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