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
        val msgFiles: Array<String> = context.assets.list("msgs/msg") ?: emptyArray()
        for (filename in msgFiles) {
            if (filename.endsWith(".msg")) {
                msgList.add(CustomProtocol(filename.removeSuffix(".msg"), "msgs/msg/$filename", CustomProtocol.Type.MSG))
            }
        }
        msgList
    }

    override suspend fun getServiceFiles(context: Context): List<CustomProtocol> = withContext(Dispatchers.IO) {
        val srvList = mutableListOf<CustomProtocol>()
        val srvFiles: Array<String> = context.assets.list("msgs/srv") ?: emptyArray()
        for (filename in srvFiles) {
            if (filename.endsWith(".srv")) {
                srvList.add(CustomProtocol(filename.removeSuffix(".srv"), "msgs/srv/$filename", CustomProtocol.Type.SRV))
            }
        }
        srvList
    }

    override suspend fun getActionFiles(context: Context): List<CustomProtocol> = withContext(Dispatchers.IO) {
        val actionList = mutableListOf<CustomProtocol>()
        val actionFiles: Array<String> = context.assets.list("msgs/action") ?: emptyArray()
        for (filename in actionFiles) {
            if (filename.endsWith(".action")) {
                actionList.add(CustomProtocol(filename.removeSuffix(".action"), "msgs/action/$filename", CustomProtocol.Type.ACTION))
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
}

