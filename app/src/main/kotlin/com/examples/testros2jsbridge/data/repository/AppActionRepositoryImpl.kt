package com.examples.testros2jsbridge.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.examples.testros2jsbridge.data.local.database.dao.ConnectionDao
import com.examples.testros2jsbridge.data.local.database.dao.GeometryMessageDao
import com.examples.testros2jsbridge.data.local.database.entities.toDto
import com.examples.testros2jsbridge.data.local.database.entities.toEntity
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.toDto
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.CustomProtocol
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.repository.AppActionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AppActionRepositoryImpl @Inject constructor(
    private val rosbridgeClient: RosbridgeClient,
    private val connectionDao: ConnectionDao,
    private val geometryMessageDao: GeometryMessageDao,
    override val messages: MutableStateFlow<List<RosMessageDto>>,
) : AppActionRepository {

    private val PREFS_NAME = "custom_protocols_prefs"
    private val PREFS_ACTIONS_KEY = "custom_app_actions"

    private val _customMessage = MutableStateFlow("")

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        // Load initial messages from the database when the repository is created.
        GlobalScope.launch(Dispatchers.IO) {
            val entities = geometryMessageDao.getAll()
            messages.value = entities.map { it.toDto() }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun saveCustomAppAction(
        action: AppAction,
        context: Context
    ) = withContext(Dispatchers.IO) {
        val prefs = getPrefs(context)
        val actions = getCustomAppActions(context).toMutableList()
        actions.removeAll { it.id == action.id }
        actions.add(action)
        // Assuming JsonUtils exists as per the original implementation
        val jsonSet = actions.map { com.examples.testros2jsbridge.core.util.JsonUtils.toJson(it) }.toSet()
        prefs.edit().putStringSet(PREFS_ACTIONS_KEY, jsonSet).apply()
    }

    override suspend fun getCustomAppActions(context: Context): List<AppAction> = withContext(Dispatchers.IO) {
        val prefs = getPrefs(context)
        val jsonSet = prefs.getStringSet(PREFS_ACTIONS_KEY, emptySet()) ?: emptySet()
        jsonSet.mapNotNull { json ->
            try {
                // Assuming JsonUtils exists as per the original implementation
                com.examples.testros2jsbridge.core.util.JsonUtils.fromJson<AppAction>(json)
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun deleteCustomAppAction(actionId: String, context: Context) = withContext(Dispatchers.IO) {
        val prefs = getPrefs(context)
        val actions = getCustomAppActions(context).toMutableList()
        actions.removeAll { it.id == actionId }
        val jsonSet = actions.map { com.examples.testros2jsbridge.core.util.JsonUtils.toJson(it) }.toSet()
        prefs.edit().putStringSet(PREFS_ACTIONS_KEY, jsonSet).apply()
    }

    override suspend fun getAvailablePackages(context: Context): List<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            context.assets.list("msgs")?.filter { folderName ->
                context.assets.list("msgs/$folderName")?.isNotEmpty() == true
            } ?: emptyList()
        } catch (e: Exception) {
            // Assuming Logger exists as per the original implementation
            com.examples.testros2jsbridge.core.util.Logger.e("AppActionRepository", "Error fetching package names", e)
            emptyList()
        }
    }

    override suspend fun saveMessage(message: RosMessageDto) {
        val all = geometryMessageDao.getAll()
        val existing = all.find { it.uuid == (message.id ?: "") }
        if (existing != null) {
            val updatedEntity = existing.copy(
                label = message.label,
                topic = message.topic.value,
                type = message.type ?: "",
                content = message.content,
                timestamp = message.timestamp
            )
            geometryMessageDao.insert(updatedEntity)
        } else {
            geometryMessageDao.insert(message.toEntity())
        }
        val updated = geometryMessageDao.getAll().map { it.toDto() }
        messages.value = updated
    }

    override suspend fun deleteMessage(message: RosMessageDto) {
        val all = geometryMessageDao.getAll()
        val entity = all.find {
            it.label == message.label &&
                    it.topic == message.topic.value &&
                    it.type == message.type &&
                    it.content == message.content
        }
        if (entity != null) {
            geometryMessageDao.delete(entity)
        }
        val updated = geometryMessageDao.getAll().map { it.toDto() }
        messages.value = updated
    }

    override suspend fun getMessage(messageId: String): RosMessageDto? {
        return messages.value.find { it.id == messageId }
    }

    override suspend fun getMessagesByTopic(topic: RosId): List<RosMessageDto> {
        return messages.value.filter { it.topic.value == topic.value }
    }

    override fun publishMessage(message: RosMessage) {
        val dto = message.toDto()
        val jsonString = Json.encodeToString(RosMessageDto.serializer(), dto)
        rosbridgeClient.send(jsonString)
        GlobalScope.launch(Dispatchers.IO) {
            geometryMessageDao.insert(dto.toEntity())
            val updated = geometryMessageDao.getAll().map { it.toDto() }
            messages.value = updated
        }
    }

    override fun clearCustomMessage() {
        _customMessage.value = ""
    }

    override fun onCustomMessageChange(newMessage: String) {
        _customMessage.value = newMessage
    }

    override fun updateConnectionStatus(status: String) {
        // Assuming 'status' is used as a connection identifier
        connectionDao.updateStatus(
            status,
            isConnected = status == "connected"
        )
    }

    override suspend fun getMessageFiles(context: Context): List<CustomProtocol> = withContext(Dispatchers.IO) {
        val msgList = mutableListOf<CustomProtocol>()
        val subfolders = context.assets.list("msgs") ?: emptyArray()

        for (subfolder in subfolders) {
            val msgFiles: Array<String> = context.assets.list("msgs/$subfolder/msg") ?: emptyArray()
            for (filename in msgFiles) {
                if (filename.endsWith(".msg")) {
                    msgList.add(
                        CustomProtocol(
                            name = filename.removeSuffix(".msg"),
                            importPath = "msgs/$subfolder/msg/$filename",
                            type = CustomProtocol.Type.MSG,
                            packageName = subfolder
                        )
                    )
                }
            }
        }
        msgList
    }

    override suspend fun getServiceFiles(context: Context): List<CustomProtocol> = withContext(Dispatchers.IO) {
        val srvList = mutableListOf<CustomProtocol>()
        val subfolders = context.assets.list("msgs") ?: emptyArray()

        for (subfolder in subfolders) {
            val srvFiles: Array<String> = context.assets.list("msgs/$subfolder/srv") ?: emptyArray()
            for (filename in srvFiles) {
                if (filename.endsWith(".srv")) {
                    srvList.add(
                        CustomProtocol(
                            name = filename.removeSuffix(".srv"),
                            importPath = "msgs/$subfolder/srv/$filename",
                            type = CustomProtocol.Type.SRV,
                            packageName = subfolder
                        )
                    )
                }
            }
        }
        srvList
    }

    override suspend fun getActionFiles(context: Context): List<CustomProtocol> = withContext(Dispatchers.IO) {
        val actionList = mutableListOf<CustomProtocol>()
        val subfolders = context.assets.list("msgs") ?: emptyArray()

        for (subfolder in subfolders) {
            val actionFiles: Array<String> = context.assets.list("msgs/$subfolder/action") ?: emptyArray()
            for (filename in actionFiles) {
                if (filename.endsWith(".action")) {
                    actionList.add(
                        CustomProtocol(
                            name = filename.removeSuffix(".action"),
                            importPath = "msgs/$subfolder/action/$filename",
                            type = CustomProtocol.Type.ACTION,
                            packageName = subfolder
                        )
                    )
                }
            }
        }
        actionList
    }

    override suspend fun importProtocols(
        context: Context,
        selected: Set<String>
    ): List<CustomProtocol> = withContext(Dispatchers.IO) {
        val allProtocols = getMessageFiles(context) + getServiceFiles(context) + getActionFiles(context)
        allProtocols.filter { selected.contains(it.importPath) }
    }
}