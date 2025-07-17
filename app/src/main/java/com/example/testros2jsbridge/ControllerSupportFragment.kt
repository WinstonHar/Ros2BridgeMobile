package com.example.testros2jsbridge

import android.content.Context
import android.hardware.input.InputManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

/*
    This fragment manages controller (gamepad/joystick) support, including mapping controller buttons to app actions and handling periodic joystick event resending.
*/

private fun runWithResourceErrorCatching(tag: String = "ControllerSupport", block: () -> Unit) {
    try {
        block()
    } catch (e: android.content.res.Resources.NotFoundException) {
        android.util.Log.e(tag, "Resource not found: ${e.message}")
    } catch (e: java.io.IOException) {
        android.util.Log.e(tag, "I/O error accessing APK or resource: ${e.message}")
    } catch (e: Exception) {
        android.util.Log.e(tag, "Unexpected error: ${e.message}", e)
    }
}

class ControllerSupportFragment : Fragment() {
    // --- Joystick Mapping Data Class ---
    data class JoystickMapping(
        var displayName: String = "",
        var topic: String? = null,
        var type: String? = null,
        var axisX: Int = android.view.MotionEvent.AXIS_X,
        var axisY: Int = android.view.MotionEvent.AXIS_Y,
        var max: Float? = 1.0f,
        var step: Float? = 0.2f,
        var deadzone: Float? = 0.1f
    )

    // --- Joystick Mapping Persistence ---
    private val PREFS_JOYSTICK_MAPPINGS = "joystick_mappings"
    private fun saveJoystickMappings(mappings: List<JoystickMapping>) {
        val prefs = requireContext().getSharedPreferences(PREFS_JOYSTICK_MAPPINGS, Context.MODE_PRIVATE)
        val arr = org.json.JSONArray()
        for (mapping in mappings) {
            val obj = org.json.JSONObject()
            obj.put("displayName", mapping.displayName)
            obj.put("topic", mapping.topic)
            obj.put("type", mapping.type)
            obj.put("axisX", mapping.axisX)
            obj.put("axisY", mapping.axisY)
            obj.put("max", mapping.max)
            obj.put("step", mapping.step)
            obj.put("deadzone", mapping.deadzone)
            arr.put(obj)
        }
        prefs.edit().putString("mappings", arr.toString()).apply()
    }
    private fun loadJoystickMappings(): MutableList<JoystickMapping> {
        val prefs = requireContext().getSharedPreferences(PREFS_JOYSTICK_MAPPINGS, Context.MODE_PRIVATE)
        val json = prefs.getString("mappings", null)
        val list = mutableListOf<JoystickMapping>()
        if (!json.isNullOrEmpty()) {
            try {
                val arr = org.json.JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        JoystickMapping(
                            displayName = obj.optString("displayName", "Joystick $i"),
                            topic = obj.optString("topic", null),
                            type = obj.optString("type", null),
                            axisX = obj.optInt("axisX", android.view.MotionEvent.AXIS_X),
                            axisY = obj.optInt("axisY", android.view.MotionEvent.AXIS_Y),
                            max = obj.optDouble("max", 1.0).toFloat(),
                            step = obj.optDouble("step", 0.2).toFloat(),
                            deadzone = obj.optDouble("deadzone", 0.1).toFloat()
                        )
                    )
                }
            } catch (_: Exception) {}
        } else {
            list.add(JoystickMapping("Left Stick"))
            list.add(JoystickMapping("Right Stick"))
        }
        return list
    }

    // --- Joystick resend state ---
    private var lastJoystickMappings: List<JoystickMapping>? = null
    private var lastJoystickDevice: InputDevice? = null
    private var lastJoystickEvent: MotionEvent? = null
    private var joystickResendActive: Boolean = false
    private val joystickResendHandler = android.os.Handler()
    private val joystickResendIntervalMs: Long = 200L
    private val joystickResendRunnable = object : Runnable {
        override fun run() {
            val mappings = lastJoystickMappings
            val dev = lastJoystickDevice
            val event = lastJoystickEvent
            if (mappings != null && dev != null && event != null) {
                for (mapping in mappings) {
                    processJoystickInput(event, dev, -1, mapping, forceSend = true)
                }
                joystickResendHandler.postDelayed(this, joystickResendIntervalMs)
            } else {
                joystickResendActive = false
            }
        }
    }

    // --- App Actions Adapter and List ---
    private lateinit var appActionsAdapter: AppActionsAdapter
    private lateinit var appActionsList: RecyclerView

    // --- Custom Protocol App Actions ---
    private var customProtocolAppActions: List<AppAction> = emptyList()
    private lateinit var sliderControllerViewModel: SliderControllerViewModel

    // --- Controller Preset System (ABXY) ---
    data class ControllerPreset(
        var name: String = "Preset",
        var abxy: Map<String, String> = mapOf(
            "A" to "",
            "B" to "",
            "X" to "",
            "Y" to ""
        )
    )

    private val PREFS_CONTROLLER_PRESETS = "controller_presets"

    private fun saveControllerPresets(presets: List<ControllerPreset>) {
        val prefs = requireContext().getSharedPreferences(PREFS_CONTROLLER_PRESETS, Context.MODE_PRIVATE)
        val arr = org.json.JSONArray()
        for (preset in presets) {
            val obj = org.json.JSONObject()
            obj.put("name", preset.name)
            val abxyObj = org.json.JSONObject()
            for ((btn, action) in preset.abxy) {
                abxyObj.put(btn, action)
            }
            obj.put("abxy", abxyObj)
            arr.put(obj)
        }
        prefs.edit().putString("presets", arr.toString()).apply()
    }

    private fun loadControllerPresets(): MutableList<ControllerPreset> {
        val prefs = requireContext().getSharedPreferences(PREFS_CONTROLLER_PRESETS, Context.MODE_PRIVATE)
        val json = prefs.getString("presets", null)
        val list = mutableListOf<ControllerPreset>()
        if (!json.isNullOrEmpty()) {
            try {
                val arr = org.json.JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("name", "Preset")
                    val abxyObj = obj.optJSONObject("abxy")
                    val abxy = mutableMapOf<String, String>()
                    if (abxyObj != null) {
                        for (btn in listOf("A", "B", "X", "Y")) {
                            abxy[btn] = abxyObj.optString(btn, "")
                        }
                    }
                    list.add(ControllerPreset(name, abxy))
                }
            } catch (_: Exception) {}
        } else {
            list.add(ControllerPreset("Default", mapOf("A" to "", "B" to "", "X" to "", "Y" to "")))
        }
        return list
    }

    // --- Preset Management UI logic ---
    private fun setupPresetManagementUI(root: View) {
        val presetSpinner = root.findViewById<android.widget.Spinner>(R.id.spinner_presets)
        val nameEdit = root.findViewById<android.widget.EditText>(R.id.edit_preset_name)
        val abtnSpinner = root.findViewById<android.widget.Spinner>(R.id.spinner_abtn)
        val bbtnSpinner = root.findViewById<android.widget.Spinner>(R.id.spinner_bbtn)
        val xbtnSpinner = root.findViewById<android.widget.Spinner>(R.id.spinner_xbtn)
        val ybtnSpinner = root.findViewById<android.widget.Spinner>(R.id.spinner_ybtn)
        val addBtn = root.findViewById<android.widget.Button>(R.id.btn_add_preset)
        val removeBtn = root.findViewById<android.widget.Button>(R.id.btn_remove_preset)
        val saveBtn = root.findViewById<android.widget.Button>(R.id.btn_save_preset)

        var presets = loadControllerPresets()
        var selectedIdx = 0

        fun getAppActionNames(): List<String> {
            return (loadAvailableAppActions() + customProtocolAppActions).map { it.displayName }.distinct().sorted()
        }

        fun updatePresetSpinners(preset: ControllerPreset) {
            val actions = listOf("") + getAppActionNames()
            val abxy = preset.abxy
            fun setSpinner(spinner: android.widget.Spinner, value: String) {
                val idx = actions.indexOf(value).takeIf { it >= 0 } ?: 0
                spinner.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, actions).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                spinner.setSelection(idx)
            }
            setSpinner(abtnSpinner, abxy["A"] ?: "")
            setSpinner(bbtnSpinner, abxy["B"] ?: "")
            setSpinner(xbtnSpinner, abxy["X"] ?: "")
            setSpinner(ybtnSpinner, abxy["Y"] ?: "")
        }

        fun updateUI() {
            if (presets.isEmpty()) presets.add(ControllerPreset("Default"))
            val presetNames = presets.map { it.name }
            presetSpinner.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, presetNames).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            presetSpinner.setSelection(selectedIdx.coerceIn(0, presets.size - 1))
            val preset = presets[selectedIdx]
            nameEdit.setText(preset.name)
            updatePresetSpinners(preset)
        }

        fun saveCurrentPreset() {
            val preset = presets[selectedIdx]
            preset.name = nameEdit.text.toString().ifEmpty { "Preset" }
            preset.abxy = mapOf(
                "A" to (abtnSpinner.selectedItem as? String ?: ""),
                "B" to (bbtnSpinner.selectedItem as? String ?: ""),
                "X" to (xbtnSpinner.selectedItem as? String ?: ""),
                "Y" to (ybtnSpinner.selectedItem as? String ?: "")
            )
            saveControllerPresets(presets)
            updateUI()
        }

        presetSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedIdx = position
                updateUI()
                updatePresetSpinners(presets[selectedIdx])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        addBtn.setOnClickListener {
            presets.add(ControllerPreset("Preset ${presets.size + 1}"))
            selectedIdx = presets.size - 1
            saveControllerPresets(presets)
            updateUI()
        }
        removeBtn.setOnClickListener {
            if (presets.size > 1) {
                presets.removeAt(selectedIdx)
                selectedIdx = selectedIdx.coerceAtMost(presets.size - 1)
                saveControllerPresets(presets)
                updateUI()
            }
        }
        saveBtn.setOnClickListener {
            saveCurrentPreset()
            android.widget.Toast.makeText(requireContext(), "Preset saved", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Update ABXY assignments on spinner change
        val abxySpinners = listOf(abtnSpinner, bbtnSpinner, xbtnSpinner, ybtnSpinner)
        for (spinner in abxySpinners) {
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                    // Optionally auto-save or update preview
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }
        }

        updateUI()
    }

    // Optimized: Loads joystick mappings off the main thread and updates UI on the main thread
    private fun setupJoystickMappingUI(root: View) {
        val container = root.findViewById<android.widget.LinearLayout?>(R.id.joystick_mapping_container)
            ?: return
        container.removeAllViews()
        // Use coroutine to load mappings in background
        viewLifecycleOwner.lifecycleScope.launch {
            val mappings = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                loadJoystickMappings()
            }
            // Now update UI on main thread
            for ((idx, mapping) in mappings.withIndex()) {
                val group = android.widget.LinearLayout(requireContext())
                group.orientation = android.widget.LinearLayout.VERTICAL
                group.setPadding(8, 16, 8, 16)
                val title = TextView(requireContext())
                title.text = mapping.displayName
                title.textSize = 16f
                group.addView(title)

                // Topic
                val topicInput = android.widget.EditText(requireContext())
                topicInput.hint = "Topic"
                topicInput.setText(mapping.topic ?: "")
                group.addView(topicInput)

                // Type
                val typeInput = android.widget.EditText(requireContext())
                typeInput.hint = "Type (e.g. geometry_msgs/msg/Twist)"
                typeInput.setText(mapping.type ?: "")
                group.addView(typeInput)

                // Max
                val maxInput = android.widget.EditText(requireContext())
                maxInput.hint = "Max value (e.g. 1.0)"
                maxInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                maxInput.setText(mapping.max?.toString() ?: "")
                group.addView(maxInput)

                // Step
                val stepInput = android.widget.EditText(requireContext())
                stepInput.hint = "Step (e.g. 0.2)"
                stepInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                stepInput.setText(mapping.step?.toString() ?: "")
                group.addView(stepInput)

                // Deadzone
                val deadzoneInput = android.widget.EditText(requireContext())
                deadzoneInput.hint = "Deadzone (e.g. 0.1)"
                deadzoneInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                deadzoneInput.setText(mapping.deadzone?.toString() ?: "")
                group.addView(deadzoneInput)

                // Save button
                val saveBtn = android.widget.Button(requireContext())
                saveBtn.text = "Save"
                saveBtn.setOnClickListener {
                    // Save on background thread
                    viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        mapping.topic = topicInput.text.toString().ifEmpty { null }
                        mapping.type = typeInput.text.toString().ifEmpty { null }
                        mapping.max = maxInput.text.toString().toFloatOrNull()
                        mapping.step = stepInput.text.toString().toFloatOrNull()
                        mapping.deadzone = deadzoneInput.text.toString().toFloatOrNull()
                        saveJoystickMappings(mappings)
                        // Show toast on main thread
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(requireContext(), "Joystick mapping saved", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                group.addView(saveBtn)
                container.addView(group)
            }
        }
    }

    private lateinit var rosViewModel: RosViewModel
    private lateinit var controllerListText: TextView

    /*
        input:    inflater - LayoutInflater, container - ViewGroup?, savedInstanceState - Bundle?
        output:   View?
        remarks:  Inflates the fragment view and sets up controller support UI
    */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Use application context and appViewModelStore for truly application-scoped RosViewModel
        val app = requireActivity().application as MyApp
        rosViewModel = ViewModelProvider(
            app.appViewModelStore,
            ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        ).get(RosViewModel::class.java)
        sliderControllerViewModel = ViewModelProvider(requireActivity()).get(SliderControllerViewModel::class.java)

        val view = try {
            inflater.inflate(R.layout.fragment_controller_support, container, false)
        } catch (e: Exception) {
            android.util.Log.e("ControllerSupport", "Failed to inflate layout: ${e.message}", e)
            // Optionally, return a fallback view or null
            return null
        }
        runWithResourceErrorCatching {
            controllerListText = view.findViewById(R.id.controllerListText)
            updateControllerList()
            setupPanelToggleUI(view)
            setupJoystickMappingUI(view)
            setupPresetManagementUI(view)

            // Setup app actions list and adapter
            appActionsList = view.findViewById<RecyclerView>(R.id.list_app_actions)
            appActionsList.layoutManager = LinearLayoutManager(requireContext())
            appActionsAdapter = AppActionsAdapter(mutableListOf())
            appActionsList.adapter = appActionsAdapter

            // Helper to update ABXY spinners in preset UI
            fun updatePresetAbxySpinners() {
                val presetContainer = view.findViewById<android.view.ViewGroup?>(R.id.preset_management_container) ?: return
                val abtnSpinner = presetContainer.findViewById<android.widget.Spinner>(R.id.spinner_abtn)
                val bbtnSpinner = presetContainer.findViewById<android.widget.Spinner>(R.id.spinner_bbtn)
                val xbtnSpinner = presetContainer.findViewById<android.widget.Spinner>(R.id.spinner_xbtn)
                val ybtnSpinner = presetContainer.findViewById<android.widget.Spinner>(R.id.spinner_ybtn)
                val presetSpinner = presetContainer.findViewById<android.widget.Spinner>(R.id.spinner_presets)
                val nameEdit = presetContainer.findViewById<android.widget.EditText>(R.id.edit_preset_name)
                if (abtnSpinner == null || bbtnSpinner == null || xbtnSpinner == null || ybtnSpinner == null || presetSpinner == null || nameEdit == null) return
                val presets = loadControllerPresets()
                val selectedIdx = presetSpinner.selectedItemPosition.coerceIn(0, presets.size - 1)
                val preset = presets[selectedIdx]
                val actions = listOf("") + (loadAvailableAppActions() + customProtocolAppActions).map { it.displayName }.distinct().sorted()
                fun setSpinner(spinner: android.widget.Spinner, value: String) {
                    val idx = actions.indexOf(value).takeIf { it >= 0 } ?: 0
                    spinner.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, actions).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                    spinner.setSelection(idx)
                }
                setSpinner(abtnSpinner, preset.abxy["A"] ?: "")
                setSpinner(bbtnSpinner, preset.abxy["B"] ?: "")
                setSpinner(xbtnSpinner, preset.abxy["X"] ?: "")
                setSpinner(ybtnSpinner, preset.abxy["Y"] ?: "")
            }

            // Observe custom protocol actions and update app actions list and ABXY spinners
            viewLifecycleOwner.lifecycleScope.launch {
                rosViewModel.customProtocolActions.collectLatest { customActions ->
                    val rosRoot = requireActivity().getPreferences(Context.MODE_PRIVATE)
                        .getString("ros_root_package", "ryan_msgs") ?: "ryan_msgs"
                    customProtocolAppActions = customActions.map { action ->
                        val rawTopic = action.fieldValues["__topic__"] ?: "/${action.proto.name}"
                        val topic = if (rawTopic.startsWith("/")) rawTopic else "/$rawTopic"
                        val rosType = when (action.proto.type.name) {
                            "MSG" -> "$rosRoot/msg/${action.proto.name}"
                            "SRV" -> "$rosRoot/srv/${action.proto.name}"
                            "ACTION" -> "$rosRoot/action/${action.proto.name}"
                            else -> action.proto.name
                        }
                        val msgFields = action.fieldValues.filter { it.key != "__topic__" }
                        val msgJson = buildString {
                            append("{")
                            msgFields.entries.forEachIndexed { idx, (k, v) ->
                                append("\"").append(k).append("\": ")
                                val asLong = v.toLongOrNull()
                                val asDouble = v.toDoubleOrNull()
                                val isBool = v.equals("true", true) || v.equals("false", true)
                                when {
                                    isBool -> append(v.toBoolean())
                                    asLong != null && v == asLong.toString() -> append(asLong)
                                    asDouble != null && v == asDouble.toString() -> append(asDouble)
                                    else -> append('"').append(v).append('"')
                                }
                                if (idx != msgFields.size - 1) append(", ")
                            }
                            append("}")
                        }
                        AppAction(
                            displayName = action.label,
                            topic = topic,
                            type = rosType,
                            source = "Custom Protocol",
                            msg = msgJson
                        )
                    }
                    updateAppActions()
                    updatePresetAbxySpinners()
                }
            }

            // Observe slider changes and update app actions pane live and ABXY spinners
            viewLifecycleOwner.lifecycleScope.launch {
                sliderControllerViewModel.sliders.collectLatest {
                    updateAppActions()
                    updatePresetAbxySpinners()
                }
            }

            updateAppActions()
            updatePresetAbxySpinners()
            setupControllerMappingUI(view)
        }
    // --- Preset Management UI logic ---
    fun setupPresetManagementUI(root: View) {
        val presetSpinner = root.findViewById<android.widget.Spinner>(R.id.spinner_presets)
        val nameEdit = root.findViewById<android.widget.EditText>(R.id.edit_preset_name)
        val abtnSpinner = root.findViewById<android.widget.Spinner>(R.id.spinner_abtn)
        val bbtnSpinner = root.findViewById<android.widget.Spinner>(R.id.spinner_bbtn)
        val xbtnSpinner = root.findViewById<android.widget.Spinner>(R.id.spinner_xbtn)
        val ybtnSpinner = root.findViewById<android.widget.Spinner>(R.id.spinner_ybtn)
        val addBtn = root.findViewById<android.widget.Button>(R.id.btn_add_preset)
        val removeBtn = root.findViewById<android.widget.Button>(R.id.btn_remove_preset)
        val saveBtn = root.findViewById<android.widget.Button>(R.id.btn_save_preset)

        var presets = loadControllerPresets()
        var selectedIdx = 0

        fun getAppActionNames(): List<String> {
            return (loadAvailableAppActions() + customProtocolAppActions).map { it.displayName }.distinct().sorted()
        }

        fun updatePresetSpinners(preset: ControllerPreset) {
            val actions = listOf("") + getAppActionNames()
            val abxy = preset.abxy
            fun setSpinner(spinner: android.widget.Spinner, value: String) {
                val idx = actions.indexOf(value).takeIf { it >= 0 } ?: 0
                spinner.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, actions).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                spinner.setSelection(idx)
            }
            setSpinner(abtnSpinner, abxy["A"] ?: "")
            setSpinner(bbtnSpinner, abxy["B"] ?: "")
            setSpinner(xbtnSpinner, abxy["X"] ?: "")
            setSpinner(ybtnSpinner, abxy["Y"] ?: "")
        }

        fun updateUI() {
            if (presets.isEmpty()) presets.add(ControllerPreset("Default"))
            val presetNames = presets.map { it.name }
            presetSpinner.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, presetNames).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            presetSpinner.setSelection(selectedIdx.coerceIn(0, presets.size - 1))
            val preset = presets[selectedIdx]
            nameEdit.setText(preset.name)
            updatePresetSpinners(preset)
        }

        fun saveCurrentPreset() {
            val preset = presets[selectedIdx]
            preset.name = nameEdit.text.toString().ifEmpty { "Preset" }
            preset.abxy = mapOf(
                "A" to (abtnSpinner.selectedItem as? String ?: ""),
                "B" to (bbtnSpinner.selectedItem as? String ?: ""),
                "X" to (xbtnSpinner.selectedItem as? String ?: ""),
                "Y" to (ybtnSpinner.selectedItem as? String ?: "")
            )
            saveControllerPresets(presets)
            updateUI()
        }

        presetSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedIdx = position
                updateUI()
                updatePresetSpinners(presets[selectedIdx])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        addBtn.setOnClickListener {
            presets.add(ControllerPreset("Preset ${presets.size + 1}"))
            selectedIdx = presets.size - 1
            saveControllerPresets(presets)
            updateUI()
        }
        removeBtn.setOnClickListener {
            if (presets.size > 1) {
                presets.removeAt(selectedIdx)
                selectedIdx = selectedIdx.coerceAtMost(presets.size - 1)
                saveControllerPresets(presets)
                updateUI()
            }
        }
        saveBtn.setOnClickListener {
            saveCurrentPreset()
            android.widget.Toast.makeText(requireContext(), "Preset saved", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Update ABXY assignments on spinner change
        val abxySpinners = listOf(abtnSpinner, bbtnSpinner, xbtnSpinner, ybtnSpinner)
        for (spinner in abxySpinners) {
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                    // Optionally auto-save or update preview
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }
        }

        updateUI()
    }
        return view
    }

    /*
        input:    root - View
        output:   None
        remarks:  Sets up the UI for mapping app actions to controller buttons
    */
    private fun setupControllerMappingUI(root: View) {
        // Detect controller buttons
        val controllerButtons = getControllerButtonList()
        val buttonAssignments = loadButtonAssignments(controllerButtons)
        lateinit var controllerButtonsAdapter: ControllerButtonsAdapter
        val controllerButtonsList = root.findViewById<RecyclerView>(R.id.list_controller_buttons)
        controllerButtonsAdapter = ControllerButtonsAdapter(controllerButtons, buttonAssignments, appActionsAdapter.actions) { btnName ->
            // Always reload app actions to get the latest msg values
            updateAppActions()
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Assign App Action to $btnName")
                .setItems(appActionsAdapter.actions.map { it.displayName }.toTypedArray()) { _, which ->
                    val selectedAction = appActionsAdapter.actions[which]
                    val newAssignments = buttonAssignments.toMutableMap()
                    newAssignments[btnName] = selectedAction
                    saveButtonAssignments(newAssignments)
                    buttonAssignments.clear(); buttonAssignments.putAll(newAssignments)
                    controllerButtonsAdapter.notifyDataSetChanged()
                }
                .setNegativeButton("Unassign") { _, _ ->
                    val newAssignments = buttonAssignments.toMutableMap()
                    newAssignments.remove(btnName)
                    saveButtonAssignments(newAssignments)
                    buttonAssignments.clear(); buttonAssignments.putAll(newAssignments)
                    controllerButtonsAdapter.notifyDataSetChanged()
                }
                .show()
        }
        controllerButtonsList.layoutManager = LinearLayoutManager(requireContext())
        controllerButtonsList.adapter = controllerButtonsAdapter
    }

    // Helper to update the app actions adapter with built-in and custom protocol actions
    private fun updateAppActions() {
        val allActions = loadAvailableAppActions() + customProtocolAppActions
        if (::appActionsAdapter.isInitialized) {
            appActionsAdapter.actions.clear()
            appActionsAdapter.actions.addAll(allActions)
            appActionsAdapter.notifyDataSetChanged()
        }
    }

    // Custom panel expand/collapse logic for App Actions
    private var isPanelVisible = true

    /*
        input:    root - View
        output:   None
        remarks:  Sets up the UI for toggling the visibility of the app actions panel
     */
    private fun setupPanelToggleUI(root: View) {
        val panel = root.findViewById<View>(R.id.panel_app_actions)
        val toggleBtn = root.findViewById<TextView>(R.id.btn_toggle_panel)

        fun updatePanelState() {
            panel.visibility = if (isPanelVisible) View.VISIBLE else View.GONE
            toggleBtn.text = if (isPanelVisible) "<" else ">"
        }

        toggleBtn.setOnClickListener {
            isPanelVisible = !isPanelVisible
            updatePanelState()
        }
        updatePanelState()
    }

    /*
        input:    text - String
        output:   String
        remarks:  Converts a string to a verticalized version (one char per line)
    */
    private fun verticalize(text: String): String = text.toCharArray().joinToString("\n")

    /*
        input:    actions - List<AppAction>
        output:   None
        remarks:  RecyclerView adapter for app actions
    */
    inner class AppActionsAdapter(val actions: MutableList<AppAction>) : RecyclerView.Adapter<AppActionsAdapter.ActionViewHolder>() {
        inner class ActionViewHolder(val nameView: TextView, val detailsView: TextView, val container: View) : RecyclerView.ViewHolder(container)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
            // Create a vertical LinearLayout with two TextViews
            val context = parent.context
            val layout = android.widget.LinearLayout(context)
            layout.orientation = android.widget.LinearLayout.VERTICAL
            layout.setPadding(16, 16, 16, 16)
            // Name (main text)
            val nameView = TextView(context)
            nameView.textSize = 16f
            nameView.setTextColor(android.graphics.Color.BLACK)
            // Details (type/values)
            val detailsView = TextView(context)
            detailsView.textSize = 13f
            detailsView.setTextColor(android.graphics.Color.DKGRAY)
            detailsView.setLineSpacing(0f, 1.1f)
            // Enable wrapping and max width for detailsView
            detailsView.setSingleLine(false)
            detailsView.ellipsize = null
            // Set max width to half of the screen width
            val displayMetrics = context.resources.displayMetrics
            val halfWidthPx = (displayMetrics.widthPixels * 0.4).toInt()
            detailsView.maxWidth = halfWidthPx
            // Add views
            layout.addView(nameView)
            layout.addView(detailsView)
            return ActionViewHolder(nameView, detailsView, layout)
        }
        override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
            val action = actions[position]
            holder.nameView.text = action.displayName
            // Compose details: each label on its own line, show actual user-set msg
            val details = buildString {
                if (action.type.isNotEmpty()) {
                    append("Type: ").append(action.type).append('\n')
                }
                if (action.topic.isNotEmpty()) {
                    append("Topic: ").append(action.topic).append('\n')
                }
                if (action.source.isNotEmpty()) {
                    append("Source: ").append(action.source).append('\n')
                }
                append("Msg: ").append(if (action.msg.isNotEmpty()) action.msg else "<not set>")
            }.trimEnd('\n')
            holder.detailsView.text = details
        }
        override fun getItemCount() = actions.size
    }

    /*
        input:    onItemClick - (String) -> Unit
        output:   None
        remarks:  RecyclerView adapter for controller buttons
    */
    inner class ControllerButtonsAdapter(
        private val buttons: List<String>,
        private val assignments: MutableMap<String, AppAction>,
        private val appActions: List<AppAction>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<ControllerButtonsAdapter.ButtonViewHolder>() {
        inner class ButtonViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
            val tv = TextView(parent.context)
            tv.textSize = 16f
            tv.setPadding(16, 16, 16, 16)
            tv.maxLines = 2
            tv.ellipsize = android.text.TextUtils.TruncateAt.END
            // Enable auto-size for dynamic horizontal fill
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                tv, 12, 22, 1, android.util.TypedValue.COMPLEX_UNIT_SP
            )
            tv.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            return ButtonViewHolder(tv)
        }
        override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
            val btnName = buttons[position]
            val action = assignments[btnName]
            holder.textView.text = if (action != null) "$btnName → ${action.displayName}" else btnName
            holder.textView.setOnClickListener { onItemClick(btnName) }
        }
        override fun getItemCount() = buttons.size
    }

    /*
        input:    None
        output:   List<AppAction>
        remarks:  Loads available app actions from SharedPreferences
    */
    private fun loadAvailableAppActions(): List<AppAction> {
        val actions = mutableListOf<AppAction>()
        // Load from SliderButtonFragment but do NOT add the base slider action ("Slider"), only add increment/decrement actions below
        val prefs = requireContext().getSharedPreferences("slider_buttons_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("saved_slider_buttons", null)
        android.util.Log.d("ControllerSupport", "Loaded slider_buttons_prefs:saved_slider_buttons: $json")
        // (Intentionally skip adding base slider actions here)
        // Add increment/decrement actions for each slider in the shared ViewModel
        val sliderStates = sliderControllerViewModel.sliders.value
        for ((idx, slider) in sliderStates.withIndex()) {
            // Increment preview
            val incNext = (slider.value + slider.step).coerceAtMost(slider.max)
            val incMsg = if (slider.value < slider.max)
                "${slider.value} + ${slider.step}"
            else
                "${slider.value}"
            actions.add(
                AppAction(
                    displayName = "Increment ${slider.name}",
                    topic = slider.topic,
                    type = slider.type,
                    source = "SliderIncrement",
                    msg = incMsg
                )
            )
            // Decrement preview
            val decNext = (slider.value - slider.step).coerceAtLeast(slider.min)
            val decMsg = if (slider.value > slider.min)
                "${slider.value} - ${slider.step}"
            else
                "${slider.value}"
            actions.add(
                AppAction(
                    displayName = "Decrement ${slider.name}",
                    topic = slider.topic,
                    type = slider.type,
                    source = "SliderDecrement",
                    msg = decMsg
                )
            )
        }
        // Load from GeometryStdMsgFragment
        val geoPrefs = requireContext().getSharedPreferences("geometry_reusable_buttons", Context.MODE_PRIVATE)
        val geoJson = geoPrefs.getString("geometry_buttons", null)
        android.util.Log.d("ControllerSupport", "Loaded geometry_reusable_buttons:geometry_buttons: $geoJson")
        if (!geoJson.isNullOrEmpty()) {
            try {
                val arr = org.json.JSONArray(geoJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("label", obj.optString("topic", ""))
                    val topic = obj.optString("topic", "")
                    val type = obj.optString("type", "")
                    // Read both "msg" and fallback to "message" if "msg" is missing
                    val msg = obj.optString("msg", obj.optString("message", ""))
                    actions.add(AppAction(name, topic, type, "Geometry", msg))
                }
            } catch (e: Exception) {
                android.util.Log.e("ControllerSupport", "Error loading geometry_reusable_buttons:geometry_buttons", e)
            }
        }
        // Load from CustomPublisherFragment (std_msgs)
        try {
            val prefs = requireContext().getSharedPreferences("custom_publishers_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("custom_publishers", null)
            android.util.Log.d("ControllerSupport", "Loaded custom_publishers_prefs:custom_publishers: $json")
            if (!json.isNullOrEmpty()) {
                val arr = com.google.gson.JsonParser.parseString(json).asJsonArray
                for (el in arr) {
                    val obj = el.asJsonObject
                    val name = obj["label"]?.asString ?: obj["topic"]?.asString ?: ""
                    val topic = obj["topic"]?.asString ?: ""
                    val type = obj["type"]?.asString ?: ""
                    val msg = if (obj.has("msg") && !obj["msg"].isJsonNull) {
                        obj["msg"].asString
                    } else {
                        ""
                    }
                    actions.add(AppAction(name, topic, type, "Standard Message", msg))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ControllerSupport", "Error loading custom_publishers_prefs:custom_publishers", e)
        }
        android.util.Log.d("ControllerSupport", "App actions loaded: $actions")
        return actions
    }

    /*
        input:    None
        output:   List<String>
        remarks:  Returns a list of common controller button names
    */
    private fun getControllerButtonList(): List<String> = listOf(
        "Button A", "Button B", "Button X", "Button Y",
        "DPad Up", "DPad Down", "DPad Left", "DPad Right",
        "L1", "R1", "L2", "R2", "Start", "Select"
    )

    private val PREFS_CONTROLLER_ASSIGN = "controller_button_assignments"

    /*
        input:    assignments - Map<String, AppAction>
        output:   None
        remarks:  Saves controller button assignments to SharedPreferences
    */
    private fun saveButtonAssignments(assignments: Map<String, AppAction>) {
        val prefs = requireContext().getSharedPreferences(PREFS_CONTROLLER_ASSIGN, Context.MODE_PRIVATE)
        val arr = org.json.JSONArray()
        for ((btn, action) in assignments) {
            val obj = org.json.JSONObject()
            obj.put("button", btn)
            obj.put("name", action.displayName)
            obj.put("topic", action.topic)
            obj.put("type", action.type)
            obj.put("source", action.source)
            obj.put("msg", action.msg)
            arr.put(obj)
        }
        prefs.edit().putString("assignments", arr.toString()).apply()
    }

    /*
        input:    controllerButtons - List<String>
        output:   MutableMap<String, AppAction>
        remarks:  Loads controller button assignments from SharedPreferences
    */
    private fun loadButtonAssignments(controllerButtons: List<String>): MutableMap<String, AppAction> {
        val prefs = requireContext().getSharedPreferences(PREFS_CONTROLLER_ASSIGN, Context.MODE_PRIVATE)
        val json = prefs.getString("assignments", null)
        val map = mutableMapOf<String, AppAction>()
        if (!json.isNullOrEmpty()) {
            try {
                val arr = org.json.JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val btn = obj.getString("button")
                    val name = obj.getString("name")
                    val topic = obj.getString("topic")
                    val type = obj.getString("type")
                    val source = obj.optString("source", "")
                    val msg = obj.optString("msg", "")
                    if (btn in controllerButtons) {
                        map[btn] = AppAction(name, topic, type, source, msg)
                    }
                }
            } catch (_: Exception) {}
        }
        return map
    }

    /*
        input:    displayName, topic, type, source, msg
        output:   None
        remarks:  Data class for app actions
    */
    data class AppAction(val displayName: String, val topic: String, val type: String, val source: String, val msg: String = "")

    /*
        input:    None
        output:   None
        remarks:  Updates the list of connected controllers in the UI
    */
    fun updateControllerList() {
        val inputManager = requireContext().getSystemService(Context.INPUT_SERVICE) as InputManager
        val deviceIds: IntArray = inputManager.inputDeviceIds
        val controllers = mutableListOf<InputDevice>()
        for (i in deviceIds.indices) {
            val id = deviceIds[i]
            val dev = InputDevice.getDevice(id)
            if (dev != null && isGameController(dev)) {
                controllers.add(dev)
            }
        }
        val sb = StringBuilder("Connected controllers:\n")
        if (controllers.isEmpty()) {
            sb.append("None detected.")
        } else {
            for (dev in controllers) {
                sb.append("- ").append(dev.name).append(" (id=").append(dev.id).append(")\n")
            }
        }
        controllerListText.text = sb.toString()
    }

    /*
        input:    device - InputDevice
        output:   Boolean
        remarks:  Checks if the device is a game controller
    */
    private fun isGameController(device: InputDevice): Boolean {
        val sources = device.sources
        return (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
               (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
    }

    /*
        input:    event - KeyEvent
        output:   Boolean
        remarks:  Handles controller key events and dispatches to mapped actions
    */
    fun onControllerKeyEvent(event: KeyEvent): Boolean {
        val actionStr = when (event.action) {
            KeyEvent.ACTION_DOWN -> "DOWN"
            KeyEvent.ACTION_UP -> "UP"
            else -> "OTHER"
        }
        val btnName = keyCodeToButtonName(event.keyCode)
        val msg = "Key $actionStr: code=${event.keyCode} (${KeyEvent.keyCodeToString(event.keyCode)}) from device ${event.device?.name}\n"
        appendEventLog(msg)

        // --- Slider integration: DPad Left/Right to decrement/increment selected slider ---
        if (actionStr == "DOWN" && btnName != null) {
            // Map DPad Left/Right to slider decrement/increment
            if (btnName == "DPad Left" || btnName == "DPad Right") {
                val slider = sliderControllerViewModel.getSelectedSlider()
                if (slider != null) {
                    if (btnName == "DPad Left") {
                        sliderControllerViewModel.decrementSelectedSlider()
                    } else {
                        sliderControllerViewModel.incrementSelectedSlider()
                    }
                    // After update, publish the new value as a ROS message
                    val updatedSlider = sliderControllerViewModel.getSelectedSlider()
                    if (updatedSlider != null) {
                        val rosType = when (updatedSlider.type) {
                            "Bool" -> "std_msgs/msg/Bool"
                            "Float32" -> "std_msgs/msg/Float32"
                            "Float64" -> "std_msgs/msg/Float64"
                            "Int16" -> "std_msgs/msg/Int16"
                            "Int32" -> "std_msgs/msg/Int32"
                            "Int64" -> "std_msgs/msg/Int64"
                            "Int8" -> "std_msgs/msg/Int8"
                            "UInt16" -> "std_msgs/msg/UInt16"
                            "UInt32" -> "std_msgs/msg/UInt32"
                            "UInt64" -> "std_msgs/msg/UInt64"
                            "UInt8" -> "std_msgs/msg/UInt8"
                            else -> "std_msgs/msg/Float32"
                        }
                        val msg = when (updatedSlider.type) {
                            "Bool" -> "{\"data\": ${if (updatedSlider.value != 0f) "true" else "false"}}"
                            "Float32", "Float64" -> "{\"data\": ${updatedSlider.value}}"
                            "Int16", "Int32", "Int64", "Int8", "UInt16", "UInt32", "UInt64", "UInt8" -> "{\"data\": ${updatedSlider.value.toLong()}}"
                            else -> "{\"data\": ${updatedSlider.value}}"
                        }
                        publishRosAction(updatedSlider.topic, rosType, msg)
                    }
                    return true // handled
                }
            }
            // Load assignments and dispatch if mapped
            val assignments = loadButtonAssignments(getControllerButtonList())
            val mappedAction = assignments[btnName]
            if (mappedAction != null) {
                triggerAppAction(mappedAction)
                return true // handled
            }
        }
        return false // not handled or not mapped
    }

    /*
        input:    keyCode - Int
        output:   String?
        remarks:  Maps Android keyCode to button names, follows xbox layout
    */
    private fun keyCodeToButtonName(keyCode: Int): String? {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> "Button A"
            KeyEvent.KEYCODE_BUTTON_B -> "Button B"
            KeyEvent.KEYCODE_BUTTON_X -> "Button X"
            KeyEvent.KEYCODE_BUTTON_Y -> "Button Y"
            KeyEvent.KEYCODE_DPAD_UP -> "DPad Up"
            KeyEvent.KEYCODE_DPAD_DOWN -> "DPad Down"
            KeyEvent.KEYCODE_DPAD_LEFT -> "DPad Left"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "DPad Right"
            KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
            KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
            KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
            KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_MENU -> "Start"
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BACK -> "Select"
            else -> null
        }
    }

    /*
        input:    action - AppAction
        output:   None
        remarks:  Dispatches the mapped app action (publishing logic)
    */
    private fun triggerAppAction(action: AppAction) {
        // Dispatch based on action.source and type, using RosViewModel
        when (action.source) {
            "Slider" -> {
                val rosType = when (action.type) {
                    "Bool" -> "std_msgs/msg/Bool"
                    "Float32" -> "std_msgs/msg/Float32"
                    "Float64" -> "std_msgs/msg/Float64"
                    "Int16" -> "std_msgs/msg/Int16"
                    "Int32" -> "std_msgs/msg/Int32"
                    "Int64" -> "std_msgs/msg/Int64"
                    "Int8" -> "std_msgs/msg/Int8"
                    "UInt16" -> "std_msgs/msg/UInt16"
                    "UInt32" -> "std_msgs/msg/UInt32"
                    "UInt64" -> "std_msgs/msg/UInt64"
                    "UInt8" -> "std_msgs/msg/UInt8"
                    else -> "std_msgs/msg/Float32"
                }
                val msg = if (action.msg.isNotEmpty()) {
                    action.msg
                } else {
                    when (action.type) {
                        "Bool" -> "{\"data\": true}"
                        "Float32", "Float64" -> "{\"data\": 1.0}"
                        "Int16", "Int32", "Int64", "Int8", "UInt16", "UInt32", "UInt64", "UInt8" -> "{\"data\": 1}"
                        else -> "{\"data\": 1.0}"
                    }
                }
                publishRosAction(action.topic, rosType, msg)
            }
            "SliderIncrement" -> {
                val idx = action.msg.toIntOrNull() ?: return
                sliderControllerViewModel.selectSlider(idx)
                sliderControllerViewModel.incrementSelectedSlider()
                val slider = sliderControllerViewModel.getSelectedSlider() ?: return
                val rosType = when (slider.type) {
                    "Bool" -> "std_msgs/msg/Bool"
                    "Float32" -> "std_msgs/msg/Float32"
                    "Float64" -> "std_msgs/msg/Float64"
                    "Int16" -> "std_msgs/msg/Int16"
                    "Int32" -> "std_msgs/msg/Int32"
                    "Int64" -> "std_msgs/msg/Int64"
                    "Int8" -> "std_msgs/msg/Int8"
                    "UInt16" -> "std_msgs/msg/UInt16"
                    "UInt32" -> "std_msgs/msg/UInt32"
                    "UInt64" -> "std_msgs/msg/UInt64"
                    "UInt8" -> "std_msgs/msg/UInt8"
                    else -> "std_msgs/msg/Float32"
                }
                val msg = when (slider.type) {
                    "Bool" -> "{\"data\": ${if (slider.value != 0f) "true" else "false"}}"
                    "Float32", "Float64" -> "{\"data\": ${slider.value}}"
                    "Int16", "Int32", "Int64", "Int8", "UInt16", "UInt32", "UInt64", "UInt8" -> "{\"data\": ${slider.value.toLong()}}"
                    else -> "{\"data\": ${slider.value}}"
                }
                publishRosAction(slider.topic, rosType, msg)
            }
            "SliderDecrement" -> {
                val idx = action.msg.toIntOrNull() ?: return
                sliderControllerViewModel.selectSlider(idx)
                sliderControllerViewModel.decrementSelectedSlider()
                val slider = sliderControllerViewModel.getSelectedSlider() ?: return
                val rosType = when (slider.type) {
                    "Bool" -> "std_msgs/msg/Bool"
                    "Float32" -> "std_msgs/msg/Float32"
                    "Float64" -> "std_msgs/msg/Float64"
                    "Int16" -> "std_msgs/msg/Int16"
                    "Int32" -> "std_msgs/msg/Int32"
                    "Int64" -> "std_msgs/msg/Int64"
                    "Int8" -> "std_msgs/msg/Int8"
                    "UInt16" -> "std_msgs/msg/UInt16"
                    "UInt32" -> "std_msgs/msg/UInt32"
                    "UInt64" -> "std_msgs/msg/UInt64"
                    "UInt8" -> "std_msgs/msg/UInt8"
                    else -> "std_msgs/msg/Float32"
                }
                val msg = when (slider.type) {
                    "Bool" -> "{\"data\": ${if (slider.value != 0f) "true" else "false"}}"
                    "Float32", "Float64" -> "{\"data\": ${slider.value}}"
                    "Int16", "Int32", "Int64", "Int8", "UInt16", "UInt32", "UInt64", "UInt8" -> "{\"data\": ${slider.value.toLong()}}"
                    else -> "{\"data\": ${slider.value}}"
                }
                publishRosAction(slider.topic, rosType, msg)
            }
            "Geometry" -> {
                val rosType = if (action.type.contains("/")) action.type else "geometry_msgs/msg/${action.type}"
                val msg = if (action.msg.isNotEmpty()) {
                    action.msg
                } else {
                    when (rosType) {
                        "geometry_msgs/msg/Twist" -> "{\"linear\":{\"x\":0.0,\"y\":0.0,\"z\":0.0},\"angular\":{\"x\":0.0,\"y\":0.0,\"z\":0.0}}"
                        "geometry_msgs/msg/Vector3" -> "{\"x\":0.0,\"y\":0.0,\"z\":0.0}"
                        else -> "{}"
                    }
                }
                publishRosAction(action.topic, rosType, msg)
            }
            "Standard Message" -> {
                val rosType = if (action.type.contains("/")) action.type else "std_msgs/msg/${action.type}"
                val msg = if (action.msg.isNotEmpty()) {
                    action.msg
                } else {
                    when (rosType) {
                        "std_msgs/msg/String" -> "{\"data\": \"pressed\"}"
                        "std_msgs/msg/Bool" -> "{\"data\": true}"
                        else -> "{\"data\": 1}"
                    }
                }
                publishRosAction(action.topic, rosType, msg)
            }
            "Custom Protocol" -> {
                val rosType = if (action.type.contains("/")) action.type else action.type
                val msg = if (action.msg.isNotEmpty()) action.msg else "{}"
                publishRosAction(action.topic, rosType, msg)
            }
            else -> {
                android.util.Log.d("ControllerSupport", "[Unknown] $action")
            }
        }
    }

    // Helper to reduce code duplication for publishing
    private fun publishRosAction(topic: String, rosType: String, msg: String) {
        rosViewModel.advertiseTopic(topic, rosType)
        rosViewModel.publishCustomRawMessage(topic, rosType, msg)
    }

    /*
        input:    event - MotionEvent
        output:   Boolean
        remarks:  Handles controller joystick/motion events
    */
    fun onControllerMotionEvent(event: MotionEvent): Boolean {
        val dev = event.device ?: return false
        var handled = false
        // Joystick mappings
        val mappings = loadJoystickMappings()
        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK && event.action == MotionEvent.ACTION_MOVE) {
            for (mapping in mappings) {
                // Process historical and current positions
                for (i in 0 until event.historySize) {
                    processJoystickInput(event, dev, i, mapping)
                }
                processJoystickInput(event, dev, -1, mapping)
                handled = true
            }
            // Start periodic resend
            lastJoystickMappings = mappings
            lastJoystickDevice = dev
            lastJoystickEvent = MotionEvent.obtain(event)
            if (!joystickResendActive) {
                joystickResendActive = true
                joystickResendHandler.postDelayed(joystickResendRunnable, joystickResendIntervalMs)
            }
        } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            // Stop periodic resend
            joystickResendHandler.removeCallbacks(joystickResendRunnable)
            joystickResendActive = false
            lastJoystickMappings = null
            lastJoystickDevice = null
            lastJoystickEvent = null
        }
        // Log axes for debug
        val axes = listOf(
            MotionEvent.AXIS_X, MotionEvent.AXIS_Y, MotionEvent.AXIS_Z,
            MotionEvent.AXIS_RX, MotionEvent.AXIS_RY, MotionEvent.AXIS_RZ,
            MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_RTRIGGER
        )
        val sb = StringBuilder("Motion: ${dev.name} (id=${dev.id})\n")
        for (axis in axes) {
            val v = event.getAxisValue(axis)
            if (v != 0f) {
                sb.append("  ${MotionEvent.axisToString(axis)}: $v\n")
            }
        }
        appendEventLog(sb.toString())
        return handled
    }

    /*
        input:    event - MotionEvent, device - InputDevice, axis - Int, historyPos - Int, mapping - JoystickMapping
        output:   Float
        remarks:  Gets the centered axis value for joystick input
    */
    private fun getCenteredAxis(event: MotionEvent, device: InputDevice, axis: Int, historyPos: Int, mapping: JoystickMapping): Float {
        val range = device.getMotionRange(axis, event.source) ?: return 0f
        val value = if (historyPos < 0) event.getAxisValue(axis) else event.getHistoricalAxisValue(axis, historyPos)
        // Normalize to [-1, 1] based on physical range
        val flat = range.flat
        val min = range.min + flat
        val max = range.max - flat
        val normalized = when {
            value > 0f -> value / max
            value < 0f -> value / -min
            else -> 0f
        }
        // Now scale to [-mapping.max, mapping.max]
        val maxAxis = mapping.max ?: 1.0f
        val deadzoneAxis = mapping.deadzone ?: 0.1f
        val scaled = normalized * maxAxis
        // Apply deadzone
        return if (Math.abs(scaled) > deadzoneAxis) scaled else 0f
    }

    private var lastJoystickSent: MutableMap<String, Pair<Float, Float>> = mutableMapOf()

    /*
        input:    event - MotionEvent, device - InputDevice, historyPos - Int, mapping - JoystickMapping, forceSend - Boolean
        output:   None
        remarks:  Processes joystick input and publishes messages if needed
    */
    private fun processJoystickInput(event: MotionEvent, device: InputDevice, historyPos: Int, mapping: JoystickMapping, forceSend: Boolean = false) {
        val x = getCenteredAxis(event, device, mapping.axisX, historyPos, mapping)
        val y = getCenteredAxis(event, device, mapping.axisY, historyPos, mapping)
        val maxValue = mapping.max ?: 1.0f
        val stepValue = mapping.step ?: 0.2f
        val deadzoneValue = mapping.deadzone ?: 0.1f
        // Quantize to step (step is in user units, between deadzone and max)
        val quantX = if (x != 0f) Math.signum(x) * (Math.ceil(((Math.abs(x) - deadzoneValue).toDouble() / stepValue.toDouble())).toInt() * stepValue + deadzoneValue) else 0f
        val quantY = if (y != 0f) Math.signum(y) * (Math.ceil(((Math.abs(y) - deadzoneValue).toDouble() / stepValue.toDouble())).toInt() * stepValue + deadzoneValue) else 0f
        // Clamp to max
        val clampedX = quantX.coerceIn(-maxValue, maxValue)
        val clampedY = quantY.coerceIn(-maxValue, maxValue)
        val displayNameKey = mapping.displayName ?: ""
        val last = lastJoystickSent[displayNameKey]

        // Only send stop if previously nonzero and now truly centered (not just in deadzone)
        // Always ensure mapping.type is fully qualified
        val rosType = if ((mapping.type ?: "").contains("/")) mapping.type ?: "geometry_msgs/msg/Twist" else "geometry_msgs/msg/${mapping.type ?: "Twist"}"
        val topic = mapping.topic ?: "/cmd_vel"
        if (clampedX == 0f && clampedY == 0f) {
            if (last != null && (last.first != 0f || last.second != 0f)) {
                lastJoystickSent[displayNameKey] = 0f to 0f
                val msg = when (rosType) {
                    "geometry_msgs/msg/Twist" -> "{\"linear\":{\"x\":0.0,\"y\":0.0,\"z\":0.0},\"angular\":{\"x\":0.0,\"y\":0.0,\"z\":0.0}}"
                    "geometry_msgs/msg/Vector3" -> "{\"x\":0.0,\"y\":0.0,\"z\":0.0}"
                    else -> "{}"
                }
                rosViewModel.advertiseTopic(topic, rosType)
                rosViewModel.publishCustomRawMessage(topic, rosType, msg)
            }
            // Do not keep sending stop if already at zero
            return
        }

        // Only send if changed or forceSend is true, and not in deadzone
        if (forceSend || last == null || last.first != clampedX || last.second != clampedY) {
            lastJoystickSent[displayNameKey] = clampedX to clampedY
            val msg = when (rosType) {
                "geometry_msgs/msg/Twist" -> "{" +
                        "\"linear\": {\"x\": ${-clampedY}, \"y\": 0.0, \"z\": 0.0}," +
                        "\"angular\": {\"x\": 0.0, \"y\": 0.0, \"z\": $clampedX}" +
                        "}"
                "geometry_msgs/msg/Vector3" -> "{\"x\":$clampedX,\"y\":0.0,\"z\":${-clampedY}}"
                else -> "{}"
            }
            rosViewModel.advertiseTopic(topic, rosType)
            rosViewModel.publishCustomRawMessage(topic, rosType, msg)
        }
    }

    /*
        input:    msg - String
        output:   None
        remarks:  Appends a message to the event log (no-op in this version)
    */
    private fun appendEventLog(msg: String) {
        // No-op: event log UI is not present in this version
    }

    /*
        input:    None
        output:   None
        remarks:  Updates controller list on resume
    */
    override fun onResume() {
        super.onResume()
        updateControllerList()
    }
}