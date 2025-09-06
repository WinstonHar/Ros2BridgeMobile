package com.examples.testros2jsbridge.presentation.ui.screens.protocol

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.core.ros.RosBridgeViewModel
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.CustomProtocol
import com.examples.testros2jsbridge.domain.model.typeString
import com.examples.testros2jsbridge.domain.repository.ProtocolRepository
import com.examples.testros2jsbridge.domain.repository.AppActionRepository
import com.examples.testros2jsbridge.presentation.state.ProtocolUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

@HiltViewModel
class ProtocolViewModel @Inject constructor(
    private val protocolRepository: ProtocolRepository
    private val appActionRepository: AppActionRepository
) : ViewModel() {
    // Reference to RosBridgeViewModel for advertisement (set externally if needed)
    var rosBridgeViewModel: RosBridgeViewModel? = null
    // --- Custom Protocol Compose UI State ---

    data class ProtocolField(
        val type: String,
        val name: String,
        val default: String?,
        val section: String = "Goal",
        val isConstant: Boolean = false
    )

    private val _activeProtocol = MutableStateFlow<CustomProtocol?>(null)
    val activeProtocol: StateFlow<CustomProtocol?> = _activeProtocol.asStateFlow()

    private val _protocolFields = MutableStateFlow<List<ProtocolField>>(emptyList())
    val protocolFields: StateFlow<List<ProtocolField>> = _protocolFields.asStateFlow()

    private val _protocolFieldValues = MutableStateFlow<Map<String, String>>(emptyMap())
    val protocolFieldValues: StateFlow<Map<String, String>> = _protocolFieldValues.asStateFlow()

    private val _editingAppAction = MutableStateFlow<AppAction?>(null)
    val editingAppAction: StateFlow<AppAction?> = _editingAppAction.asStateFlow()

    private val _packageNames = MutableStateFlow<List<String>>(emptyList())
    val packageNames: StateFlow<List<String>> = _packageNames.asStateFlow()

    /**
     * Parse a .msg/.srv/.action file from assets and update protocolFields.
     */
    suspend fun loadProtocolFields(context: Context, packageName: String, selectedProtocol: ProtocolUiState.ProtocolFile) {
        try{
            // Convert ProtocolUiState.ProtocolFile to CustomProtocol
            val packageName = selectedProtocol.importPath.substringBeforeLast("/")
            val customProtocol = CustomProtocol(
                name = selectedProtocol.name,
                importPath = selectedProtocol.importPath,
                type = CustomProtocol.Type.valueOf(selectedProtocol.type.name),
                packageName = packageName
            )

            // Update the active protocol
            _activeProtocol.value = customProtocol

            // Parse the fields for the selected protocol
            val fields = parseProtocolFieldsFromAssets(context, selectedProtocol.importPath, selectedProtocol.type)
            _protocolFields.value = fields

            // Initialize field values with defaults
            _protocolFieldValues.value = fields.associate { it.name to (it.default ?: "") }

            // Filter available protocols by package name
            _availableMsgProtocols.value = appActionRepository.getMessageFiles(context).filter { it.packageName == packageName }
            _availableSrvProtocols.value = appActionRepository.getServiceFiles(context).filter { it.packageName == packageName }
            _availableActionProtocols.value = appActionRepository.getActionFiles(context).filter { it.packageName == packageName }
        } catch (e: Exception) {
            Logger.e("ProtocolViewModel", "Error loading protocol fields", e)
        }
    }

    /**
     * Utility to parse protocol fields from assets (msg/srv/action).
     */
    fun parseProtocolFieldsFromAssets(context: Context, importPath: String, type: ProtocolUiState.ProtocolType): List<ProtocolField> {
        val fields = mutableListOf<ProtocolField>()
        var currentSection = "Goal"
        try {
            context.assets.open(importPath).bufferedReader().useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        // Section header detection for .action files
                        if (type == ProtocolUiState.ProtocolType.ACTION && trimmed.startsWith("#")) {
                            val lower = trimmed.lowercase()
                            if ("goal" in lower) currentSection = "Goal"
                            else if ("result" in lower) currentSection = "Result"
                            else if ("feedback" in lower) currentSection = "Feedback"
                        }
                        continue
                    }
                    if ((type == ProtocolUiState.ProtocolType.SRV || type == ProtocolUiState.ProtocolType.ACTION) && trimmed == "---") {
                        // For .srv and .action, section delimiter
                        if (type == ProtocolUiState.ProtocolType.ACTION) {
                            // Switch section for .action: Goal -> Result -> Feedback
                            currentSection = when (currentSection) {
                                "Goal" -> "Result"
                                "Result" -> "Feedback"
                                else -> currentSection
                            }
                        }
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

    /**
     * Update a protocol field value.
     */
    fun updateProtocolFieldValue(name: String, value: String) {
        _protocolFieldValues.value = _protocolFieldValues.value.toMutableMap().apply { put(name, value) }
    }

    /**
     * Set editing AppAction (for edit mode in UI).
     */
    fun setEditingAppAction(action: AppAction?, context: Context? = null) {
        _editingAppAction.value = action
        if (action != null) {
            val protocol = findProtocolForAppAction(action)
            if (protocol != null && context != null) {
                val fields = parseProtocolFieldsFromAssets(context, protocol.importPath, ProtocolUiState.ProtocolType.valueOf(protocol.type.name))
                _activeProtocol.value = protocol
                _protocolFields.value = fields
                val msgMap = parseMsgJsonToFieldMap(action.msg)
                _protocolFieldValues.value = fields.associate { it.name to (msgMap[it.name] ?: it.default ?: "") }
            }
        }
    }

    // Helper to find protocol for an AppAction (by topic/type or other unique identifier)
    private fun findProtocolForAppAction(action: AppAction): CustomProtocol? {
        val allProtocols = _availableMsgProtocols.value + _availableSrvProtocols.value + _availableActionProtocols.value
        return allProtocols.find { it.name == action.type || it.name == action.displayName }
    }

    // Helper to parse msg JSON string into a map of field values
    private fun parseMsgJsonToFieldMap(msg: String): Map<String, String> {
        return try {
            val json = Json.parseToJsonElement(msg).jsonObject
            json.mapValues { (_, v) -> v.toString().trim('"') }
        } catch (e: Exception) {
            Logger.e("ProtocolViewModel", "Error parsing msg JSON: $msg", e)
            emptyMap()
        }
    }
    // State for available protocols
    private val _availableMsgProtocols = MutableStateFlow<List<CustomProtocol>>(emptyList())
    val availableMsgProtocols: StateFlow<List<CustomProtocol>> = _availableMsgProtocols.asStateFlow()

    private val _availableSrvProtocols = MutableStateFlow<List<CustomProtocol>>(emptyList())
    val availableSrvProtocols: StateFlow<List<CustomProtocol>> = _availableSrvProtocols.asStateFlow()

    private val _availableActionProtocols = MutableStateFlow<List<CustomProtocol>>(emptyList())
    val availableActionProtocols: StateFlow<List<CustomProtocol>> = _availableActionProtocols.asStateFlow()

    // State for selected protocol import paths
    private val _selectedProtocols = MutableStateFlow<Set<String>>(emptySet())
    val selectedProtocols: StateFlow<Set<String>> = _selectedProtocols.asStateFlow()

    // State for custom app actions
    private val _customAppActions = MutableStateFlow<List<AppAction>>(emptyList())
    val customAppActions: StateFlow<List<AppAction>> = _customAppActions.asStateFlow()

    // Load all available protocols from assets
    fun loadAvailableProtocols(context: Context, packageName: String) {
        viewModelScope.launch {
            try {
                // Fetch all protocols
                val allMessages = appActionRepository.getMessageFiles(context)
                val allServices = appActionRepository.getServiceFiles(context)
                val allActions = appActionRepository.getActionFiles(context)

                // Filter protocols by the selected package
                val messages = allMessages.filter { it.packageName == packageName }
                val services = allServices.filter { it.packageName == packageName }
                val actions = allActions.filter { it.packageName == packageName }

                // Update the UI state with the filtered protocols
                _uiState.value = _uiState.value.copy(
                    availableMessages = messages.map {
                        ProtocolUiState.ProtocolFile(it.name, it.importPath, ProtocolUiState.ProtocolType.MSG)
                    },
                    availableServices = services.map {
                        ProtocolUiState.ProtocolFile(it.name, it.importPath, ProtocolUiState.ProtocolType.SRV)
                    },
                    availableActions = actions.map {
                        ProtocolUiState.ProtocolFile(it.name, it.importPath, ProtocolUiState.ProtocolType.ACTION)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    showErrorDialog = true,
                    errorMessage = e.message
                )
            }
        }
    }

    // Select or deselect a protocol by importPath
    fun toggleProtocolSelection(importPath: String) {
        val newSet = _selectedProtocols.value.toMutableSet().apply {
            if (contains(importPath)) remove(importPath) else add(importPath)
        }
        _selectedProtocols.value = newSet
        _uiState.value = _uiState.value.copy(selectedProtocols = newSet)
    }

    // Import selected protocols as CustomProtocol objects
    fun importSelectedProtocols(context: Context, onResult: (List<CustomProtocol>) -> Unit = {}) {
        viewModelScope.launch {
            val imported = appActionRepository.importProtocols(context, _selectedProtocols.value)
            onResult(imported)
        }
    }

    // Load all custom app actions
    fun loadCustomAppActions(context: Context) {
        viewModelScope.launch {
            _customAppActions.value = appActionRepository.getCustomAppActions(context)
        }
    }

    // Save a new or edited custom app action
    fun saveCustomAppAction(context: Context, action: AppAction) {
        viewModelScope.launch {
            appActionRepository.saveCustomAppAction(action, context)
            loadCustomAppActions(context)
        }
    }

    // Delete a custom app action by id
    fun deleteCustomAppAction(context: Context, actionId: String) {
        viewModelScope.launch {
            appActionRepository.deleteCustomAppAction(actionId, context)
            loadCustomAppActions(context)
        }
    }

    // Utility: clear selected protocols
    fun clearSelectedProtocols() {
        _selectedProtocols.value = emptySet()
    }
    // --- UI State for Compose screens (single source of truth) ---
    private val _uiState = MutableStateFlow(ProtocolUiState())
    val uiState: StateFlow<ProtocolUiState> = _uiState

    // Load all available protocols and update UI state
    fun loadProtocols(context: Context) {
        viewModelScope.launch {
            val messages = appActionRepository.getMessageFiles(context)
            val services = appActionRepository.getServiceFiles(context)
            val actions = appActionRepository.getActionFiles(context)
            _uiState.value = _uiState.value.copy(
                availableMessages = messages.map { ProtocolUiState.ProtocolFile(it.name, it.importPath, ProtocolUiState.ProtocolType.valueOf(it.type.name)) },
                availableServices = services.map { ProtocolUiState.ProtocolFile(it.name, it.importPath, ProtocolUiState.ProtocolType.valueOf(it.type.name)) },
                availableActions = actions.map { ProtocolUiState.ProtocolFile(it.name, it.importPath, ProtocolUiState.ProtocolType.valueOf(it.type.name)) }
            )
        }
    }

    // Import selected protocols and update UI state
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

    // Helper to build message JSON for protocol fields, matching legacy logic and rosbridge envelope
    fun buildProtocolMsgJson(
        protocolFields: List<ProtocolField>,
        protocolFieldValues: Map<String, String>,
        topicOverride: String? = null
    ): String {
        // Clean up field names (remove comments, trim)
        fun cleanFieldName(name: String): String = name.split("#")[0].trim()

        // Determine protocol type
        val protocolType = activeProtocol.value?.type
        val resolvedTopic = topicOverride ?: protocolFieldValues["topic"] ?: protocolFieldValues["__topic__"] ?: "/"
        val resolvedTypeString = activeProtocol.value?.typeString ?: ""
        // ADVERTISE before sending
        when (protocolType) {
            CustomProtocol.Type.MSG -> rosBridgeViewModel?.advertiseTopic(resolvedTopic, resolvedTypeString)
            CustomProtocol.Type.SRV -> rosBridgeViewModel?.advertiseService(resolvedTopic, resolvedTypeString)
            CustomProtocol.Type.ACTION -> rosBridgeViewModel?.advertiseAction(resolvedTopic, resolvedTypeString)
            else -> {}
        }

        val msgEntries = protocolFields
            .filter { field ->
                // Exclude constants and meta fields
                if (field.isConstant) return@filter false
                val fieldName = cleanFieldName(field.name)
                if (fieldName in setOf("displayName", "topic", "type", "source", "msg")) return@filter false

                when (protocolType) {
                    CustomProtocol.Type.ACTION -> field.section == "Goal"
                    CustomProtocol.Type.SRV -> field.section == "Goal" // Only fields before --- (Request)
                    CustomProtocol.Type.MSG -> fieldName == fieldName.lowercase() // Only lowercase fields
                    else -> true
                }
            }
            .map { field ->
                val fieldName = cleanFieldName(field.name)
                val value = protocolFieldValues[field.name] ?: ""
                val asLong = value.toLongOrNull()
                val asDouble = value.toDoubleOrNull()
                val isBool = value.equals("true", true) || value.equals("false", true)
                val jsonValue = when {
                    isBool -> value.toBoolean().toString()
                    asLong != null && value == asLong.toString() -> asLong.toString()
                    asDouble != null && value == asDouble.toString() -> asDouble.toString()
                    else -> "\"" + value.replace("\"", "\\\"") + "\""
                }
                "\"$fieldName\": $jsonValue"
            }
        val msgJson = "{" + msgEntries.joinToString(", ") + "}"
        val envelope = "{" +
            "\"op\": \"publish\"," +
            "\"topic\": \"$resolvedTopic\"," +
            "\"msg\": $msgJson" +
            "}"
        return envelope
    }

    fun loadPackageNames(context: Context) {
        viewModelScope.launch {
            try {
                val packages = appActionRepository.getAvailablePackages(context)
                _uiState.value = _uiState.value.copy(packageNames = packages)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    showErrorDialog = true,
                    errorMessage = e.message
                )
            }
        }
    }
}