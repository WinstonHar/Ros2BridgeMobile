package com.example.testros2jsbridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.app.AlertDialog
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.google.android.material.slider.Slider
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit

/*
    This fragment provides a UI for creating, saving, and publishing slider-based ROS messages.
    Users can configure sliders for various ROS message types, save configurations, and publish messages.
*/

class SliderButtonFragment : Fragment() {
    /*
        Centralized function to update ViewModel sliders from savedButtons
    */
    private fun updateViewModelSliders() {
        val validSliders = savedButtons.filter { it.type.isNotBlank() && it.topic.isNotBlank() }
            .map { btn ->
                SliderControllerViewModel.SliderState(
                    name = btn.name ?: btn.topic,
                    topic = btn.topic,
                    type = btn.type,
                    min = btn.min,
                    max = btn.max,
                    step = btn.step,
                    value = btn.value
                )
            }
        sliderControllerViewModel.setSliders(validSliders)
    }

    /*
        input:    type - String representing the ROS message type
        output:   Boolean indicating if the type is an integer type
        remarks:  Checks if the provided type is an integer ROS type
    */
    private fun isIntegerType(type: String): Boolean =
        type in listOf("Int16", "Int32", "Int64", "Int8", "UInt16", "UInt32", "UInt64", "UInt8")

    /*
        input:    None
        output:   None
        remarks:  Saves all slider button configurations to SharedPreferences
    */
    private fun saveSavedButtons() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (btn in savedButtons) arr.put(btn.toJson())
        prefs.edit { putString(PREFS_KEY, arr.toString()) }
    }

    /*
        input:    None
        output:   None
        remarks:  Refreshes the UI to display all saved slider buttons
    */
    private fun refreshSavedButtonsUI() {
        val layout = view?.findViewById<LinearLayout>(R.id.layout_slider_buttons) ?: return
        while (layout.childCount > 1) {
            layout.removeViewAt(1)
        }
        for (btn in savedButtons) {
            addSavedSliderButton(layout, btn)
        }
        // Also update ViewModel sliders so app actions stay in sync
        updateViewModelSliders()
    }

    /*
        input:    layout - LinearLayout, config - SliderButtonConfig
        output:   None
        remarks:  Adds a saved slider button to the UI with publish and delete functionality
    */
    // Map to keep track of UI sliders by topic (or name if available)
    private val sliderMap = mutableMapOf<String, Slider>()

    /*
        input:    layout - LinearLayout, config - SliderButtonConfig
        output:   None
        remarks:  Adds a saved slider button to the UI with publish and delete functionality.
    */
    private fun addSavedSliderButton(layout: LinearLayout, config: SliderButtonConfig) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8)
        }
        val nameLabel = TextView(requireContext()).apply {
            text = config.name ?: "Saved Slider"
            textSize = 16f
        }
        val topicLabel = TextView(requireContext()).apply {
            text = "Topic: ${config.topic}"
            textSize = 14f
        }
        val typeLabel = TextView(requireContext()).apply {
            text = "Type: ${config.type}"
            textSize = 14f
        }
        val slider = Slider(requireContext()).apply {
            valueFrom = config.min
            valueTo = config.max
            stepSize = config.step
            value = if (isIntegerType(config.type)) config.value.toLong().toFloat() else config.value
            setLabelFormatter { v -> if (isIntegerType(config.type)) v.toLong().toString() else v.toString() }
        }
        // Store reference for live updates
        val sliderKey = config.name ?: config.topic
        sliderMap[sliderKey] = slider

        val valueLabel = TextView(requireContext()).apply {
            text = if (isIntegerType(config.type)) config.value.toLong().toString() else config.value.toString()
            textSize = 16f
        }
        slider.addOnChangeListener { _, v, _ ->
            if (isIntegerType(config.type)) {
                val intVal = v.toLong()
                valueLabel.text = intVal.toString()
                config.value = intVal.toFloat()
                slider.value = intVal.toFloat() // snap
            } else {
                valueLabel.text = v.toString()
                config.value = v
            }
            updateViewModelSliders()
        }
        val publishBtn = Button(requireContext()).apply {
            text = "Publish"
            setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.purple_500))
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            setOnClickListener {
                val topic = config.topic
                val type = config.type
                val msg = when (type) {
                    "Bool" -> "{\"data\": ${if (config.value != 0f) "true" else "false"}}"
                    "Float32", "Float64" -> "{\"data\": ${config.value}}"
                    "Int16", "Int32", "Int64", "Int8", "UInt16", "UInt32", "UInt64", "UInt8" -> "{\"data\": ${config.value.toLong()}}"
                    else -> "{\"data\": ${config.value}}"
                }
                val rosType = when (type) {
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
                rosViewModel.advertiseTopic(topic, rosType)
                rosViewModel.publishCustomRawMessage(topic, rosType, msg)
            }
        }
        val deleteBtn = Button(requireContext()).apply {
            text = "Delete"
            setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_dark))
            setOnClickListener {
                savedButtons.remove(config)
                saveSavedButtons()
                refreshSavedButtonsUI()
            }
        }
        row.addView(nameLabel)
        row.addView(topicLabel)
        row.addView(typeLabel)
        row.addView(slider)
        row.addView(valueLabel)
        row.addView(publishBtn)
        row.addView(deleteBtn)
        layout.addView(row)
    }

    /*
        input:    layout - LinearLayout, config - SliderButtonConfig
        output:   None
        remarks:  Adds an editable slider for creating new saved slider buttons
    */
    private fun addSliderButton(layout: LinearLayout, config: SliderButtonConfig) {
        val row = createSliderRow()
        val topicEdit = createTopicEdit(config)
        val minEdit = createMinEdit(config)
        val maxEdit = createMaxEdit(config)
        val stepEdit = createStepEdit(config)
        val typeSpinner = createTypeSpinner(config)
        val slider = createSlider(config)
        val valueLabel = createValueLabel(config)

        fun updateSliderForType(type: String) {
            if (isIntegerType(type)) {
                val userStep = stepEdit.text.toString().toFloatOrNull()?.takeIf { it >= 1f } ?: 1f
                slider.stepSize = userStep
                slider.setLabelFormatter { v -> v.toLong().toString() }
                slider.value = (slider.value / userStep).toInt() * userStep
            } else {
                val userStep = stepEdit.text.toString().toFloatOrNull() ?: config.step
                slider.stepSize = userStep
                slider.setLabelFormatter { v -> v.toString() }
            }
        }

        setupSliderInitialValues(slider, config, valueLabel, ::updateSliderForType)
        observeSliderViewModel(slider, config, valueLabel)
        setupSliderRangeUpdater(minEdit, maxEdit, stepEdit, typeSpinner, slider, config, valueLabel, ::updateSliderForType)

        val publishBtn = createPublishButton(topicEdit, typeSpinner, config, slider)
        val saveBtn = createSaveButton(topicEdit, minEdit, maxEdit, stepEdit, typeSpinner, slider, config)

        row.apply {
            addView(topicEdit)
            addView(typeSpinner)
            addView(minEdit)
            addView(maxEdit)
            addView(stepEdit)
            addView(slider)
            addView(valueLabel)
            addView(publishBtn)
            addView(saveBtn)
        }
        layout.addView(row, 0)
    }

    private fun createSliderRow(): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8)
        }

    private fun createTopicEdit(config: SliderButtonConfig): EditText =
        EditText(requireContext()).apply {
            setText(config.topic)
            hint = "Topic (e.g. /slider_topic)"
        }

    private fun createMinEdit(config: SliderButtonConfig): EditText =
        EditText(requireContext()).apply {
            setText(config.min.toString())
            hint = "Min value"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        }

    private fun createMaxEdit(config: SliderButtonConfig): EditText =
        EditText(requireContext()).apply {
            setText(config.max.toString())
            hint = "Max value"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        }

    private fun createStepEdit(config: SliderButtonConfig): EditText =
        EditText(requireContext()).apply {
            setText(config.step.toString())
            hint = "Step size"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

    private fun createTypeSpinner(config: SliderButtonConfig): android.widget.Spinner =
        android.widget.Spinner(requireContext()).apply {
            val types = listOf("Float32", "Float64", "Int16", "Int32", "Int64", "Int8", "UInt16", "UInt32", "UInt64", "UInt8")
            adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(types.indexOf(config.type).coerceAtLeast(0))
        }

    private fun createSlider(config: SliderButtonConfig): Slider =
        Slider(requireContext())

    private fun createValueLabel(config: SliderButtonConfig): TextView =
        TextView(requireContext()).apply {
            text = if (isIntegerType(config.type)) config.value.toLong().toString() else config.value.toString()
            textSize = 16f
        }

    private fun setupSliderInitialValues(
        slider: Slider,
        config: SliderButtonConfig,
        valueLabel: TextView,
        updateSliderForType: (String) -> Unit
    ) {
        slider.valueFrom = config.min
        slider.valueTo = config.max
        val initialStep = config.step.takeIf { it > 0f } ?: 1f
        slider.stepSize = initialStep
        slider.value = if (isIntegerType(config.type)) config.value.toLong().toFloat() else config.value
        updateSliderForType(config.type)
        slider.addOnChangeListener { _, v, _ ->
            if (isIntegerType(config.type)) {
                val intVal = v.toLong()
                valueLabel.text = intVal.toString()
                if (config.value != intVal.toFloat()) config.value = intVal.toFloat()
                if (slider.value != intVal.toFloat()) slider.value = intVal.toFloat()
            } else {
                valueLabel.text = v.toString()
                if (config.value != v) config.value = v
            }
        }
    }

    private fun observeSliderViewModel(
        slider: Slider,
        config: SliderButtonConfig,
        valueLabel: TextView
    ) {
        viewLifecycleOwner?.lifecycleScope?.launch {
            viewLifecycleOwner?.lifecycle?.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                sliderControllerViewModel.sliders.collect { sliderStates ->
                    val key = config.name ?: config.topic
                    val state = sliderStates.find { it.name == key || it.topic == config.topic }
                    if (state != null && slider.value != state.value) {
                        slider.value = state.value
                        valueLabel.text = if (isIntegerType(config.type)) state.value.toLong().toString() else state.value.toString()
                    }
                }
            }
        }
    }

    private fun setupSliderRangeUpdater(
        minEdit: EditText,
        maxEdit: EditText,
        stepEdit: EditText,
        typeSpinner: android.widget.Spinner,
        slider: Slider,
        config: SliderButtonConfig,
        valueLabel: TextView,
        updateSliderForType: (String) -> Unit
    ) {
        val updateSliderRange = lambda@{
            val min = minEdit.text.toString().toFloatOrNull() ?: config.min
            val max = maxEdit.text.toString().toFloatOrNull() ?: config.max
            val step = stepEdit.text.toString().toFloatOrNull() ?: config.step
            val type = typeSpinner.selectedItem.toString()
            var valid = true
            stepEdit.error = null
            maxEdit.error = null
            if (isIntegerType(type)) {
                val minInt = min.toLong()
                val maxInt = max.toLong()
                val stepInt = step.toLong()
                val rangeInt = maxInt - minInt
                if (step < 1f) {
                    stepEdit.error = "Step must be >= 1 for int types"
                    valid = false
                } else if (stepInt == 0L || rangeInt % stepInt != 0L) {
                    stepEdit.error = "Step must divide (max-min) exactly for int types"
                    maxEdit.error = "Step must divide (max-min) exactly for int types"
                    valid = false
                }
            } else {
                if (step <= 0f) {
                    stepEdit.error = "Step must be > 0"
                    valid = false
                } else {
                    val range = max - min
                    if (step > range) {
                        stepEdit.error = "Step too large for range"
                        valid = false
                    }
                }
            }
            if (!valid) return@lambda
            config.min = min
            config.max = max
            config.step = step
            slider.valueFrom = min
            slider.valueTo = max
            val userStep = if (isIntegerType(type)) step.takeIf { it >= 1f } ?: 1f else step
            slider.stepSize = userStep
            updateSliderForType(type)
            val snapped = min + (((slider.value - min) / userStep).toInt() * userStep)
            val clamped = snapped.coerceIn(min, max)
            slider.value = clamped
            config.value = clamped
            valueLabel.text = if (isIntegerType(type)) clamped.toLong().toString() else clamped.toString()
        }
        typeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                config.type = parent.getItemAtPosition(position) as String
                updateSliderRange()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
        minEdit.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) updateSliderRange() }
        maxEdit.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) updateSliderRange() }
        stepEdit.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) updateSliderRange() }
    }

    private fun createPublishButton(
        topicEdit: EditText,
        typeSpinner: android.widget.Spinner,
        config: SliderButtonConfig,
        slider: Slider
    ): Button = Button(requireContext()).apply {
        text = "Publish"
        setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.purple_500))
        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        setOnClickListener {
            val topic = topicEdit.text.toString().trim()
            if (topic.isEmpty()) {
                topicEdit.error = "Topic required"
                return@setOnClickListener
            }
            val type = typeSpinner.selectedItem.toString()
            val msg = when (type) {
                "Bool" -> "{\"data\": ${if (config.value != 0f) "true" else "false"}}"
                "Float32", "Float64" -> "{\"data\": ${config.value}}"
                "Int16", "Int32", "Int64", "Int8", "UInt16", "UInt32", "UInt64", "UInt8" -> "{\"data\": ${config.value.toLong()}}"
                else -> "{\"data\": ${config.value}}"
            }
            val rosType = when (type) {
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
            rosViewModel.advertiseTopic(topic, rosType)
            rosViewModel.publishCustomRawMessage(topic, rosType, msg)
        }
    }

    private fun createSaveButton(
        topicEdit: EditText,
        minEdit: EditText,
        maxEdit: EditText,
        stepEdit: EditText,
        typeSpinner: android.widget.Spinner,
        slider: Slider,
        config: SliderButtonConfig
    ): Button = Button(requireContext()).apply {
        text = "Save Current Button"
        setOnClickListener {
            val input = EditText(requireContext())
            AlertDialog.Builder(requireContext())
                .setTitle("Save Slider Button")
                .setMessage("Enter a name for this slider:")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Name required", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val min = minEdit.text.toString().toFloatOrNull() ?: config.min
                    val max = maxEdit.text.toString().toFloatOrNull() ?: config.max
                    val step = stepEdit.text.toString().toFloatOrNull() ?: config.step
                    val type = typeSpinner.selectedItem.toString()
                    val topic = topicEdit.text.toString().trim()
                    val value = slider.value
                    val newConfig = SliderButtonConfig(topic, min, max, step, value, type, name)
                    savedButtons.add(newConfig)
                    saveSavedButtons()
                    refreshSavedButtonsUI()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // Use application-scoped RosViewModel for consistent connection state
    private val rosViewModel: RosViewModel by lazy {
        val app = requireActivity().application as MyApp
        androidx.lifecycle.ViewModelProvider(
            app.appViewModelStore,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )[RosViewModel::class.java]
    }
    private val sliderControllerViewModel: SliderControllerViewModel by activityViewModels()

    /*
        input:    fields for slider config
        output:   None
        remarks:  Data class for slider button configuration
    */
    data class SliderButtonConfig(
        var topic: String,
        var min: Float,
        var max: Float,
        var step: Float,
        var value: Float,
        var type: String,
        var name: String? = null
    ) {
        fun toJson(): JSONObject {
            val obj = JSONObject()
            obj.put("topic", topic)
            obj.put("min", min)
            obj.put("max", max)
            obj.put("step", step)
            obj.put("value", value)
            obj.put("type", type)
            obj.put("name", name ?: "")
            return obj
        }
        companion object {
            fun fromJson(obj: JSONObject): SliderButtonConfig {
                val name = if (obj.has("name")) obj.optString("name", "") else ""
                return SliderButtonConfig(
                    obj.getString("topic"),
                    obj.getDouble("min").toFloat(),
                    obj.getDouble("max").toFloat(),
                    obj.getDouble("step").toFloat(),
                    obj.getDouble("value").toFloat(),
                    obj.getString("type"),
                    name
                )
            }
        }
    }

    private val sliderButtons = mutableListOf<SliderButtonConfig>()
    private val savedButtons = mutableListOf<SliderButtonConfig>()
    private val PREFS_NAME = "slider_buttons_prefs"
    private val PREFS_KEY = "saved_slider_buttons"

    /*
        input:    None
        output:   None
        remarks:  Loads saved slider button configurations from SharedPreferences
    */
    private fun loadSavedButtons() {
        savedButtons.clear()
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(PREFS_KEY, null)
        if (!json.isNullOrEmpty()) {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                savedButtons.add(SliderButtonConfig.fromJson(obj))
            }
        }
    }

    /*
        input:    inflater - LayoutInflater, container - ViewGroup?, savedInstanceState - Bundle?
        output:   View?
        remarks:  Inflates the fragment view and sets up the slider UI
    */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_slider_button, container, false)
        val layout = view.findViewById<LinearLayout>(R.id.layout_slider_buttons)

        // On load: clear all children, add editable slider, then add all saved sliders
        loadSavedButtons()
        layout.removeAllViews()
        val default = SliderButtonConfig("/slider_topic", 0f, 100f, 1f, 50f, "Float32")
        sliderButtons.clear()
        sliderButtons.add(default)
        addSliderButton(layout, default)
        for (btn in savedButtons) {
            addSavedSliderButton(layout, btn)
        }

        // No need to call setSliders here; refreshSavedButtonsUI handles it

        return view
    }

    /*
        input:    view - View, savedInstanceState - Bundle?
        output:   None
        remarks:  Called after the fragment view is created; observes ViewModel sliders and updates UI sliders live.
    */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observe ViewModel sliders and update UI sliders live
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                sliderControllerViewModel.sliders.collect { sliderStates ->
                    for (sliderState in sliderStates) {
                        val key = sliderState.name
                        val uiSlider = sliderMap[key]
                        if (uiSlider != null) {
                            // Only update if value differs to avoid infinite loop
                            if (uiSlider.value != sliderState.value) {
                                uiSlider.value = sliderState.value
                            }
                        }
                    }
                }
            }
        }
    }
}