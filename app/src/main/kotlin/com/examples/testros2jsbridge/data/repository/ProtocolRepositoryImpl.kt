package com.examples.testros2jsbridge.data.repository


import android.content.Context
import android.content.SharedPreferences
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.CustomProtocol
import com.examples.testros2jsbridge.domain.repository.ProtocolRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProtocolRepositoryImpl : ProtocolRepository {
    private val PREFS_NAME = "custom_protocols_prefs"
    private val PREFS_ACTIONS_KEY = "custom_app_actions"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun getMessageFiles(context: Context): List<CustomProtocol> = withContext(Dispatchers.IO) {
        val msgList = mutableListOf<CustomProtocol>()
        
        val subfolders = context.assets.list("msgs") ?: emptyArray() // List subfolders (e.g., user_msgs, geometry_msgs, std_msgs)

        for (subfolder in subfolders) {
            val msgFiles: Array<String> = context.assets.list("msgs/$subfolder/msg") ?: emptyArray()
            for (filename in msgFiles) {
                if (filename.endsWith(".msg")) {
                    msgList.add(
                        CustomProtocol(
                            name = filename.removeSuffix(".msg"),
                            importPath = "msgs/$subfolder/msg/$filename",
                            type = CustomProtocol.Type.MSG,
                            packageName = subfolder // Add the subfolder as the package name
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

    override suspend fun importProtocols(context: Context, selected: Set<String>): List<CustomProtocol> = withContext(Dispatchers.IO) {
        // For now, just return the selected protocols as CustomProtocol objects
        val allProtocols = getMessageFiles(context) + getServiceFiles(context) + getActionFiles(context)
        allProtocols.filter { selected.contains(it.importPath) }
    }

    override suspend fun saveCustomAppAction(action: AppAction, context: Context) = withContext(Dispatchers.IO) {
        val prefs = getPrefs(context)
        val actions = getCustomAppActions(context).toMutableList()
        actions.removeAll { it.id == action.id }
        actions.add(action)
        val jsonSet = actions.map { com.examples.testros2jsbridge.core.util.JsonUtils.toJson(it) }.toSet()
        prefs.edit().putStringSet(PREFS_ACTIONS_KEY, jsonSet).apply()
    }

    override suspend fun getCustomAppActions(context: Context): List<AppAction> = withContext(Dispatchers.IO) {
        val prefs = getPrefs(context)
        val jsonSet = prefs.getStringSet(PREFS_ACTIONS_KEY, emptySet()) ?: emptySet()
        jsonSet.mapNotNull { json ->
            try {
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

    override suspend fun getAvailablePackages(context: Context): List<String> {
        return try {
            context.assets.list("msgs")?.filter { folderName ->
                context.assets.list("msgs/$folderName")?.isNotEmpty() == true
            } ?: emptyList()
        } catch (e: Exception) {
            com.examples.testros2jsbridge.core.util.Logger.e("ProtocolRepository", "Error fetching package names", e)
            emptyList()
        }
    }
}

