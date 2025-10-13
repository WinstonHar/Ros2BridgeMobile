package com.examples.testros2jsbridge.data.repository

import android.content.Context
import com.examples.testros2jsbridge.data.local.database.dao.ConnectionDao
import com.examples.testros2jsbridge.data.local.database.dao.GeometryMessageDao
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.CustomProtocol
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.repository.AppActionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppActionRepositoryImpl @Inject constructor(
    private val rosbridgeClient: RosbridgeClient,
    private val connectionDao: ConnectionDao,
    private val geometryMessageDao: GeometryMessageDao,
    private val context: Context
) : AppActionRepository {

    override val messages = MutableStateFlow<List<RosMessageDto>>(emptyList())

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val sharedPreferences = context.getSharedPreferences("app_actions_prefs", Context.MODE_PRIVATE)

    override suspend fun saveCustomAppAction(action: AppAction, context: Context): Int {
        val actions = getCustomAppActions(context).toMutableList()
        val existingIndex = actions.indexOfFirst { it.id == action.id }

        if (existingIndex >= 0) {
            actions[existingIndex] = action
        } else {
            actions.add(action)
        }

        val actionsJson = json.encodeToString(actions)
        sharedPreferences.edit().putString("custom_actions", actionsJson).apply()

        return if (existingIndex >= 0) existingIndex else actions.size - 1
    }

    override suspend fun getCustomAppActions(context: Context): List<AppAction> {
        val actionsJson = sharedPreferences.getString("custom_actions", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<AppAction>>(actionsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun deleteCustomAppAction(actionId: String, context: Context) {
        val actions = getCustomAppActions(context).toMutableList()
        actions.removeAll { it.id == actionId }

        val actionsJson = json.encodeToString(actions)
        sharedPreferences.edit().putString("custom_actions", actionsJson).apply()
    }

    override suspend fun getAvailablePackages(context: Context): List<String> {
        // TODO: Implement package discovery logic
        return listOf("std_msgs", "geometry_msgs", "sensor_msgs", "nav_msgs")
    }

    override suspend fun saveMessage(message: RosMessageDto) {
        messages.value = messages.value + message
    }

    override suspend fun deleteMessage(message: RosMessageDto) {
        messages.value = messages.value.filterNot { it == message }
    }

    override suspend fun getMessage(messageId: String): RosMessageDto? {
        return messages.value.find { it.id == messageId }
    }

    override suspend fun getMessagesByTopic(topic: RosId): List<RosMessageDto> {
        return messages.value.filter { it.topic.value == topic.value }
    }

    override fun publishMessage(message: RosMessage) {
        // TODO: Implement message publishing via rosbridgeClient
    }

    override fun clearCustomMessage() {
        // TODO: Implement custom message clearing
    }

    override fun onCustomMessageChange(newMessage: String) {
        // TODO: Implement custom message change handling
    }

    override fun updateConnectionStatus(status: String) {
        // TODO: Implement connection status update
    }

    override suspend fun getMessageFiles(context: Context): List<CustomProtocol> {
        // TODO: Implement message file discovery from assets or filesystem
        return emptyList()
    }

    override suspend fun getServiceFiles(context: Context): List<CustomProtocol> {
        // TODO: Implement service file discovery from assets or filesystem
        return emptyList()
    }

    override suspend fun getActionFiles(context: Context): List<CustomProtocol> {
        // TODO: Implement action file discovery from assets or filesystem
        return emptyList()
    }

    override suspend fun importProtocols(context: Context, selected: Set<String>): List<CustomProtocol> {
        // TODO: Implement protocol import logic
        return emptyList()
    }
}
