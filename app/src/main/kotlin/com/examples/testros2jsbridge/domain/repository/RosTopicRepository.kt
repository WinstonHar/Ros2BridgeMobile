package com.examples.testros2jsbridge.domain.repository

/*
Topic management interface
 */

import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosTopicDto
import com.examples.testros2jsbridge.domain.model.RosTopic
import kotlinx.coroutines.flow.StateFlow

interface RosTopicRepository {
    val subscribedTopics: StateFlow<Set<RosTopicDto>>

    fun subscribe(topic: RosTopic)
    fun unsubscribe(topicName: String)
    fun clearSubscriptions()
    suspend fun getTopics(): List<RosTopicDto>
}