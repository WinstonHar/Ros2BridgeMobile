package com.examples.testros2jsbridge.core.ros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.core.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.UUID
import javax.inject.Inject

/**
 * Central ViewModel for all ROS2/rosbridge communication (messages, services, actions).
 * Ported from legacy RosViewModel for exact compatibility.
 */
 @HiltViewModel
class RosBridgeViewModel @Inject constructor(
    private val rosbridgeClient: RosbridgeClient
) : ViewModel() {
    // --- Helper Data Classes for Rosbridge Operations ---
    @Serializable
    data class RosBridgeTopic(val op: String, val id: String? = null, val topic: String, val type: String? = null)

    @Serializable
    data class RosBridgeServiceCall(
        val op: String,
        val id: String,
        val service: String,
        val type: String,
        val args: JsonElement? = null
    )

    @Serializable
    data class RosBridgeAdvertise(
        val op: String,
        val id: String = "advertise_${System.currentTimeMillis()}",
        val topic: String,
        val type: String,
        val latch: Boolean? = false,
        val queue_size: Int? = 100
    )

    @Serializable
    data class StdString(val data: String)

    private val topicHandlers = mutableMapOf<String, (String) -> Unit>()

    private fun uuidToByteArrayString(uuid: String): String {
        return try {
            val u = UUID.fromString(uuid)
            val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
            bb.putLong(u.mostSignificantBits)
            bb.putLong(u.leastSignificantBits)
            bb.array().joinToString(",", prefix = "[", postfix = "]") { it.toUByte().toString() }
        } catch (e: Exception) {
            Logger.w("RosBridgeViewModel", "Failed to parse UUID '$uuid'. Defaulting to zero array.", e)
            (0..15).joinToString(",", prefix = "[", postfix = "]") { "0" }
        }
    }

    data class RosActionNames(
        val feedbackTopic: String,
        val statusTopic: String,
        val cancelTopic: String,
        val sendGoalService: String,
        val getResultService: String,
        val feedbackType: String,
        val sendGoalType: String,
        val getResultType: String
    )

    private fun getActionNames(actionName: String, actionType: String): RosActionNames? {
        val parts = actionType.split('/')
        if (parts.size < 3) return null
        val pkg = parts[0]
        val actionNameBase = parts[2]
        return RosActionNames(
            feedbackTopic = "$actionName/feedback",
            statusTopic = "$actionName/status",
            cancelTopic = "$actionName/cancel",
            sendGoalService = "$actionName/_action/send_goal",
            getResultService = "$actionName/_action/get_result",
            feedbackType = "$pkg/action/${actionNameBase}_Feedback",
            sendGoalType = "$pkg/action/${actionNameBase}_SendGoal",
            getResultType = "$pkg/action/${actionNameBase}_GetResult"
        )
    }

    // --- Message Publishing ---
    fun publishMessage(topic: String, type: String, rawJson: String) {
        viewModelScope.launch {
            if (!isConnected()) {
                Logger.w("RosBridgeViewModel", "Cannot publish: Not connected.")
                return@launch
            }
            try {
                val msg = buildJsonObject {
                    put("op", JsonPrimitive("publish"))
                    put("topic", JsonPrimitive(topic))
                    put("msg", Json.parseToJsonElement(rawJson))
                }
                rosbridgeClient.send(msg.toString())
                Logger.d("RosBridgeViewModel", "Published: topic=$topic, type=$type, msg=$rawJson")
            } catch (e: Exception) {
                Logger.e("RosBridgeViewModel", "Error publishing message", e)
            }
        }
    }

    // --- Service Call (with queuing) ---
    private val lastServiceRequestIdMap = mutableMapOf<String, String>()
    private val lastServiceBusyMap = mutableMapOf<String, Boolean>()
    private val pendingServiceRequestMap = mutableMapOf<String, Triple<String, String, (String) -> Unit>>()

    fun callService(serviceName: String, serviceType: String, requestJson: String, onResult: (String) -> Unit) {
        val busy = lastServiceBusyMap[serviceName] ?: false
        if (busy) {
            pendingServiceRequestMap[serviceName] = Triple(serviceType, requestJson, onResult)
            Logger.d("RosBridgeViewModel", "Service $serviceName busy, queuing new request.")
            return
        }
        lastServiceBusyMap[serviceName] = true
        val id = "service_call_${System.currentTimeMillis()}"
        lastServiceRequestIdMap[serviceName] = id
        topicHandlers[id] = { response ->
            lastServiceBusyMap[serviceName] = false
            onResult(response)
            if (pendingServiceRequestMap.containsKey(serviceName)) {
                val (nextType, nextJson, nextCb) = pendingServiceRequestMap.remove(serviceName)!!
                callService(serviceName, nextType, nextJson, nextCb)
            }
        }
        viewModelScope.launch {
            if (!isConnected()) {
                Logger.w("RosBridgeViewModel", "Cannot call service: Not connected.")
                return@launch
            }
            try {
                val msg = buildJsonObject {
                    put("op", JsonPrimitive("call_service"))
                    put("service", JsonPrimitive(serviceName))
                    put("args", Json.parseToJsonElement(requestJson))
                    put("id", JsonPrimitive(id))
                }
                rosbridgeClient.send(msg.toString())
                Logger.d("RosBridgeViewModel", "ServiceCall: service=$serviceName, type=$serviceType, req=$requestJson")
            } catch (e: Exception) {
                Logger.e("RosBridgeViewModel", "Error calling service", e)
            }
            delay(10000L)
            if (lastServiceBusyMap[serviceName] == true && lastServiceRequestIdMap[serviceName] == id) {
                lastServiceBusyMap[serviceName] = false
                Logger.w("RosBridgeViewModel", "Service call timeout for $serviceName")
            }
        }
    }

    // --- Action Goal (send_goal, get_result, cancel) ---
    private val lastGoalIdMap = mutableMapOf<String, String>()
    private val lastGoalStatusMap = mutableMapOf<String, String>()
    private val pendingGoalMap = mutableMapOf<String, Triple<String, String, JsonObject>>()

    fun sendActionGoal(
        actionName: String,
        actionType: String,
        goalFields: JsonObject,
        goalUuid: String = UUID.randomUUID().toString(),
        onResult: ((String) -> Unit)? = null
    ) {
        val prevGoalId = lastGoalIdMap[actionName]
        val prevGoalStatus = lastGoalStatusMap[actionName]
        if (prevGoalId != null && prevGoalStatus != null && prevGoalStatus !in setOf("SUCCEEDED", "CANCELED", "ABORTED", "REJECTED")) {
            cancelActionGoal(actionName, actionType, prevGoalId)
            pendingGoalMap[actionName] = Triple(actionType, goalUuid, goalFields)
            return
        }
        lastGoalIdMap[actionName] = goalUuid
        lastGoalStatusMap[actionName] = "PENDING"
        val names = getActionNames(actionName, actionType) ?: return
        val uuidBytes = uuidToByteArrayString(goalUuid)
        val sendGoalMsg = buildJsonObject {
            put("goal_id", buildJsonObject { put("uuid", Json.parseToJsonElement(uuidBytes)) })
            put("goal", goalFields)
        }
        val id = "action_goal_${System.currentTimeMillis()}"
        if (onResult != null) {
            topicHandlers[id] = onResult
        }
        viewModelScope.launch {
            if (!isConnected()) {
                Logger.w("RosBridgeViewModel", "Cannot send action goal: Not connected.")
                return@launch
            }
            try {
                val msg = buildJsonObject {
                    put("op", JsonPrimitive("call_service"))
                    put("service", JsonPrimitive(names.sendGoalService))
                    put("args", sendGoalMsg)
                    put("id", JsonPrimitive(id))
                }
                rosbridgeClient.send(msg.toString())
                Logger.d("RosBridgeViewModel", "SendActionGoal: action=$actionName, type=$actionType, uuid=$goalUuid, fields=$goalFields")
            } catch (e: Exception) {
                Logger.e("RosBridgeViewModel", "Error sending action goal", e)
            }
        }
    }

    private fun cancelActionGoal(actionName: String, actionType: String, uuid: String) {
        val names = getActionNames(actionName, actionType) ?: return
        val cancelTopic = names.cancelTopic
        val cancelType = "action_msgs/msg/GoalInfo"
        val now = System.currentTimeMillis()
        val sec = (now / 1000).toInt()
        val nanosec = ((now % 1000) * 1_000_000).toInt()
        val uuidBytes = uuidToByteArrayString(uuid)
        val cancelMsgJson = buildJsonObject {
            put("stamp", buildJsonObject {
                put("sec", sec)
                put("nanosec", nanosec)
            })
            put("goal_id", buildJsonObject {
                put("uuid", Json.parseToJsonElement(uuidBytes))
            })
        }
        viewModelScope.launch {
            if (!isConnected()) {
                Logger.w("RosBridgeViewModel", "Cannot cancel action goal: Not connected.")
                return@launch
            }
            try {
                val msg = buildJsonObject {
                    put("op", JsonPrimitive("publish"))
                    put("topic", JsonPrimitive(cancelTopic))
                    put("msg", cancelMsgJson)
                }
                rosbridgeClient.send(msg.toString())
                Logger.d("RosBridgeViewModel", "CancelActionGoal: topic=$cancelTopic, type=$cancelType, msg=$cancelMsgJson")
            } catch (e: Exception) {
                Logger.e("RosBridgeViewModel", "Error cancelling action goal", e)
            }
        }
    }

    // Make isConnected a private member function
    private fun isConnected(): Boolean {
        return try {
            val webSocketField = rosbridgeClient.javaClass.getDeclaredField("webSocket")
            webSocketField.isAccessible = true
            webSocketField.get(rosbridgeClient) != null
        } catch (e: Exception) {
            false
        }
    }
}
