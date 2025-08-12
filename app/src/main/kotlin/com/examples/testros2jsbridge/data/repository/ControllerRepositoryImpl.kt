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
import kotlin.apply

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
        // Load all configs, update or add this one, then save the full list
        val configs = loadControllerConfigs().toMutableList()
        val idx = configs.indexOfFirst { it.name == controller.name }
        if (idx >= 0) {
            configs[idx] = controller
        } else {
            configs.add(controller)
        }
        saveControllerConfigs(configs)
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
                val assignmentsObj = JSONObject()
                preset.buttonAssignments.forEach { (btn, action) ->
                    assignmentsObj.put(btn, action.displayName)
                }
                put("buttonAssignments", assignmentsObj)
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
                    val assignmentsMap = mutableMapOf<String, AppAction>()
                    obj.optJSONObject("buttonAssignments")?.let { assignmentsObj ->
                        assignmentsObj.keys().forEach { key ->
                            val displayName = assignmentsObj.getString(key)
                            // Only displayName is saved, so create a minimal AppAction
                            assignmentsMap[key] = AppAction(
                                id = key,
                                displayName = displayName,
                                topic = "",
                                type = "",
                                source = "",
                                msg = ""
                            )
                        }
                    }
                    list.add(
                        ControllerPreset(
                            name = obj.optString("name", "Preset"),
                            topic = obj.optString("topic", "") as RosId?,
                            buttonAssignments = assignmentsMap
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        if (list.isEmpty()) {
            list.add(ControllerPreset("Default", RosId("/cmd_vel"), emptyMap()))
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
                    "buttonAssignments" to cp.buttonAssignments
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
                val assignmentsRaw = cp["buttonAssignments"] as? Map<String, Any> ?: emptyMap()
                val assignmentsMap = assignmentsRaw.mapValues { (key, value) ->
                    when (value) {
                        is Map<*, *> -> AppAction(
                            id = key,
                            displayName = value["displayName"] as? String ?: key,
                            topic = value["topic"] as? String ?: "",
                            type = value["type"] as? String ?: "",
                            source = value["source"] as? String ?: "",
                            msg = value["msg"] as? String ?: ""
                        )
                        is String -> AppAction(
                            id = key,
                            displayName = value,
                            topic = "",
                            type = "",
                            source = "",
                            msg = ""
                        )
                        else -> AppAction(
                            id = key,
                            displayName = key,
                            topic = "",
                            type = "",
                            source = "",
                            msg = ""
                        )
                    }
                }
                controllerPresets.add(
                    ControllerPreset(
                        name = cp["name"] as? String ?: "",
                        topic = (cp["topic"] as? String ?: "") as RosId?,
                        buttonAssignments = assignmentsMap
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
        
        // --- Import AppActions ---
        val appActionsList = configMap["appActions"] as? List<*> ?: emptyList<Any>()
        val appActionsJsonArr = JSONArray()
        for (aa in appActionsList) {
            if (aa is Map<*, *>) {
                val displayName = aa["displayName"] as? String ?: ""
                val topic = aa["topic"] as? String ?: ""
                val type = aa["type"] as? String ?: ""
                val source = aa["source"] as? String ?: ""
                val msg = aa["msg"] as? String ?: ""
                val id = (aa["id"] as? String) ?: (displayName + "_" + topic)
                val obj = JSONObject().apply {
                    put("id", id)
                    put("displayName", displayName)
                    put("topic", topic)
                    put("type", type)
                    put("source", source)
                    put("msg", msg)
                }
                appActionsJsonArr.put(obj)
            }
        }
        // Save to SharedPreferences (imported_app_actions)
        val importedPrefs = context.getSharedPreferences("imported_app_actions", Context.MODE_PRIVATE)
        importedPrefs.edit().putString("imported_app_actions", appActionsJsonArr.toString()).apply()
    }

    // --- Controller Configs ---
    private val PREFS_CONTROLLER_CONFIGS = "controller_configs"

    fun saveControllerConfigs(configs: List<ControllerConfig>) {
        val prefs = context.getSharedPreferences(PREFS_CONTROLLER_CONFIGS, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        configs.forEach { config ->
            val obj = JSONObject().apply {
                put("name", config.name)
                put("addressingMode", config.addressingMode.value)
                put("sensitivity", config.sensitivity)
                put("invertYAxis", config.invertYAxis)
                put("deadZone", config.deadZone)
                put("customProfileName", config.customProfileName)
                put("joystickPublishRate", config.joystickPublishRate)

                // Serialize buttonAssignments
                val assignmentsObj = JSONObject()
                config.buttonAssignments.forEach { (btn, action) ->
                    val actionObj = JSONObject().apply {
                        put("id", action.id)
                        put("displayName", action.displayName)
                        put("topic", action.topic)
                        put("type", action.type)
                        put("source", action.source)
                        put("msg", action.msg)
                    }
                    assignmentsObj.put(btn, actionObj)
                }
                put("buttonAssignments", assignmentsObj)

                // Serialize joystickMappings
                val mappingsArr = org.json.JSONArray()
                config.joystickMappings.forEach { mapping ->
                    val mappingObj = JSONObject().apply {
                        put("displayName", mapping.displayName)
                        put("topic", mapping.topic?.value)
                        put("type", mapping.type)
                        put("axisX", mapping.axisX)
                        put("axisY", mapping.axisY)
                        put("max", mapping.max)
                        put("step", mapping.step)
                        put("deadzone", mapping.deadzone)
                    }
                    mappingsArr.put(mappingObj)
                }
                put("joystickMappings", mappingsArr)

                // Serialize controllerPresets
                val presetsArr = org.json.JSONArray()
                config.controllerPresets.forEach { preset ->
                    val presetObj = JSONObject().apply {
                        put("name", preset.name)
                        put("topic", preset.topic?.value)
                        // Preset buttonAssignments
                        val presetAssignmentsObj = JSONObject()
                        preset.buttonAssignments.forEach { (btn, action) ->
                            val actionObj = JSONObject().apply {
                                put("id", action.id)
                                put("displayName", action.displayName)
                                put("topic", action.topic)
                                put("type", action.type)
                                put("source", action.source)
                                put("msg", action.msg)
                            }
                            presetAssignmentsObj.put(btn, actionObj)
                        }
                        put("buttonAssignments", presetAssignmentsObj)
                        // Preset joystickMappings
                        val presetMappingsArr = org.json.JSONArray()
                        preset.joystickMappings.forEach { mapping ->
                            val mappingObj = JSONObject().apply {
                                put("displayName", mapping.displayName)
                                put("topic", mapping.topic?.value)
                                put("type", mapping.type)
                                put("axisX", mapping.axisX)
                                put("axisY", mapping.axisY)
                                put("max", mapping.max)
                                put("step", mapping.step)
                                put("deadzone", mapping.deadzone)
                            }
                            presetMappingsArr.put(mappingObj)
                        }
                        put("joystickMappings", presetMappingsArr)
                    }
                    presetsArr.put(presetObj)
                }
                put("controllerPresets", presetsArr)

                // buttonPresets (simple string map)
                val buttonPresetsObj = JSONObject()
                config.buttonPresets.forEach { (k, v) -> buttonPresetsObj.put(k, v) }
                put("buttonPresets", buttonPresetsObj)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("configs", jsonArray.toString()).apply()
    }

    fun loadControllerConfigs(): MutableList<ControllerConfig> {
        android.util.Log.d("ControllerRepositoryImpl", "Loading controller configs from SharedPreferences")
        val debugList = mutableListOf<String>()
        val prefs = context.getSharedPreferences(PREFS_CONTROLLER_CONFIGS, Context.MODE_PRIVATE)
        val jsonString = prefs.getString("configs", null)
        android.util.Log.d("ControllerRepositoryImpl", "Raw JSON from SharedPreferences: $jsonString")
        val list = mutableListOf<ControllerConfig>()
        if (!jsonString.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    // Deserialize buttonAssignments
                    val assignmentsMap = mutableMapOf<String, com.examples.testros2jsbridge.domain.model.AppAction>()
                    obj.optJSONObject("buttonAssignments")?.let { assignmentsObj ->
                        val keys = assignmentsObj.keys()
                        while (keys.hasNext()) {
                            val btn = keys.next()
                            val actionObj = assignmentsObj.getJSONObject(btn)
                            val action = com.examples.testros2jsbridge.domain.model.AppAction(
                                id = actionObj.optString("id"),
                                displayName = actionObj.optString("displayName"),
                                topic = actionObj.optString("topic"),
                                type = actionObj.optString("type"),
                                source = actionObj.optString("source"),
                                msg = actionObj.optString("msg")
                            )
                            assignmentsMap[btn] = action
                        }
                    }
                    // Deserialize joystickMappings
                    val mappingsList = mutableListOf<com.examples.testros2jsbridge.domain.model.JoystickMapping>()
                    obj.optJSONArray("joystickMappings")?.let { mappingsArr ->
                        for (j in 0 until mappingsArr.length()) {
                            val mappingObj = mappingsArr.getJSONObject(j)
                            val mapping = com.examples.testros2jsbridge.domain.model.JoystickMapping(
                                displayName = mappingObj.optString("displayName"),
                                topic = mappingObj.optString("topic")?.let { t -> if (t.isNotBlank()) com.examples.testros2jsbridge.domain.model.RosId(t) else null },
                                type = mappingObj.optString("type"),
                                axisX = mappingObj.optInt("axisX", 0),
                                axisY = mappingObj.optInt("axisY", 1),
                                max = if (mappingObj.has("max") && !mappingObj.isNull("max")) mappingObj.optDouble("max").toFloat() else null,
                                step = if (mappingObj.has("step") && !mappingObj.isNull("step")) mappingObj.optDouble("step").toFloat() else null,
                                deadzone = if (mappingObj.has("deadzone") && !mappingObj.isNull("deadzone")) mappingObj.optDouble("deadzone").toFloat() else null
                            )
                            mappingsList.add(mapping)
                        }
                    }
                    // Deserialize controllerPresets
                    val presetsList = mutableListOf<com.examples.testros2jsbridge.domain.model.ControllerPreset>()
                    obj.optJSONArray("controllerPresets")?.let { presetsArr ->
                        for (j in 0 until presetsArr.length()) {
                            val presetObj = presetsArr.getJSONObject(j)
                            val presetAssignmentsMap = mutableMapOf<String, com.examples.testros2jsbridge.domain.model.AppAction>()
                            presetObj.optJSONObject("buttonAssignments")?.let { presetAssignmentsObj ->
                                val keys = presetAssignmentsObj.keys()
                                while (keys.hasNext()) {
                                    val btn = keys.next()
                                    val actionObj = presetAssignmentsObj.getJSONObject(btn)
                                    val action = com.examples.testros2jsbridge.domain.model.AppAction(
                                        id = actionObj.optString("id"),
                                        displayName = actionObj.optString("displayName"),
                                        topic = actionObj.optString("topic"),
                                        type = actionObj.optString("type"),
                                        source = actionObj.optString("source"),
                                        msg = actionObj.optString("msg")
                                    )
                                    presetAssignmentsMap[btn] = action
                                }
                            }
                            val presetMappingsList = mutableListOf<com.examples.testros2jsbridge.domain.model.JoystickMapping>()
                            presetObj.optJSONArray("joystickMappings")?.let { presetMappingsArr ->
                                for (k in 0 until presetMappingsArr.length()) {
                                    val mappingObj = presetMappingsArr.getJSONObject(k)
                                    val mapping = com.examples.testros2jsbridge.domain.model.JoystickMapping(
                                        displayName = mappingObj.optString("displayName"),
                                        topic = mappingObj.optString("topic")?.let { t -> if (t.isNotBlank()) com.examples.testros2jsbridge.domain.model.RosId(t) else null },
                                        type = mappingObj.optString("type"),
                                        axisX = mappingObj.optInt("axisX", 0),
                                        axisY = mappingObj.optInt("axisY", 1),
                                        max = if (mappingObj.has("max") && !mappingObj.isNull("max")) mappingObj.optDouble("max").toFloat() else null,
                                        step = if (mappingObj.has("step") && !mappingObj.isNull("step")) mappingObj.optDouble("step").toFloat() else null,
                                        deadzone = if (mappingObj.has("deadzone") && !mappingObj.isNull("deadzone")) mappingObj.optDouble("deadzone").toFloat() else null
                                    )
                                    presetMappingsList.add(mapping)
                                }
                            }
                            val preset = com.examples.testros2jsbridge.domain.model.ControllerPreset(
                                name = presetObj.optString("name", "Preset"),
                                topic = presetObj.optString("topic")?.let { t -> if (t.isNotBlank()) com.examples.testros2jsbridge.domain.model.RosId(t) else null },
                                buttonAssignments = presetAssignmentsMap,
                                joystickMappings = presetMappingsList
                            )
                            presetsList.add(preset)
                        }
                    }
                    // buttonPresets
                    val buttonPresetsMap = mutableMapOf<String, String>()
                    obj.optJSONObject("buttonPresets")?.let { buttonPresetsObj ->
                        val keys = buttonPresetsObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            buttonPresetsMap[k] = buttonPresetsObj.optString(k)
                        }
                    }
                    val loadedConfig = ControllerConfig(
                        name = obj.optString("name", "Unnamed Config"),
                        addressingMode = obj.optString("addressingMode", "DIRECT").let { com.examples.testros2jsbridge.domain.model.RosId(it) },
                        sensitivity = obj.optDouble("sensitivity", 1.0).toFloat(),
                        buttonPresets = buttonPresetsMap,
                        invertYAxis = obj.optBoolean("invertYAxis", false),
                        deadZone = obj.optDouble("deadZone", 0.05).toFloat(),
                        customProfileName = if (obj.has("customProfileName") && !obj.isNull("customProfileName")) obj.optString("customProfileName") else null,
                        joystickMappings = mappingsList,
                        controllerPresets = presetsList,
                        buttonAssignments = assignmentsMap,
                        joystickPublishRate = obj.optInt("joystickPublishRate", 5)
                    )
                    debugList.add("Loaded config: ${loadedConfig.name}, assignments: ${loadedConfig.buttonAssignments}, mappings: ${loadedConfig.joystickMappings}")
                    list.add(loadedConfig)
                }
                android.util.Log.d("ControllerRepositoryImpl", debugList.joinToString("\n"))
            } catch (_: Exception) {}
        }
        return list
    }
}