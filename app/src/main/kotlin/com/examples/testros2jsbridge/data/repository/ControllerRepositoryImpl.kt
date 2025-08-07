package com.examples.testros2jsbridge.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.model.ControllerPreset
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject
import com.examples.testros2jsbridge.domain.model.JoystickMapping
import com.examples.testros2jsbridge.domain.model.RosId

class ControllerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SharedPreferences
) : ControllerRepository {

    // --- Data Classes ---
    // Use domain layer types for all config and mapping
    // Remove local data classes, use ControllerConfig and related domain models
    // --- State ---
    private val _controller = MutableStateFlow(ControllerConfig())
    override val controller: Flow<ControllerConfig> = _controller.asStateFlow()

    // --- Joystick Mappings ---
    private val PREFS_JOYSTICK_MAPPINGS = "joystick_mappings"
    private val PREFS_JOYSTICK_ADDRESSING = "joystick_addressing_mode"
    private val PREFS_JOYSTICK_RATE = "joystick_publish_rate"

    override suspend fun saveController(controller: ControllerConfig) {
        _controller.value = controller
        // Persist as needed
    }

    override suspend fun getController(): ControllerConfig {
        // Return the current controller config from the state flow
        return _controller.value
    }

    fun saveJoystickMappings(mappings: List<JoystickMapping>) {
        val prefs = context.getSharedPreferences(PREFS_JOYSTICK_MAPPINGS, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        mappings.forEach { mapping ->
            val obj = JSONObject().apply {
                put("displayName", mapping.displayName)
                put("topic", mapping.topic)
                put("type", mapping.type)
                put("axisX", mapping.axisX)
                put("axisY", mapping.axisY)
                put("max", mapping.max)
                put("step", mapping.step)
                put("deadzone", mapping.deadzone)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("mappings", jsonArray.toString()).apply()
    }

    fun loadJoystickMappings(): MutableList<JoystickMapping> {
        val prefs = context.getSharedPreferences(PREFS_JOYSTICK_MAPPINGS, Context.MODE_PRIVATE)
        val jsonString = prefs.getString("mappings", null)
        val list = mutableListOf<JoystickMapping>()
        if (!jsonString.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        JoystickMapping(
                            displayName = obj.optString("displayName", "Joystick"),
                            topic = obj.optString("topic", null) as RosId?,
                            type = obj.optString("type", null),
                            axisX = obj.optInt("axisX", 0),
                            axisY = obj.optInt("axisY", 1),
                            max = obj.optDouble("max", 1.0).toFloat(),
                            step = obj.optDouble("step", 0.2).toFloat(),
                            deadzone = obj.optDouble("deadzone", 0.1).toFloat()
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        if (list.isEmpty()) {
            list.add(JoystickMapping("Left Stick"))
            list.add(JoystickMapping("Right Stick"))
        }
        return list
    }

    fun saveJoystickAddressingMode(mode: String) {
        val prefs = context.getSharedPreferences(PREFS_JOYSTICK_MAPPINGS, Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_JOYSTICK_ADDRESSING, mode).apply()
    }

    fun loadJoystickAddressingMode(): String {
        val prefs = context.getSharedPreferences(PREFS_JOYSTICK_MAPPINGS, Context.MODE_PRIVATE)
        return prefs.getString(PREFS_JOYSTICK_ADDRESSING, "DIRECT") ?: "DIRECT"
    }

    fun saveJoystickPublishRate(rate: Int) {
        val prefs = context.getSharedPreferences(PREFS_JOYSTICK_MAPPINGS, Context.MODE_PRIVATE)
        prefs.edit().putInt(PREFS_JOYSTICK_RATE, rate).apply()
    }

    fun loadJoystickPublishRate(): Int {
        val prefs = context.getSharedPreferences(PREFS_JOYSTICK_MAPPINGS, Context.MODE_PRIVATE)
        return prefs.getInt(PREFS_JOYSTICK_RATE, 5)
    }

    // --- Controller Presets ---
    private val PREFS_CONTROLLER_PRESETS = "controller_presets"

    fun saveControllerPresets(presets: List<ControllerPreset>) {
        val prefs = context.getSharedPreferences(PREFS_CONTROLLER_PRESETS, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        presets.forEach { preset ->
            val obj = JSONObject().apply {
                put("name", preset.name)
                put("topic", preset.topic)
                put("abxy", JSONObject(preset.abxy as Map<*, *>))
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("presets", jsonArray.toString()).apply()
    }

    fun loadControllerPresets(): MutableList<ControllerPreset> {
        val prefs = context.getSharedPreferences(PREFS_CONTROLLER_PRESETS, Context.MODE_PRIVATE)
        val jsonString = prefs.getString("presets", null)
        val list = mutableListOf<ControllerPreset>()
        if (!jsonString.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val abxyMap = mutableMapOf<String, String>()
                    obj.optJSONObject("abxy")?.let { abxyObj ->
                        abxyObj.keys().forEach { key ->
                            abxyMap[key] = abxyObj.getString(key)
                        }
                    }
                    list.add(
                        ControllerPreset(
                            name = obj.optString("name", "Preset"),
                            topic = obj.optString("topic", "") as RosId?,
                            abxy = abxyMap
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        if (list.isEmpty()) {
            list.add(ControllerPreset("Default", "/cmd_vel" as RosId?, mapOf("A" to "", "B" to "", "X" to "", "Y" to "")))
        }
        return list
    }

    // --- Button Assignments ---
    private val PREFS_CONTROLLER_ASSIGN = "controller_button_assignments"

    fun saveButtonAssignments(assignments: Map<String, AppAction>) {
        val prefs = context.getSharedPreferences(PREFS_CONTROLLER_ASSIGN, Context.MODE_PRIVATE)
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
        prefs.edit().putString("assignments", arr.toString()).apply()
    }

    fun loadButtonAssignments(controllerButtons: List<String>): MutableMap<String, AppAction> {
        val prefs = context.getSharedPreferences(PREFS_CONTROLLER_ASSIGN, Context.MODE_PRIVATE)
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
                        map[btn] = AppAction(
                            id = btn, // Use button name as id for assignments
                            displayName = name,
                            topic = topic,
                            type = type,
                            source = source,
                            msg = msg
                        )
                    }
                }
            } catch (_: Exception) {}
        }
        return map
    }

    // --- App Actions ---
    fun getControllerButtonList(): List<String> = listOf(
        "Button A", "Button B", "Button X", "Button Y",
        "L1", "R1", "L2", "R2", "Start", "Select"
    )

    fun loadAvailableAppActions(): List<AppAction> {
        val actions = mutableListOf<AppAction>()

        // Slider actions
        val sliderPrefs = context.getSharedPreferences("slider_buttons_prefs", Context.MODE_PRIVATE)
        val sliderJson = sliderPrefs.getString("saved_slider_buttons", null)
        if (!sliderJson.isNullOrEmpty()) {
            try {
                val arr = JSONArray(sliderJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("name", "")
                    val topic = obj.optString("topic", "")
                    val type = obj.optString("type", "")
                    val min = obj.optDouble("min", 0.0).toFloat()
                    val max = obj.optDouble("max", 1.0).toFloat()
                    val step = obj.optDouble("step", 0.1).toFloat()
                    val value = obj.optDouble("value", 0.0).toFloat()
                    actions.add(AppAction(
                        id = "slider_inc_${name}_$topic",
                        displayName = "Increment $name",
                        topic = topic,
                        type = type,
                        source = "SliderIncrement",
                        msg = actions.size.toString()
                    ))
                    actions.add(AppAction(
                        id = "slider_dec_${name}_$topic",
                        displayName = "Decrement $name",
                        topic = topic,
                        type = type,
                        source = "SliderDecrement",
                        msg = actions.size.toString()
                    ))
                }
            } catch (_: Exception) {}
        }

        // Geometry actions
        val geoPrefs = context.getSharedPreferences("geometry_reusable_buttons", Context.MODE_PRIVATE)
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
                    actions.add(AppAction(
                        id = "geometry_${name}_$topic",
                        displayName = name,
                        topic = topic,
                        type = type,
                        source = "Geometry",
                        msg = msg
                    ))
                }
            } catch (_: Exception) {}
        }

        // Custom publisher actions
        try {
            val prefs = context.getSharedPreferences("custom_publishers_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("custom_publishers", null)
            if (!json.isNullOrEmpty()) {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("label", obj.optString("topic", ""))
                    val topic = obj.optString("topic", "")
                    val type = obj.optString("type", "")
                    val msg = obj.optString("msg", "")
                    actions.add(AppAction(
                        id = "stdmsg_${name}_$topic",
                        displayName = name,
                        topic = topic,
                        type = type,
                        source = "Standard Message",
                        msg = msg
                    ))
                }
            }
        } catch (_: Exception) {}

        // Imported app actions
        val importedPrefs = context.getSharedPreferences("imported_app_actions", Context.MODE_PRIVATE)
        val importedJson = importedPrefs.getString("imported_app_actions", null)
        if (!importedJson.isNullOrEmpty()) {
            try {
                val arr = JSONArray(importedJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("displayName") == "Cycle Presets") continue
                    actions.add(AppAction(
                        id = obj.optString("id", obj.optString("displayName", "") + "_" + obj.optString("topic", "")),
                        displayName = obj.optString("displayName", ""),
                        topic = obj.optString("topic", ""),
                        type = obj.optString("type", ""),
                        source = obj.optString("source", ""),
                        msg = obj.optString("msg", "")
                    ))
                }
            } catch (_: Exception) {}
        }

        // Cycle preset actions
        actions.add(AppAction(
            id = "cycle_presets_forward",
            displayName = "Cycle Presets Forwards",
            topic = "/ignore",
            type = "Set",
            source = "CyclePresetForward",
            msg = ""
        ))
        actions.add(AppAction(
            id = "cycle_presets_backward",
            displayName = "Cycle Presets Backwards",
            topic = "/ignore",
            type = "Set",
            source = "CyclePresetBackward",
            msg = ""
        ))

        return actions
    }

    // --- Export/Import Config ---
    fun exportConfigToStream(out: OutputStream) {
        val yaml = Yaml()
        val latestJoystickMappings = loadJoystickMappings()
        val latestControllerPresets = loadControllerPresets()
        val latestButtonAssignments = loadButtonAssignments(getControllerButtonList())
        val latestAppActions = loadAvailableAppActions().distinctBy { it.displayName }

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

    fun importConfigFromStream(inp: InputStream) {
        val yaml = Yaml()
        val configMap = yaml.load<Map<String, Any>>(InputStreamReader(inp))

        val joystickMappings = mutableListOf<JoystickMapping>()
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
                        topic = jm["topic"] as? String as RosId?,
                        type = jm["type"] as? String
                    )
                )
            }
        }
        saveJoystickMappings(joystickMappings)

        val controllerPresets = mutableListOf<ControllerPreset>()
        val cpList = configMap["controllerPresets"] as? List<*> ?: emptyList<Any>()
        for (cp in cpList) {
            if (cp is Map<*, *>) {
                controllerPresets.add(
                    ControllerPreset(
                        name = cp["name"] as? String ?: "",
                        topic = (cp["topic"] as? String ?: "") as RosId?,
                        abxy = (cp["abxy"] as? Map<String, String>) ?: mapOf("A" to "", "B" to "", "X" to "", "Y" to "")
                    )
                )
            }
        }
        saveControllerPresets(controllerPresets)

        val buttonAssignments = mutableMapOf<String, AppAction>()
        val baMap = configMap["buttonAssignments"] as? Map<*, *> ?: emptyMap<String, Any>()
        for ((key, value) in baMap) {
            if (key is String && value is Map<*, *>) {
                buttonAssignments[key] = AppAction(
                    id = key,
                    displayName = value["displayName"] as? String ?: "",
                    topic = value["topic"] as? String ?: "",
                    type = value["type"] as? String ?: "",
                    source = value["source"] as? String ?: "",
                    msg = value["msg"] as? String ?: ""
                )
            }
        }
        saveButtonAssignments(buttonAssignments)
        // AppActions import omitted for brevity
    }

    // --- Helpers ---
    fun keyCodeToButtonName(keyCode: Int): String? {
        return when (keyCode) {
            96 -> "Button A"
            97 -> "Button B"
            99 -> "Button X"
            100 -> "Button Y"
            19 -> "DPad Up"
            20 -> "DPad Down"
            21 -> "DPad Left"
            22 -> "DPad Right"
            102 -> "L1"
            103 -> "R1"
            104 -> "L2"
            105 -> "R2"
            108, 82 -> "Start"
            109, 4 -> "Select"
            else -> null
        }
    }

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
}