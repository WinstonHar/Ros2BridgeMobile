package com.examples.testros2jsbridge.domain.repository

import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.CustomProtocol
import com.examples.testros2jsbridge.domain.model.RosId
import kotlinx.coroutines.flow.MutableStateFlow

interface AppActionRepository {
    // Protocol-related methods
    suspend fun saveCustomAppAction(action: AppAction, context: Context)
    suspend fun getCustomAppActions(context: Context): List<AppAction>
    suspend fun deleteCustomAppAction(actionId: String, context: Context)
    suspend fun getAvailablePackages(context: Context): List<String>

    // ROS message-related methods (from RosMessageRepository)
    val messages: MutableStateFlow<List<RosMessageDto>>
    suspend fun saveMessage(message: RosMessageDto)
    suspend fun deleteMessage(message: RosMessageDto)
    suspend fun getMessage(messageId: String): RosMessageDto?
    suspend fun getMessagesByTopic(topic: RosId): List<RosMessageDto>
    fun publishMessage(message: com.examples.testros2jsbridge.domain.model.RosMessage)
    fun clearCustomMessage()
    fun onCustomMessageChange(newMessage: String)
    fun updateConnectionStatus(status: String)

    // Protocol file management
    suspend fun getMessageFiles(context: android.content.Context): List<CustomProtocol>
    suspend fun getServiceFiles(context: android.content.Context): List<CustomProtocol>
    suspend fun getActionFiles(context: android.content.Context): List<CustomProtocol>
    suspend fun importProtocols(context: android.content.Context, selected: Set<String>): List<CustomProtocol>
}