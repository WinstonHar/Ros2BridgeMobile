package com.example.testros2jsbridge

import org.json.JSONArray
import org.json.JSONObject
import android.content.Context
import android.hardware.input.InputManager
import androidx.lifecycle.ViewModelProvider
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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sign
import androidx.core.content.edit

/*
    This fragment manages controller (gamepad/joystick) support, including mapping controller buttons to app actions and handling periodic joystick event resending.
*/

/*
    input:    tag - String, block - () -> Unit
    output:   None
    remarks:  Runs a block and catches resource errors, logging them
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

    // --- Companion Object for Constants and Helpers ---
    companion object {
        private const val TAG = "ControllerSupport"

        /*
            input:    context - Context, prefsName - String, key - String, list - List<T>, toJson - (T) -> JSONObject
            output:   None
            remarks:  Saves a list to SharedPreferences as JSON
        */
        private fun <T> saveListToPrefs(context: Context, prefsName: String, key: String, list: List<T>, toJson: (T) -> JSONObject) {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            list.forEach { item -> jsonArray.put(toJson(item)) }
            prefs.edit { putString(key, jsonArray.toString()) }
        }

        /*
            input:    context - Context, prefsName - String, key - String, fromJson - (JSONObject) -> T
            output:   MutableList<T>
            remarks:  Loads a list from SharedPreferences as JSON
        */
        private fun <T> loadListFromPrefs(context: Context, prefsName: String, key: String, fromJson: (JSONObject) -> T): MutableList<T> {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(key, null)
            val list = mutableListOf<T>()
            if (!jsonString.isNullOrEmpty()) {
                try {
                    val jsonArray = JSONArray(jsonString)
                    for (i in 0 until jsonArray.length()) {
                        list.add(fromJson(jsonArray.getJSONObject(i)))
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to parse JSON from $prefsName for key $key", e)
                }
            }
            return list
        }

        // --- ROS Message and Type Helpers ---
        /*
            input:    type - String
            output:   String
            remarks:  Returns the ROS standard message type string
        */
        fun getRosStdMsgType(type: String): String {
            return when (type) {
                "Bool", "Float32", "Float64", "Int16", "Int32", "Int64", "Int8",
                "UInt16", "UInt32", "UInt64", "UInt8", "String" -> "std_msgs/msg/$type"
                else -> if (type.contains("/")) type else "std_msgs/msg/Float32" // Default fallback
            }
        }

        /*
            input:    type - String, value - Any
            output:   String
            remarks:  Returns the JSON string for a ROS message
        */
        fun getRosStdMsgJson(type: String, value: Any): String {
            val data = when (type) {
                "Bool" -> if (value.toString().toFloatOrNull() != 0f) "true" else "false"
                "String" -> "\"$value\""
                else -> value.toString() // For numeric types
            }
            return "{\"data\": $data}"
        }

        // --- UI Creation Helpers ---
        /*
            input:    context - Context, container - LinearLayout, hint - String, initialValue - String?, inputType - Int
            output:   EditText
            remarks:  Creates and adds an EditText to a container
        */
        fun createSettingInput(
            context: Context,
            container: android.widget.LinearLayout,
            hint: String,
            initialValue: String?,
            inputType: Int = android.text.InputType.TYPE_CLASS_TEXT
        ): android.widget.EditText {
            val editText = android.widget.EditText(context).apply {
                this.hint = hint
                this.setText(initialValue ?: "")
                this.inputType = inputType
            }
            container.addView(editText)
            return editText
        }
    }

    // --- Joystick Mapping Data Class ---
    data class JoystickMapping(
        var displayName: String = "",
        var topic: String? = "",
        var type: String? = "",
        var axisX: Int = android.view.MotionEvent.AXIS_X,
        var axisY: Int = android.view.MotionEvent.AXIS_Y,
        var max: Float? = 1.0f,
        var step: Float? = 0.2f,
        var deadzone: Float? = 0.1f
    )

    // --- Joystick Mapping Persistence ---
    private val PREFS_JOYSTICK_MAPPINGS = "joystick_mappings"
    /*
        input:    mappings - List<JoystickMapping>
        output:   None
        remarks:  Saves joystick mappings to SharedPreferences
    */
    private fun saveJoystickMappings(mappings: List<JoystickMapping>) {
        saveListToPrefs(requireContext(), PREFS_JOYSTICK_MAPPINGS, "mappings", mappings) { mapping ->
            JSONObject().apply {
                put("displayName", mapping.displayName)
                put("topic", mapping.topic)
                put("type", mapping.type)
                put("axisX", mapping.axisX)
                put("axisY", mapping.axisY)
                put("max", mapping.max)
                put("step", mapping.step)
                put("deadzone", mapping.deadzone)
            }
        }
    }

    /*
        input:    None
        output:   MutableList<JoystickMapping>
        remarks:  Loads joystick mappings from SharedPreferences
    */
    private fun loadJoystickMappings(): MutableList<JoystickMapping> {
        val list = loadListFromPrefs(requireContext(), PREFS_JOYSTICK_MAPPINGS, "mappings") { obj ->
            JoystickMapping(
                displayName = obj.optString("displayName", "Joystick"),
                topic = obj.optString("topic", "").ifEmpty { null },
                type = obj.optString("type", "").ifEmpty { null },
                axisX = obj.optInt("axisX", android.view.MotionEvent.AXIS_X),
                axisY = obj.optInt("axisY", android.view.MotionEvent.AXIS_Y),
                max = obj.optDouble("max", 1.0).toFloat(),
                step = obj.optDouble("step", 0.2).toFloat(),
                deadzone = obj.optDouble("deadzone", 0.1).toFloat()
            )
        }
        if (list.isEmpty()) {
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
    @Suppress("DEPRECATION")
    private val joystickResendHandler = android.os.Handler(android.os.Looper.getMainLooper())
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

    // --- Controller Preset System (Refactored) ---
    data class ControllerPreset(
        var name: String = "Preset",
        var topic: String = "",
        var abxy: Map<String, String> = mapOf(
            "A" to "",
            "B" to "",
            "X" to "",
            "Y" to ""
        )
    )

    private val PREFS_CONTROLLER_PRESETS = "controller_presets"
    /*
        input:    presets - List<ControllerPreset>
        output:   None
        remarks:  Saves controller presets to SharedPreferences
    */
    private fun saveControllerPresets(presets: List<ControllerPreset>) {
        saveListToPrefs(requireContext(), PREFS_CONTROLLER_PRESETS, "presets", presets) { preset ->
            JSONObject().apply {
                put("name", preset.name)
                put("topic", preset.topic)
                put("abxy", JSONObject(preset.abxy as Map<*, *>))
            }
        }
    }

    /*
        input:    None
        output:   MutableList<ControllerPreset>
        remarks:  Loads controller presets from SharedPreferences
    */
    private fun loadControllerPresets(): MutableList<ControllerPreset> {
        val list = loadListFromPrefs(requireContext(), PREFS_CONTROLLER_PRESETS, "presets") { obj ->
            val abxyMap = mutableMapOf<String, String>()
            obj.optJSONObject("abxy")?.let { abxyObj ->
                abxyObj.keys().forEach { key ->
                    abxyMap[key] = abxyObj.getString(key)
                }
            }
            ControllerPreset(
                name = obj.optString("name", "Preset"),
                topic = obj.optString("topic", ""),
                abxy = abxyMap
            )
        }
        if (list.isEmpty()) {
            list.add(ControllerPreset("Default", "/cmd_vel", mapOf("A" to "", "B" to "", "X" to "", "Y" to "")))
        }
        return list
    }

    // --- Preset Management UI logic ---
    /*
        input:    root - View
        output:   None
        remarks:  Sets up the preset management UI
    */
    private fun setupPresetManagementUI(root: View) {
        val presetSpinner = root.findViewById<android.widget.Spinner>(R.id.spinner_presets)
        val nameEdit = root.findViewById<android.widget.EditText>(R.id.edit_preset_name)
        val topicEdit = root.findViewById<android.widget.EditText>(R.id.edit_preset_topic)
        val buttonSpinners = mapOf(
            "A" to root.findViewById<android.widget.Spinner>(R.id.spinner_abtn),
            "B" to root.findViewById<android.widget.Spinner>(R.id.spinner_bbtn),
            "X" to root.findViewById<android.widget.Spinner>(R.id.spinner_xbtn),
            "Y" to root.findViewById<android.widget.Spinner>(R.id.spinner_ybtn)
        )
        val addBtn = root.findViewById<android.widget.Button>(R.id.btn_add_preset)
        val removeBtn = root.findViewById<android.widget.Button>(R.id.btn_remove_preset)
        val saveBtn = root.findViewById<android.widget.Button>(R.id.btn_save_preset)

        val presets = loadControllerPresets()
        var selectedIdx = 0

        fun getAppActionNames() = listOf("") + (loadAvailableAppActions() + customProtocolAppActions).map { it.displayName }.distinct().sorted()

        // Helper to setup a single action spinner
        fun setupActionSpinner(spinner: android.widget.Spinner, actions: List<String>, selectedValue: String) {
            val listener = spinner.onItemSelectedListener
            spinner.onItemSelectedListener = null // Avoid re-triggering events
            
            spinner.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, actions).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            val selectionIndex = actions.indexOf(selectedValue).takeIf { it >= 0 } ?: 0
            spinner.setSelection(selectionIndex, false)

            spinner.onItemSelectedListener = listener // Restore listener
        }

        fun updateUI() {
            if (presets.isEmpty()) {
                presets.add(ControllerPreset("Default"))
                selectedIdx = 0
            }
            val presetNames = presets.map { it.name }
            val adapter = presetSpinner.adapter
            val listener = presetSpinner.onItemSelectedListener
            presetSpinner.onItemSelectedListener = null
            if (adapter is android.widget.ArrayAdapter<*>) {
                @Suppress("UNCHECKED_CAST")
                val stringAdapter = adapter as android.widget.ArrayAdapter<String>
                stringAdapter.clear()
                stringAdapter.addAll(presetNames)
                stringAdapter.notifyDataSetChanged()
            } else {
                presetSpinner.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, presetNames).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
            }
            val safeIdx = selectedIdx.coerceIn(0, presets.size - 1)
            if (presetSpinner.selectedItemPosition != safeIdx) {
                presetSpinner.setSelection(safeIdx, false)
            }
            presetSpinner.onItemSelectedListener = listener
            if (safeIdx < presets.size) {
                val preset = presets[safeIdx]
                if (!nameEdit.hasFocus()) nameEdit.setText(preset.name)
                if (!topicEdit.hasFocus()) topicEdit.setText(preset.topic)
                val appActionNames = getAppActionNames()
                buttonSpinners.forEach { (btnKey, spinner) ->
                    setupActionSpinner(spinner, appActionNames, preset.abxy[btnKey] ?: "")
                }
            }
        }

        fun saveCurrentPreset() {
            val preset = presets[selectedIdx]
            preset.name = nameEdit.text.toString().ifEmpty { "Preset" }
            preset.topic = topicEdit.text.toString().ifEmpty { "" }
            saveControllerPresets(presets)
            updateUI()
        }

        presetSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedIdx = position
                updateUI()
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
        }

        buttonSpinners.forEach { (btnKey, spinner) ->
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                    if (selectedIdx < presets.size) {
                        val selectedActionName = parent.getItemAtPosition(position) as? String ?: ""
                        val currentPreset = presets[selectedIdx]
                        if (currentPreset.abxy[btnKey] != selectedActionName) {
                            val newAbxy = currentPreset.abxy.toMutableMap()
                            newAbxy[btnKey] = selectedActionName
                            currentPreset.abxy = newAbxy
                        }
                    }
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }
        }

        nameEdit.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.post {
                    v.requestFocus()
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showSoftInput(v, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }

        updateUI()
    }

    // --- Joystick Mapping UI ---
    /*
        input:    root - View
        output:   None
        remarks:  Sets up the joystick mapping UI
    */
    private fun setupJoystickMappingUI(root: View) {
        val container = root.findViewById<android.widget.LinearLayout?>(R.id.joystick_mapping_container) ?: return
        container.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val mappings = loadJoystickMappings()
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                mappings.forEach { mapping ->
                    val group = android.widget.LinearLayout(requireContext()).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        setPadding(8, 16, 8, 16)
                    }
                    TextView(requireContext()).apply { text = mapping.displayName; textSize = 16f }.also { group.addView(it) }

                    // Use helper to create inputs
                    val topicInput = createSettingInput(requireContext(), group, "Topic", mapping.topic)
                    val typeInput = createSettingInput(requireContext(), group, "Type (e.g. geometry_msgs/msg/Twist)", mapping.type)
                    val numberInputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                    val maxInput = createSettingInput(requireContext(), group, "Max value (e.g. 1.0)", mapping.max?.toString(), numberInputType)
                    val stepInput = createSettingInput(requireContext(), group, "Step (e.g. 0.2)", mapping.step?.toString(), numberInputType)
                    val deadzoneInput = createSettingInput(requireContext(), group, "Deadzone (e.g. 0.1)", mapping.deadzone?.toString(), numberInputType)
                    val saveBtn = android.widget.Button(requireContext())
                    saveBtn.text = "Save"
                    saveBtn.setOnClickListener {
                        viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            mapping.topic = topicInput.text.toString().ifEmpty { null }
                            mapping.type = typeInput.text.toString().ifEmpty { null }
                            mapping.max = maxInput.text.toString().toFloatOrNull()
                            mapping.step = stepInput.text.toString().toFloatOrNull()
                            mapping.deadzone = deadzoneInput.text.toString().toFloatOrNull()
                            saveJoystickMappings(mappings)
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
        val app = requireActivity().application as MyApp
        rosViewModel = ViewModelProvider(
            app.appViewModelStore,
            ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )[RosViewModel::class.java]
        sliderControllerViewModel = ViewModelProvider(requireActivity())[SliderControllerViewModel::class.java]

        val view = try {
            inflater.inflate(R.layout.fragment_controller_support, container, false)
        } catch (e: Exception) {
            android.util.Log.e("ControllerSupport", "Failed to inflate layout: ${e.message}", e)
            return null
        }
        runWithResourceErrorCatching {
            controllerListText = view.findViewById(R.id.controllerListText)
            updateControllerList()
            setupPanelToggleUI(view)
            setupJoystickMappingUI(view)
            setupPresetManagementUI(view)

            appActionsList = view.findViewById<RecyclerView>(R.id.list_app_actions)
            appActionsList.layoutManager = LinearLayoutManager(requireContext())
            appActionsAdapter = AppActionsAdapter(mutableListOf())
            appActionsList.adapter = appActionsAdapter

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
                }
            }

            // Observe slider changes and update app actions pane live and ABXY spinners
            viewLifecycleOwner.lifecycleScope.launch {
                sliderControllerViewModel.sliders.collectLatest {
                    updateAppActions()
                }
            }

            updateAppActions()
            setupControllerMappingUI(view)
        }
        return view
    }

    /*
        input:    root - View
        output:   None
        remarks:  Sets up the controller mapping UI
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

    /*
        input:    None
        output:   None
        remarks:  Updates the app actions adapter with available actions
    */
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
        input:    actions - MutableList<AppAction>
        output:   None
        remarks:  RecyclerView adapter for app actions
    */
    inner class AppActionsAdapter(val actions: MutableList<AppAction>) : RecyclerView.Adapter<AppActionsAdapter.ActionViewHolder>() {
        inner class ActionViewHolder(val nameView: TextView, val detailsView: TextView, private val container: View) : RecyclerView.ViewHolder(container)
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
                append("Msg: ").append(action.msg.ifEmpty { "<not set>" })
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
        remarks:  Loads available app actions from SharedPreferences and ViewModel
    */
    private fun loadAvailableAppActions(): List<AppAction> {
        val actions = mutableListOf<AppAction>()
        // Load from SliderButtonFragment
        val prefs = requireContext().getSharedPreferences("slider_buttons_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("saved_slider_buttons", null)

        // Add increment/decrement actions for each slider, now with the index in the message
        val sliderStates = sliderControllerViewModel.sliders.value
        for ((idx, slider) in sliderStates.withIndex()) {
            // Store the INDEX in the message, which is what triggerAppAction needs.
            actions.add(
                AppAction(
                    displayName = "Increment ${slider.name}",
                    topic = slider.topic,
                    type = slider.type,
                    source = "SliderIncrement",
                    msg = idx.toString()
                )
            )
            actions.add(
                AppAction(
                    displayName = "Decrement ${slider.name}",
                    topic = slider.topic,
                    type = slider.type,
                    source = "SliderDecrement",
                    msg = idx.toString()
                )
            )
        }
        val geoPrefs = requireContext().getSharedPreferences("geometry_reusable_buttons", Context.MODE_PRIVATE)
        val geoJson = geoPrefs.getString("geometry_buttons", null)
        if (!geoJson.isNullOrEmpty()) {
            try {
                val arr = org.json.JSONArray(geoJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("label", obj.optString("topic", ""))
                    val topic = obj.optString("topic", "")
                    val type = obj.optString("type", "")
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
        //Add Cycle Presets action
        actions.add(
            AppAction(
                displayName = "Cycle Presets",
                topic = "",
                type = "Set",
                source = "Controller",
                msg = ""
            )
        )
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
        prefs.edit { putString("assignments", arr.toString()) }
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

    /*de
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
    private fun updateControllerList() {
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

    // --- Controller Key Event Handling (Refactored) ---
    fun onControllerKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val btnName = keyCodeToButtonName(event.keyCode) ?: return false
        val slider = sliderControllerViewModel.getSelectedSlider()
        if (slider != null && (btnName == "DPad Left" || btnName == "DPad Right")) {
            if (btnName == "DPad Left") sliderControllerViewModel.decrementSelectedSlider() else sliderControllerViewModel.incrementSelectedSlider()
            val updatedSlider = sliderControllerViewModel.getSelectedSlider() ?: return true
            val rosType = getRosStdMsgType(updatedSlider.type)
            val msg = getRosStdMsgJson(updatedSlider.type, updatedSlider.value)
            publishRosAction(updatedSlider.topic, rosType, msg)
            return true
        }
        val assignments = loadButtonAssignments(getControllerButtonList())
        assignments[btnName]?.let {
            triggerAppAction(it)
            return true
        }
        return false
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

    // --- Trigger App Action ---
    private fun triggerAppAction(action: AppAction) {
        // Helper to get the default message if the action's message is empty
        fun getDefaultMessage(action: AppAction): String {
            return when (action.source) {
                "Geometry" -> when (action.type) {
                    "geometry_msgs/msg/Twist" -> "{\"linear\":{\"x\":0.0,\"y\":0.0,\"z\":0.0},\"angular\":{\"x\":0.0,\"y\":0.0,\"z\":0.0}}"
                    "geometry_msgs/msg/Vector3" -> "{\"x\":0.0,\"y\":0.0,\"z\":0.0}"
                    else -> "{}"
                }
                "Standard Message" -> when (action.type) {
                    "std_msgs/msg/String" -> "{\"data\": \"pressed\"}"
                    "std_msgs/msg/Bool" -> "{\"data\": true}"
                    else -> "{\"data\": 1}"
                }
                else -> "{}"
            }
        }

        when (action.source) {
            "SliderIncrement", "SliderDecrement" -> {
                val sliderIndex = action.msg.toIntOrNull() ?: return

                sliderControllerViewModel.selectSlider(sliderIndex)
                if (action.source == "SliderIncrement") {
                    sliderControllerViewModel.incrementSelectedSlider()
                } else {
                    sliderControllerViewModel.decrementSelectedSlider()
                }

                // Get the updated slider to publish its new state
                val updatedSlider = sliderControllerViewModel.getSelectedSlider() ?: return
                val rosType = getRosStdMsgType(updatedSlider.type)
                val msg = getRosStdMsgJson(updatedSlider.type, updatedSlider.value)
                publishRosAction(updatedSlider.topic, rosType, msg)
            }
            "Geometry", "Standard Message", "Custom Protocol" -> {
                val message = action.msg.ifEmpty { getDefaultMessage(action) }
                publishRosAction(action.topic, action.type, message)
            }
            "CyclePreset" -> {
                val presets = loadControllerPresets()
                val prefs = requireContext().getSharedPreferences(PREFS_CONTROLLER_PRESETS, Context.MODE_PRIVATE)
                val currentIdx = prefs.getInt("selected_preset_idx", 0)
                val nextIdx = if (presets.isNotEmpty()) (currentIdx + 1) % presets.size else 0
                prefs.edit { putInt("selected_preset_idx", nextIdx) }
                android.util.Log.d(TAG, "Cycled to preset: ${presets.getOrNull(nextIdx)?.name}")
            }
            else -> android.util.Log.d(TAG, "Triggered unknown action source: ${action.source}")
        }
    }

    /*
        input:    topic - String, rosType - String, msg - String
        output:   None
        remarks:  Publishes a ROS action message
    */
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
                // Create local immutable copies to allow for smart casting
                val currentTopic = mapping.topic
                val currentType = mapping.type

                // Check that the required values are not null before proceeding
                if (currentTopic != null && currentType != null) {
                    val x = getCenteredAxis(event, dev, mapping.axisX, -1, mapping)
                    val y = getCenteredAxis(event, dev, mapping.axisY, -1, mapping)

                    if (currentType == "geometry_msgs/msg/TwistStamped") {
                        val header = "{\"stamp\":\"now\",\"frame_id\":\"base_link\"}"
                        val twist = "{\"linear\":{\"x\":$x,\"y\":0.0,\"z\":0.0},\"angular\":{\"x\":0.0,\"y\":0.0,\"z\":$y}}"
                        val msg = "{\"header\":$header,\"twist\":$twist}"
                        // Use the safe, non-null local variables
                        publishRosAction(currentTopic, currentType, msg)
                    } else if (currentType == "geometry_msgs/msg/Twist") {
                        val msg = "{\"linear\":{\"x\":$x,\"y\":0.0,\"z\":0.0},\"angular\":{\"x\":0.0,\"y\":0.0,\"z\":$y}}"
                        // Use the safe, non-null local variables
                        publishRosAction(currentTopic, currentType, msg)
                    }
                    handled = true
                }
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
        return if (abs(scaled) > deadzoneAxis) scaled else 0f
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
        val quantX = if (x != 0f) sign(x) * (ceil(((abs(x) - deadzoneValue).toDouble() / stepValue.toDouble())).toInt() * stepValue + deadzoneValue) else 0f
        val quantY = if (y != 0f) sign(y) * (ceil(((abs(y) - deadzoneValue).toDouble() / stepValue.toDouble())).toInt() * stepValue + deadzoneValue) else 0f
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