package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosTopicDto
import com.examples.testros2jsbridge.domain.model.RosTopic
import com.examples.testros2jsbridge.domain.repository.RosTopicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RosTopicRepositoryImpl @Inject constructor() : RosTopicRepository {
    private val _subscribedTopics = MutableStateFlow<Set<RosTopicDto>>(emptySet())
    override val subscribedTopics: StateFlow<Set<RosTopicDto>> = _subscribedTopics.asStateFlow()

    override fun subscribe(topic: RosTopic) {
        // TODO: Implement topic subscription logic
        // This would typically interact with a ROS bridge service to subscribe to the topic
    }

    override fun unsubscribe(topicName: String) {
        _subscribedTopics.value = _subscribedTopics.value.filterNot { it.name == topicName }.toSet()
    }

    override fun clearSubscriptions() {
        _subscribedTopics.value = emptySet()
    }

    override suspend fun getTopics(): List<RosTopicDto> {
        // TODO: Implement logic to fetch available topics from ROS bridge
        // For now, return the list of subscribed topics
        return _subscribedTopics.value.toList()
    }
}
