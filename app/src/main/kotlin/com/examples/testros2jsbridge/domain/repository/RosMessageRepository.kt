package com.examples.testros2jsbridge.domain.repository

/*
Message handling contract
 */

import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.RosMessage
import kotlinx.coroutines.flow.MutableStateFlow

interface RosMessageRepository{
    suspend fun deleteMessage(message: RosMessageDto)
    val messages: MutableStateFlow<List<RosMessageDto>>

    suspend fun saveMessage(message: RosMessageDto)
    suspend fun getMessage(messageId: String): RosMessageDto?
    suspend fun getMessagesByTopic(topic: RosId): List<RosMessageDto>

    fun publishMessage(message: RosMessage)
    fun clearCustomMessage()
    fun onCustomMessageChange(newMessage: String)
    fun updateConnectionStatus(status: String)
}