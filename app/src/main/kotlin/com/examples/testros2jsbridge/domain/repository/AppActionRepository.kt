package com.examples.testros2jsbridge.domain.repository

import android.content.Context
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.CustomProtocol
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.RosMessage
import kotlinx.coroutines.flow.Flow

interface AppActionRepository {
    val messages: Flow<List<RosMessageDto>>
    suspend fun saveCustomAppAction(action: com.examples.testros2jsbridge.domain.model.AppAction, context: Context): Int
    fun getCustomAppActions(context: Context): Flow<List<com.examples.testros2jsbridge.domain.model.AppAction>>
    suspend fun deleteCustomAppAction(actionId: String, context: Context)
    suspend fun getAvailablePackages(context: Context): List<String>
    suspend fun saveMessage(message: RosMessageDto)
    suspend fun deleteMessage(message: RosMessageDto)
    suspend fun getMessage(messageId: String): RosMessageDto?
    suspend fun getMessagesByTopic(topic: RosId): List<RosMessageDto>
    fun publishMessage(message: RosMessage)
    fun clearCustomMessage()
    fun onCustomMessageChange(newMessage: String)
    fun updateConnectionStatus(status: String)
    suspend fun getMessageFiles(context: Context): List<CustomProtocol>
    suspend fun getServiceFiles(context: Context): List<CustomProtocol>
    suspend fun getActionFiles(context: Context): List<CustomProtocol>
    suspend fun importProtocols(context: Context, selected: Set<String>): List<CustomProtocol>
}
