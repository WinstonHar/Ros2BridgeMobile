package com.example.testros2jsbridge

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

/*
    ImportCustomProtocolsFragment scans the msgs folder for .msg, .srv, and .action files and displays them in three sections with checkboxes.
    User selections are managed via a ViewModel and can be used elsewhere in the app.
*/

class ImportCustomProtocolsFragment : Fragment() {
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
        val configFields = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
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
                // Show config fields for the first selected by default
                if (selectedProtocols.isNotEmpty()) {
                    showConfigFields(configFields, selectedProtocols[0])
                } else {
                    configFields.removeAllViews()
                }
                dropdown.setSelection(0, false)
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
    private fun showConfigFields(container: LinearLayout, proto: CustomProtocolsViewModel.ProtocolFile) {
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

        // Helper to parse .msg, .srv, .action files
        // Now returns Triple<type, name, value?> for fixed fields
        fun parseFieldsFromAsset(importPath: String, type: CustomProtocolsViewModel.ProtocolType): List<Triple<String, String, String?>> {
            val fields = mutableListOf<Triple<String, String, String?>>()
            context.assets.open(importPath).bufferedReader().useLines { lines ->
                if (type == CustomProtocolsViewModel.ProtocolType.SRV) {
                    var isResponse = false
                    lines.forEach { line ->
                        val clean = line.trim()
                        if (clean.startsWith("#")) return@forEach
                        if (clean == "---") { isResponse = true; return@forEach }
                        if (clean.isEmpty()) return@forEach
                        val parts = clean.split(Regex("\\s+"))
                        if (parts.size >= 2) {
                            val label = if (isResponse) "response" else "request"
                            // Check for constant (fixed) value
                            if (parts.size >= 4 && parts[2] == "=") {
                                fields.add(Triple("$label:${parts[0]}", parts[1], parts[3]))
                            } else {
                                fields.add(Triple("$label:${parts[0]}", parts[1], null))
                            }
                        }
                    }
                } else {
                    lines.forEach { line ->
                        val clean = line.trim()
                        if (clean.isEmpty() || clean.startsWith("#")) return@forEach
                        val parts = clean.split(Regex("\\s+"))
                        if (parts.size >= 2) {
                            // Check for constant (fixed) value
                            if (parts.size >= 4 && parts[2] == "=") {
                                fields.add(Triple(parts[0], parts[1], parts[3]))
                            } else {
                                fields.add(Triple(parts[0], parts[1], null))
                            }
                        }
                    }
                }
            }
            return fields
        }

        val fields = when (proto.type) {
            CustomProtocolsViewModel.ProtocolType.MSG -> parseFieldsFromAsset(proto.importPath, proto.type)
            CustomProtocolsViewModel.ProtocolType.SRV -> parseFieldsFromAsset(proto.importPath, proto.type)
            CustomProtocolsViewModel.ProtocolType.ACTION -> parseFieldsFromAsset(proto.importPath, proto.type)
        }

        // Separate fillable and fixed fields
        val fillableFields = fields.filter { (_, name, _) -> name.matches(Regex("[a-z0-9_]+")) }
        val fixedFields = fields.filter { (_, name, _) -> name.matches(Regex("[A-Z0-9_]+")) }

        // Add topic field (configurable, prefilled with /rawtopic)
        val topicLayout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val topicLabel = TextView(context).apply { text = "Topic" }
        val topicInput = android.widget.EditText(context).apply {
            hint = "Topic"
            tag = "__topic__"
            setText("/${proto.name}")
        }
        topicLayout.addView(topicLabel)
        topicLayout.addView(topicInput)
        container.addView(topicLayout)

        // Show fillable fields as editable
        fillableFields.forEach { (type, name, value) ->
            val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            val label = TextView(context).apply { text = "$type $name" }
            val input = android.widget.EditText(context).apply { hint = name; tag = name }
            layout.addView(label)
            layout.addView(input)
            container.addView(layout)
        }
        // Show fixed fields as non-editable, display their constant value if present
        fixedFields.forEach { (type, name, value) ->
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

        // Add Save as App Action button
        val saveButton = android.widget.Button(context).apply { text = "Save as App Action" }
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
            android.app.AlertDialog.Builder(context)
                .setTitle("Save Custom Protocol Action")
                .setView(labelInput)
                .setPositiveButton("Save") { _, _ ->
                    val label = labelInput.text.toString().ifBlank { proto.name }
                    // Store topic as a special key in fieldValues
                    val finalValues = values.toMutableMap()
                    if (topicValue != null) finalValues["__topic__"] = topicValue!!
                    rosViewModel.addCustomProtocolAction(RosViewModel.CustomProtocolAction(label, proto, finalValues))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        container.addView(saveButton)
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
                    setOnClickListener {
                        // Publish the custom protocol action using the same logic as ControllerSupportFragment
                        // Use configured topic if present, else fallback to /proto.name
                        val rawTopic = action.fieldValues["__topic__"] ?: "/${action.proto.name}"
                        val topic = if (rawTopic.startsWith("/")) rawTopic else "/$rawTopic"
                        val type = action.proto.type.name
                        // Use full ROS 2 type string (e.g. ryan_msgs/msg/AvatarEmotion)
                        val rosType = when (action.proto.type) {
                            CustomProtocolsViewModel.ProtocolType.MSG -> "$rosRoot/msg/${action.proto.name}"
                            CustomProtocolsViewModel.ProtocolType.SRV -> "$rosRoot/srv/${action.proto.name}"
                            CustomProtocolsViewModel.ProtocolType.ACTION -> "$rosRoot/action/${action.proto.name}"
                            else -> action.proto.name
                        }
                        // Compose message from fieldValues as JSON (simple flat map), skip __topic__
                        // Output numbers as numbers, not as strings
                        val msgJson = action.fieldValues.entries.filter { it.key != "__topic__" }
                            .joinToString(", ", "{", "}") { (k, v) -> "\"$k\": $v" }
                        rosViewModel.advertiseTopic(topic, rosType)
                        rosViewModel.publishCustomRawMessage(topic, rosType, msgJson)
                    }
                }
                val removeBtn = android.widget.Button(requireContext()).apply {
                    text = "✕"
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    setOnClickListener {
                        android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Delete Saved Action")
                            .setMessage("Are you sure you want to delete this saved action?")
                            .setPositiveButton("Delete") { _, _ ->
                                rosViewModel.removeCustomProtocolAction(index)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    contentDescription = "Remove ${action.label}"
                }
                row.addView(btn)
                row.addView(removeBtn)
                layout.addView(row)
            }
        }
    }
}