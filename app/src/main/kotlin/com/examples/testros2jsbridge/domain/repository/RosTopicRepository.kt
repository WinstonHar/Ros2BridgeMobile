package com.examples.testros2jsbridge.domain.repository

/*
Topic management interface
 */

import com.examples.testros2jsbridge.domain.model.*
import kotlinx.coroutines.flow.Flow

interface RosTopicRepository : com.examples.testros2jsbridge.core.base.RosRepository {
    val topics: Flow<RosTopic>

    suspend fun saveTopic(topic: RosTopic)
    suspend fun getTopic(topicId: RosId): RosTopic
}