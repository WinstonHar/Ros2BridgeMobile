package com.example.testros2jsbridge

import android.content.Context
import android.hardware.input.InputManager
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sign

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

    /*
        input:    next - boolean
        output:   None
        remarks:  creates the app actions for cycling presets forwards and backwards
    */
    fun cyclePreset(next: Boolean = true) {
        val action = if (next) {
            AppAction(
                displayName = "Cycle Presets Forwards",
                topic = "/ignore",
                type = "Set",
                source = "CyclePresetForward",
                msg = ""
            )
        } else {
            AppAction(
                displayName = "Cycle Presets Backwards",
                topic = "/ignore",
                type = "Set",
                source = "CyclePresetBackward",
                msg = ""
            )
        }
        triggerAppAction(action)
    }

    /*
        input:    keyCode - Int, event - KeyEvent
        output:   None
        remarks:  Handler for key down events
    */
    fun handleKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) return false
        return onControllerKeyEvent(event)
    }

    /*
        input:    keyCode - Int, event - KeyEvent
        output:   None
        remarks:  Handler for key up events
    */
    fun handleKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        //not implemeneted
        return false
    }

    /*
        input:    event - MotionEvent
        output:   None
        remarks:  Handler for generic motion events
    */
    fun handleGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event == null) return false
        return onControllerMotionEvent(event)
    }

    // Callback for activity to set
    var presetOverlayCallback: ControllerOverviewActivity.PresetOverlayCallback? = null

    // --- Config Data ---
    private val joystickMappings: MutableList<JoystickMapping> = mutableListOf()
    private val controllerPresets: MutableList<ControllerPreset> = mutableListOf()
    private val buttonAssignments: MutableMap<String, AppAction> = mutableMapOf()
    private val appActions: MutableList<AppAction> = mutableListOf()

    // --- Joystick Addressing Mode ---
    private enum class JoystickAddressingMode(val displayName: String) { DIRECT("Direct"), INVERTED_ROTATED("Inverted/Rotated") }
    private var joystickAddressingMode: JoystickAddressingMode = JoystickAddressingMode.DIRECT
    private val PREFS_JOYSTICK_ADDRESSING = "joystick_addressing_mode"

    /*
        input:    mode - JoystickAddressingMode
        output:   None
        remarks:  Saves the selected joystick addressing mode to shared preferences
    */
    private fun saveJoystickAddressingMode(mode: JoystickAddressingMode) {
        val prefs = requireContext().getSharedPreferences(PREFS_JOYSTICK_MAPPINGS, Context.MODE_PRIVATE)
        prefs.edit { putString(PREFS_JOYSTICK_ADDRESSING, mode.name) }
    }

    /*
        input:    None
        output:   joystick addressing mode (or direct by default)
        remarks:  returns users selected addressing mode from sharedpreferences
    */
    private fun loadJoystickAddressingMode(): JoystickAddressingMode {
        val prefs = requireContext().getSharedPreferences(PREFS_JOYSTICK_MAPPINGS, Context.MODE_PRIVATE)
        val name = prefs.getString(PREFS_JOYSTICK_ADDRESSING, JoystickAddressingMode.DIRECT.name)
        return JoystickAddressingMode.entries.find { it.name == name } ?: JoystickAddressingMode.DIRECT
    }

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
        var axisX: Int = MotionEvent.AXIS_X,
        var axisY: Int = MotionEvent.AXIS_Y,
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
    fun loadJoystickMappings(): MutableList<JoystickMapping> {
        val list = loadListFromPrefs(requireContext(), PREFS_JOYSTICK_MAPPINGS, "mappings") { obj ->
            JoystickMapping(
                displayName = obj.optString("displayName", "Joystick"),
                topic = obj.optString("topic", "").ifEmpty { null },
                type = obj.optString("type", "").ifEmpty { null },
                axisX = obj.optInt("axisX", MotionEvent.AXIS_X),
                axisY = obj.optInt("axisY", MotionEvent.AXIS_Y),
                max = obj.optDouble("max", 1.0).toFloat(),
                step = obj.optDouble("step", 0.2).toFloat(),
                deadzone = obj.optDouble("deadzone", 0.1).toFloat()
            )
        }
        if (list.isEmpty() && joystickMappings.isEmpty()) {
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
    private val joystickResendHandler = android.os.Handler(android.os.Looper.getMainLooper())
    // Default to 5 times per second
    private var joystickPublishRate: Int = 5
    private val joystickResendRunnable = object : Runnable {
        override fun run() {
            val mappings = lastJoystickMappings
            val dev = lastJoystickDevice
            val event = lastJoystickEvent
            if (mappings != null && dev != null && event != null) {
                for (mapping in mappings) {
                    processJoystickInput(event, dev, -1, mapping, forceSend = true)
                }
                val intervalMs = if (joystickPublishRate > 0) (1000L / joystickPublishRate) else 200L
                joystickResendHandler.postDelayed(this, intervalMs)
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
    fun loadControllerPresets(): MutableList<ControllerPreset> {
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

    private fun setupActionSpinner(
        spinner: android.widget.Spinner,
        actions: List<String>,
        selectedValue: String
    ) {
        val listener = spinner.onItemSelectedListener
        spinner.onItemSelectedListener = null
        spinner.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, actions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val selectionIndex = actions.indexOf(selectedValue).takeIf { it >= 0 } ?: 0
        spinner.setSelection(selectionIndex, false)
        spinner.onItemSelectedListener = listener
    }

    private fun ensureDefaultPreset(presets: MutableList<ControllerPreset>, selectedIdxRef: (Int) -> Unit) {
        if (presets.isEmpty()) {
            presets.add(ControllerPreset("Default"))
            selectedIdxRef(0)
        }
    }

    private fun updatePresetSpinnerAdapter(presetSpinner: android.widget.Spinner, presetNames: List<String>) {
        val adapter = presetSpinner.adapter
        if (adapter is android.widget.ArrayAdapter<*>) {
            @Suppress("UNCHECKED_CAST")
            val stringAdapter = adapter as android.widget.ArrayAdapter<String>
            stringAdapter.clear()
            stringAdapter.addAll(presetNames)
            stringAdapter.notifyDataSetChanged()
        } else {
            presetSpinner.adapter = android.widget.ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                presetNames
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
    }

    private fun setSpinnerSelectionSafely(
        presetSpinner: android.widget.Spinner,
        safeIdx: Int,
        listener: android.widget.AdapterView.OnItemSelectedListener?
    ) {
        if (presetSpinner.selectedItemPosition != safeIdx) {
            presetSpinner.setSelection(safeIdx, false)
        }
        presetSpinner.onItemSelectedListener = listener
    }

    private fun updatePresetFields(
        presets: MutableList<ControllerPreset>,
        safeIdx: Int,
        nameEdit: android.widget.EditText,
        buttonSpinners: Map<String, android.widget.Spinner>
    ) {
        if (safeIdx < presets.size) {
            val preset = presets[safeIdx]
            if (!nameEdit.hasFocus()) nameEdit.setText(preset.name)
            val appActionNames = 
                listOf("") + (loadAvailableAppActions() + customProtocolAppActions).map { it.displayName }.distinct().sorted()
            buttonSpinners.forEach { (btnKey, spinner) ->
                setupActionSpinner(spinner, appActionNames, preset.abxy[btnKey] ?: "")
            }
        }
    }

    private data class PresetUI(
        val presetSpinner: android.widget.Spinner,
        val nameEdit: android.widget.EditText,
        val buttonSpinners: Map<String, android.widget.Spinner>,
        val addBtn: Button,
        val removeBtn: Button,
        val saveBtn: Button
    )

    private fun initPresetUI(root: View): PresetUI {
        return PresetUI(
            presetSpinner = root.findViewById(R.id.spinner_presets),
            nameEdit = root.findViewById(R.id.edit_preset_name),
            buttonSpinners = mapOf(
                "A" to root.findViewById(R.id.spinner_abtn),
                "B" to root.findViewById(R.id.spinner_bbtn),
                "X" to root.findViewById(R.id.spinner_xbtn),
                "Y" to root.findViewById(R.id.spinner_ybtn)
            ),
            addBtn = root.findViewById(R.id.btn_add_preset),
            removeBtn = root.findViewById(R.id.btn_remove_preset),
            saveBtn = root.findViewById(R.id.btn_save_preset)
        )
    }

    private fun setupPresetSpinnerListener(
        presetSpinner: android.widget.Spinner,
        onPresetSelected: (Int) -> Unit
    ){
        presetSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                onPresetSelected(position)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    private fun updatePresetUI(
        presets: MutableList<ControllerPreset>,
        selectedIdx: Int,
        presetSpinner: android.widget.Spinner,
        nameEdit: android.widget.EditText,
        buttonSpinners: Map<String, android.widget.Spinner>
    ){
        ensureDefaultPreset(presets) { /* ignore idx update here */ }
        val presetNames = presets.map { it.name }
        val listener = presetSpinner.onItemSelectedListener
        presetSpinner.onItemSelectedListener = null
        updatePresetSpinnerAdapter(presetSpinner, presetNames)
        val safeIdx = selectedIdx.coerceIn(0, presets.size - 1)
        setSpinnerSelectionSafely(presetSpinner, safeIdx, listener)
        updatePresetFields(presets, safeIdx, nameEdit, buttonSpinners)
    }

    private fun setupPresetManagementUI(root: View) {
        val ui = initPresetUI(root)
        val presets = loadControllerPresets()
        var selectedIdx = 0

        fun updateUI() = updatePresetUI(presets, selectedIdx, ui.presetSpinner, ui.nameEdit, ui.buttonSpinners)
        fun saveCurrentPreset() {
            val preset = presets[selectedIdx]
            preset.name = ui.nameEdit.text.toString().ifEmpty { "Preset" }
            saveControllerPresets(presets)
            updateUI()
        }

        setupPresetSpinnerListener(ui.presetSpinner) { idx ->
            selectedIdx = idx
            updateUI()
        }

        ui.addBtn.setOnClickListener {
            presets.add(ControllerPreset("Preset ${presets.size + 1}"))
            selectedIdx = presets.size - 1
            saveControllerPresets(presets)
            updateUI()
        }
        ui.removeBtn.setOnClickListener {
            if (presets.size > 1) {
                presets.removeAt(selectedIdx)
                selectedIdx = selectedIdx.coerceAtMost(presets.size - 1)
                saveControllerPresets(presets)
                updateUI()
            }
        }
        ui.saveBtn.setOnClickListener { saveCurrentPreset() }

        ui.buttonSpinners.forEach { (btnKey, spinner) ->
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

        ui.nameEdit.setOnFocusChangeListener { v, hasFocus ->
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
                addPublishRateSection(container)
                addAddressingModeSection(container)
                addJoystickMappingSections(container, mappings)
            }
        }
    }

    private fun addPublishRateSection(container: android.widget.LinearLayout) {
        val rateGroup = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
        }
        val rateLabel = TextView(requireContext()).apply {
            text = "Joystick Publish Rate (Hz): "
            textSize = 15f
        }
        joystickPublishRate = loadJoystickPublishRate()
        val rateInput = android.widget.EditText(requireContext()).apply {
            hint = "e.g. 5"
            setText(joystickPublishRate.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        rateGroup.addView(rateLabel)
        rateGroup.addView(rateInput)
        container.addView(rateGroup, 0)
        rateInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val rate = s?.toString()?.toIntOrNull()
                if (rate != null && rate > 0) {
                    joystickPublishRate = rate
                    saveJoystickPublishRate(rate)
                    if (joystickResendActive) {
                        joystickResendHandler.removeCallbacks(joystickResendRunnable)
                        val intervalMs = if (joystickPublishRate > 0) (1000L / joystickPublishRate) else 200L
                        joystickResendHandler.postDelayed(joystickResendRunnable, intervalMs)
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun addAddressingModeSection(container: android.widget.LinearLayout) {
        val addressingGroup = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
        }
        val addressingLabel = TextView(requireContext()).apply {
            text = "Joystick Addressing Mode: "
            textSize = 15f
        }
        val addressingSpinner = android.widget.Spinner(requireContext())
        val addressingModes = JoystickAddressingMode.entries.map { it.displayName }
        addressingSpinner.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, addressingModes).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        joystickAddressingMode = loadJoystickAddressingMode()
        addressingSpinner.setSelection(JoystickAddressingMode.entries.indexOf(joystickAddressingMode), false)
        addressingSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                joystickAddressingMode = JoystickAddressingMode.entries.toTypedArray()[position]
                saveJoystickAddressingMode(joystickAddressingMode)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
        addressingGroup.addView(addressingLabel)
        addressingGroup.addView(addressingSpinner)
        container.addView(addressingGroup, 1)
    }

    private fun addJoystickMappingSections(container: android.widget.LinearLayout, mappings: List<JoystickMapping>) {
        mappings.forEach { mapping ->
            val group = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(8, 16, 8, 16)
            }
            TextView(requireContext()).apply { text = mapping.displayName; textSize = 16f }.also { group.addView(it) }
            val topicInput = createSettingInput(requireContext(), group, "Topic", mapping.topic)
            val typeInput = createSettingInput(requireContext(), group, "Type (e.g. geometry_msgs/msg/Twist)", mapping.type)
            val numberInputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            val maxInput = createSettingInput(requireContext(), group, "Max value (e.g. 1.0)", mapping.max?.toString(), numberInputType)
            val stepInput = createSettingInput(requireContext(), group, "Step (e.g. 0.2)", mapping.step?.toString(), numberInputType)
            val deadzoneInput = createSettingInput(requireContext(), group, "Deadzone (e.g. 0.1)", mapping.deadzone?.toString(), numberInputType)
            val saveBtn = Button(requireContext())
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

    private lateinit var rosViewModel: RosViewModel
    private lateinit var controllerListText: TextView

    private fun initViewModels(app: MyApp) {
        rosViewModel = ViewModelProvider(
            app.appViewModelStore,
            ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )[RosViewModel::class.java]
        sliderControllerViewModel = ViewModelProvider(requireActivity())[SliderControllerViewModel::class.java]
    }

    private fun loadSlidersFromPrefs() {
        val sliderPrefs = requireContext().getSharedPreferences("slider_buttons_prefs", Context.MODE_PRIVATE)
        val sliderJson = sliderPrefs.getString("saved_slider_buttons", null)
        if (!sliderJson.isNullOrEmpty()) {
            val arr = JSONArray(sliderJson)
            val sliders = mutableListOf<SliderButtonFragment.SliderButtonConfig>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                sliders.add(
                    SliderButtonFragment.SliderButtonConfig(
                        name = obj.optString("name", null.toString()),
                        topic = obj.optString("topic", ""),
                        type = obj.optString("type", ""),
                        min = obj.optDouble("min", 0.0).toFloat(),
                        max = obj.optDouble("max", 1.0).toFloat(),
                        step = obj.optDouble("step", 0.1).toFloat(),
                        value = obj.optDouble("value", 0.0).toFloat()
                    )
                )
            }
            sliderControllerViewModel.setSliders(
                sliders.filter { it.type.isNotBlank() && it.topic.isNotBlank() }
                    .map { config ->
                        SliderControllerViewModel.SliderState(
                            name = config.name ?: "",
                            topic = config.topic,
                            type = config.type,
                            min = config.min,
                            max = config.max,
                            step = config.step,
                            value = config.value
                        )
                    }
            )
        }
    }

    private fun setupAppActionsRecycler(view: View) {
        appActionsList = view.findViewById<RecyclerView>(R.id.list_app_actions)
        appActionsList.layoutManager = LinearLayoutManager(requireContext())
        appActionsAdapter = AppActionsAdapter(mutableListOf())
        appActionsList.adapter = appActionsAdapter
    }

    private fun setupObservers(viewLifecycleOwner: LifecycleOwner) {
        // Custom protocol actions
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
                    val jsonObject = JSONObject()
                    msgFields.forEach { (key, value) ->
                        try {
                            val jsonValue: Any = try {
                                JSONArray(value)
                            } catch (e1: org.json.JSONException) {
                                try {
                                    JSONObject(value)
                                } catch (e2: org.json.JSONException) {
                                    value
                                }
                            }
                            jsonObject.put(key, jsonValue)
                        } catch (e: org.json.JSONException) {
                            jsonObject.put(key, value)
                        }
                    }
                    val msgJson = jsonObject.toString()
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
        // Slider changes
        viewLifecycleOwner.lifecycleScope.launch {
            sliderControllerViewModel.sliders.collectLatest {
                updateAppActions()
            }
        }
    }

    private fun setupConfigButtons(view: View) {
        view.findViewById<Button>(R.id.btn_export_config)?.setOnClickListener {
            (activity as? MainActivity)?.exportConfigLauncher?.launch("configs.yaml")
        }
        view.findViewById<Button>(R.id.btn_import_config)?.setOnClickListener {
            (activity as? MainActivity)?.importConfigLauncher?.launch(
                arrayOf("application/x-yaml", "text/yaml", "text/plain", "application/octet-stream", "*/*")
            )
        }
    }

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
        initViewModels(app)

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
            setupAppActionsRecycler(view)
            loadSlidersFromPrefs()
            rosViewModel.loadCustomProtocolActionsFromPrefs()
            setupObservers(viewLifecycleOwner)
            updateAppActions()
            setupControllerMappingUI(view)
        }
        setupConfigButtons(view)
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
                if (action.source == "SliderIncrement" || action.source == "SliderDecrement") {
                    val idx = action.msg.toIntOrNull()
                    val sliderStates = sliderControllerViewModel.sliders.value
                    val slider = sliderStates.getOrNull(idx ?: -1)
                    if (slider != null) {
                        val nextValue = if (action.source == "SliderIncrement") slider.value + slider.step else slider.value - slider.step
                        val outOfBounds = nextValue > slider.max || nextValue < slider.min
                        append("Value: ").append(
                            if (outOfBounds) slider.value.toString()
                            else slider.value.toString() + if (action.source == "SliderIncrement") " (+${slider.step})" else " (-${slider.step})"
                        )
                        append('\n')
                    }
                } else {
                    append("Msg: ").append(action.msg.ifEmpty { "<not set>" })
                }
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
        actions += loadSliderActions()
        actions += loadGeometryActions()
        actions += loadCustomPublisherActions()
        actions += loadImportedAppActions()
        actions += loadCyclePresetActions()
        return actions
    }

    private fun loadSliderActions(): List<AppAction> {
        val actions = mutableListOf<AppAction>()
        val sliderStates = sliderControllerViewModel.sliders.value
        for ((idx, slider) in sliderStates.withIndex()) {
            actions.add(AppAction("Increment ${slider.name}", slider.topic, slider.type, "SliderIncrement", idx.toString()))
            actions.add(AppAction("Decrement ${slider.name}", slider.topic, slider.type, "SliderDecrement", idx.toString()))
        }
        return actions
    }

    private fun loadGeometryActions(): List<AppAction> {
        val actions = mutableListOf<AppAction>()
        val geoPrefs = requireContext().getSharedPreferences("geometry_reusable_buttons", Context.MODE_PRIVATE)
        val geoJson = geoPrefs.getString("geometry_buttons", null)
        if (!geoJson.isNullOrEmpty()) {
            try {
                val arr = JSONArray(geoJson)
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
        return actions
    }

    private fun loadCustomPublisherActions(): List<AppAction> {
        val actions = mutableListOf<AppAction>()
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
                    val msg = if (obj.has("msg") && !obj["msg"].isJsonNull) obj["msg"].asString else ""
                    actions.add(AppAction(name, topic, type, "Standard Message", msg))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ControllerSupport", "Error loading custom_publishers_prefs:custom_publishers", e)
        }
        return actions
    }

    private fun loadImportedAppActions(): List<AppAction> {
        val actions = mutableListOf<AppAction>()
        val importedPrefs = requireContext().getSharedPreferences("imported_app_actions", Context.MODE_PRIVATE)
        val importedJson = importedPrefs.getString("imported_app_actions", null)
        if (!importedJson.isNullOrEmpty()) {
            try {
                val arr = JSONArray(importedJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("displayName") == "Cycle Presets") continue
                    actions.add(AppAction(
                        obj.optString("displayName", ""),
                        obj.optString("topic", ""),
                        obj.optString("type", ""),
                        obj.optString("source", ""),
                        obj.optString("msg", "")
                    ))
                }
            } catch (e: Exception) {
                android.util.Log.e("ControllerSupport", "Error loading imported_app_actions", e)
            }
        }
        return actions
    }

    private fun loadCyclePresetActions(): List<AppAction> = listOf(
        AppAction("Cycle Presets Forwards", "/ignore", "Set", "CyclePresetForward", ""),
        AppAction("Cycle Presets Backwards", "/ignore", "Set", "CyclePresetBackward", "")
    )

    /*
        input:    None
        output:   List<String>
        remarks:  Returns a list of common controller button names
        These were removed as they were unsettable due to ui connection "DPad Up", "DPad Down", "DPad Left", "DPad Right",
    */

    fun getControllerButtonList(): List<String> = listOf(
        "Button A", "Button B", "Button X", "Button Y",
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
        val arr = JSONArray()
        for ((btn, action) in assignments) {
            val obj = JSONObject()
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
    fun loadButtonAssignments(controllerButtons: List<String>): MutableMap<String, AppAction> {
        val prefs = requireContext().getSharedPreferences(PREFS_CONTROLLER_ASSIGN, Context.MODE_PRIVATE)
        val json = prefs.getString("assignments", null)
        val map = mutableMapOf<String, AppAction>()
        if (!json.isNullOrEmpty()) {
            try {
                val arr = JSONArray(json)
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

    /*
        input:    event - KeyEvent
        output:   Boolean
        remarks:  controller for key event handling
    */
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
        val assignedAction = assignments[btnName]
        if (assignedAction != null) {
            triggerAppAction(assignedAction)
            return true
        }
        // Do nothing if not assigned
        return false
    }

    /*
        input:    keyCode - Int
        output:   String?
        remarks:  Maps Android keyCode to button names, follows xbox layout
    */
    fun keyCodeToButtonName(keyCode: Int): String? {
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
        remarks:  triggers action for each respective source in the expected way
    */
    fun triggerAppAction(action: AppAction) {
        android.util.Log.d(TAG, "triggerAppAction called: $action")
        when (action.source) {
            "SliderIncrement", "SliderDecrement" -> handleSliderAction(action)
            "Geometry", "Standard Message" -> handleStandardOrGeometryAction(action)
            "Custom Protocol" -> handleCustomProtocolAction(action)
            "CyclePreset", "CyclePresetForward" -> handleCyclePreset(forward = true)
            "CyclePresetBackward" -> handleCyclePreset(forward = false)
            else -> android.util.Log.d(TAG, "Triggered unknown action source: ${action.source}")
        }
    }

    private fun handleSliderAction(action: AppAction) {
        val sliderIndex = action.msg.toIntOrNull() ?: return
        sliderControllerViewModel.selectSlider(sliderIndex)
        if (action.source == "SliderIncrement") {
            sliderControllerViewModel.incrementSelectedSlider()
        } else {
            sliderControllerViewModel.decrementSelectedSlider()
        }
        val updatedSlider = sliderControllerViewModel.getSelectedSlider() ?: return
        val rosType = getRosStdMsgType(updatedSlider.type)
        val msg = getRosStdMsgJson(updatedSlider.type, updatedSlider.value)
        publishRosAction(updatedSlider.topic, rosType, msg)
    }

    private fun handleStandardOrGeometryAction(action: AppAction) {
        val message = action.msg.ifEmpty { getDefaultMessage(action) }
        publishRosAction(action.topic, action.type, message)
    }

    private fun handleCustomProtocolAction(action: AppAction) {
        val protoType = when {
            action.type.contains("/msg/") -> "MSG"
            action.type.contains("/srv/") -> "SRV"
            action.type.contains("/action/") -> "ACTION"
            else -> ""
        }
        val topic = action.topic
        val rosType = action.type
        val msgJson = action.msg

        when (protoType) {
            "MSG" -> {
                rosViewModel.advertiseTopic(topic, rosType)
                rosViewModel.publishCustomRawMessage(topic, rosType, msgJson)
            }
            "SRV" -> {
                rosViewModel.advertiseService(topic, rosType)
                rosViewModel.callCustomService(topic, rosType, msgJson) { result ->
                    appendEventLog("Service result: $result")
                }
            }
            "ACTION" -> {
                val jsonGoalFields = try {
                    kotlinx.serialization.json.Json.parseToJsonElement(msgJson).jsonObject
                } catch (e: Exception) {
                    appendEventLog("Failed to parse action goal fields: ${e.message}")
                    return
                }
                val newGoalUuid = java.util.UUID.randomUUID().toString()
                rosViewModel.sendOrQueueActionGoal(topic, rosType, jsonGoalFields, newGoalUuid) { result ->
                    appendEventLog("Action result: $result")
                }
                rosViewModel.subscribeToActionFeedback(topic, rosType) { feedback ->
                    appendEventLog("Action feedback: $feedback")
                }
                rosViewModel.subscribeToActionStatus(topic) { status ->
                    appendEventLog("Action status: $status")
                }
            }
            else -> {
                appendEventLog("Unknown protocol type: $protoType")
            }
        }
    }

    private fun handleCyclePreset(forward: Boolean) {
        val presets = loadControllerPresets()
        val prefs = requireContext().getSharedPreferences(PREFS_CONTROLLER_PRESETS, Context.MODE_PRIVATE)
        val currentIdx = prefs.getInt("selected_preset_idx", 0)
        val newIdx = if (presets.isNotEmpty()) {
            if (forward) (currentIdx + 1) % presets.size else (currentIdx - 1 + presets.size) % presets.size
        } else 0
        prefs.edit { putInt("selected_preset_idx", newIdx) }
        android.util.Log.d(TAG, "Cycled to preset: ${presets.getOrNull(newIdx)?.name}")

        val newPreset = presets.getOrNull(newIdx)
        if (newPreset != null) {
            val abxy = newPreset.abxy
            val allActions = loadAvailableAppActions() + customProtocolAppActions
            val newAssignments = loadButtonAssignments(getControllerButtonList()).toMutableMap()
            listOf("A", "B", "X", "Y").forEach { btn ->
                newAssignments.remove("Button $btn")
            }
            listOf("A", "B", "X", "Y").forEach { btn ->
                val actionName = abxy[btn] ?: ""
                val action = allActions.find { it.displayName == actionName }
                if (action != null) {
                    newAssignments["Button $btn"] = action
                }
            }
            saveButtonAssignments(newAssignments)
            buttonAssignments.clear()
            buttonAssignments.putAll(newAssignments)
            setupControllerMappingUI(requireView())
        }
        presetOverlayCallback?.onPresetCycled()
        presetOverlayCallback?.onShowPresetsOverlay()
    }

    private fun getDefaultMessage(action: AppAction): String {
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

        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    lastJoystickMappings = loadJoystickMappings() 
                    lastJoystickDevice = dev
                    lastJoystickEvent?.recycle()
                    lastJoystickEvent = MotionEvent.obtain(event)
                    if (!joystickResendActive) {
                        joystickResendActive = true
                        joystickPublishRate = loadJoystickPublishRate()
                        val intervalMs = if (joystickPublishRate > 0) (1000L / joystickPublishRate) else 200L
                        joystickResendHandler.postDelayed(joystickResendRunnable, intervalMs)
                    }
                    return true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    joystickResendHandler.removeCallbacks(joystickResendRunnable)
                    joystickResendActive = false
                    val mappingsToStop = lastJoystickMappings
                    if (mappingsToStop != null) {
                        for (mapping in mappingsToStop) {
                            val topic = mapping.topic
                            val type = mapping.type
                            if (!topic.isNullOrEmpty() && !type.isNullOrEmpty()) {
                                android.util.Log.d(TAG, "Sending explicit STOP to topic: $topic")
                                val rosType = if (type.contains("/")) type else "geometry_msgs/msg/Twist"
                                val msg = when (rosType){
                                    "geometry_msgs/msg/TwistStamped" -> {
                                        val header = "{\"stamp\":\"now\",\"frame_id\":\"base_link\"}"
                                        val twist = "{\"linear\":{\"x\":0.0,\"y\":0.0,\"z\":0.0},\"angular\":{\"x\":0.0,\"y\":0.0,\"z\":0.0}}"
                                        "{\"header\":$header,\"twist\":$twist}"
                                    }
                                    "geometry_msgs/msg/Twist" -> "{\"linear\":{\"x\":0.0,\"y\":0.0,\"z\":0.0},\"angular\":{\"x\":0.0,\"y\":0.0,\"z\":0.0}}"
                                    else -> "{}"
                                }
                                publishRosAction(topic, rosType, msg)
                            }
                        }
                    }

                    lastJoystickMappings = null
                    lastJoystickDevice = null
                    lastJoystickEvent?.recycle()
                    lastJoystickEvent = null
                    lastJoystickSent.clear()
                    return true
                }
            }
        }
        return false
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
        if (mapping.topic.isNullOrEmpty() || mapping.type.isNullOrEmpty()) return
        
        var x = getCenteredAxis(event, device, mapping.axisX, historyPos, mapping)
        var y = getCenteredAxis(event, device, mapping.axisY, historyPos, mapping)
        
        when (joystickAddressingMode) {
            JoystickAddressingMode.DIRECT -> { /* No change */ }
            JoystickAddressingMode.INVERTED_ROTATED -> {
                val temp = x
                x = -y
                y = temp
            }
        }

        val maxValue = mapping.max ?: 1.0f
        val stepValue = mapping.step ?: 0.2f
        val deadzoneValue = mapping.deadzone ?: 0.1f
        
        val quantX = if (x != 0f) sign(x) * (ceil(((abs(x) - deadzoneValue) / stepValue)).toInt() * stepValue + deadzoneValue) else 0f
        val quantY = if (y != 0f) sign(y) * (ceil(((abs(y) - deadzoneValue) / stepValue)).toInt() * stepValue + deadzoneValue) else 0f
        
        val clampedX = quantX.coerceIn(-maxValue, maxValue)
        val clampedY = quantY.coerceIn(-maxValue, maxValue)
        
        val displayNameKey = mapping.displayName
        val last = lastJoystickSent[displayNameKey]
        val rosType = if ((mapping.type ?: "").contains("/")) mapping.type!! else "geometry_msgs/msg/${mapping.type}"
        val topic = mapping.topic!!

        // --- STOP LOGIC ---
        if (clampedX == 0f && clampedY == 0f) {
            if (last != null && (last.first != 0f || last.second != 0f)) {
                lastJoystickSent[displayNameKey] = 0f to 0f
                
                val msg = when (rosType) {
                    "geometry_msgs/msg/TwistStamped" -> {
                        val header = "{\"stamp\":\"now\",\"frame_id\":\"base_link\"}"
                        val twist = "{\"linear\":{\"x\":0.0,\"y\":0.0,\"z\":0.0},\"angular\":{\"x\":0.0,\"y\":0.0,\"z\":0.0}}"
                        "{\"header\":$header,\"twist\":$twist}"
                    }
                    "geometry_msgs/msg/Twist" -> "{\"linear\":{\"x\":0.0,\"y\":0.0,\"z\":0.0},\"angular\":{\"x\":0.0,\"y\":0.0,\"z\":0.0}}"
                    "geometry_msgs/msg/Vector3" -> "{\"x\":0.0,\"y\":0.0,\"z\":0.0}"
                    else -> "{}"
                }
                
                if (msg != "{}") {
                    publishRosAction(topic, rosType, msg)
                }
            }
            return
        }

        // --- MOVEMENT LOGIC ---
        if (forceSend || last == null || last.first != clampedX || last.second != clampedY) {
            lastJoystickSent[displayNameKey] = clampedX to clampedY

            val msg = when (rosType) {
                "geometry_msgs/msg/TwistStamped" -> {
                    val header = "{\"stamp\":\"now\",\"frame_id\":\"base_link\"}"
                    val twist = "{\"linear\":{\"x\":${-clampedY},\"y\":0.0,\"z\":0.0},\"angular\":{\"x\":0.0,\"y\":0.0,\"z\":${clampedX}}}"
                    "{\"header\":$header,\"twist\":$twist}"
                }
                "geometry_msgs/msg/Twist" -> {
                    "{\"linear\":{\"x\":${-clampedY},\"y\":0.0,\"z\":0.0},\"angular\":{\"x\":0.0,\"y\":0.0,\"z\":${clampedX}}}"
                }
                "geometry_msgs/msg/Vector3" -> "{\"x\":$clampedX,\"y\":0.0,\"z\":${-clampedY}}"
                else -> "{}"
            }
            
            if (msg != "{}") {
                publishRosAction(topic, rosType, msg)
            }
        }
    }

    /*
        input:    msg - String
        output:   None
        remarks:  Appends a message to the event log (no-op in this version)
    */
    private fun appendEventLog(msg: String) {
        android.util.Log.i("ControllerSupport", msg)
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

    private val PREFS_JOYSTICK_RATE = "joystick_publish_rate"

    /*
        input:    rate - int
        output:   None
        remarks:  saves joystick publishing rate to shared preference
    */
    private fun saveJoystickPublishRate(rate: Int) {
        val prefs = requireContext().getSharedPreferences(PREFS_JOYSTICK_MAPPINGS, Context.MODE_PRIVATE)
        prefs.edit { putInt(PREFS_JOYSTICK_RATE, rate) }
    }

    /*
        input:    None
        output:   Int
        remarks:  Loads joystick publishing rate from shared prefrences
    */
    private fun loadJoystickPublishRate(): Int {
        val prefs = requireContext().getSharedPreferences(PREFS_JOYSTICK_MAPPINGS, Context.MODE_PRIVATE)
        return prefs.getInt(PREFS_JOYSTICK_RATE, 5)
    }

    /*
        input:    out - OutputStream (file path)
        output:   None
        remarks:  Creates the info for export of data to yaml file
    */
    fun exportConfigToStream(out: OutputStream) {
        val yaml = org.yaml.snakeyaml.Yaml()
        val latestJoystickMappings = loadJoystickMappings()
        val latestControllerPresets = loadControllerPresets()
        val latestButtonAssignments = loadButtonAssignments(getControllerButtonList())
        val latestAppActions = (loadAvailableAppActions() + customProtocolAppActions).distinctBy { it.displayName }

        val configMap = mapOf(
            "joystickMappings" to latestJoystickMappings.map { jm ->
                mapOf(
                    "axisX" to jm.axisX,
                    "axisY" to jm.axisY,
                    "deadzone" to jm.deadzone,
                    "displayName" to jm.displayName,
                    "max" to jm.max,
                    "step" to jm.step,
                    "topic" to jm.topic,
                    "type" to jm.type
                )
            },
            "controllerPresets" to latestControllerPresets.map { cp ->
                mapOf(
                    "name" to cp.name,
                    "topic" to cp.topic,
                    "abxy" to cp.abxy
                )
            },
            "buttonAssignments" to latestButtonAssignments.mapValues { (_, action) ->
                mapOf(
                    "displayName" to action.displayName,
                    "msg" to action.msg,
                    "source" to action.source,
                    "topic" to action.topic,
                    "type" to action.type
                )
            },
            "appActions" to latestAppActions.map { aa ->
                mapOf(
                    "displayName" to aa.displayName,
                    "msg" to aa.msg,
                    "source" to aa.source,
                    "topic" to aa.topic,
                    "type" to aa.type
                )
            }
        )
        yaml.dump(configMap, OutputStreamWriter(out))
    }

    /*
        input:    inp - InputStream (filepath)
        output:   None
        remarks:  Imports the app actions and other data from yaml file selected by user
    */
    fun importConfigFromStream(inp: InputStream) {
        android.util.Log.i("ControllerSupportFragment", "Import started")
        val yaml = org.yaml.snakeyaml.Yaml()
        val configMap = yaml.load<Map<String, Any>>(InputStreamReader(inp))

        parseJoystickMappings(configMap)
        parseControllerPresets(configMap)
        parseButtonAssignments(configMap)
        parseAppActions(configMap)

        persistImportedConfig()
        updateImportedAppActionsPrefs()
        refreshUIAfterImport()
        android.util.Log.i("ControllerSupportFragment", "Import finished")
    }

    private fun parseJoystickMappings(configMap: Map<String, Any>) {
        joystickMappings.clear()
        val jmList = configMap["joystickMappings"] as? List<*> ?: emptyList<Any>()
        for (jm in jmList) {
            if (jm is Map<*, *>) {
                joystickMappings.add(
                    JoystickMapping(
                        axisX = (jm["axisX"] as? Int) ?: 0,
                        axisY = (jm["axisY"] as? Int) ?: 1,
                        deadzone = (jm["deadzone"] as? Float ?: (jm["deadzone"] as? Double)?.toFloat() ?: (jm["deadzone"] as? Number)?.toFloat() ?: 0.1f),
                        displayName = jm["displayName"] as? String ?: "",
                        max = (jm["max"] as? Float ?: (jm["max"] as? Double)?.toFloat() ?: (jm["max"] as? Number)?.toFloat() ?: 1.0f),
                        step = (jm["step"] as? Float ?: (jm["step"] as? Double)?.toFloat() ?: (jm["step"] as? Number)?.toFloat() ?: 0.2f),
                        topic = jm["topic"] as? String,
                        type = jm["type"] as? String
                    )
                )
            }
        }
        android.util.Log.i("ControllerSupportFragment", "Imported joystickMappings: ${joystickMappings.size}")
    }

    private fun parseControllerPresets(configMap: Map<String, Any>) {
        controllerPresets.clear()
        val cpList = configMap["controllerPresets"] as? List<*> ?: emptyList<Any>()
        for (cp in cpList) {
            if (cp is Map<*, *>) {
                controllerPresets.add(
                    ControllerPreset(
                        name = cp["name"] as? String ?: "",
                        topic = cp["topic"] as? String ?: "",
                        abxy = (cp["abxy"] as? Map<String, String>) ?: mapOf("A" to "", "B" to "", "X" to "", "Y" to "")
                    )
                )
            }
        }
        android.util.Log.i("ControllerSupportFragment", "Imported controllerPresets: ${controllerPresets.size}")
    }

    private fun parseButtonAssignments(configMap: Map<String, Any>) {
        buttonAssignments.clear()
        val baMap = configMap["buttonAssignments"] as? Map<*, *> ?: emptyMap<String, Any>()
        for ((key, value) in baMap) {
            if (key is String && value is Map<*, *>) {
                buttonAssignments[key] = AppAction(
                    displayName = value["displayName"] as? String ?: "",
                    msg = value["msg"] as? String ?: "",
                    source = value["source"] as? String ?: "",
                    topic = value["topic"] as? String ?: "",
                    type = value["type"] as? String ?: ""
                )
            }
        }
        android.util.Log.i("ControllerSupportFragment", "Imported buttonAssignments: ${buttonAssignments.size}")
    }

    private fun parseAppActions(configMap: Map<String, Any>) {
        appActions.clear()
        val aaList = configMap["appActions"] as? List<*> ?: emptyList<Any>()
        for (aa in aaList) {
            if (aa is Map<*, *>) {
                appActions.add(
                    AppAction(
                        displayName = aa["displayName"] as? String ?: "",
                        msg = aa["msg"] as? String ?: "",
                        source = aa["source"] as? String ?: "",
                        topic = aa["topic"] as? String ?: "",
                        type = aa["type"] as? String ?: ""
                    )
                )
            }
        }
        android.util.Log.i("ControllerSupportFragment", "Imported appActions: ${appActions.size}")
    }

    private fun persistImportedConfig() {
        saveJoystickMappings(joystickMappings)
        saveControllerPresets(controllerPresets)
        saveButtonAssignments(buttonAssignments)
    }

    private fun updateImportedAppActionsPrefs() {
        val importedPrefs = requireContext().getSharedPreferences("imported_app_actions", Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (action in appActions) {
            val obj = JSONObject()
            obj.put("displayName", action.displayName)
            obj.put("topic", action.topic)
            obj.put("type", action.type)
            obj.put("source", action.source)
            obj.put("msg", action.msg)
            arr.put(obj)
        }
        importedPrefs.edit().putString("imported_app_actions", arr.toString()).apply()
    }

    private fun refreshUIAfterImport() {
        if (::appActionsAdapter.isInitialized) {
            appActionsAdapter.actions.clear()
            appActionsAdapter.actions.addAll(appActions)
            appActionsAdapter.notifyDataSetChanged()
        } else {
            updateAppActions()
        }
        setupPresetManagementUI(requireView())
        setupJoystickMappingUI(requireView())
        setupControllerMappingUI(requireView())
    }
}