package com.example.testros2jsbridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

/*
    This fragment provides a UI for sending and saving reusable geometry_msgs and std_msgs messages, including dynamic field generation and validation.
*/

class GeometryStdMsgFragment : Fragment() {
    private val PERSIST_PREFS = "geometry_std_msg_prefs"
    private val KEY_TOPIC = "topic"
    private val KEY_TYPE = "type"
    private val KEY_FIELDS = "fields_json"
    /*
        input:    type - String, layout - LinearLayout
        output:   Null if valid, or error message String if invalid
        remarks:  Validates user input for geometry_msgs message fields
    */
    private fun validateGeometryMsgInput(type: String, layout: LinearLayout): String? {
        /*
            input:    s - String
            output:   Boolean
            remarks:  Checks if the string can be parsed as a float
        */
        fun isFloat(s: String) = s.toFloatOrNull() != null
        /*
            input:    s - String
            output:   Boolean
            remarks:  Checks if the string can be parsed as an integer
        */
        fun isInt(s: String) = s.toIntOrNull() != null
        /*
            input:    s - String
            output:   Boolean
            remarks:  Checks if the string can be parsed as a long
        */
        fun isLong(s: String) = s.toLongOrNull() != null
        /*
            input:    prefix - String (optional)
            output:   Null if valid, or error message String if invalid
            remarks:  Validates that all Vector3 fields (x, y, z) are floats
        */
        fun checkVector3(prefix: String = ""): String? {
            val x = getValue(layout, if (prefix.isEmpty()) "x" else "${prefix}_x")
            val y = getValue(layout, if (prefix.isEmpty()) "y" else "${prefix}_y")
            val z = getValue(layout, if (prefix.isEmpty()) "z" else "${prefix}_z")
            if (!isFloat(x) || !isFloat(y) || !isFloat(z)) return "All Vector3 fields must be floats (x, y, z)"
            return null
        }
        /*
            input:    prefix - String (optional)
            output:   Null if valid, or error message String if invalid
            remarks:  Validates that all Quaternion fields (x, y, z, w) are floats
        */
        fun checkQuaternion(prefix: String = ""): String? {
            val x = getValue(layout, if (prefix.isEmpty()) "x" else "${prefix}_x")
            val y = getValue(layout, if (prefix.isEmpty()) "y" else "${prefix}_y")
            val z = getValue(layout, if (prefix.isEmpty()) "z" else "${prefix}_z")
            val w = getValue(layout, if (prefix.isEmpty()) "w" else "${prefix}_w")
            if (!isFloat(x) || !isFloat(y) || !isFloat(z) || !isFloat(w)) return "All Quaternion fields must be floats (x, y, z, w)"
            return null
        }
        /*
            input:    label - String (default "covariance")
            output:   Null if valid, or error message String if invalid
            remarks:  Validates that all covariance fields are floats
        */
        fun checkCovariance(label: String = "covariance"): String? {
            for (i in 0 until 36) {
                val v = getValue(layout, "$label$i")
                if (!isFloat(v)) return "Covariance[$i] must be a float"
            }
            return null
        }
        /*
            input:    label - String (default "points")
            output:   Null if valid, or error message String if invalid
            remarks:  Validates that all Point32 array fields are floats
        */
        fun checkPoint32Array(label: String = "points"): String? {
            for (i in 0 until 3) {
                val x = getValue(layout, "${label}_${i}_x")
                val y = getValue(layout, "${label}_${i}_y")
                val z = getValue(layout, "${label}_${i}_z")
                if (!isFloat(x) || !isFloat(y) || !isFloat(z)) return "$label[$i] fields must be floats (x, y, z)"
            }
            return null
        }
        /*
            input:    label - String (default "poses")
            output:   Null if valid, or error message String if invalid
            remarks:  Validates that all Pose array fields are valid
        */
        fun checkPoseArray(label: String = "poses"): String? {
            for (i in 0 until 2) {
                val err1 = checkVector3("${label}[$i] position")
                val err2 = checkQuaternion("${label}[$i] orientation")
                if (err1 != null) return "Pose[$i] position: $err1"
                if (err2 != null) return "Pose[$i] orientation: $err2"
            }
            return null
        }
        /*
            input:    tag - String
            output:   Null if valid, or error message String if invalid
            remarks:  Validates that the field is a float
        */
        fun checkFloatField(tag: String): String? = if (!isFloat(getValue(layout, tag))) "$tag must be a float" else null
        /*
            input:    tag - String
            output:   Null if valid, or error message String if invalid
            remarks:  Validates that the field is an integer
        */
        fun checkIntField(tag: String): String? = if (!isInt(getValue(layout, tag))) "$tag must be an integer" else null
        // Add more as needed for new types
        return when (type) {
            // Acceleration
            "Accel" -> checkVector3("linear") ?: checkVector3("angular")
            "AccelStamped" -> checkVector3("linear") ?: checkVector3("angular")
            "AccelWithCovariance" -> checkVector3("linear") ?: checkVector3("angular") ?: checkCovariance()
            "AccelWithCovarianceStamped" -> checkVector3("linear") ?: checkVector3("angular") ?: checkCovariance()
            // Inertia
            "Inertia" -> checkFloatField("m (mass)") ?: checkVector3("com") ?: checkFloatField("ixx") ?: checkFloatField("ixy") ?: checkFloatField("ixz") ?: checkFloatField("iyy") ?: checkFloatField("iyz") ?: checkFloatField("izz")
            "InertiaStamped" -> checkFloatField("m (mass)") ?: checkVector3("com") ?: checkFloatField("ixx") ?: checkFloatField("ixy") ?: checkFloatField("ixz") ?: checkFloatField("iyy") ?: checkFloatField("iyz") ?: checkFloatField("izz")
            // Point
            "Point" -> checkVector3()
            "Point32" -> checkFloatField("x (float32)") ?: checkFloatField("y (float32)") ?: checkFloatField("z (float32)")
            "PointStamped" -> checkVector3()
            // Polygon
            "Polygon" -> checkPoint32Array()
            "PolygonStamped" -> checkPoint32Array()
            // Pose
            "Pose" -> checkVector3("position") ?: checkQuaternion("orientation")
            "PoseArray" -> checkPoseArray()
            "PoseStamped" -> checkVector3("position") ?: checkQuaternion("orientation")
            "PoseWithCovariance" -> checkVector3("position") ?: checkQuaternion("orientation") ?: checkCovariance()
            "PoseWithCovarianceStamped" -> checkVector3("position") ?: checkQuaternion("orientation") ?: checkCovariance()
            // Quaternion
            "Quaternion" -> checkQuaternion()
            "QuaternionStamped" -> checkQuaternion("quaternion")
            // Transform
            "Transform" -> checkVector3("translation") ?: checkQuaternion("rotation")
            "TransformStamped" -> checkVector3("translation") ?: checkQuaternion("rotation")
            // Twist
            "Twist" -> checkVector3("linear") ?: checkVector3("angular")
            "TwistStamped" -> checkVector3("linear") ?: checkVector3("angular")
            "TwistWithCovariance" -> checkVector3("linear") ?: checkVector3("angular") ?: checkCovariance()
            "TwistWithCovarianceStamped" -> checkVector3("linear") ?: checkVector3("angular") ?: checkCovariance()
            // Vector3
            "Vector3" -> checkVector3()
            "Vector3Stamped" -> checkVector3("vector")
            // Wrench
            "Wrench" -> checkVector3("force") ?: checkVector3("torque")
            "WrenchStamped" -> checkVector3("force") ?: checkVector3("torque")
            else -> null
        }
    }
    private lateinit var rosViewModel: RosViewModel

    private val geometryTypes = listOf(
        // Acceleration
        "Accel", "AccelStamped", "AccelWithCovariance", "AccelWithCovarianceStamped",
        // Inertia
        "Inertia", "InertiaStamped",
        // Point
        "Point", "Point32", "PointStamped",
        // Polygon
        "Polygon", "PolygonStamped",
        // Pose
        "Pose", "PoseArray", "PoseStamped", "PoseWithCovariance", "PoseWithCovarianceStamped",
        // Quaternion
        "Quaternion", "QuaternionStamped",
        // Transform
        "Transform", "TransformStamped",
        // Twist
        "Twist", "TwistStamped", "TwistWithCovariance", "TwistWithCovarianceStamped",
        // Vector3
        "Vector3", "Vector3Stamped",
        // Wrench
        "Wrench", "WrenchStamped"
    )
    /*
        input:    label - String, topic - String, type - String, message - String
        output:   None
        remarks:  Data class for reusable message buttons
    */
    data class ReusableMsgButton(val label: String, val topic: String, val type: String, val message: String)
    private val savedMessages = mutableListOf<ReusableMsgButton>()
    // Persist advertised topics for the fragment session
    private val advertisedTopics = mutableSetOf<String>()
    private var savedButtonsLayout: LinearLayout? = null
    private lateinit var topicEditText: TextInputEditText
    private val gson = Gson()
    private val PREFS_KEY = "geometry_reusable_buttons"

    /*
        input:    inflater - LayoutInflater, container - ViewGroup?, savedInstanceState - Bundle?
        output:   View?
        remarks:  Inflates the fragment view and sets up geometry message UI
    */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rosViewModel = ViewModelProvider(requireActivity()).get(RosViewModel::class.java)
        val view = inflater.inflate(R.layout.fragment_geometry_std_msg, container, false)
        val spinnerType = view.findViewById<Spinner>(R.id.spinner_geometry_type)
        val dynamicFieldsLayout = view.findViewById<LinearLayout>(R.id.layout_dynamic_fields)
        val publishButton = view.findViewById<Button>(R.id.button_publish_geometry)

        // Add a layout for saved message buttons below the dynamic fields
        val parentLayout = view.findViewById<LinearLayout>(R.id.geometry_std_msg_root) ?: view as LinearLayout
        savedButtonsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        parentLayout.addView(savedButtonsLayout)

        // Add topic input (no default value)
        val topicLayout = TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox)
        topicLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        topicLayout.hint = "Topic Name"
        topicEditText = TextInputEditText(requireContext())
        topicEditText.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        // Restore topic if present
        val prefs = requireContext().getSharedPreferences(PERSIST_PREFS, Context.MODE_PRIVATE)
        topicEditText.setText(prefs.getString(KEY_TOPIC, "/"))
        topicLayout.addView(topicEditText)
        parentLayout.addView(topicLayout, 0)

        // Add a button to save the current message as a reusable button
        val saveButton = Button(requireContext()).apply {
            text = "Save as Reusable Button"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        parentLayout.addView(saveButton)

        spinnerType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, geometryTypes).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Restore type selection if present
        val savedType = prefs.getString(KEY_TYPE, geometryTypes[0])
        val savedTypeIndex = geometryTypes.indexOf(savedType).takeIf { it >= 0 } ?: 0
        spinnerType.setSelection(savedTypeIndex)

        // Restore dynamic field values if present
        val savedFieldsJson = prefs.getString(KEY_FIELDS, null)
        var lastType = geometryTypes[savedTypeIndex]
        fun saveFieldsToPrefs(type: String, layout: LinearLayout) {
            val map = mutableMapOf<String, String>()
            for (i in 0 until layout.childCount) {
                val child = layout.getChildAt(i)
                if (child is TextInputLayout && child.childCount > 0 && child.getChildAt(0) is EditText) {
                    val edit = child.getChildAt(0) as EditText
                    val tag = edit.tag as? String ?: continue
                    map[tag] = edit.text?.toString() ?: ""
                } else if (child is LinearLayout) {
                    // Recursively check for nested fields
                    for (j in 0 until child.childCount) {
                        val sub = child.getChildAt(j)
                        if (sub is TextInputLayout && sub.childCount > 0 && sub.getChildAt(0) is EditText) {
                            val edit = sub.getChildAt(0) as EditText
                            val tag = edit.tag as? String ?: continue
                            map[tag] = edit.text?.toString() ?: ""
                        }
                    }
                }
            }
            prefs.edit().putString(KEY_FIELDS, gson.toJson(map)).apply()
        }

        fun restoreFieldsFromPrefs(layout: LinearLayout, json: String?) {
            if (json == null) return
            val map: Map<String, String> = try { gson.fromJson(json, object : TypeToken<Map<String, String>>() {}.type) } catch (_: Exception) { emptyMap() }
            for (i in 0 until layout.childCount) {
                val child = layout.getChildAt(i)
                if (child is TextInputLayout && child.childCount > 0 && child.getChildAt(0) is EditText) {
                    val edit = child.getChildAt(0) as EditText
                    val tag = edit.tag as? String ?: continue
                    edit.setText(map[tag] ?: "")
                } else if (child is LinearLayout) {
                    for (j in 0 until child.childCount) {
                        val sub = child.getChildAt(j)
                        if (sub is TextInputLayout && sub.childCount > 0 && sub.getChildAt(0) is EditText) {
                            val edit = sub.getChildAt(0) as EditText
                            val tag = edit.tag as? String ?: continue
                            edit.setText(map[tag] ?: "")
                        }
                    }
                }
            }
        }

        // Build initial fields and restore values
        buildFieldsForType(lastType, dynamicFieldsLayout, inflater)
        restoreFieldsFromPrefs(dynamicFieldsLayout, savedFieldsJson)

        // Save topic and type on change
        topicEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                prefs.edit().putString(KEY_TOPIC, s?.toString() ?: "").apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view_: View?, position: Int, id: Long) {
                val selectedType = geometryTypes[position]
                if (selectedType != lastType) {
                    // Clear fields when type changes
                    buildFieldsForType(selectedType, dynamicFieldsLayout, inflater)
                    prefs.edit().putString(KEY_TYPE, selectedType).remove(KEY_FIELDS).apply()
                    lastType = selectedType
                } else {
                    // Restore fields if type is the same
                    buildFieldsForType(selectedType, dynamicFieldsLayout, inflater)
                    restoreFieldsFromPrefs(dynamicFieldsLayout, prefs.getString(KEY_FIELDS, null))
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Save dynamic field values on change
        val fieldWatcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                saveFieldsToPrefs(lastType, dynamicFieldsLayout)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        // Attach watcher to all EditTexts in dynamicFieldsLayout
        fun attachWatcher(layout: LinearLayout) {
            for (i in 0 until layout.childCount) {
                val child = layout.getChildAt(i)
                if (child is TextInputLayout && child.childCount > 0 && child.getChildAt(0) is EditText) {
                    (child.getChildAt(0) as EditText).addTextChangedListener(fieldWatcher)
                } else if (child is LinearLayout) {
                    for (j in 0 until child.childCount) {
                        val sub = child.getChildAt(j)
                        if (sub is TextInputLayout && sub.childCount > 0 && sub.getChildAt(0) is EditText) {
                            (sub.getChildAt(0) as EditText).addTextChangedListener(fieldWatcher)
                        }
                    }
                }
            }
        }
        attachWatcher(dynamicFieldsLayout)

        // Load persistent reusable buttons
        loadSavedButtons()
        refreshSavedButtons()

        publishButton.setOnClickListener {
            val selectedType = spinnerType.selectedItem.toString()
            val topic = topicEditText.text?.toString()?.trim() ?: ""
            if (topic.isEmpty()) {
                topicEditText.error = "Topic required"
                return@setOnClickListener
            }
            val errorMsg = validateGeometryMsgInput(selectedType, dynamicFieldsLayout)
            if (errorMsg != null) {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Invalid Input for $selectedType")
                    .setMessage(errorMsg)
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }
            val message = buildMessageFromFields(selectedType, dynamicFieldsLayout)
            val type = "geometry_msgs/msg/$selectedType"
            // Always advertise before publishing, and wait for completion
            viewLifecycleOwner.lifecycleScope.launch {
                rosViewModel.advertiseTopic(topic, type)
                kotlinx.coroutines.delay(200)
                rosViewModel.publishCustomRawMessage(topic, type, message)
            }
        }

        saveButton.setOnClickListener {
            val selectedType = spinnerType.selectedItem.toString()
            val topic = topicEditText.text?.toString()?.trim() ?: ""
            if (topic.isEmpty()) {
                topicEditText.error = "Topic required"
                return@setOnClickListener
            }
            // Fill empty fields with 0.0 (float) or 0 (int) for the saved message only
            try {
                val filledMessage = buildMessageWithDefaults(selectedType, dynamicFieldsLayout)
                val labelInput = EditText(requireContext())
                labelInput.hint = "Label for this message"
                val dialog = android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Save Message Button")
                    .setView(labelInput)
                    .setPositiveButton("Save") { _, _ ->
                        val label = labelInput.text.toString().ifBlank { selectedType }
                        savedMessages.add(ReusableMsgButton(label, topic, selectedType, filledMessage))
                        saveButtonsToPrefs()
                        refreshSavedButtons()
                    }
                    .setNegativeButton("Cancel", null)
                    .create()
                dialog.show()
            } catch (e: IllegalArgumentException) {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Invalid Field Value")
                    .setMessage(e.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

    // Helper to build message with default 0.0/0 for empty fields (for saving only)
    fun buildMessageWithDefaults(type: String, layout: LinearLayout): String {
        // Helper to get value or default for float/int
        fun getOrDefault(tag: String, isFloat: Boolean): String {
            val editText = layout.findViewWithTag<EditText>(tag)
            val value = editText?.text?.toString()?.trim()
            if (value.isNullOrEmpty()) {
                return if (isFloat) "0.0" else "0"
            }
            if (isFloat) {
                // Only allow valid float (including decimals)
                val d = value!!.toDoubleOrNull()
                if (d == null) throw IllegalArgumentException("Field '$tag' must be a float value.")
                return value
            } else {
                // Only allow valid int (no decimals)
                if (value!!.contains('.')) throw IllegalArgumentException("Field '$tag' must be an integer value (no decimal point).")
                val l = value.toLongOrNull()
                if (l == null) throw IllegalArgumentException("Field '$tag' must be an integer value.")
                return value
            }
        }
        fun vector3(prefix: String = ""): String =
            "\"x\":" + getOrDefault(if (prefix.isEmpty()) "x" else "${prefix}_x", true) + "," +
            "\"y\":" + getOrDefault(if (prefix.isEmpty()) "y" else "${prefix}_y", true) + "," +
            "\"z\":" + getOrDefault(if (prefix.isEmpty()) "z" else "${prefix}_z", true)
        fun quaternion(prefix: String = ""): String =
            "\"x\":" + getOrDefault(if (prefix.isEmpty()) "x" else "${prefix}_x", true) + "," +
            "\"y\":" + getOrDefault(if (prefix.isEmpty()) "y" else "${prefix}_y", true) + "," +
            "\"z\":" + getOrDefault(if (prefix.isEmpty()) "z" else "${prefix}_z", true) + "," +
            "\"w\":" + getOrDefault(if (prefix.isEmpty()) "w" else "${prefix}_w", true)
        fun header(): String =
            "\"frame_id\":\"" + getOrDefault("header_frame_id", false) + "\""
        fun point32Array(label: String = "points"): String =
            (0 until 3).joinToString(",") { i ->
                "{\"x\":" + getOrDefault("${label}_${i}_x", true) + ",\"y\":" + getOrDefault("${label}_${i}_y", true) + ",\"z\":" + getOrDefault("${label}_${i}_z", true) + "}"
            }
        fun poseArray(label: String = "poses"): String =
            (0 until 2).joinToString(",") { i ->
                "{\"position\":{${vector3("${label}[$i] position")}},\"orientation\":{${quaternion("${label}[$i] orientation")}}}"
            }
        fun covarianceArray(label: String = "covariance"): String =
            (0 until 36).joinToString(",") { getOrDefault("$label$it", true) }
        return when (type) {
            "Accel" -> "{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}"
            "AccelStamped" -> "{\"header\":{${header()}},\"accel\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}}"
            "AccelWithCovariance" -> "{\"accel\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}"
            "AccelWithCovarianceStamped" -> "{\"header\":{${header()}},\"accel\":{\"accel\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}}"
            "Inertia" -> "{\"m\":" + getOrDefault("m (mass)", true) + ",\"com\":{${vector3("com")}},\"ixx\":" + getOrDefault("ixx", true) + ",\"ixy\":" + getOrDefault("ixy", true) + ",\"ixz\":" + getOrDefault("ixz", true) + ",\"iyy\":" + getOrDefault("iyy", true) + ",\"iyz\":" + getOrDefault("iyz", true) + ",\"izz\":" + getOrDefault("izz", true) + "}"
            "InertiaStamped" -> "{\"header\":{${header()}},\"inertia\":{\"m\":" + getOrDefault("m (mass)", true) + ",\"com\":{${vector3("com")}},\"ixx\":" + getOrDefault("ixx", true) + ",\"ixy\":" + getOrDefault("ixy", true) + ",\"ixz\":" + getOrDefault("ixz", true) + ",\"iyy\":" + getOrDefault("iyy", true) + ",\"iyz\":" + getOrDefault("iyz", true) + ",\"izz\":" + getOrDefault("izz", true) + "}}"
            "Point" -> "{${vector3()}}"
            "Point32" -> "{\"x\":" + getOrDefault("x (float32)", true) + ",\"y\":" + getOrDefault("y (float32)", true) + ",\"z\":" + getOrDefault("z (float32)", true) + "}"
            "PointStamped" -> "{\"header\":{${header()}},\"point\":{${vector3()}}}"
            "Polygon" -> "{\"points\":[${point32Array()}]}"
            "PolygonStamped" -> "{\"header\":{${header()}},\"polygon\":{\"points\":[${point32Array()}]}}"
            "Pose" -> "{\"position\":{${vector3("position")}},\"orientation\":{${quaternion("orientation")}}}"
            "PoseArray" -> "{\"header\":{${header()}},\"poses\":[${poseArray()}]}"
            "PoseStamped" -> "{\"header\":{${header()}},\"pose\":{\"position\":{${vector3("position")}},\"orientation\":{${quaternion("orientation")}}}}"
            "PoseWithCovariance" -> "{\"pose\":{\"position\":{${vector3("position")}},\"orientation\":{${quaternion("orientation")}}},\"covariance\":[${covarianceArray()}]}"
            "PoseWithCovarianceStamped" -> "{\"header\":{${header()}},\"pose\":{\"position\":{${vector3("position")}},\"orientation\":{${quaternion("orientation")}}},\"covariance\":[${covarianceArray()}]}"
            "Quaternion" -> "{${quaternion()}}"
            "QuaternionStamped" -> "{\"header\":{${header()}},\"quaternion\":{${quaternion("quaternion")}}}"
            "Transform" -> "{\"translation\":{${vector3("translation")}},\"rotation\":{${quaternion("rotation")}}}"
            "TransformStamped" -> "{\"header\":{${header()}},\"child_frame_id\":\"" + getOrDefault("child_frame_id", false) + "\",\"transform\":{\"translation\":{${vector3("translation")}},\"rotation\":{${quaternion("rotation")}}}}"
            "Twist" -> "{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}"
            "TwistStamped" -> "{\"header\":{${header()}},\"twist\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}}"
            "TwistWithCovariance" -> "{\"twist\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}"
            "TwistWithCovarianceStamped" -> "{\"header\":{${header()}},\"twist\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}"
            "Vector3" -> "{${vector3()}}"
            "Vector3Stamped" -> "{\"header\":{${header()}},\"vector\":{${vector3("vector")}}}"
            "Wrench" -> "{\"force\":{${vector3("force")}},\"torque\":{${vector3("torque")}}}"
            "WrenchStamped" -> "{\"header\":{${header()}},\"wrench\":{\"force\":{${vector3("force")}},\"torque\":{${vector3("torque")}}}}"
            else -> "{}"
        }
    }

        return view
    }

    /*
        input:    type - String, layout - LinearLayout, inflater - LayoutInflater
        output:   None
        remarks:  Dynamically builds input fields for the selected geometry_msgs type
    */
    private fun buildFieldsForType(type: String, layout: LinearLayout, inflater: LayoutInflater) {
        layout.removeAllViews()
        when (type) {
            "Accel" -> {
                layout.addView(createVector3Fields(inflater, layout, "linear (float64)"))
                layout.addView(createVector3Fields(inflater, layout, "angular (float64)"))
            }
            "AccelStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createVector3Fields(inflater, layout, "linear (float64)"))
                layout.addView(createVector3Fields(inflater, layout, "angular (float64)"))
            }
            "AccelWithCovariance" -> {
                layout.addView(createVector3Fields(inflater, layout, "linear (float64)"))
                layout.addView(createVector3Fields(inflater, layout, "angular (float64)"))
                layout.addView(createCovarianceFields(inflater, layout, "covariance (float64)"))
            }
            "AccelWithCovarianceStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createVector3Fields(inflater, layout, "linear (float64)"))
                layout.addView(createVector3Fields(inflater, layout, "angular (float64)"))
                layout.addView(createCovarianceFields(inflater, layout, "covariance (float64)"))
            }
            "Inertia" -> {
                layout.addView(createFloatField(inflater, layout, "m (mass, float64)"))
                layout.addView(createVector3Fields(inflater, layout, "com (float64)"))
                layout.addView(createFloatField(inflater, layout, "ixx (float64)"))
                layout.addView(createFloatField(inflater, layout, "ixy (float64)"))
                layout.addView(createFloatField(inflater, layout, "ixz (float64)"))
                layout.addView(createFloatField(inflater, layout, "iyy (float64)"))
                layout.addView(createFloatField(inflater, layout, "iyz (float64)"))
                layout.addView(createFloatField(inflater, layout, "izz (float64)"))
            }
            "InertiaStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createFloatField(inflater, layout, "m (mass, float64)"))
                layout.addView(createVector3Fields(inflater, layout, "com (float64)"))
                layout.addView(createFloatField(inflater, layout, "ixx (float64)"))
                layout.addView(createFloatField(inflater, layout, "ixy (float64)"))
                layout.addView(createFloatField(inflater, layout, "ixz (float64)"))
                layout.addView(createFloatField(inflater, layout, "iyy (float64)"))
                layout.addView(createFloatField(inflater, layout, "iyz (float64)"))
                layout.addView(createFloatField(inflater, layout, "izz (float64)"))
            }
            "Point" -> {
                layout.addView(createFloatField(inflater, layout, "x (float64)"))
                layout.addView(createFloatField(inflater, layout, "y (float64)"))
                layout.addView(createFloatField(inflater, layout, "z (float64)"))
            }
            "Point32" -> {
                layout.addView(createFloatField(inflater, layout, "x (float32)"))
                layout.addView(createFloatField(inflater, layout, "y (float32)"))
                layout.addView(createFloatField(inflater, layout, "z (float32)"))
            }
            "PointStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createFloatField(inflater, layout, "x (float64)"))
                layout.addView(createFloatField(inflater, layout, "y (float64)"))
                layout.addView(createFloatField(inflater, layout, "z (float64)"))
            }
            "Polygon" -> {
                layout.addView(createPoint32ArrayFields(inflater, layout, "points (float32)"))
            }
            "PolygonStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createPoint32ArrayFields(inflater, layout, "points (float32)"))
            }
            "Pose" -> {
                layout.addView(createPointFields(inflater, layout, "position (float64)"))
                layout.addView(createQuaternionFields(inflater, layout, "orientation (float64)"))
            }
            "PoseArray" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createPoseArrayFields(inflater, layout, "poses (float64)"))
            }
            "PoseStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createPointFields(inflater, layout, "position (float64)"))
                layout.addView(createQuaternionFields(inflater, layout, "orientation (float64)"))
            }
            "PoseWithCovariance" -> {
                layout.addView(createPointFields(inflater, layout, "position (float64)"))
                layout.addView(createQuaternionFields(inflater, layout, "orientation (float64)"))
                layout.addView(createCovarianceFields(inflater, layout, "covariance (float64)"))
            }
            "PoseWithCovarianceStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createPointFields(inflater, layout, "position (float64)"))
                layout.addView(createQuaternionFields(inflater, layout, "orientation (float64)"))
                layout.addView(createCovarianceFields(inflater, layout, "covariance (float64)"))
            }
            "Quaternion" -> {
                layout.addView(createFloatField(inflater, layout, "x (float64)"))
                layout.addView(createFloatField(inflater, layout, "y (float64)"))
                layout.addView(createFloatField(inflater, layout, "z (float64)"))
                layout.addView(createFloatField(inflater, layout, "w (float64)"))
            }
            "QuaternionStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createQuaternionFields(inflater, layout, "quaternion (float64)"))
            }
            "Transform" -> {
                layout.addView(createVector3Fields(inflater, layout, "translation (float64)"))
                layout.addView(createQuaternionFields(inflater, layout, "rotation (float64)"))
            }
            "TransformStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createStringField(inflater, layout, "child_frame_id (string)"))
                layout.addView(createVector3Fields(inflater, layout, "translation (float64)"))
                layout.addView(createQuaternionFields(inflater, layout, "rotation (float64)"))
            }
            "Twist" -> {
                layout.addView(createVector3Fields(inflater, layout, "linear (float64)"))
                layout.addView(createVector3Fields(inflater, layout, "angular (float64)"))
            }
            "TwistStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createVector3Fields(inflater, layout, "linear (float64)"))
                layout.addView(createVector3Fields(inflater, layout, "angular (float64)"))
            }
            "TwistWithCovariance" -> {
                layout.addView(createVector3Fields(inflater, layout, "linear (float64)"))
                layout.addView(createVector3Fields(inflater, layout, "angular (float64)"))
                layout.addView(createCovarianceFields(inflater, layout, "covariance (float64)"))
            }
            "TwistWithCovarianceStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createVector3Fields(inflater, layout, "linear (float64)"))
                layout.addView(createVector3Fields(inflater, layout, "angular (float64)"))
                layout.addView(createCovarianceFields(inflater, layout, "covariance (float64)"))
            }
            "Vector3" -> {
                layout.addView(createVector3Fields(inflater, layout, "(float64)"))
            }
            "Vector3Stamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createVector3Fields(inflater, layout, "vector (float64)"))
            }
            "Wrench" -> {
                layout.addView(createVector3Fields(inflater, layout, "force (float64)"))
                layout.addView(createVector3Fields(inflater, layout, "torque (float64)"))
            }
            "WrenchStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createVector3Fields(inflater, layout, "force (float64)"))
                layout.addView(createVector3Fields(inflater, layout, "torque (float64)"))
            }
        }
    }

    /*
        input:    inflater - LayoutInflater, parent - ViewGroup, label - String
        output:   View (LinearLayout containing x, y, z float fields)
        remarks:  Creates a vertical group of float input fields for a Vector3 (x, y, z)
    */
    private fun createVector3Fields(inflater: LayoutInflater, parent: ViewGroup, label: String): View {
        val group = LinearLayout(requireContext())
        group.orientation = LinearLayout.VERTICAL
        val prefix = if (label.isNotEmpty()) "$label " else ""
        val xTag = if (label.isNotEmpty()) "${label}_x" else "x"
        val yTag = if (label.isNotEmpty()) "${label}_y" else "y"
        val zTag = if (label.isNotEmpty()) "${label}_z" else "z"
        val xLayout = createFloatField(inflater, group, "$prefix x", xTag)
        val yLayout = createFloatField(inflater, group, "$prefix y", yTag)
        val zLayout = createFloatField(inflater, group, "$prefix z", zTag)
        group.addView(xLayout)
        group.addView(yLayout)
        group.addView(zLayout)
        return group
    }

    /*
        input:    inflater - LayoutInflater, parent - ViewGroup, label - String
        output:   View (LinearLayout containing x, y, z float fields)
        remarks:  Creates a group of float input fields for a Point (delegates to createVector3Fields)
    */
    private fun createPointFields(inflater: LayoutInflater, parent: ViewGroup, label: String): View {
        return createVector3Fields(inflater, parent, label)
    }

    /*
        input:    inflater - LayoutInflater, parent - ViewGroup, label - String
        output:   View (LinearLayout containing x, y, z, w float fields)
        remarks:  Creates a vertical group of float input fields for a Quaternion (x, y, z, w)
    */
    private fun createQuaternionFields(inflater: LayoutInflater, parent: ViewGroup, label: String): View {
        val group = LinearLayout(requireContext())
        group.orientation = LinearLayout.VERTICAL
        val prefix = if (label.isNotEmpty()) "$label " else ""
        val xTag = if (label.isNotEmpty()) "${label}_x" else "x"
        val yTag = if (label.isNotEmpty()) "${label}_y" else "y"
        val zTag = if (label.isNotEmpty()) "${label}_z" else "z"
        val wTag = if (label.isNotEmpty()) "${label}_w" else "w"
        val xLayout = createFloatField(inflater, group, "$prefix x", xTag)
        val yLayout = createFloatField(inflater, group, "$prefix y", yTag)
        val zLayout = createFloatField(inflater, group, "$prefix z", zTag)
        val wLayout = createFloatField(inflater, group, "$prefix w", wTag)
        group.addView(xLayout)
        group.addView(yLayout)
        group.addView(zLayout)
        group.addView(wLayout)
        return group
    }

    /*
        input:    inflater - LayoutInflater, parent - ViewGroup
        output:   View (TextInputLayout for frame_id)
        remarks:  Creates a string input field for the header's frame_id
    */
    private fun createHeaderFields(inflater: LayoutInflater, parent: ViewGroup): View {
        return createStringField(inflater, parent, "header frame_id", "header_frame_id")
    }

    /*
        input:    inflater - LayoutInflater, parent - ViewGroup, hint - String, tag - String (optional)
        output:   View (TextInputLayout for float input)
        remarks:  Creates a float input field with the given hint and tag
    */
    private fun createFloatField(inflater: LayoutInflater, parent: ViewGroup, hint: String, tag: String = hint): View {
        val layout = TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox)
        layout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 8, 0, 8)
        }
        layout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        layout.hint = hint
        val editText = TextInputEditText(requireContext())
        editText.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        editText.tag = tag
        // Only allow valid float input
        editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        layout.addView(editText)
        return layout
    }

    /*
        input:    inflater - LayoutInflater, parent - ViewGroup, hint - String, tag - String (optional)
        output:   View (TextInputLayout for string input)
        remarks:  Creates a string input field with the given hint and tag
    */
    private fun createStringField(inflater: LayoutInflater, parent: ViewGroup, hint: String, tag: String = hint): View {
        val layout = TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox)
        layout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 8, 0, 8)
        }
        layout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        layout.hint = hint
        val editText = TextInputEditText(requireContext())
        editText.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        editText.tag = tag
        layout.addView(editText)
        return layout
    }
    /*
        input:    None
        output:   None
        remarks:  Refreshes the UI for all saved message buttons
    */
    private fun refreshSavedButtons() {
        savedButtonsLayout?.let { layout ->
            layout.removeAllViews()
            for ((index, btnData) in savedMessages.withIndex()) {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                val btn = Button(requireContext()).apply {
                    text = btnData.label
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        viewLifecycleOwner.lifecycleScope.launch {
                            val topicKey = btnData.topic + "|" + btnData.type
                            val type = "geometry_msgs/msg/${btnData.type}"
                            if (!advertisedTopics.contains(topicKey)) {
                                rosViewModel.advertiseTopic(btnData.topic, type)
                                advertisedTopics.add(topicKey)
                                kotlinx.coroutines.delay(200)
                            }
                            rosViewModel.publishCustomRawMessage(btnData.topic, type, btnData.message)
                        }
                    }
                }
                val removeBtn = Button(requireContext()).apply {
                    text = "✕"
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    setOnClickListener {
                        android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Delete Saved Message")
                            .setMessage("Are you sure you want to delete this saved message button?")
                            .setPositiveButton("Delete") { _, _ ->
                                savedMessages.removeAt(index)
                                saveButtonsToPrefs()
                                refreshSavedButtons()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    contentDescription = "Remove ${btnData.label}"
                }
                row.addView(btn)
                row.addView(removeBtn)
                layout.addView(row)
            }
        }
    }

    /*
        input:    None
        output:   None
        remarks:  Saves the list of reusable message buttons to shared preferences as JSON
    */
    private fun saveButtonsToPrefs() {
        val prefs = requireContext().getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        val json = gson.toJson(savedMessages)
        prefs.edit().putString("geometry_buttons", json).apply()
    }

    /*
        input:    None
        output:   None
        remarks:  Loads the list of reusable message buttons from shared preferences
    */
    private fun loadSavedButtons() {
        val prefs = requireContext().getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        val json = prefs.getString("geometry_buttons", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<ReusableMsgButton>>() {}.type
            val loaded: MutableList<ReusableMsgButton> = gson.fromJson(json, type)
            savedMessages.clear()
            savedMessages.addAll(loaded)
        }
    }

    /*
        input:    inflater - LayoutInflater, parent - ViewGroup, label - String
        output:   View (LinearLayout containing 36 float fields)
        remarks:  Creates a vertical group of float input fields for a 6x6 covariance matrix
    */
    private fun createCovarianceFields(inflater: LayoutInflater, parent: ViewGroup, label: String): View {
        val group = LinearLayout(requireContext())
        group.orientation = LinearLayout.VERTICAL
        for (i in 0 until 36) {
            group.addView(createFloatField(inflater, group, "$label[$i]", "$label$i"))
        }
        return group
    }

    /*
        input:    inflater - LayoutInflater, parent - ViewGroup, label - String
        output:   View (LinearLayout containing 3 Point32 fields)
        remarks:  Creates a group of float input fields for an array of 3 Point32s (x, y, z for each)
    */
    private fun createPoint32ArrayFields(inflater: LayoutInflater, parent: ViewGroup, label: String): View {
        // For simplicity, just create 3 Point32s (x, y, z for each)
        val group = LinearLayout(requireContext())
        group.orientation = LinearLayout.VERTICAL
        for (i in 0 until 3) {
            group.addView(createFloatField(inflater, group, "$label[$i] x (float32)", "${label}_${i}_x"))
            group.addView(createFloatField(inflater, group, "$label[$i] y (float32)", "${label}_${i}_y"))
            group.addView(createFloatField(inflater, group, "$label[$i] z (float32)", "${label}_${i}_z"))
        }
        return group
    }

    /*
        input:    inflater - LayoutInflater, parent - ViewGroup, label - String
        output:   View (LinearLayout containing 2 Pose fields)
        remarks:  Creates a group of input fields for an array of 2 Poses (position and orientation for each)
    */
    private fun createPoseArrayFields(inflater: LayoutInflater, parent: ViewGroup, label: String): View {
        // For simplicity, just create 2 Poses
        val group = LinearLayout(requireContext())
        group.orientation = LinearLayout.VERTICAL
        for (i in 0 until 2) {
            group.addView(createPointFields(inflater, group, "$label[$i] position"))
            group.addView(createQuaternionFields(inflater, group, "$label[$i] orientation"))
        }
        return group
    }

    /*
        input:    layout - LinearLayout, tag - String
        output:   String (value of the EditText with the given tag, or "0" if empty/invalid)
        remarks:  Retrieves and sanitizes the value from an EditText field by tag
    */
    private fun getValue(layout: LinearLayout, tag: String): String {
        val editText = layout.findViewWithTag<EditText>(tag)
        val value = editText?.text?.toString()?.trim() ?: ""
        return if (value.isEmpty()) "0" else value.toFloatOrNull()?.toString() ?: "0"
    }

    /*
        input:    type - String, layout - LinearLayout
        output:   String (JSON message for the given geometry_msgs type)
        remarks:  Serializes the input fields into a JSON string for the selected geometry_msgs type. Includes inner helpers for each message structure.
    */
    // Generic message builder to reduce redundancy
    fun buildMessage(
        type: String,
        layout: LinearLayout,
        valueProvider: (tag: String, isFloat: Boolean) -> String
    ): String {
        fun vector3(prefix: String = ""): String =
            "\"x\":" + valueProvider(if (prefix.isEmpty()) "x" else "${prefix}_x", true) + "," +
            "\"y\":" + valueProvider(if (prefix.isEmpty()) "y" else "${prefix}_y", true) + "," +
            "\"z\":" + valueProvider(if (prefix.isEmpty()) "z" else "${prefix}_z", true)
        fun quaternion(prefix: String = ""): String =
            "\"x\":" + valueProvider(if (prefix.isEmpty()) "x" else "${prefix}_x", true) + "," +
            "\"y\":" + valueProvider(if (prefix.isEmpty()) "y" else "${prefix}_y", true) + "," +
            "\"z\":" + valueProvider(if (prefix.isEmpty()) "z" else "${prefix}_z", true) + "," +
            "\"w\":" + valueProvider(if (prefix.isEmpty()) "w" else "${prefix}_w", true)
        fun header(): String =
            "\"frame_id\":\"" + valueProvider("header_frame_id", false) + "\""
        fun point32Array(label: String = "points"): String =
            (0 until 3).joinToString(",") { i ->
                "{\"x\":" + valueProvider("${label}_${i}_x", true) + ",\"y\":" + valueProvider("${label}_${i}_y", true) + ",\"z\":" + valueProvider("${label}_${i}_z", true) + "}"
            }
        fun poseArray(label: String = "poses"): String =
            (0 until 2).joinToString(",") { i ->
                "{\"position\":{${vector3("${label}[$i] position")}},\"orientation\":{${quaternion("${label}[$i] orientation")}}}"
            }
        fun covarianceArray(label: String = "covariance"): String =
            (0 until 36).joinToString(",") { valueProvider("$label$it", true) }
        return when (type) {
            "Accel" -> "{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}"
            "AccelStamped" -> "{\"header\":{${header()}},\"accel\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}}"
            "AccelWithCovariance" -> "{\"accel\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}"
            "AccelWithCovarianceStamped" -> "{\"header\":{${header()}},\"accel\":{\"accel\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}}"
            "Inertia" -> "{\"m\":" + valueProvider("m (mass)", true) + ",\"com\":{${vector3("com")}},\"ixx\":" + valueProvider("ixx", true) + ",\"ixy\":" + valueProvider("ixy", true) + ",\"ixz\":" + valueProvider("ixz", true) + ",\"iyy\":" + valueProvider("iyy", true) + ",\"iyz\":" + valueProvider("iyz", true) + ",\"izz\":" + valueProvider("izz", true) + "}"
            "InertiaStamped" -> "{\"header\":{${header()}},\"inertia\":{\"m\":" + valueProvider("m (mass)", true) + ",\"com\":{${vector3("com")}},\"ixx\":" + valueProvider("ixx", true) + ",\"ixy\":" + valueProvider("ixy", true) + ",\"ixz\":" + valueProvider("ixz", true) + ",\"iyy\":" + valueProvider("iyy", true) + ",\"iyz\":" + valueProvider("iyz", true) + ",\"izz\":" + valueProvider("izz", true) + "}}"
            "Point" -> "{${vector3()}}"
            "Point32" -> "{\"x\":" + valueProvider("x (float32)", true) + ",\"y\":" + valueProvider("y (float32)", true) + ",\"z\":" + valueProvider("z (float32)", true) + "}"
            "PointStamped" -> "{\"header\":{${header()}},\"point\":{${vector3()}}}"
            "Polygon" -> "{\"points\":[${point32Array()}]}"
            "PolygonStamped" -> "{\"header\":{${header()}},\"polygon\":{\"points\":[${point32Array()}]}}"
            "Pose" -> "{\"position\":{${vector3("position")}},\"orientation\":{${quaternion("orientation")}}}"
            "PoseArray" -> "{\"header\":{${header()}},\"poses\":[${poseArray()}]}"
            "PoseStamped" -> "{\"header\":{${header()}},\"pose\":{\"position\":{${vector3("position")}},\"orientation\":{${quaternion("orientation")}}}}"
            "PoseWithCovariance" -> "{\"pose\":{\"position\":{${vector3("position")}},\"orientation\":{${quaternion("orientation")}}},\"covariance\":[${covarianceArray()}]}"
            "PoseWithCovarianceStamped" -> "{\"header\":{${header()}},\"pose\":{\"position\":{${vector3("position")}},\"orientation\":{${quaternion("orientation")}}},\"covariance\":[${covarianceArray()}]}"
            "Quaternion" -> "{${quaternion()}}"
            "QuaternionStamped" -> "{\"header\":{${header()}},\"quaternion\":{${quaternion("quaternion")}}}"
            "Transform" -> "{\"translation\":{${vector3("translation")}},\"rotation\":{${quaternion("rotation")}}}"
            "TransformStamped" -> "{\"header\":{${header()}},\"child_frame_id\":\"" + valueProvider("child_frame_id", false) + "\",\"transform\":{\"translation\":{${vector3("translation")}},\"rotation\":{${quaternion("rotation")}}}}"
            "Twist" -> "{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}"
            "TwistStamped" -> "{\"header\":{${header()}},\"twist\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}}"
            "TwistWithCovariance" -> "{\"twist\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}"
            "TwistWithCovarianceStamped" -> "{\"header\":{${header()}},\"twist\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}"
            "Vector3" -> "{${vector3()}}"
            "Vector3Stamped" -> "{\"header\":{${header()}},\"vector\":{${vector3("vector")}}}"
            "Wrench" -> "{\"force\":{${vector3("force")}},\"torque\":{${vector3("torque")}}}"
            "WrenchStamped" -> "{\"header\":{${header()}},\"wrench\":{\"force\":{${vector3("force")}},\"torque\":{${vector3("torque")}}}}"
            else -> "{}"
        }
    }

    private fun buildMessageFromFields(type: String, layout: LinearLayout): String {
        return buildMessage(type, layout) { tag, isFloat -> getValue(layout, tag) }
    }

    private fun buildMessageWithDefaults(type: String, layout: LinearLayout): String {
        fun getOrDefault(tag: String, isFloat: Boolean): String {
            val editText = layout.findViewWithTag<EditText>(tag)
            val value = editText?.text?.toString()?.trim()
            return if (value.isNullOrEmpty()) {
                if (isFloat) "0.0" else "0"
            } else value
        }
        return buildMessage(type, layout, ::getOrDefault)
    }
}