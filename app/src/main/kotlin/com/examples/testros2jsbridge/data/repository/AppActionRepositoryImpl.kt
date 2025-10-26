package com.examples.testros2jsbridge.data.repository

import android.content.Context
import android.content.res.AssetManager
import androidx.core.content.edit
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
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
        sharedPreferences.edit { putString("custom_actions", actionsJson) }

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
        sharedPreferences.edit { putString("custom_actions", actionsJson) }
    }

    override suspend fun getAvailablePackages(context: Context): List<String> {
        return try {
            context.assets.list("msgs")?.toList() ?: emptyList()
        } catch (e: IOException) {
            emptyList()
        }
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
        return discoverFiles(context, ".msg", CustomProtocol.Type.MSG)
    }

    override suspend fun getServiceFiles(context: Context): List<CustomProtocol> {
        return discoverFiles(context, ".srv", CustomProtocol.Type.SRV)
    }

    override suspend fun getActionFiles(context: Context): List<CustomProtocol> {
        return discoverFiles(context, ".action", CustomProtocol.Type.ACTION)
    }

    override suspend fun importProtocols(context: Context, selected: Set<String>): List<CustomProtocol> {
        val protocols = mutableListOf<CustomProtocol>()
        selected.forEach { packageName ->
            protocols.addAll(discoverAndReadFiles(context, packageName, ".msg", CustomProtocol.Type.MSG))
            protocols.addAll(discoverAndReadFiles(context, packageName, ".srv", CustomProtocol.Type.SRV))
            protocols.addAll(discoverAndReadFiles(context, packageName, ".action", CustomProtocol.Type.ACTION))
        }
        return protocols
    }

    private fun discoverFiles(context: Context, extension: String, type: CustomProtocol.Type): List<CustomProtocol> {
        val protocols = mutableListOf<CustomProtocol>()
        val assetManager = context.assets
        val basePath = "msgs"
        try {
            val packages = assetManager.list(basePath) ?: return emptyList()
            for (pkg in packages) {
                val protocolDir = when (type) {
                    CustomProtocol.Type.MSG -> "msg"
                    CustomProtocol.Type.SRV -> "srv"
                    CustomProtocol.Type.ACTION -> "action"
                }
                val filesPath = "$basePath/$pkg/$protocolDir"
                try {
                    val files = assetManager.list(filesPath) ?: continue
                    for (file in files) {
                        if (file.endsWith(extension)) {
                            protocols.add(
                                CustomProtocol(
                                    name = file.removeSuffix(extension),
                                    importPath = "$filesPath/$file",
                                    type = type,
                                    packageName = pkg
                                )
                            )
                        }
                    }
                } catch (e: IOException) {
                    // Directory does not exist, which is fine.
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return protocols
    }

    private fun discoverAndReadFiles(context: Context, packageName: String, extension: String, type: CustomProtocol.Type): List<CustomProtocol> {
        val protocols = mutableListOf<CustomProtocol>()
        val assetManager = context.assets
        val basePath = "msgs"
        val protocolDir = when (type) {
            CustomProtocol.Type.MSG -> "msg"
            CustomProtocol.Type.SRV -> "srv"
            CustomProtocol.Type.ACTION -> "action"
        }
        val filesPath = "$basePath/$packageName/$protocolDir"
        try {
            val files = assetManager.list(filesPath) ?: return emptyList()
            for (file in files) {
                if (file.endsWith(extension)) {
                    val filePath = "$filesPath/$file"
                    val msgType = readFileContent(assetManager, filePath)
                    protocols.add(
                        CustomProtocol(
                            name = file.removeSuffix(extension),
                            importPath = filePath,
                            type = type,
                            msgType = msgType,
                            packageName = packageName
                        )
                    )
                }
            }
        } catch (e: IOException) {
            // Directory does not exist, which is fine.
        }
        return protocols
    }

    private fun readFileContent(assetManager: AssetManager, path: String): String {
        return try {
            assetManager.open(path).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            ""
        }
    }
}
