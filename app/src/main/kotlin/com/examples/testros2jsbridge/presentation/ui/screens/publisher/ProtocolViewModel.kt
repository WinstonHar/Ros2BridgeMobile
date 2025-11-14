package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.core.ros.RosBridgeViewModel
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.CustomProtocol
import com.examples.testros2jsbridge.domain.model.typeString
import com.examples.testros2jsbridge.domain.repository.AppActionRepository
import com.examples.testros2jsbridge.presentation.state.ProtocolUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class ProtocolViewModel @Inject constructor(
    private val appActionRepository: AppActionRepository
) : ViewModel() {

    var rosBridgeViewModel: RosBridgeViewModel? = null

    data class ProtocolField(
        val type: String,
        val name: String,
        val default: String?,
        val section: String = "Goal",
        val isConstant: Boolean = false
    )

    private val _uiState = MutableStateFlow(ProtocolUiState())
    val uiState: StateFlow<ProtocolUiState> = _uiState

    private val _activeProtocol = MutableStateFlow<CustomProtocol?>(null)
    val activeProtocol: StateFlow<CustomProtocol?> = _activeProtocol.asStateFlow()

    private val _protocolFields = MutableStateFlow<List<ProtocolField>>(emptyList())
    val protocolFields: StateFlow<List<ProtocolField>> = _protocolFields.asStateFlow()

    private val _protocolFieldValues = MutableStateFlow<Map<String, String>>(emptyMap())
    val protocolFieldValues: StateFlow<Map<String, String>> = _protocolFieldValues.asStateFlow()

    private val _editingAppAction = MutableStateFlow<AppAction?>(null)
    val editingAppAction: StateFlow<AppAction?> = _editingAppAction.asStateFlow()

    private val _customAppActions = MutableStateFlow<List<AppAction>>(emptyList())
    val customAppActions: StateFlow<List<AppAction>> = _customAppActions.asStateFlow()

    private var allMessages: List<CustomProtocol> = emptyList()
    private var allServices: List<CustomProtocol> = emptyList()
    private var allActions: List<CustomProtocol> = emptyList()

    private val isInitialized = AtomicBoolean(false)

    fun initialize(context: Context) {
        if (isInitialized.getAndSet(true)) return

        viewModelScope.launch {
            try {
                val packages = appActionRepository.getAvailablePackages(context)
                _uiState.value = _uiState.value.copy(packageNames = packages)

                allMessages = appActionRepository.getMessageFiles(context)
                allServices = appActionRepository.getServiceFiles(context)
                allActions = appActionRepository.getActionFiles(context)

                appActionRepository.getCustomAppActions(context).collect { actions ->
                    _customAppActions.value = actions
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(showErrorDialog = true, errorMessage = e.message)
            }
        }
    }

    fun onPackageSelected(packageName: String) {
        val messages = allMessages.filter { it.packageName == packageName }
        val services = allServices.filter { it.packageName == packageName }
        val actions = allActions.filter { it.packageName == packageName }

        _uiState.value = _uiState.value.copy(
            availableMessages = messages.map { ProtocolUiState.ProtocolFile(it.name, it.importPath, ProtocolUiState.ProtocolType.MSG, it.packageName) },
            availableServices = services.map { ProtocolUiState.ProtocolFile(it.name, it.importPath, ProtocolUiState.ProtocolType.SRV, it.packageName) },
            availableActions = actions.map { ProtocolUiState.ProtocolFile(it.name, it.importPath, ProtocolUiState.ProtocolType.ACTION, it.packageName) }
        )
    }

    fun loadProtocolFields(context: Context, selectedProtocol: ProtocolUiState.ProtocolFile) {
        try {
            val packageName = selectedProtocol.importPath.substringBefore('/')
            val customProtocol = CustomProtocol(
                name = selectedProtocol.name,
                importPath = selectedProtocol.importPath,
                type = CustomProtocol.Type.valueOf(selectedProtocol.type.name),
                packageName = packageName
            )
            _activeProtocol.value = customProtocol

            val fields = parseProtocolFieldsFromAssets(context, selectedProtocol.importPath, selectedProtocol.type)
            _protocolFields.value = fields
            _protocolFieldValues.value = fields.associate { it.name to (it.default ?: "") }
        } catch (e: Exception) {
            Logger.e("ProtocolViewModel", "Error loading protocol fields", e)
        }
    }

    fun parseProtocolFieldsFromAssets(context: Context, importPath: String, type: ProtocolUiState.ProtocolType): List<ProtocolField> {
        val fields = mutableListOf<ProtocolField>()
        var currentSection = "Goal"
        try {
            context.assets.open(importPath).bufferedReader().useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        if (type == ProtocolUiState.ProtocolType.ACTION && trimmed.startsWith("#")) {
                            val lower = trimmed.lowercase()
                            if ("goal" in lower) currentSection = "Goal"
                            else if ("result" in lower) currentSection = "Result"
                            else if ("feedback" in lower) currentSection = "Feedback"
                        }
                        continue
                    }
                    if ((type == ProtocolUiState.ProtocolType.SRV || type == ProtocolUiState.ProtocolType.ACTION) && trimmed == "---") {
                        currentSection = if (type == ProtocolUiState.ProtocolType.SRV) "Response" else "Result"
                        continue
                    }
                    val parts = trimmed.split(" ", limit = 2)
                    if (parts.size >= 2) {
                        val typePart = parts[0]
                        val nameAndDefault = parts[1].split("=", limit = 2)
                        val namePart = nameAndDefault[0].trim()
                        val defaultPart = nameAndDefault.getOrNull(1)?.trim()
                        val isConstant = parts[1].contains("=")
                        fields.add(ProtocolField(typePart, namePart, defaultPart, currentSection, isConstant))
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("ProtocolViewModel", "Error parsing protocol fields from $importPath", e)
        }
        return fields
    }

    fun updateProtocolFieldValue(name: String, value: String) {
        _protocolFieldValues.value = _protocolFieldValues.value.toMutableMap().apply { put(name, value) }
    }

    fun setEditingAppAction(action: AppAction?, context: Context? = null) {
        _editingAppAction.value = action
        if (action != null) {
            val protocol = findProtocolForAppAction(action)
            if (protocol != null && context != null) {
                val uiProtocolType = when (protocol.type) {
                    CustomProtocol.Type.MSG -> ProtocolUiState.ProtocolType.MSG
                    CustomProtocol.Type.SRV -> ProtocolUiState.ProtocolType.SRV
                    CustomProtocol.Type.ACTION -> ProtocolUiState.ProtocolType.ACTION
                }
                val fields = parseProtocolFieldsFromAssets(context, protocol.importPath, uiProtocolType)
                _activeProtocol.value = protocol
                _protocolFields.value = fields
                val msgMap = parseMsgJsonToFieldMap(action.msg)
                _protocolFieldValues.value = fields.associate { it.name to (msgMap[it.name] ?: it.default ?: "") }
            }
        }
    }

    private fun findProtocolForAppAction(action: AppAction): CustomProtocol? {
        val allProtocols = allMessages + allServices + allActions
        return allProtocols.find { it.name == action.displayName && it.type.name == action.type }
    }

    private fun parseMsgJsonToFieldMap(msg: String): Map<String, String> {
        return try {
            val json = Json.parseToJsonElement(msg).jsonObject
            json.mapValues { (_, v) -> v.toString().trim('"') }
        } catch (e: Exception) {
            Logger.e("ProtocolViewModel", "Error parsing msg JSON: $msg", e)
            emptyMap()
        }
    }

    fun saveCustomAppAction(context: Context, action: AppAction) {
        viewModelScope.launch {
            appActionRepository.saveCustomAppAction(action, context)
            _uiState.value = _uiState.value.copy(actionSaved = true)
        }
    }

    fun onActionSaved() {
        _uiState.value = _uiState.value.copy(actionSaved = false)
    }

    fun deleteCustomAppAction(context: Context, actionId: String) {
        viewModelScope.launch {
            appActionRepository.deleteCustomAppAction(actionId, context)
        }
    }

    fun importProtocols(context: Context, selected: Set<String>) {
        _uiState.value = _uiState.value.copy(isImporting = true)
        viewModelScope.launch {
            try {
                appActionRepository.importProtocols(context, selected)
                _uiState.value = _uiState.value.copy(isImporting = false, selectedProtocols = selected)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isImporting = false, showErrorDialog = true, errorMessage = e.message)
            }
        }
    }

    fun dismissErrorDialog() {
        _uiState.value = _uiState.value.copy(showErrorDialog = false, errorMessage = null)
    }

    fun buildMsgArgsJson(): String {
        fun cleanFieldName(name: String): String = name.split("#")[0].trim()

        val protocolType = _activeProtocol.value?.type
        val fields = _protocolFields.value
        val values = _protocolFieldValues.value

        val msgEntries = fields
            .filter { field ->
                if (field.isConstant) return@filter false
                val fieldName = cleanFieldName(field.name)
                if (fieldName in setOf("displayName", "topic", "__topic__", "type", "source", "msg")) return@filter false

                when (protocolType) {
                    CustomProtocol.Type.SRV, CustomProtocol.Type.ACTION -> field.section == "Goal"
                    CustomProtocol.Type.MSG -> true
                    else -> true
                }
            }
            .mapNotNull { field ->
                val fieldName = cleanFieldName(field.name)
                val valueStr = values[field.name]

                if (valueStr != null) {
                    val jsonElement = try {
                        Json.parseToJsonElement(valueStr)
                    } catch (_: Exception) {
                        JsonPrimitive(valueStr)
                    }
                    fieldName to jsonElement
                } else {
                    null
                }
            }
        return JsonObject(msgEntries.toMap()).toString()
    }

    fun triggerProtocol() {
        val protocol = _activeProtocol.value ?: return
        val topicOrServiceName = _protocolFieldValues.value["__topic__"] ?: protocol.name
        val typeString = protocol.typeString
        val msgArgsJson = buildMsgArgsJson()

        Logger.d("ProtocolViewModel", "Triggering protocol: Type=${protocol.type}, Name=$topicOrServiceName, TypeString=$typeString, MsgArgs=$msgArgsJson")

        val ros = rosBridgeViewModel ?: run {
            Logger.e("ProtocolViewModel", "RosBridgeViewModel is not attached!")
            return
        }

        when (protocol.type) {
            CustomProtocol.Type.MSG -> {
                ros.publishMessage(topicOrServiceName, typeString, msgArgsJson)
            }
            CustomProtocol.Type.SRV -> {
                ros.sendOrQueueServiceRequest(topicOrServiceName, typeString, msgArgsJson) { result ->
                    Logger.d("ProtocolViewModel", "Service result for '$topicOrServiceName': $result")
                }
            }
            CustomProtocol.Type.ACTION -> {
                ros.sendOrQueueActionGoal(
                    actionName = topicOrServiceName,
                    actionType = typeString,
                    goalFields = Json.parseToJsonElement(msgArgsJson).jsonObject
                ) { result ->
                    Logger.d("ProtocolViewModel", "Action result for '$topicOrServiceName': $result")
                }
            }
        }
    }
}
