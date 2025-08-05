package com.example.testros2jsbridge

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject

/*
    ImportCustomProtocolsFragment scans the msgs folder for .msg, .srv, and .action files and displays them in three sections with checkboxes.
    User selections are managed via a ViewModel and can be used elsewhere in the app.
*/

class ImportCustomProtocolsFragment : Fragment() {
    
    private val advertisedActions = mutableSetOf<String>()
    private val advertisedServices = mutableSetOf<String>()

    /*
        input:    savedInstanceState - Bundle?
        output:   None
        remarks:  Called when the fragment is created; sets up ROS disconnect handler.
    */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rosViewModel.onRosbridgeDisconnected = {
            advertisedActions.clear()
            advertisedServices.clear()
        }
    }

    /*
        input:    value - String
        output:   String
        remarks:  Formats a JSON value, quoting strings and leaving numbers/bools/null as is.
    */
    private fun formatJsonValue(value: String): String {
        return if (value.matches(Regex("^-?\\d+(\\.\\d+)?$")) ||
            value.equals("true", ignoreCase = true) ||
            value.equals("false", ignoreCase = true) ||
            value.equals("null", ignoreCase = true)) {
            value
        } else if (value.trim().startsWith("[") && value.trim().endsWith("]")) {
            value
        } else {
            "\"" + value.replace("\"", "\\\"") + "\""
        }
    }
    private var editingActionIndex: Int? = null
    private var editingFieldValues: Map<String, String>? = null
    private val viewModel: CustomProtocolsViewModel by viewModels()
    private val rosViewModel: RosViewModel by lazy {
        val app = requireActivity().application as MyApp
        androidx.lifecycle.ViewModelProvider(
            app.appViewModelStore,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )[RosViewModel::class.java]
    }

    // Data class for a saved custom protocol action
    data class SavedCustomProtocolAction(
        val label: String,
        val proto: CustomProtocolsViewModel.ProtocolFile,
        val fieldValues: Map<String, String>
    )

    private val PREFS_KEY = "custom_protocol_actions"
    private val savedActions = mutableListOf<SavedCustomProtocolAction>()
    private var savedActionsLayout: LinearLayout? = null

    /*
        input:    inflater - LayoutInflater, container - ViewGroup?, savedInstanceState - Bundle?
        output:   View?
        remarks:  Inflates the fragment view and sets up protocol import UI.
    */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_import_custom_protocols, container, false)
        val context = requireContext()

        // Create main layout containers
        val msgSection = createSectionLayout(context, "Messages")
        val srvSection = createSectionLayout(context, "Services")
        val actionSection = createSectionLayout(context, "Actions")
        val scroll = ScrollView(context)
        val vertical = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        // Add rosbridge status and ROS root input
        val statusIndicator = createStatusIndicator(context)
        val rosRootInput = createRosRootInput(context)
        val rosRootLayout = createRosRootLayout(context, rosRootInput)
        vertical.addView(statusIndicator)
        vertical.addView(rosRootLayout)

        // Add protocol sections
        vertical.addView(actionSection)
        vertical.addView(msgSection)
        vertical.addView(srvSection)
        scroll.addView(vertical)
        (root as ViewGroup).addView(scroll)

        // Observe rosbridge connection status
        observeRosbridgeStatus(statusIndicator)

        // Save root package on change
        rosRootInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveRosRootInput(rosRootInput)
        }

        // Scan msgs folder in assets on open
        viewModel.scanMsgsAssets(requireContext())

        // Observe and update protocol sections
        observeProtocolsSection(viewModel.actions, actionSection, "Actions")
        observeProtocolsSection(viewModel.messages, msgSection, "Messages")
        observeProtocolsSection(viewModel.services, srvSection, "Services")

        // Add configuration area
        val configHeader = createConfigHeader(context)
        val configLayout = createConfigLayout(context)
        vertical.addView(configHeader)
        vertical.addView(configLayout)

        // Add dropdown and config fields
        val dropdown = android.widget.Spinner(context)
        configLayout.addView(dropdown)
        val configFields = createConfigFieldsLayout(context)
        configLayout.addView(configFields)

        // Observe selected protocols and update dropdown/config fields
        observeSelectedProtocols(dropdown, configFields)

        // Handle dropdown selection changes
        setupDropdownListener(dropdown, configFields)

        // Add saved actions area
        savedActionsLayout = createSavedActionsLayout(context)
        vertical.addView(savedActionsLayout)
        observeSavedActions()

        rosViewModel.loadCustomProtocolActionsFromPrefs()
        return root
    }

    // --- Helper functions ---

    private fun createSectionLayout(context: Context, title: String): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply { text = title })
        }
    }

    private fun createStatusIndicator(context: Context): TextView {
        return TextView(context).apply {
            text = "rosbridge: Connecting..."
            textSize = 16f
            setPadding(0, 8, 0, 8)
        }
    }

    private fun createRosRootInput(context: Context): android.widget.EditText {
        return android.widget.EditText(context).apply {
            hint = "ryan_msgs"
            setText(requireActivity().getPreferences(Context.MODE_PRIVATE).getString("ros_root_package", "ryan_msgs"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    private fun createRosRootLayout(context: Context, rosRootInput: android.widget.EditText): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(context).apply { text = "ROS Root Package: "; textSize = 16f })
            addView(rosRootInput)
        }
    }

    private fun saveRosRootInput(rosRootInput: android.widget.EditText) {
        val value = rosRootInput.text.toString().ifBlank { "ryan_msgs" }
        requireActivity().getPreferences(Context.MODE_PRIVATE).edit().putString("ros_root_package", value).apply()
    }

    private fun observeRosbridgeStatus(statusIndicator: TextView) {
        viewLifecycleOwner.lifecycleScope.launch {
            rosViewModel.connectionStatus.collectLatest { status ->
                statusIndicator.text = "rosbridge: $status"
                statusIndicator.setTextColor(
                    when {
                        status.equals("Connected", ignoreCase = true) -> 0xFF388E3C.toInt()
                        status.startsWith("Error", ignoreCase = true) -> 0xFFD32F2F.toInt()
                        else -> 0xFF757575.toInt()
                    }
                )
            }
        }
    }

    private fun observeProtocolsSection(
        flow: kotlinx.coroutines.flow.StateFlow<List<CustomProtocolsViewModel.ProtocolFile>>,
        section: LinearLayout,
        title: String
    ) {
        val context = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            flow.collectLatest { protos ->
                section.removeAllViews()
                section.addView(TextView(context).apply { text = title })
                protos.forEach { proto ->
                    val cb = CheckBox(context)
                    cb.text = "${proto.name} (${proto.importPath})"
                    cb.isChecked = viewModel.selected.value.contains(proto.importPath)
                    cb.setOnCheckedChangeListener { _, isChecked ->
                        val newSet = viewModel.selected.value.toMutableSet()
                        if (isChecked) newSet.add(proto.importPath) else newSet.remove(proto.importPath)
                        viewModel.setSelected(newSet)
                    }
                    section.addView(cb)
                }
            }
        }
    }

    private fun createConfigHeader(context: Context): TextView {
        return TextView(context).apply {
            text = "Configure Selected Protocol"
            textSize = 18f
            setPadding(0, 32, 0, 8)
        }
    }

    private fun createConfigLayout(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 8, 16, 8)
        }
    }

    private fun createConfigFieldsLayout(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            tag = "configFields"
        }
    }

    private fun observeSelectedProtocols(dropdown: android.widget.Spinner, configFields: LinearLayout) {
        val context = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selected.collectLatest { selectedSet ->
                val allProtocols = viewModel.actions.value + viewModel.messages.value + viewModel.services.value
                val selectedProtocols = allProtocols.filter { selectedSet.contains(it.importPath) }
                val names = selectedProtocols.map { "${it.name} (${it.type.name})" }
                val adapter = android.widget.ArrayAdapter(context, android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                dropdown.adapter = adapter
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
                    if (selectedProtocols.isNotEmpty()) {
                        showConfigFields(configFields, selectedProtocols[0])
                    } else {
                        configFields.removeAllViews()
                    }
                    dropdown.setSelection(0, false)
                }
            }
        }
    }

    private fun setupDropdownListener(dropdown: android.widget.Spinner, configFields: LinearLayout) {
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
    }

    private fun createSavedActionsLayout(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun observeSavedActions() {
        viewLifecycleOwner.lifecycleScope.launch {
            rosViewModel.customProtocolActions.collectLatest {
                refreshSavedActions(it)
            }
        }
    }

    /*
        input:    container - LinearLayout, proto - ProtocolFile, prefill - Map<String, String>?
        output:   None
        remarks:  Shows configuration fields for a selected protocol (.msg, .srv, .action).
    */
    private fun showConfigFields(
        container: LinearLayout,
        proto: CustomProtocolsViewModel.ProtocolFile,
        prefill: Map<String, String>? = null
    ) {
        container.removeAllViews()
        val context = requireContext()

        addProtocolInfoViews(container, proto)
        val sectionFields = getSectionFields(proto, context)
        val (editableFields, fixedFields) = splitFields(sectionFields, proto)

        addTopicField(container, proto, prefill)
        addEditableFields(container, editableFields, prefill)
        addFixedFields(container, fixedFields)

        addSaveAndCancelButtons(container, proto, prefill)
    }

    private fun addProtocolInfoViews(container: LinearLayout, proto: CustomProtocolsViewModel.ProtocolFile) {
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
    }

    private fun getSectionFields(
        proto: CustomProtocolsViewModel.ProtocolFile,
        context: Context
    ): List<Triple<String, String, String?>> {
        val sectionFields: Map<String, List<Triple<String, String, String?>>> = when (proto.type) {
            CustomProtocolsViewModel.ProtocolType.MSG -> mapOf("msg" to (parseFieldsBySection(proto.importPath, proto.type, context)["msg"] ?: emptyList()))
            CustomProtocolsViewModel.ProtocolType.SRV -> parseFieldsBySection(proto.importPath, proto.type, context)
            CustomProtocolsViewModel.ProtocolType.ACTION -> parseFieldsBySection(proto.importPath, proto.type, context)
        }
        return when (proto.type) {
            CustomProtocolsViewModel.ProtocolType.MSG -> sectionFields["msg"] ?: emptyList()
            CustomProtocolsViewModel.ProtocolType.SRV -> sectionFields["request"] ?: emptyList()
            CustomProtocolsViewModel.ProtocolType.ACTION -> sectionFields["goal"] ?: emptyList()
        }
    }

    private fun splitFields(
        fillableFields: List<Triple<String, String, String?>>,
        proto: CustomProtocolsViewModel.ProtocolFile
    ): Pair<List<Triple<String, String, String?>>, List<Triple<String, String, String?>>> {
        val fixedFields = fillableFields.filter { (_, name, _) -> name.matches(Regex("[A-Z0-9_]+")) }
        val editableFields = fillableFields.filter { (_, name, _) -> name.matches(Regex("[a-z0-9_]+")) }
        return Pair(editableFields, fixedFields)
    }

    private fun addTopicField(container: LinearLayout, proto: CustomProtocolsViewModel.ProtocolFile, prefill: Map<String, String>?) {
        val context = requireContext()
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
    }

    private fun addEditableFields(
        container: LinearLayout,
        editableFields: List<Triple<String, String, String?>>,
        prefill: Map<String, String>?
    ) {
        val context = requireContext()
        for ((type, name, _) in editableFields) {
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
    }

    private fun addFixedFields(
        container: LinearLayout,
        fixedFields: List<Triple<String, String, String?>>
    ) {
        val context = requireContext()
        for ((type, name, value) in fixedFields) {
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
    }

    private fun addSaveAndCancelButtons(
        container: LinearLayout,
        proto: CustomProtocolsViewModel.ProtocolFile,
        prefill: Map<String, String>?
    ) {
        val context = requireContext()
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
            handleSaveButtonClick(container, proto, context)
        }
        buttonLayout.addView(saveButton)
        if (editingActionIndex != null) {
            val cancelButton = android.widget.Button(context).apply {
                text = "Cancel"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f)
            }
            cancelButton.setOnClickListener {
                handleCancelButtonClick()
            }
            buttonLayout.addView(cancelButton)
        }
        container.addView(buttonLayout)
    }

    private fun handleSaveButtonClick(container: LinearLayout, proto: CustomProtocolsViewModel.ProtocolFile, context: Context) {
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
                    refreshConfigFieldsUI()
                } else {
                    rosViewModel.addCustomProtocolAction(RosViewModel.CustomProtocolAction(label, proto, finalValues))
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                editingActionIndex = null
                editingFieldValues = null
                refreshConfigFieldsUI()
            }
            .show()
    }

    private fun handleCancelButtonClick() {
        editingActionIndex = null
        editingFieldValues = null
        refreshConfigFieldsUI()
    }

    private fun refreshConfigFieldsUI() {
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

    private fun parseFieldsBySection(
        importPath: String,
        type: CustomProtocolsViewModel.ProtocolType,
        context: Context
    ): Map<String, List<Triple<String, String, String?>>> {
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

    /*
        input:    actions - List<CustomProtocolAction>
        output:   None
        remarks:  Refreshes the UI for all saved custom protocol actions.
    */
    private fun refreshSavedActions(actions: List<RosViewModel.CustomProtocolAction>) {
        savedActionsLayout?.let { layout ->
            layout.removeAllViews()
            val rosRoot = requireActivity().getPreferences(Context.MODE_PRIVATE)
                .getString("ros_root_package", "ryan_msgs") ?: "ryan_msgs"
            actions.forEachIndexed { index, action ->
                val (row, resultLayout) = createActionRow(index, action, rosRoot)
                layout.addView(row)
                layout.addView(resultLayout)
            }
            layout.addView(createForceClearButton())
        }
    }

    private fun createActionRow(
        index: Int,
        action: RosViewModel.CustomProtocolAction,
        rosRoot: String
    ): Pair<LinearLayout, LinearLayout> {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val btn = createActionButton(action, rosRoot, index)
        val resultDisplay = TextView(context).apply {
            textSize = 14f
            setPadding(8, 8, 8, 8)
        }
        val resultLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(resultDisplay)
        }
        btn.setOnClickListener {
            handleActionButtonClick(action, rosRoot, resultDisplay)
        }
        val editBtn = createEditButton(index, action)
        val removeBtn = createRemoveButton(index, action)
        row.addView(btn)
        row.addView(editBtn)
        row.addView(removeBtn)
        return Pair(row, resultLayout)
    }

    private fun createActionButton(
        action: RosViewModel.CustomProtocolAction,
        rosRoot: String,
        index: Int
    ): android.widget.Button {
        return android.widget.Button(requireContext()).apply {
            text = action.label
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    private fun handleActionButtonClick(
        action: RosViewModel.CustomProtocolAction,
        rosRoot: String,
        resultDisplay: TextView
    ) {
        if (rosViewModel.connectionStatus.value != "Connected") {
            android.widget.Toast.makeText(
                requireContext(),
                "Not connected to rosbridge!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        val (topic, msgJson) = buildTopicAndJson(action)
        try {
            when (action.proto.type) {
                CustomProtocolsViewModel.ProtocolType.MSG -> handleMsgAction(topic, rosRoot, action, msgJson)
                CustomProtocolsViewModel.ProtocolType.SRV -> handleSrvAction(topic, rosRoot, action, msgJson, resultDisplay)
                CustomProtocolsViewModel.ProtocolType.ACTION -> handleActionAction(topic, rosRoot, action, msgJson, resultDisplay)
                else -> handleMsgAction(topic, rosRoot, action, msgJson)
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                requireContext(),
                "ROS action failed: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun buildTopicAndJson(action: RosViewModel.CustomProtocolAction): Pair<String, String> {
        val rawTopic = action.fieldValues["__topic__"] ?: "/${action.proto.name}"
        val topic = if (rawTopic.startsWith("/")) rawTopic else "/$rawTopic"
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
        return Pair(topic, jsonBuilder.toString())
    }

    private fun handleMsgAction(topic: String, rosRoot: String, action: RosViewModel.CustomProtocolAction, msgJson: String) {
        val msgType = "$rosRoot/msg/${action.proto.name}"
        rosViewModel.advertiseTopic(topic, msgType)
        rosViewModel.publishCustomRawMessage(topic, msgType, msgJson)
    }

    private fun handleSrvAction(
        topic: String,
        rosRoot: String,
        action: RosViewModel.CustomProtocolAction,
        msgJson: String,
        resultDisplay: TextView
    ) {
        val srvType = "$rosRoot/srv/${action.proto.name}"
        if (!advertisedServices.contains(topic)) {
            rosViewModel.advertiseService(topic, srvType)
            advertisedServices.add(topic)
        }
        requireActivity().runOnUiThread { resultDisplay.text = "" }
        rosViewModel.callCustomService(topic, srvType, msgJson) { resultMsg ->
            rosViewModel.appendToMessageHistory(resultMsg)
            requireActivity().runOnUiThread {
                val safeMsg = resultMsg.ifBlank { "(no data)" }
                resultDisplay.append("[Service Result]:\n$safeMsg\n\n")
            }
        }
    }

    private fun handleActionAction(
        topic: String,
        rosRoot: String,
        action: RosViewModel.CustomProtocolAction,
        msgJson: String,
        resultDisplay: TextView
    ) {
        requireActivity().runOnUiThread { resultDisplay.text = "" }
        val baseType = "$rosRoot/action/${action.proto.name}"
        fun appendToDisplay(label: String, msg: String?) {
            requireActivity().runOnUiThread {
                val safeMsg = if (msg.isNullOrBlank()) "(no data)" else msg
                resultDisplay.append("[$label]:\n$safeMsg\n\n")
            }
        }
        val goalFields = try {
            kotlinx.serialization.json.Json.parseToJsonElement(msgJson).jsonObject
        } catch (e: Exception) {
            android.widget.Toast.makeText(requireContext(), "Invalid action goal JSON", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val newGoalUuid = java.util.UUID.randomUUID().toString()
        rosViewModel.sendOrQueueActionGoal(topic, baseType, goalFields, newGoalUuid) { resultMsg ->
            rosViewModel.appendToMessageHistory(resultMsg)
            appendToDisplay("Result", resultMsg)
        }
        rosViewModel.subscribeToActionFeedback(topic, baseType) { feedbackMsg ->
            appendToDisplay("Feedback", feedbackMsg)
        }
        rosViewModel.subscribeToActionStatus(topic) { statusMsg ->
            appendToDisplay("Status", statusMsg)
        }
    }

    private fun createEditButton(index: Int, action: RosViewModel.CustomProtocolAction): android.widget.Button {
        return android.widget.Button(requireContext()).apply {
            text = "O"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                editingActionIndex = index
                editingFieldValues = action.fieldValues
                val allProtocols = viewModel.actions.value + viewModel.messages.value + viewModel.services.value
                val idx = allProtocols.indexOfFirst { it.importPath == action.proto.importPath }
                if (idx >= 0) {
                    val selectedSet = viewModel.selected.value.toMutableSet()
                    selectedSet.add(action.proto.importPath)
                    viewModel.setSelected(selectedSet)
                }
                val fragmentView = view ?: return@setOnClickListener
                val configFields = fragmentView.findViewWithTag<LinearLayout>("configFields")
                if (configFields != null) {
                    showConfigFields(configFields, action.proto, action.fieldValues)
                }
            }
            contentDescription = "Edit ${action.label}"
        }
    }

    private fun createRemoveButton(index: Int, action: RosViewModel.CustomProtocolAction): android.widget.Button {
        return android.widget.Button(requireContext()).apply {
            text = "✕"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Saved Action")
                    .setMessage("Are you sure you want to delete this saved action?")
                    .setPositiveButton("Delete") { _, _ ->
                        editingActionIndex = null
                        editingFieldValues = null
                        rosViewModel.removeCustomProtocolAction(index)
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
    }

    private fun createForceClearButton(): android.widget.Button {
        return android.widget.Button(requireContext()).apply {
            text = "Force Clear"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                rosViewModel.forceClearAllActionBusyLocks()
                rosViewModel.forceClearAllServiceBusyLocks()
            }
            contentDescription = "Force clear all busy locks"
        }
    }
}