package com.examples.testros2jsbridge.domain.usecase.controller

import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.JoystickMapping
import com.examples.testros2jsbridge.domain.repository.RosActionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

class HandleControllerInputUseCase @Inject constructor(
    private val rosActionRepository: RosActionRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun triggerAppAction(action: AppAction) {
        if (action.topic.isBlank() || action.type.isBlank()) return
        val msgJson = try {
            Json.parseToJsonElement(action.msg)
        } catch (e: Exception) {
            JsonObject(mapOf("data" to JsonPrimitive(action.msg)))
        }
        val typeLower = action.type.lowercase()
        scope.launch {
            when {
                typeLower.contains("/msg/") -> {
                    // Standard message publish
                    val jsonObject = kotlinx.serialization.json.buildJsonObject {
                        put("op", JsonPrimitive("publish"))
                        put("id", JsonPrimitive("publish_${action.topic.replace("/", "_")}_${System.currentTimeMillis()}"))
                        put("topic", JsonPrimitive(action.topic))
                        put("msg", msgJson)
                        put("latch", JsonPrimitive(false))
                    }
                    rosActionRepository.publishRaw(jsonObject.toString())
                }
                typeLower.contains("/srv/") -> {
                    // Service call
                    val jsonObject = kotlinx.serialization.json.buildJsonObject {
                        put("op", JsonPrimitive("call_service"))
                        put("id", JsonPrimitive("service_${action.topic.replace("/", "_")}_${System.currentTimeMillis()}"))
                        put("service", JsonPrimitive(action.topic))
                        put("args", msgJson)
                        put("type", JsonPrimitive(action.type))
                    }
                    rosActionRepository.publishRaw(jsonObject.toString())
                }
                typeLower.contains("/action/") -> {
                    // Action goal logic (send goal, subscribe feedback/status, handle result)
                    val actionName = action.topic
                    val actionType = action.type
                    val goalUuid = java.util.UUID.randomUUID().toString()
                    // Recursively map JsonElement to ActionFieldValue
                    //This functionality needs to be ported toi new domain model
                    rosActionRepository.sendOrQueueActionGoal(
                        actionName = actionName,
                        actionType = actionType,
                        goalFields = msgJson.jsonObject,
                        goalUuid = goalUuid,
                        onResult = null // Optionally handle result callback
                    )
                }
                else -> {
                    // Fallback to publish
                    val jsonObject = kotlinx.serialization.json.buildJsonObject {
                        put("op", JsonPrimitive("publish"))
                        put("id", JsonPrimitive("publish_${action.topic.replace("/", "_")}_${System.currentTimeMillis()}"))
                        put("topic", JsonPrimitive(action.topic))
                        put("msg", msgJson)
                        put("latch", JsonPrimitive(false))
                    }
                    rosActionRepository.publishRaw(jsonObject.toString())
                }
            }
        }
    }


    private val keyCodeToButtonMap: Map<Int, String> = mapOf(
        96 to "Button A",
        97 to "Button B",
        99 to "Button X",
        100 to "Button Y",
        102 to "L1",
        103 to "R1",
        104 to "L2",
        105 to "R2",
        108 to "Start",
        82 to "Select"
    )

    fun handleKeyEvent(keyCode: Int, assignments: Map<String, AppAction>): AppAction? {
        val buttonName = keyCodeToButtonMap[keyCode]
        return buttonName?.let { assignments[it] }
    }

    fun handleJoystickInput(
        x: Float,
        y: Float,
        mapping: JoystickMapping
    ): Pair<Float, Float> {
        val maxValue = mapping.max ?: 1.0f
        val stepValue = mapping.step ?: 0.2f
        val deadzoneValue = mapping.deadzone ?: 0.1f

        val quantX = Math.signum(x) * Math.ceil(((Math.abs(x) - deadzoneValue) / stepValue).toDouble()) + deadzoneValue
        val quantY = Math.signum(y) * Math.ceil(((Math.abs(y) - deadzoneValue) / stepValue).toDouble()) + deadzoneValue

        return clamped(quantX, maxValue) to clamped(quantY, maxValue)
    }

    private fun clamped(value: Double, maxValue: Float): Float {
        val maxVal = maxValue.toDouble()    
        return Math.min(Math.max(value, -maxVal), maxVal).toFloat()
    }
}