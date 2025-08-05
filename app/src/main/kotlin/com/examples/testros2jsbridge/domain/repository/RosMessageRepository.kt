package com.examples.testros2jsbridge.domain.repository

/*
Message handling contract
 */

import com.examples.testros2jsbridge.domain.model.*
import kotlinx.coroutines.flow.Flow

interface RosMessageRepository : com.examples.testros2jsbridge.core.base.RosRepository {
    val messages: Flow<RosMessage>

    suspend fun saveMessage(message: RosMessage)
    suspend fun getMessage(messageId: RosId): RosMessage
    suspend fun getMessagesByTopic(topicId: RosId): List<RosMessage>
}