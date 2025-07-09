package com.example.testros2jsbridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
    private val rosViewModel: RosViewModel by activityViewModels()

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
        // No default topic
        topicEditText.setText("/")
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

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view_: View?, position: Int, id: Long) {
                val selectedType = geometryTypes[position]
                buildFieldsForType(selectedType, dynamicFieldsLayout, inflater)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

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
            val message = buildMessageFromFields(selectedType, dynamicFieldsLayout)
            val labelInput = EditText(requireContext())
            labelInput.hint = "Label for this message"
            val dialog = android.app.AlertDialog.Builder(requireContext())
                .setTitle("Save Message Button")
                .setView(labelInput)
                .setPositiveButton("Save") { _, _ ->
                    val label = labelInput.text.toString().ifBlank { selectedType }
                    savedMessages.add(ReusableMsgButton(label, topic, selectedType, message))
                    saveButtonsToPrefs()
                    refreshSavedButtons()
                }
                .setNegativeButton("Cancel", null)
                .create()
            dialog.show()
        }

        // Build initial fields
        buildFieldsForType(geometryTypes[0], dynamicFieldsLayout, inflater)

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
                layout.addView(createVector3Fields(inflater, layout, "linear"))
                layout.addView(createVector3Fields(inflater, layout, "angular"))
            }
            "AccelStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createVector3Fields(inflater, layout, "linear"))
                layout.addView(createVector3Fields(inflater, layout, "angular"))
            }
            "AccelWithCovariance" -> {
                layout.addView(createVector3Fields(inflater, layout, "linear"))
                layout.addView(createVector3Fields(inflater, layout, "angular"))
                layout.addView(createCovarianceFields(inflater, layout, "covariance"))
            }
            "AccelWithCovarianceStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createVector3Fields(inflater, layout, "linear"))
                layout.addView(createVector3Fields(inflater, layout, "angular"))
                layout.addView(createCovarianceFields(inflater, layout, "covariance"))
            }
            "Inertia" -> {
                layout.addView(createFloatField(inflater, layout, "m (mass)"))
                layout.addView(createVector3Fields(inflater, layout, "com"))
                layout.addView(createFloatField(inflater, layout, "ixx"))
                layout.addView(createFloatField(inflater, layout, "ixy"))
                layout.addView(createFloatField(inflater, layout, "ixz"))
                layout.addView(createFloatField(inflater, layout, "iyy"))
                layout.addView(createFloatField(inflater, layout, "iyz"))
                layout.addView(createFloatField(inflater, layout, "izz"))
            }
            "InertiaStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createFloatField(inflater, layout, "m (mass)"))
                layout.addView(createVector3Fields(inflater, layout, "com"))
                layout.addView(createFloatField(inflater, layout, "ixx"))
                layout.addView(createFloatField(inflater, layout, "ixy"))
                layout.addView(createFloatField(inflater, layout, "ixz"))
                layout.addView(createFloatField(inflater, layout, "iyy"))
                layout.addView(createFloatField(inflater, layout, "iyz"))
                layout.addView(createFloatField(inflater, layout, "izz"))
            }
            "Point" -> {
                layout.addView(createFloatField(inflater, layout, "x"))
                layout.addView(createFloatField(inflater, layout, "y"))
                layout.addView(createFloatField(inflater, layout, "z"))
            }
            "Point32" -> {
                layout.addView(createFloatField(inflater, layout, "x (float32)"))
                layout.addView(createFloatField(inflater, layout, "y (float32)"))
                layout.addView(createFloatField(inflater, layout, "z (float32)"))
            }
            "PointStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createFloatField(inflater, layout, "x"))
                layout.addView(createFloatField(inflater, layout, "y"))
                layout.addView(createFloatField(inflater, layout, "z"))
            }
            "Polygon" -> {
                layout.addView(createPoint32ArrayFields(inflater, layout, "points"))
            }
            "PolygonStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createPoint32ArrayFields(inflater, layout, "points"))
            }
            "Pose" -> {
                layout.addView(createPointFields(inflater, layout, "position"))
                layout.addView(createQuaternionFields(inflater, layout, "orientation"))
            }
            "PoseArray" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createPoseArrayFields(inflater, layout, "poses"))
            }
            "PoseStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createPointFields(inflater, layout, "position"))
                layout.addView(createQuaternionFields(inflater, layout, "orientation"))
            }
            "PoseWithCovariance" -> {
                layout.addView(createPointFields(inflater, layout, "position"))
                layout.addView(createQuaternionFields(inflater, layout, "orientation"))
                layout.addView(createCovarianceFields(inflater, layout, "covariance"))
            }
            "PoseWithCovarianceStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createPointFields(inflater, layout, "position"))
                layout.addView(createQuaternionFields(inflater, layout, "orientation"))
                layout.addView(createCovarianceFields(inflater, layout, "covariance"))
            }
            "Quaternion" -> {
                layout.addView(createFloatField(inflater, layout, "x"))
                layout.addView(createFloatField(inflater, layout, "y"))
                layout.addView(createFloatField(inflater, layout, "z"))
                layout.addView(createFloatField(inflater, layout, "w"))
            }
            "QuaternionStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createQuaternionFields(inflater, layout, "quaternion"))
            }
            "Transform" -> {
                layout.addView(createVector3Fields(inflater, layout, "translation"))
                layout.addView(createQuaternionFields(inflater, layout, "rotation"))
            }
            "TransformStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createStringField(inflater, layout, "child_frame_id"))
                layout.addView(createVector3Fields(inflater, layout, "translation"))
                layout.addView(createQuaternionFields(inflater, layout, "rotation"))
            }
            "Twist" -> {
                layout.addView(createVector3Fields(inflater, layout, "linear"))
                layout.addView(createVector3Fields(inflater, layout, "angular"))
            }
            "TwistStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createVector3Fields(inflater, layout, "linear"))
                layout.addView(createVector3Fields(inflater, layout, "angular"))
            }
            "TwistWithCovariance" -> {
                layout.addView(createVector3Fields(inflater, layout, "linear"))
                layout.addView(createVector3Fields(inflater, layout, "angular"))
                layout.addView(createCovarianceFields(inflater, layout, "covariance"))
            }
            "TwistWithCovarianceStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createVector3Fields(inflater, layout, "linear"))
                layout.addView(createVector3Fields(inflater, layout, "angular"))
                layout.addView(createCovarianceFields(inflater, layout, "covariance"))
            }
            "Vector3" -> {
                layout.addView(createVector3Fields(inflater, layout, ""))
            }
            "Vector3Stamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createVector3Fields(inflater, layout, "vector"))
            }
            "Wrench" -> {
                layout.addView(createVector3Fields(inflater, layout, "force"))
                layout.addView(createVector3Fields(inflater, layout, "torque"))
            }
            "WrenchStamped" -> {
                layout.addView(createHeaderFields(inflater, layout))
                layout.addView(createVector3Fields(inflater, layout, "force"))
                layout.addView(createVector3Fields(inflater, layout, "torque"))
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
    private fun buildMessageFromFields(type: String, layout: LinearLayout): String {
        /*
            input:    prefix - String (optional)
            output:   String (JSON for Vector3 fields)
            remarks:  Helper to serialize x, y, z fields as JSON
        */
        fun vector3(prefix: String = ""): String =
            "\"x\":" + getValue(layout, if (prefix.isEmpty()) "x" else "${prefix}_x") + "," +
            "\"y\":" + getValue(layout, if (prefix.isEmpty()) "y" else "${prefix}_y") + "," +
            "\"z\":" + getValue(layout, if (prefix.isEmpty()) "z" else "${prefix}_z")

        /*
            input:    prefix - String (optional)
            output:   String (JSON for Quaternion fields)
            remarks:  Helper to serialize x, y, z, w fields as JSON
        */
        fun quaternion(prefix: String = ""): String =
            "\"x\":" + getValue(layout, if (prefix.isEmpty()) "x" else "${prefix}_x") + "," +
            "\"y\":" + getValue(layout, if (prefix.isEmpty()) "y" else "${prefix}_y") + "," +
            "\"z\":" + getValue(layout, if (prefix.isEmpty()) "z" else "${prefix}_z") + "," +
            "\"w\":" + getValue(layout, if (prefix.isEmpty()) "w" else "${prefix}_w")

        /*
            input:    None
            output:   String (JSON for header field)
            remarks:  Helper to serialize the header's frame_id field
        */
        fun header(): String =
            "\"frame_id\":\"" + getValue(layout, "header_frame_id") + "\""

        /*
            input:    label - String (default "points")
            output:   String (JSON array for Point32 fields)
            remarks:  Helper to serialize an array of 3 Point32s
        */
        fun point32Array(label: String = "points"): String =
            (0 until 3).joinToString(",") { i ->
                "{\"x\":" + getValue(layout, "${label}_${i}_x") + ",\"y\":" + getValue(layout, "${label}_${i}_y") + ",\"z\":" + getValue(layout, "${label}_${i}_z") + "}"
            }

        /*
            input:    label - String (default "poses")
            output:   String (JSON array for Pose fields)
            remarks:  Helper to serialize an array of 2 Poses (position and orientation)
        */
        fun poseArray(label: String = "poses"): String =
            (0 until 2).joinToString(",") { i ->
                "{\"position\":{${vector3("${label}[$i] position")}},\"orientation\":{${quaternion("${label}[$i] orientation")}}}"
            }

        /*
            input:    label - String (default "covariance")
            output:   String (comma-separated values for covariance array)
            remarks:  Helper to serialize a 36-element covariance array
        */
        fun covarianceArray(label: String = "covariance"): String =
            (0 until 36).joinToString(",") { getValue(layout, "$label$it") }

        return when (type) {
            "Accel" -> "{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}"
            "AccelStamped" -> "{\"header\":{${header()}},\"accel\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}}"
            "AccelWithCovariance" -> "{\"accel\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}"
            "AccelWithCovarianceStamped" -> "{\"header\":{${header()}},\"accel\":{\"accel\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}}"
            "Inertia" -> "{\"m\":" + getValue(layout, "m (mass)") + ",\"com\":{${vector3("com")}},\"ixx\":" + getValue(layout, "ixx") + ",\"ixy\":" + getValue(layout, "ixy") + ",\"ixz\":" + getValue(layout, "ixz") + ",\"iyy\":" + getValue(layout, "iyy") + ",\"iyz\":" + getValue(layout, "iyz") + ",\"izz\":" + getValue(layout, "izz") + "}"
            "InertiaStamped" -> "{\"header\":{${header()}},\"inertia\":{\"m\":" + getValue(layout, "m (mass)") + ",\"com\":{${vector3("com")}},\"ixx\":" + getValue(layout, "ixx") + ",\"ixy\":" + getValue(layout, "ixy") + ",\"ixz\":" + getValue(layout, "ixz") + ",\"iyy\":" + getValue(layout, "iyy") + ",\"iyz\":" + getValue(layout, "iyz") + ",\"izz\":" + getValue(layout, "izz") + "}}"
            "Point" -> "{${vector3()}}"
            "Point32" -> "{\"x\":" + getValue(layout, "x (float32)") + ",\"y\":" + getValue(layout, "y (float32)") + ",\"z\":" + getValue(layout, "z (float32)") + "}"
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
            "TransformStamped" -> "{\"header\":{${header()}},\"child_frame_id\":\"" + getValue(layout, "child_frame_id") + "\",\"transform\":{\"translation\":{${vector3("translation")}},\"rotation\":{${quaternion("rotation")}}}}"
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
}