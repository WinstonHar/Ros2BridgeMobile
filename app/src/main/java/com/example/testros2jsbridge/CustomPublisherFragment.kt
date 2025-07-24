package com.example.testros2jsbridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText
import androidx.fragment.app.Fragment
import android.util.Log
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.gson.Gson
import androidx.core.content.edit

/*
    This fragment allows users to create, save, and manage custom publisher buttons for sending standard ROS messages with user-defined content.
*/

class CustomPublisherFragment : Fragment() {
    /*
        input:    messageType - String, userInput - String
        output:   Null if valid, or error message String if invalid
        remarks:  Validates user input for the selected std_msgs type
    */
    private fun validateStdMsgInput(messageType: String, userInput: String): String? {
        return when (messageType) {
            "Bool" -> {
                val v = userInput.trim().lowercase()
                if (v == "true" || v == "1" || v == "false" || v == "0") null
                else "Input must be 1 (true) or 0 (false), or 'true'/'false'"
            }
            "Byte", "Char", "Int8" -> {
                val n = userInput.toIntOrNull()
                if (n == null || n < -128 || n > 127) "Input must be an integer between -128 and 127"
                else null
            }
            "UInt8" -> {
                val n = userInput.toIntOrNull()
                if (n == null || n < 0 || n > 255) "Input must be an integer between 0 and 255"
                else null
            }
            "Int16" -> {
                val n = userInput.toIntOrNull()
                if (n == null || n < -32768 || n > 32767) "Input must be an integer between -32768 and 32767"
                else null
            }
            "UInt16" -> {
                val n = userInput.toIntOrNull()
                if (n == null || n < 0 || n > 65535) "Input must be an integer between 0 and 65535"
                else null
            }
            "Int32" -> {
                val n = userInput.toLongOrNull()
                if (n == null || n < -2147483648L || n > 2147483647L) "Input must be a 32-bit integer between -2147483648 and 2147483647"
                else null
            }
            "UInt32" -> {
                val n = userInput.toLongOrNull()
                if (n == null || n < 0L || n > 4294967295L) "Input must be an unsigned 32-bit integer between 0 and 4294967295"
                else null
            }
            "Int64" -> {
                val n = userInput.toLongOrNull()
                if (n == null) "Input must be a 64-bit integer"
                else null
            }
            "UInt64" -> {
                val n = userInput.toBigIntegerOrNull()
                if (n == null || n < java.math.BigInteger.ZERO) "Input must be an unsigned 64-bit integer (>= 0)"
                else null
            }
            "Float32", "Float64" -> {
                val n = userInput.toDoubleOrNull()
                if (n == null) "Input must be a floating point number"
                else null
            }
            "ColorRGBA" -> {
                val parts = userInput.split(",").map { it.trim() }
                if (parts.size != 4) "Input must be 4 comma-separated floats: r,g,b,a (0.0-1.0)"
                else if (parts.any { it.toFloatOrNull() == null }) "All values must be floats (e.g. 0.5,0.5,0.5,1.0)"
                else null
            }
            "Empty" -> null
            "String" -> null
            "Float32MultiArray", "Float64MultiArray" -> {
                val arr = userInput.split(",").map { it.trim() }
                if (arr.isEmpty() || arr.any { it.isNotEmpty() && it.toDoubleOrNull() == null }) "Input must be comma-separated floats (e.g. 1.0,2.0,3.0)"
                else null
            }
            "Int8MultiArray", "UInt8MultiArray", "Int16MultiArray", "UInt16MultiArray", "Int32MultiArray", "UInt32MultiArray", "Int64MultiArray", "UInt64MultiArray" -> {
                val arr = userInput.split(",").map { it.trim() }
                if (arr.isEmpty() || arr.any { it.isNotEmpty() && it.toLongOrNull() == null }) "Input must be comma-separated integers (e.g. 1,2,3)"
                else null
            }
            "ByteMultiArray" -> {
                val arr = userInput.split(",").map { it.trim() }
                if (arr.isEmpty() || arr.any { it.isNotEmpty() && it.toIntOrNull() == null }) "Input must be comma-separated bytes (0-255)"
                else if (arr.any { it.isNotEmpty() && (it.toInt() < 0 || it.toInt() > 255) }) "Each value must be between 0 and 255"
                else null
            }
            "Header" -> null
            "MultiArrayDimension" -> {
                val parts = userInput.split(",").map { it.trim() }
                if (parts.size != 3) "Input must be 3 comma-separated values: label,size,stride"
                else if (parts[1].toIntOrNull() == null || parts[2].toIntOrNull() == null) "Size and stride must be integers"
                else null
            }
            "MultiArrayLayout" -> null
            else -> null
        }
    }

    private val PREFS_NAME = "custom_publishers_prefs"
    private val SAVED_BUTTONS_KEY = "custom_publishers"

    /*
        input:    None
        output:   None
        remarks:  Holds static members for the fragment
    */
    companion object {
        private val gson = Gson()
    }

    /*
        input:    messageType - String, userInput - String
        output:   String containing JSON for the std_msgs type
        remarks:  Builds correct JSON for all supported std_msgs types
    */
    private fun buildStdMsgJson(messageType: String, userInput: String): String {
        if (messageType.isBlank()) {
            Log.w("CustomPublisherFragment", "buildStdMsgJson called with blank messageType; returning empty JSON.")
            return "{}" // fallback for legacy/malformed data
        }
        return when (messageType) {
            "Bool" -> {
                val boolValue = when (userInput.trim().lowercase()) {
                    "true", "1" -> true
                    "false", "0" -> false
                    else -> false
                }
                """{"data": $boolValue}"""
            }
            "Byte", "Char", "Int8", "UInt8" -> {
                val num = userInput.toIntOrNull() ?: 0
                """{"data": $num}"""
            }
            "Int16", "UInt16" -> {
                val num = userInput.toIntOrNull() ?: 0
                """{"data": $num}"""
            }
            "Int32", "UInt32" -> {
                val num = userInput.toLongOrNull() ?: 0L
                """{"data": $num}"""
            }
            "Int64", "UInt64" -> {
                val num = userInput.toLongOrNull() ?: 0L
                """{"data": $num}"""
            }
            "Float32", "Float64" -> {
                val num = userInput.toDoubleOrNull() ?: 0.0
                """{"data": $num}"""
            }
            "String" -> {
                """{"data": ${Gson().toJson(userInput)}}"""
            }
            "ColorRGBA" -> {
                val parts = userInput.split(",").map { it.trim().toFloatOrNull() ?: 0f }
                val (r, g, b, a) = (parts + List(4) { 0f }).take(4)
                """{"r":$r,"g":$g,"b":$b,"a":$a}"""
            }
            "Empty" -> {
                "{}"
            }
            "Float32MultiArray", "Float64MultiArray" -> {
                val arr = userInput.split(",").mapNotNull { it.trim().toDoubleOrNull() }
                """{"data": $arr}"""
            }
            "Int8MultiArray", "UInt8MultiArray", "Int16MultiArray", "UInt16MultiArray", "Int32MultiArray", "UInt32MultiArray", "Int64MultiArray", "UInt64MultiArray" -> {
                val arr = userInput.split(",").mapNotNull { it.trim().toLongOrNull() }
                """{"data": $arr}"""
            }
            "ByteMultiArray" -> {
                val arr = userInput.split(",").mapNotNull { it.trim().toIntOrNull() }
                """{"data": $arr}"""
            }
            "Header" -> {
                """{"frame_id": ${Gson().toJson(userInput)}}"""
            }
            "MultiArrayDimension" -> {
                val parts = userInput.split(",")
                val label = if (parts.isNotEmpty()) Gson().toJson(parts[0].trim()) else "\"\""
                val size = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val stride = parts.getOrNull(2)?.toIntOrNull() ?: 0
                """{"label":$label,"size":$size,"stride":$stride}"""
            }
            "MultiArrayLayout" -> {
                """{"dim":[],"data_offset":0}"""
            }
            else -> {
                """{"data": ${Gson().toJson(userInput)}}"""
            }
        }
    }

    private lateinit var rosViewModel: RosViewModel

    /*
        input:    None
        output:   None
        remarks:  Cleans up resources when the view is destroyed
    */
    override fun onDestroyView() {
        super.onDestroyView()
        rosViewModel.clearCustomMessage()
    }

    /*
        input:    inflater - LayoutInflater, container - ViewGroup?, savedInstanceState - Bundle?
        output:   View?
        remarks:  Inflates the fragment view and sets up custom publisher UI
    */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rosViewModel = ViewModelProvider(requireActivity()).get(RosViewModel::class.java)

        val view = inflater.inflate(R.layout.activity_custom_publisher, container, false)

        val topicEditText = view.findViewById<TextInputEditText>(R.id.edittext_topic)
        topicEditText.setText("/")
        val messageEditText = view.findViewById<TextInputEditText>(R.id.edittext_message)
        val addPublisherButton = view.findViewById<Button>(R.id.button_add_publisher)
        val customButtonsLayout = view.findViewById<LinearLayout>(R.id.layout_custom_buttons)

        val messageBaseTypes = listOf(
            "Bool", "Byte", "Char", "ColorRGBA", "Empty", "Float32", "Float64", "Header",
            "Int16", "Int32", "Int64", "Int8", "String", "UInt16", "UInt32", "UInt64", "UInt8"
        )

        val messageBaseTypeSpinner = view.findViewById<Spinner>(R.id.spinner_message_base_type)
        val baseTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, messageBaseTypes)
        baseTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        messageBaseTypeSpinner.adapter = baseTypeAdapter

        // Load saved publishers from SharedPreferences
        val loaded = loadSavedPublishers()
        if (loaded != null) {
            rosViewModel.setCustomPublishers(loaded)
        }

        addPublisherButton.setOnClickListener {
            val topic = topicEditText.text.toString()
            val messageType = messageBaseTypeSpinner.selectedItem?.toString() ?: ""
            val userInput = messageEditText.text.toString()
            Log.d("CustomPublisherFragment", "topic='$topic', messageType='$messageType', message='$userInput'")
            if (topic.isEmpty()) topicEditText.error = "Topic required"
            if (userInput.isEmpty()) messageEditText.error = "Message required"
            if (topic.isEmpty() || messageType.isEmpty() || userInput.isEmpty()) {
                Log.w("CustomPublisherFragment", "Missing required fields")
                return@setOnClickListener
            }
            val errorMsg = validateStdMsgInput(messageType, userInput)
            if (errorMsg != null) {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Invalid Input for $messageType")
                    .setMessage(errorMsg)
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }
            val newPublisher = RosViewModel.CustomPublisher(topic, messageType, userInput)
            val updated = rosViewModel.customPublishers.value + newPublisher
            rosViewModel.setCustomPublishers(updated)
            savePublishersToPrefs(updated)
            messageEditText.text?.clear()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val advertisedTopics = mutableSetOf<String>()
            rosViewModel.customPublishers.collect { publishers ->
                customButtonsLayout.removeAllViews()
                for ((index, publisher) in publishers.withIndex()) {
                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    }
                    val newButton = Button(requireContext()).apply {
                        text = "Publish to ${publisher.topic}"
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        setOnClickListener {
                            viewLifecycleOwner.lifecycleScope.launch {
                                val topicKey = publisher.topic + "|" + publisher.messageType
                                if (!advertisedTopics.contains(topicKey)) {
                                    rosViewModel.advertiseTopic(publisher.topic, "std_msgs/msg/${publisher.messageType}")
                                    advertisedTopics.add(topicKey)
                                    kotlinx.coroutines.delay(200)
                                }
                                val message = buildStdMsgJson(publisher.messageType, publisher.message)
                                rosViewModel.publishCustomRawMessage(publisher.topic, "std_msgs/msg/${publisher.messageType}", message)
                            }
                        }
                    }
                    val removeBtn = Button(requireContext()).apply {
                        text = "✕"
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        setOnClickListener {
                            android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Delete Publisher")
                                .setMessage("Are you sure you want to delete this publisher button?")
                                .setPositiveButton("Delete") { _, _ ->
                                    val current = rosViewModel.customPublishers.value.toMutableList()
                                    if (index in current.indices) {
                                        current.removeAt(index)
                                        rosViewModel.setCustomPublishers(current)
                                        savePublishersToPrefs(current)
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                        contentDescription = "Remove publisher for ${publisher.topic}"
                    }
                    row.addView(newButton)
                    row.addView(removeBtn)
                    customButtonsLayout.addView(row)
                }
            }
        }


        return view
    }

    /*
        input:    list - List of CustomPublisher
        output:   None
        remarks:  Saves custom publisher buttons to SharedPreferences
    */
    private fun savePublishersToPrefs(list: List<RosViewModel.CustomPublisher>) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        // Build a JSON array of objects with label, topic, type (base type), msg fields
        val arr = org.json.JSONArray()
        for (pub in list) {
            val obj = org.json.JSONObject()
            obj.put("label", pub.topic)
            obj.put("topic", pub.topic)
            val baseType = pub.messageType.substringAfterLast('/')
            obj.put("type", baseType)
            obj.put("msg", pub.message)
            arr.put(obj)
        }
        prefs.edit { putString(SAVED_BUTTONS_KEY, arr.toString()) }
    }

    /*
        input:    None
        output:   List of CustomPublisher or null
        remarks:  Loads saved custom publisher buttons from SharedPreferences
    */
    private fun loadSavedPublishers(): List<RosViewModel.CustomPublisher>? {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        val json = prefs.getString(SAVED_BUTTONS_KEY, null)
        if (json.isNullOrBlank()) return null
        return try {
            val arr = org.json.JSONArray(json)
            val result = mutableListOf<RosViewModel.CustomPublisher>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val topic = obj.optString("topic", "")
                // Accept both "type": "Bool" and "type": "std_msgs/msg/Bool"
                val rawType = obj.optString("type", "")
                val baseType = if (rawType.startsWith("std_msgs/msg/")) rawType.removePrefix("std_msg/msg/") else rawType
                val msg = obj.optString("msg", "")
                if (topic.isNotBlank() && baseType.isNotBlank()) {
                    result.add(RosViewModel.CustomPublisher(topic, baseType, msg))
                }
            }
            result
        } catch (_: Exception) { null }
    }
}