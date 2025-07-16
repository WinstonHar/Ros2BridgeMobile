package com.example.testros2jsbridge

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.intOrNull
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import android.widget.LinearLayout
import android.widget.CheckBox
import android.widget.TextView
import android.widget.ScrollView
import android.content.Context
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.example.testros2jsbridge.CustomProtocolsViewModel

/*
    ImportCustomProtocolsFragment scans the msgs folder for .msg, .srv, and .action files and displays them in three sections with checkboxes.
    User selections are managed via a ViewModel and can be used elsewhere in the app.
*/

class ImportCustomProtocolsFragment : Fragment() {
    // ...existing code...
    // Track advertised actions and services to ensure idempotency
    private val advertisedActions = mutableSetOf<String>()
    private val advertisedServices = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Clear advertised sets when rosbridge disconnects
        rosViewModel.onRosbridgeDisconnected = {
            advertisedActions.clear()
            advertisedServices.clear()
        }
    }

    // No need to track lastGoalUuid here; handled by RosViewModel
    // Helper to format JSON values: quote strings, leave numbers/bools/null as is
    private fun formatJsonValue(value: String): String {
        return if (value.matches(Regex("^-?\\d+(\\.\\d+)?$")) ||
            value.equals("true", ignoreCase = true) ||
            value.equals("false", ignoreCase = true) ||
            value.equals("null", ignoreCase = true)) {
            value
        } else if (value.trim().startsWith("[") && value.trim().endsWith("]")) {
            // Allow raw JSON arrays for list fields
            value
        } else {
            // Escape any embedded quotes in the string value
            "\"" + value.replace("\"", "\\\"") + "\""
        }
    }
    // Track which action is being edited (null if not editing)
    private var editingActionIndex: Int? = null
    private var editingFieldValues: Map<String, String>? = null
    // Use viewModels() delegate for correct ViewModel scoping
    private val viewModel: CustomProtocolsViewModel by viewModels()
    private val rosViewModel: RosViewModel by activityViewModels()

    // Data class for a saved custom protocol action
    data class SavedCustomProtocolAction(
        val label: String,
        val proto: CustomProtocolsViewModel.ProtocolFile,
        val fieldValues: Map<String, String>
    )

    private val PREFS_KEY = "custom_protocol_actions"
    private val savedActions = mutableListOf<SavedCustomProtocolAction>()
    private var savedActionsLayout: LinearLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_import_custom_protocols, container, false)
        val context = requireContext()
        val msgSection = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val srvSection = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val actionSection = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        // Add section headers
        msgSection.addView(TextView(context).apply { text = "Messages" })
        srvSection.addView(TextView(context).apply { text = "Services" })
        actionSection.addView(TextView(context).apply { text = "Actions" })

        // Add to root layout (replace with your layout logic)
        val scroll = ScrollView(context)
        val vertical = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        // --- Add rosbridge connection status indicator at the very top ---
        val statusIndicator = TextView(context).apply {
            text = "rosbridge: Connecting..."
            textSize = 16f
            setPadding(0, 8, 0, 8)
        }
        vertical.addView(statusIndicator)

        // --- Add ROS root package input at the top ---
        val rosRootLayout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val rosRootLabel = TextView(context).apply { text = "ROS Root Package: "; textSize = 16f }
        val rosRootInput = android.widget.EditText(context).apply {
            hint = "ryan_msgs"
            setText(requireActivity().getPreferences(Context.MODE_PRIVATE).getString("ros_root_package", "ryan_msgs"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        rosRootLayout.addView(rosRootLabel)
        rosRootLayout.addView(rosRootInput)
        vertical.addView(rosRootLayout)

        vertical.addView(actionSection)
        vertical.addView(msgSection)
        vertical.addView(srvSection)
        scroll.addView(vertical)
        (root as ViewGroup).addView(scroll)
        // Observe rosbridge connection status and update indicator
        viewLifecycleOwner.lifecycleScope.launch {
            rosViewModel.connectionStatus.collectLatest { status ->
                statusIndicator.text = "rosbridge: $status"
                statusIndicator.setTextColor(
                    when {
                        status.equals("Connected", ignoreCase = true) -> 0xFF388E3C.toInt() // green
                        status.startsWith("Error", ignoreCase = true) -> 0xFFD32F2F.toInt() // red
                        else -> 0xFF757575.toInt() // gray
                    }
                )
            }
        }

        // Save root package on change
        rosRootInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = rosRootInput.text.toString().ifBlank { "ryan_msgs" }
                requireActivity().getPreferences(Context.MODE_PRIVATE).edit().putString("ros_root_package", value).apply()
            }
        }

        // Scan msgs folder in assets on open
        viewModel.scanMsgsAssets(requireContext())

        // Observe and update UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actions.collectLatest { actions ->
                actionSection.removeAllViews()
                actionSection.addView(TextView(context).apply { text = "Actions" })
                actions.forEach { proto ->
                    val cb = CheckBox(context)
                    cb.text = "${proto.name} (${proto.importPath})"
                    cb.isChecked = viewModel.selected.value.contains(proto.importPath)
                    cb.setOnCheckedChangeListener { _, isChecked ->
                        val newSet = viewModel.selected.value.toMutableSet()
                        if (isChecked) newSet.add(proto.importPath) else newSet.remove(proto.importPath)
                        viewModel.setSelected(newSet)
                    }
                    actionSection.addView(cb)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.messages.collectLatest { msgs ->
                msgSection.removeAllViews()
                msgSection.addView(TextView(context).apply { text = "Messages" })
                msgs.forEach { proto ->
                    val cb = CheckBox(context)
                    cb.text = "${proto.name} (${proto.importPath})"
                    cb.isChecked = viewModel.selected.value.contains(proto.importPath)
                    cb.setOnCheckedChangeListener { _, isChecked ->
                        val newSet = viewModel.selected.value.toMutableSet()
                        if (isChecked) newSet.add(proto.importPath) else newSet.remove(proto.importPath)
                        viewModel.setSelected(newSet)
                    }
                    msgSection.addView(cb)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.services.collectLatest { srvs ->
                srvSection.removeAllViews()
                srvSection.addView(TextView(context).apply { text = "Services" })
                srvs.forEach { proto ->
                    val cb = CheckBox(context)
                    cb.text = "${proto.name} (${proto.importPath})"
                    cb.isChecked = viewModel.selected.value.contains(proto.importPath)
                    cb.setOnCheckedChangeListener { _, isChecked ->
                        val newSet = viewModel.selected.value.toMutableSet()
                        if (isChecked) newSet.add(proto.importPath) else newSet.remove(proto.importPath)
                        viewModel.setSelected(newSet)
                    }
                    srvSection.addView(cb)
                }
            }
        }

        // --- Configuration area below checkboxes ---
        val configHeader = TextView(context).apply {
            text = "Configure Selected Protocol"
            textSize = 18f
            setPadding(0, 32, 0, 8)
        }
        val configLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 8, 16, 8)
        }
        vertical.addView(configHeader)
        vertical.addView(configLayout)

        // Dropdown to select from selected protocols
        val dropdown = android.widget.Spinner(context)
        configLayout.addView(dropdown)

        // Placeholder for dynamic configuration fields
        val configFields = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            tag = "configFields"
        }
        configLayout.addView(configFields)

        // Observe selected protocols and update dropdown
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selected.collectLatest { selectedSet ->
                val allProtocols = viewModel.actions.value + viewModel.messages.value + viewModel.services.value
                val selectedProtocols = allProtocols.filter { selectedSet.contains(it.importPath) }
                val names = selectedProtocols.map { "${it.name} (${it.type.name})" }
                val adapter = android.widget.ArrayAdapter(context, android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                dropdown.adapter = adapter
                // If editing, select the protocol and prefill fields
                if (editingActionIndex != null && editingActionIndex!! < rosViewModel.customProtocolActions.value.size) {
                    val action = rosViewModel.customProtocolActions.value[editingActionIndex!!]
                    val idx = selectedProtocols.indexOfFirst { it.importPath == action.proto.importPath }
                    if (idx >= 0) {
                        dropdown.setSelection(idx, false)
                        showConfigFields(configFields, selectedProtocols[idx], editingFieldValues)
                    } else if (selectedProtocols.isNotEmpty()) {
                        showConfigFields(configFields, selectedProtocols[0])
                    } else {
                        configFields.removeAllViews()
                    }
                } else {
                    // Show config fields for the first selected by default
                    if (selectedProtocols.isNotEmpty()) {
                        showConfigFields(configFields, selectedProtocols[0])
                    } else {
                        configFields.removeAllViews()
                    }
                    dropdown.setSelection(0, false)
                }
            }
        }
        // Update config fields when dropdown selection changes
        dropdown.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val allProtocols = viewModel.actions.value + viewModel.messages.value + viewModel.services.value
                val selectedProtocols = allProtocols.filter { viewModel.selected.value.contains(it.importPath) }
                if (position in selectedProtocols.indices) {
                    showConfigFields(configFields, selectedProtocols[position])
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                configFields.removeAllViews()
            }
        }

        // Add a layout for saved actions below the config area
        savedActionsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        vertical.addView(savedActionsLayout)
        // Observe shared custom protocol actions and update UI
        viewLifecycleOwner.lifecycleScope.launch {
            rosViewModel.customProtocolActions.collectLatest {
                refreshSavedActions(it)
            }
        }
        rosViewModel.loadCustomProtocolActionsFromPrefs()

        return root
    }

    // Show configuration fields for a selected protocol (supports .msg, .srv, .action)
    private fun showConfigFields(container: LinearLayout, proto: CustomProtocolsViewModel.ProtocolFile, prefill: Map<String, String>? = null) {
        container.removeAllViews()
        val context = requireContext()
        val nameView = TextView(context).apply {
            text = "Name: ${proto.name}"
            textSize = 16f
        }
        val typeView = TextView(context).apply {
            text = "Type: ${proto.type}"
        }
        val importView = TextView(context).apply {
            text = "Import Path: ${proto.importPath}"
        }
        container.addView(nameView)
        container.addView(typeView)
        container.addView(importView)

        // Helper to parse .msg, .srv, .action files and split by section
        fun parseFieldsBySection(importPath: String, type: CustomProtocolsViewModel.ProtocolType): Map<String, List<Triple<String, String, String?>>> {
            val sections = mutableMapOf<String, MutableList<Triple<String, String, String?>>>()
            var currentSection = when (type) {
                CustomProtocolsViewModel.ProtocolType.SRV -> "request"
                CustomProtocolsViewModel.ProtocolType.ACTION -> "goal"
                else -> "msg"
            }
            context.assets.open(importPath).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val clean = line.trim()
                    if (clean.startsWith("#")) return@forEach
                    if (clean == "---") {
                        currentSection = when (currentSection) {
                            "request" -> "response"
                            "goal" -> "result"
                            "result" -> "feedback"
                            else -> "other"
                        }
                        return@forEach
                    }
                    if (clean.isEmpty()) return@forEach
                    val parts = clean.split(Regex("\\s+"))
                    if (parts.size >= 2) {
                        // Check for constant (fixed) value
                        if (parts.size >= 4 && parts[2] == "=") {
                            sections.getOrPut(currentSection) { mutableListOf() }
                                .add(Triple(parts[0], parts[1], parts[3]))
                        } else {
                            sections.getOrPut(currentSection) { mutableListOf() }
                                .add(Triple(parts[0], parts[1], null))
                        }
                    }
                }
            }
            return sections
        }

        val sectionFields: Map<String, List<Triple<String, String, String?>>> = when (proto.type) {
            CustomProtocolsViewModel.ProtocolType.MSG -> mapOf("msg" to (parseFieldsBySection(proto.importPath, proto.type)["msg"] ?: emptyList()))
            CustomProtocolsViewModel.ProtocolType.SRV -> parseFieldsBySection(proto.importPath, proto.type)
            CustomProtocolsViewModel.ProtocolType.ACTION -> parseFieldsBySection(proto.importPath, proto.type)
            else -> emptyMap()
        }

        // For MSG, all fields are fillable
        // For SRV, only request fields are fillable
        // For ACTION, only goal fields are fillable
        val fillableFields: List<Triple<String, String, String?>> = when (proto.type) {
            CustomProtocolsViewModel.ProtocolType.MSG -> sectionFields["msg"] ?: emptyList()
            CustomProtocolsViewModel.ProtocolType.SRV -> sectionFields["request"] ?: emptyList()
            CustomProtocolsViewModel.ProtocolType.ACTION -> sectionFields["goal"] ?: emptyList()
            else -> emptyList()
        }
        val fixedFields: List<Triple<String, String, String?>> = fillableFields.filter { (_, name, _) -> name.matches(Regex("[A-Z0-9_]+")) }
        val editableFields: List<Triple<String, String, String?>> = fillableFields.filter { (_, name, _) -> name.matches(Regex("[a-z0-9_]+")) }

        // Add topic field (configurable, prefilled with /rawtopic)
        val topicLayout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val topicLabel = TextView(context).apply { text = "Topic" }
        val topicInput = android.widget.EditText(context).apply {
            hint = "Topic"
            tag = "__topic__"
            setText(prefill?.get("__topic__") ?: "/${proto.name}")
        }
        topicLayout.addView(topicLabel)
        topicLayout.addView(topicInput)
        container.addView(topicLayout)

        // Show editable fields as editable
        for (triple in editableFields) {
            val (type, name, value) = triple
            val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            val label = TextView(context).apply { text = "$type $name" }
            val input = android.widget.EditText(context).apply {
                hint = name
                tag = name
                setText(prefill?.get(name) ?: "")
            }
            layout.addView(label)
            layout.addView(input)
            container.addView(layout)
        }
        // Show fixed fields as non-editable, display their constant value if present
        for (triple in fixedFields) {
            val (type, name, value) = triple
            val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            val label = TextView(context).apply { text = "$type $name (fixed)" }
            val input = android.widget.EditText(context).apply {
                hint = name
                tag = name
                isEnabled = false
                setText(value ?: "")
            }
            layout.addView(label)
            layout.addView(input)
            container.addView(layout)
        }

        // Add Save/Update and Cancel buttons with layout tweaks
        val buttonLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val saveButton = android.widget.Button(context).apply {
            text = if (editingActionIndex != null) "Update App Action" else "Save as App Action"
            layoutParams = if (editingActionIndex != null) {
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.75f)
            } else {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        }
        saveButton.setOnClickListener {
            val values = mutableMapOf<String, String>()
            var topicValue: String? = null
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is LinearLayout && child.childCount == 2 && child.getChildAt(1) is android.widget.EditText) {
                    val input = child.getChildAt(1) as android.widget.EditText
                    val tag = input.tag as String
                    if (tag == "__topic__") {
                        topicValue = input.text.toString()
                    } else if (tag.matches(Regex("[a-z0-9_]+"))) {
                        values[tag] = input.text.toString()
                    }
                }
            }
            // Prompt for label
            val labelInput = android.widget.EditText(context)
            labelInput.hint = "Label for this action"
            if (editingActionIndex != null && editingActionIndex!! < rosViewModel.customProtocolActions.value.size) {
                labelInput.setText(rosViewModel.customProtocolActions.value[editingActionIndex!!].label)
            }
            android.app.AlertDialog.Builder(context)
                .setTitle(if (editingActionIndex != null) "Update Custom Protocol Action" else "Save Custom Protocol Action")
                .setView(labelInput)
                .setPositiveButton(if (editingActionIndex != null) "Update" else "Save") { _, _ ->
                    val label = labelInput.text.toString().ifBlank { proto.name }
                    val finalValues = values.toMutableMap()
                    if (topicValue != null) finalValues["__topic__"] = topicValue!!
                    if (editingActionIndex != null && editingActionIndex!! < rosViewModel.customProtocolActions.value.size) {
                        // Overwrite existing action
                        val updated = RosViewModel.CustomProtocolAction(label, proto, finalValues)
                        val list = rosViewModel.customProtocolActions.value.toMutableList()
                        list[editingActionIndex!!] = updated
                        rosViewModel.setCustomProtocolActions(list)
                        // Clear editing state and refresh config fields
                        editingActionIndex = null
                        editingFieldValues = null
                        // Refresh config fields UI
                        val fragmentView = view
                        if (fragmentView != null) {
                            val configFields = fragmentView.findViewWithTag<LinearLayout>("configFields")
                            val allProtocols = viewModel.actions.value + viewModel.messages.value + viewModel.services.value
                            val selectedProtocols = allProtocols.filter { viewModel.selected.value.contains(it.importPath) }
                            if (configFields != null) {
                                if (selectedProtocols.isNotEmpty()) {
                                    showConfigFields(configFields, selectedProtocols[0])
                                } else {
                                    configFields.removeAllViews()
                                }
                            }
                        }
                    } else {
                        rosViewModel.addCustomProtocolAction(RosViewModel.CustomProtocolAction(label, proto, finalValues))
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    // On dialog cancel, clear editing state and refresh config fields
                    editingActionIndex = null
                    editingFieldValues = null
                    val fragmentView = view
                    if (fragmentView != null) {
                        val configFields = fragmentView.findViewWithTag<LinearLayout>("configFields")
                        val allProtocols = viewModel.actions.value + viewModel.messages.value + viewModel.services.value
                        val selectedProtocols = allProtocols.filter { viewModel.selected.value.contains(it.importPath) }
                        if (configFields != null) {
                            if (selectedProtocols.isNotEmpty()) {
                                showConfigFields(configFields, selectedProtocols[0])
                            } else {
                                configFields.removeAllViews()
                            }
                        }
                    }
                }
                .show()
        }
        buttonLayout.addView(saveButton)
        // Add Cancel button if editing, with 25% width
        if (editingActionIndex != null) {
            val cancelButton = android.widget.Button(context).apply {
                text = "Cancel"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f)
            }
            cancelButton.setOnClickListener {
                editingActionIndex = null
                editingFieldValues = null
                // Refresh config fields UI
                val fragmentView = view
                if (fragmentView != null) {
                    val configFields = fragmentView.findViewWithTag<LinearLayout>("configFields")
                    val allProtocols = viewModel.actions.value + viewModel.messages.value + viewModel.services.value
                    val selectedProtocols = allProtocols.filter { viewModel.selected.value.contains(it.importPath) }
                    if (configFields != null) {
                        if (selectedProtocols.isNotEmpty()) {
                            showConfigFields(configFields, selectedProtocols[0])
                        } else {
                            configFields.removeAllViews()
                        }
                    }
                }
            }
            buttonLayout.addView(cancelButton)
        }
        container.addView(buttonLayout)
    }

    private fun refreshSavedActions(actions: List<RosViewModel.CustomProtocolAction>) {
        savedActionsLayout?.let { layout ->
            layout.removeAllViews()
            val rosRoot = requireActivity().getPreferences(Context.MODE_PRIVATE).getString("ros_root_package", "ryan_msgs") ?: "ryan_msgs"
            for ((index, action) in actions.withIndex()) {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                val btn = android.widget.Button(requireContext()).apply {
                    text = action.label
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                // Always create a new result/feedback display for this row
                val resultDisplay = TextView(requireContext()).apply {
                    textSize = 14f
                    setPadding(8, 8, 8, 8)
                }
                // Always add the result/feedback display below the button row, never replacing the row
                val resultLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                }
                resultLayout.addView(resultDisplay)

                btn.setOnClickListener {
                    if (rosViewModel.connectionStatus.value != "Connected") {
                        android.widget.Toast.makeText(requireContext(), "Not connected to rosbridge!", android.widget.Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val rawTopic = action.fieldValues["__topic__"] ?: "/${action.proto.name}"
                    val topic = if (rawTopic.startsWith("/")) rawTopic else "/$rawTopic"
                    val type = action.proto.type.name
                    val jsonBuilder = StringBuilder()
                    jsonBuilder.append("{")
                    val filtered = action.fieldValues.entries.filter { it.key != "__topic__" }
                    filtered.forEachIndexed { idx, (k, v) ->
                        jsonBuilder.append("\"").append(k).append("\": ")
                        val trimmed = v.trim()
                        val isJson = (trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))
                        if (isJson) {
                            try {
                                kotlinx.serialization.json.Json.parseToJsonElement(trimmed)
                                jsonBuilder.append(trimmed)
                            } catch (e: Exception) {
                                jsonBuilder.append(formatJsonValue(v))
                            }
                        } else {
                            jsonBuilder.append(formatJsonValue(v))
                        }
                        if (idx < filtered.size - 1) jsonBuilder.append(", ")
                    }
                    jsonBuilder.append("}")
                    val msgJson = jsonBuilder.toString()
                    android.util.Log.d("ROS2JSBridge", "Button clicked: topic=$topic, type=$type, json=$msgJson")

                    try {
                        when (action.proto.type) {
                            CustomProtocolsViewModel.ProtocolType.MSG -> {
                                val msgType = "$rosRoot/msg/${action.proto.name}"
                                rosViewModel.advertiseTopic(topic, msgType)
                                rosViewModel.publishCustomRawMessage(topic, msgType, msgJson)
                            }
                            CustomProtocolsViewModel.ProtocolType.SRV -> {
                                val srvType = "$rosRoot/srv/${action.proto.name}"
                                if (!advertisedServices.contains(topic)) {
                                    rosViewModel.advertiseService(topic, srvType)
                                    advertisedServices.add(topic)
                                }
                                rosViewModel.callCustomService(topic, srvType, msgJson)
                            }
                            CustomProtocolsViewModel.ProtocolType.ACTION -> {
                                requireActivity().runOnUiThread { resultDisplay.text = "" } // Clear previous result
                                val baseType = "$rosRoot/action/${action.proto.name}"
                                fun appendToDisplay(label: String, msg: String?) {
                                    requireActivity().runOnUiThread {
                                        val safeMsg = if (msg.isNullOrBlank()) "(no data)" else msg
                                        // Only append to the resultDisplay, never replace the row or buttons
                                        resultDisplay.append("[$label]:\n$safeMsg\n\n")
                                    }
                                }
                                val goalFields = try {
                                    kotlinx.serialization.json.Json.parseToJsonElement(msgJson).jsonObject
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(requireContext(), "Invalid action goal JSON", android.widget.Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }
                                val newGoalUuid = java.util.UUID.randomUUID().toString()
                                rosViewModel.sendOrQueueActionGoal(topic, baseType, goalFields, newGoalUuid) { resultMsg ->
                                    appendToDisplay("Result", resultMsg)
                                }
                                rosViewModel.subscribeToActionFeedback(topic, baseType) { feedbackMsg ->
                                    appendToDisplay("Feedback", feedbackMsg)
                                }
                                rosViewModel.subscribeToActionStatus(topic) { statusMsg ->
                                    appendToDisplay("Status", statusMsg)
                                }
                            }
                            else -> {
                                val msgType = "$rosRoot/msg/${action.proto.name}"
                                rosViewModel.advertiseTopic(topic, msgType)
                                rosViewModel.publishCustomRawMessage(topic, msgType, msgJson)
                            }
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(requireContext(), "ROS action failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                row.addView(btn)
                val editBtn = android.widget.Button(requireContext()).apply {
                    text = "O"
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    setOnClickListener {
                        // Set editing state and prefill config
                        editingActionIndex = index
                        editingFieldValues = action.fieldValues
                        // Select protocol in dropdown and prefill fields
                        val allProtocols = viewModel.actions.value + viewModel.messages.value + viewModel.services.value
                        val idx = allProtocols.indexOfFirst { it.importPath == action.proto.importPath }
                        if (idx >= 0) {
                            val selectedSet = viewModel.selected.value.toMutableSet()
                            selectedSet.add(action.proto.importPath)
                            viewModel.setSelected(selectedSet)
                        }
                        // Immediately update config fields UI by tag
                        val fragmentView = view ?: return@setOnClickListener
                        val configFields = fragmentView.findViewWithTag<LinearLayout>("configFields")
                        if (configFields != null) {
                            showConfigFields(configFields, action.proto, action.fieldValues)
                        }
                    }
                    contentDescription = "Edit ${action.label}"
                }
                val removeBtn = android.widget.Button(requireContext()).apply {
                    text = "✕"
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    setOnClickListener {
                        android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Delete Saved Action")
                            .setMessage("Are you sure you want to delete this saved action?")
                            .setPositiveButton("Delete") { _, _ ->
                                // Reset editing state if deleting
                                editingActionIndex = null
                                editingFieldValues = null
                                rosViewModel.removeCustomProtocolAction(index)
                                // Refresh config fields to default state
                                val fragmentView = view
                                if (fragmentView != null) {
                                    val configFields = fragmentView.findViewWithTag<LinearLayout>("configFields")
                                    val allProtocols = viewModel.actions.value + viewModel.messages.value + viewModel.services.value
                                    val selectedProtocols = allProtocols.filter { viewModel.selected.value.contains(it.importPath) }
                                    if (configFields != null) {
                                        if (selectedProtocols.isNotEmpty()) {
                                            showConfigFields(configFields, selectedProtocols[0])
                                        } else {
                                            configFields.removeAllViews()
                                        }
                                    }
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    contentDescription = "Remove ${action.label}"
                }
                row.addView(editBtn)
                row.addView(removeBtn)
                // Add the result display layout as a separate row below the button row
                layout.addView(row)
                layout.addView(resultLayout)
            }
        }
    }
}