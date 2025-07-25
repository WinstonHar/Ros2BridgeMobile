package com.example.testros2jsbridge

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.content.Context
import androidx.core.content.edit

/*
    CustomProtocolsViewModel scans the msgs folder for .msg, .srv, and .action files and manages user selections for import.
    It exposes lists of available and selected protocols for use in the UI.
*/

class CustomProtocolsViewModel(application: Application) : AndroidViewModel(application) {
    private val PREFS_NAME = "custom_protocols_prefs"
    private val PREFS_SELECTED_KEY = "selected_import_paths"
    data class ProtocolFile(val name: String, val importPath: String, val type: ProtocolType)
    enum class ProtocolType { MSG, SRV, ACTION }

    private val _messages = MutableStateFlow<List<ProtocolFile>>(emptyList())
    val messages: StateFlow<List<ProtocolFile>> = _messages
    private val _services = MutableStateFlow<List<ProtocolFile>>(emptyList())
    val services: StateFlow<List<ProtocolFile>> = _services
    private val _actions = MutableStateFlow<List<ProtocolFile>>(emptyList())
    val actions: StateFlow<List<ProtocolFile>> = _actions

    private val _selected = MutableStateFlow(loadSelectedFromPrefs())
    val selected: StateFlow<Set<String>> = _selected

    /*
        input:    None
        output:   SharedPreferences
        remarks:  Returns the SharedPreferences instance for custom protocols
    */
    private fun getPrefs(): android.content.SharedPreferences {
        return getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /*
        input:    None
        output:   Set<String>
        remarks:  Loads selected protocol import paths from SharedPreferences
    */
    private fun loadSelectedFromPrefs(): Set<String> {
        val prefs = getPrefs()
        val set = prefs.getStringSet(PREFS_SELECTED_KEY, null)
        return set ?: emptySet()
    }

    /*
        input:    selected - Set<String>
        output:   None
        remarks:  Saves selected protocol import paths to SharedPreferences
    */
    private fun saveSelectedToPrefs(selected: Set<String>) {
        val prefs = getPrefs()
        prefs.edit { putStringSet(PREFS_SELECTED_KEY, selected) }
    }

    /*
        input:    context - Context
        output:   None
        remarks:  Scans the assets msgs folder for .msg, .srv, and .action files and updates StateFlows
    */    fun scanMsgsAssets(context: Context) {
        viewModelScope.launch {
            val msgList = mutableListOf<ProtocolFile>()
            val srvList = mutableListOf<ProtocolFile>()
            val actionList = mutableListOf<ProtocolFile>()
            try {
                // Scan msgs/msg for .msg files
                val msgFiles: Array<String> = context.assets.list("msgs/msg") ?: emptyArray()
                for (filename in msgFiles) {
                    if (filename.endsWith(".msg")) {
                        msgList.add(ProtocolFile(filename.removeSuffix(".msg"), "msgs/msg/$filename", ProtocolType.MSG))
                    }
                }
                // Scan msgs/srv for .srv files
                val srvFiles: Array<String> = context.assets.list("msgs/srv") ?: emptyArray()
                for (filename in srvFiles) {
                    if (filename.endsWith(".srv")) {
                        srvList.add(ProtocolFile(filename.removeSuffix(".srv"), "msgs/srv/$filename", ProtocolType.SRV))
                    }
                }
                // Scan msgs/action for .action files
                val actionFiles: Array<String> = context.assets.list("msgs/action") ?: emptyArray()
                for (filename in actionFiles) {
                    if (filename.endsWith(".action")) {
                        actionList.add(ProtocolFile(filename.removeSuffix(".action"), "msgs/action/$filename", ProtocolType.ACTION))
                    }
                }
            } catch (e: Exception) {
                //errors would end up here
            }
            _messages.value = msgList
            _services.value = srvList
            _actions.value = actionList
        }
    }

    /*
        input:    selected - Set<String>
        output:   None
        remarks:  Updates the selected protocol import paths and saves to SharedPreferences
    */
    fun setSelected(selected: Set<String>) {
        _selected.value = selected
        saveSelectedToPrefs(selected)
    }

}
