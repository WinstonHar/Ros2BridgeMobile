package com.example.testros2jsbridge

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.content.Context

/*
    CustomProtocolsViewModel scans the msgs folder for .msg, .srv, and .action files and manages user selections for import.
    It exposes lists of available and selected protocols for use in the UI.
*/

class CustomProtocolsViewModel(application: Application) : AndroidViewModel(application) {
    data class ProtocolFile(val name: String, val importPath: String, val type: ProtocolType)
    enum class ProtocolType { MSG, SRV, ACTION }

    private val _messages = MutableStateFlow<List<ProtocolFile>>(emptyList())
    val messages: StateFlow<List<ProtocolFile>> = _messages
    private val _services = MutableStateFlow<List<ProtocolFile>>(emptyList())
    val services: StateFlow<List<ProtocolFile>> = _services
    private val _actions = MutableStateFlow<List<ProtocolFile>>(emptyList())
    val actions: StateFlow<List<ProtocolFile>> = _actions

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected

    // Call this to scan the msgs folder in assets
    fun scanMsgsAssets(context: Context) {
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
                // Handle error if needed
            }
            _messages.value = msgList
            _services.value = srvList
            _actions.value = actionList
        }
    }


    fun setSelected(selected: Set<String>) {
        _selected.value = selected
    }

    // relImport no longer needed for assets
}
