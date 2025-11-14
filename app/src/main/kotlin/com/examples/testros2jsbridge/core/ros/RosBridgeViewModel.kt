package com.examples.testros2jsbridge.core.ros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        rosbridgeClient.onMessageListener = {
            handleRosbridgeMessage(it)
        }
    }

    private val advertisedTopics: MutableSet<Pair<String, String>> = mutableSetOf()
    private val advertisedServices: MutableSet<Pair<String, String>> = mutableSetOf()
    private val advertisedActions: MutableSet<Pair<String, String>> = mutableSetOf()

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
    data class RosBridgeAdvertiseService(
        val op: String,
        val id: String? = null,
        val service: String,
        val type: String
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

    // --- Topic Advertisement ---
    fun advertiseTopic(topic: String, type: String) {
        val key = topic to type
        if (advertisedTopics.contains(key)) return

        viewModelScope.launch {
            if (!isConnected()) {
                Logger.w("RosBridgeViewModel", "Cannot advertise topic: Not connected.")
                return@launch
            }
            try {
                val advertiseMsg = RosBridgeAdvertise(
                    op = "advertise",
                    topic = topic,
                    type = type
                )
                val advertiseJson = json.encodeToString(advertiseMsg)
                rosbridgeClient.send(advertiseJson)
                Logger.d("RosBridgeViewModel", "Advertised: topic=$topic, type=$type")
                advertisedTopics.add(key)
            } catch (e: Exception) {
                Logger.e("RosBridgeViewModel", "Error advertising topic: topic=$topic, type=$type", e)
            }
        }
    }

    // --- Message Publishing ---
    fun publishMessage(topic: String, type: String, rawJson: String) {
        viewModelScope.launch {
            Logger.d("RosBridgeViewModel", "Publish called: topic=$topic, type=$type, rawJson=$rawJson")
            if (!isConnected()) {
                Logger.w("RosBridgeViewModel", "Cannot publish: Not connected.")
                return@launch
            }

            try {
                val parsedJson = json.parseToJsonElement(rawJson).jsonObject
                val actualType = parsedJson["type"]?.jsonPrimitive?.content ?: type
                val messagePayload = parsedJson["msg"]?.jsonObject ?: parsedJson

                if (actualType.contains("/action/")) {
                    advertiseAction(topic, actualType)
                    sendOrQueueActionGoal(topic, actualType, messagePayload)
                    return@launch
                }

                if (actualType.contains("/srv/")) {
                    sendOrQueueServiceRequest(topic, actualType, messagePayload.toString()) { result ->
                        Logger.d("RosBridgeViewModel", "Service response for '$topic': $result")
                    }
                    return@launch
                }

                // For regular topic publishing
                advertiseTopic(topic, actualType)

                val msg = buildJsonObject {
                    put("op", JsonPrimitive("publish"))
                    put("topic", JsonPrimitive(topic))
                    put("msg", messagePayload)
                }
                rosbridgeClient.send(msg.toString())
                Logger.d("RosBridgeViewModel", "Published: topic=$topic, type=$actualType, msg=$messagePayload")
            } catch (e: Exception) {
                Logger.e("RosBridgeViewModel", "Error publishing message: topic=$topic, type=$type, rawJson=$rawJson", e)
            }
        }
    }

    //not necessary?
    // --- Service Advertisement ---
    fun advertiseService(serviceName: String, serviceType: String) {
        val key = serviceName to serviceType
        if (advertisedServices.contains(key)) return
        viewModelScope.launch {
            try {
                val advertiseServiceMsg = RosBridgeAdvertiseService(
                    op = "advertise_service",
                    id = "advertise_service_${System.currentTimeMillis()}",
                    service = serviceName,
                    type = serviceType
                )
                val jsonMsg = json.encodeToString(advertiseServiceMsg)
                rosbridgeClient.send(jsonMsg)
                Logger.d("RosBridgeViewModel", "Advertised service: $serviceName, type=$serviceType, msg=$jsonMsg")
                advertisedServices.add(key)
            } catch (e: Exception) {
                Logger.e("RosBridgeViewModel", "Error advertising service", e)
            }
        }
    }


    // --- Action Advertisement ---
    fun advertiseAction(actionName: String, actionType: String) {
        val key = actionName to actionType
        if (advertisedActions.contains(key)) return
        val names = getActionNames(actionName, actionType) ?: return
        // Advertise feedback, status, cancel topics
        listOf(
            names.feedbackTopic to names.feedbackType,
            names.statusTopic to "action_msgs/msg/GoalStatusArray",
            names.cancelTopic to "action_msgs/msg/GoalInfo"
        ).forEach { (topic, type) ->
            advertiseTopic(topic, type)
        }
        // Advertise send_goal and get_result services
        advertiseService(names.sendGoalService, names.sendGoalType)
        advertiseService(names.getResultService, names.getResultType)
        advertisedActions.add(key)
        Logger.d("RosBridgeViewModel", "Advertised action: $actionName")
    }

    // --- Service Call (with queuing) ---
    private val lastServiceRequestIdMap = mutableMapOf<String, String>()
    private val lastServiceBusyMap = mutableMapOf<String, Boolean>()
    private val pendingServiceRequestMap = mutableMapOf<String, Triple<String, String, (String) -> Unit>>()

    fun sendOrQueueServiceRequest(serviceName: String, serviceType: String, requestJson: String, onResult: (String) -> Unit) {
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
                sendOrQueueServiceRequest(serviceName, nextType, nextJson, nextCb)
            }
        }
        viewModelScope.launch {
            if (!isConnected()) {
                Logger.w("RosBridgeViewModel", "Cannot call service: Not connected.")
                lastServiceBusyMap[serviceName] = false // Clear busy flag
                // Also check for pending requests
                if (pendingServiceRequestMap.containsKey(serviceName)) {
                     val (nextType, nextJson, nextCb) = pendingServiceRequestMap.remove(serviceName)!!
                     sendOrQueueServiceRequest(serviceName, nextType, nextJson, nextCb)
                }
                return@launch
            }
            try {
                val parsedJson = json.parseToJsonElement(requestJson).jsonObject
                val actualType = parsedJson["type"]?.jsonPrimitive?.content ?: serviceType
                val actualArgs = parsedJson["msg"]?.jsonObject ?: parsedJson

                val serviceCallMsg = RosBridgeServiceCall(
                    op = "call_service",
                    id = id,
                    service = serviceName,
                    type = actualType,
                    args = actualArgs
                )
                val jsonMsg = json.encodeToString(serviceCallMsg)
                rosbridgeClient.send(jsonMsg)
                Logger.d("RosBridgeViewModel", "ServiceCall: service=$serviceName, type=$actualType, msg=$jsonMsg")
            } catch (e: Exception) {
                Logger.e("RosBridgeViewModel", "Error calling service", e)
                lastServiceBusyMap[serviceName] = false // Clear busy flag on error
            }
            delay(10000L)
            if (lastServiceBusyMap[serviceName] == true && lastServiceRequestIdMap[serviceName] == id) {
                lastServiceBusyMap[serviceName] = false
                Logger.w("RosBridgeViewModel", "Service call timeout for $serviceName")
                 if (pendingServiceRequestMap.containsKey(serviceName)) {
                     val (nextType, nextJson, nextCb) = pendingServiceRequestMap.remove(serviceName)!!
                     sendOrQueueServiceRequest(serviceName, nextType, nextJson, nextCb)
                }
            }
        }
    }


    // --- Action Goal (send_goal, get_result, cancel) ---
    private data class PendingGoal(val actionType: String, val goalUuid: String, val goalFields: JsonObject, val onResult: ((String) -> Unit)?)
    private val actionResultHandlers = mutableMapOf<String, (String) -> Unit>()
    private val lastGoalIdMap = mutableMapOf<String, String>()
    private val lastGoalStatusMap = mutableMapOf<String, String>()
    private val pendingGoalMap = mutableMapOf<String, PendingGoal>()

    fun sendOrQueueActionGoal(
        actionName: String,
        actionType: String,
        goalFields: JsonObject,
        goalUuid: String = UUID.randomUUID().toString(),
        onResult: ((String) -> Unit)? = null
    ) {
        val prevGoalId = lastGoalIdMap[actionName]
        val prevGoalStatus = lastGoalStatusMap[actionName]
        if (prevGoalId != null && prevGoalStatus != null && prevGoalStatus !in setOf("SUCCEEDED", "CANCELED", "ABORTED", "REJECTED", "LOST")) {
            cancelActionGoal(actionName, actionType, prevGoalId)
            pendingGoalMap[actionName] = PendingGoal(actionType, goalUuid, goalFields, onResult)
            Logger.d("RosBridgeViewModel", "Previous goal active. Cancelling and queuing new goal for $actionName")
            return
        }

        lastGoalIdMap[actionName] = goalUuid
        lastGoalStatusMap[actionName] = "PENDING"
        if (onResult != null) {
            actionResultHandlers[goalUuid] = onResult
        }

        val names = getActionNames(actionName, actionType) ?: return
        val uuidBytes = uuidToByteArrayString(goalUuid)
        val sendGoalMsg = buildJsonObject {
            put("goal_id", buildJsonObject { put("uuid", json.parseToJsonElement(uuidBytes)) })
            put("goal", goalFields)
        }

        sendOrQueueServiceRequest(names.sendGoalService, names.sendGoalType, sendGoalMsg.toString()) { response ->
            Logger.d("RosBridgeViewModel", "Sent action goal for $actionName. Response: $response")
            subscribeToActionStatus(actionName, actionType)
        }
    }

    private fun getActionResult(actionName: String, actionType: String, goalUuid: String, onResult: (String) -> Unit) {
        val names = getActionNames(actionName, actionType) ?: return
        val uuidBytes = uuidToByteArrayString(goalUuid)
        val getResultMsg = buildJsonObject {
            put("goal_id", buildJsonObject { put("uuid", json.parseToJsonElement(uuidBytes)) })
        }
        sendOrQueueServiceRequest(names.getResultService, names.getResultType, getResultMsg.toString(), onResult)
    }

    private fun subscribeToActionStatus(actionName: String, actionType: String) {
        val names = getActionNames(actionName, actionType) ?: return
        val key = names.statusTopic to "action_msgs/msg/GoalStatusArray"
        if (advertisedTopics.contains(key)) return

        val msg = buildJsonObject {
            put("op", "subscribe")
            put("topic", names.statusTopic)
            put("type", "action_msgs/msg/GoalStatusArray")
        }
        rosbridgeClient.send(msg.toString())
        advertisedTopics.add(key)
        Logger.d("RosBridgeViewModel", "Subscribed to action status: $actionName")
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
                put("uuid", json.parseToJsonElement(uuidBytes))
            })
        }
        publishMessage(cancelTopic, cancelType, cancelMsgJson.toString())
        Logger.d("RosBridgeViewModel", "Sent cancel for action $actionName, goal $uuid")
    }

    private fun handleStatusUpdate(actionName: String, actionType: String, statusList: JsonArray) {
        val goalId = lastGoalIdMap[actionName] ?: return

        val goalStatusJson = statusList.firstOrNull {
             it.jsonObject["goal_info"]?.jsonObject?.get("goal_id")?.jsonObject?.get("uuid")?.toString() == uuidToByteArrayString(goalId)
        }

        if (goalStatusJson == null) {
            Logger.w("RosBridgeViewModel", "No status found for goalId $goalId in status message.")
            return
        }

        val statusCode = goalStatusJson.jsonObject["status"]?.jsonPrimitive?.intOrNull ?: -1

        val statusStr = when(statusCode) {
            0 -> "PENDING"
            1 -> "CANCELED"
            2 -> "ACTIVE"
            3 -> "PREEMPTED"
            4 -> "SUCCEEDED"
            5 -> "ABORTED"
            6 -> "REJECTED"
            7 -> "PREEMPTING"
            8 -> "RECALLING"
            9 -> "RECALLED"
            10 -> "LOST"
            else -> "UNKNOWN"
        }

        lastGoalStatusMap[actionName] = statusStr
        Logger.d("RosBridgeViewModel", "Action '$actionName' status updated to $statusStr")


        if (statusStr in setOf("SUCCEEDED", "ABORTED", "CANCELED", "REJECTED", "LOST")) {
            val onResult = actionResultHandlers.remove(goalId)
            if (onResult != null) {
                getActionResult(actionName, actionType, goalId, onResult)
            }

            if (pendingGoalMap.containsKey(actionName)) {
                val pending = pendingGoalMap.remove(actionName)!!
                Logger.d("RosBridgeViewModel", "Executing pending goal for action '$actionName'")
                sendOrQueueActionGoal(actionName, pending.actionType, pending.goalFields, pending.goalUuid, pending.onResult)
            }
        }
    }

    private fun isConnected(): Boolean {
        return rosbridgeClient.isConnected()
    }

    private fun handleRosbridgeMessage(message: String) {
        try {
            val jsonObj = json.parseToJsonElement(message).jsonObject
            when (jsonObj["op"]?.jsonPrimitive?.content) {
                "service_response" -> {
                    handleServiceResponse(jsonObj)
                }
                "publish" -> {
                    val topic = jsonObj["topic"]?.jsonPrimitive?.content ?: return
                    if (topic.endsWith("/status")) {
                        val actionName = topic.removeSuffix("/status")
                        val actionType = advertisedActions.firstOrNull { it.first == actionName }?.second
                        if (actionType != null) {
                            val statusList = jsonObj["msg"]?.jsonObject?.get("status_list")?.jsonArray
                            if (statusList != null) {
                                handleStatusUpdate(actionName, actionType, statusList)
                            }
                        }
                    }
                }
                // Handle other op types if needed
            }
        } catch (e: Exception) {
            Logger.e("RosBridgeViewModel", "Error handling rosbridge message: $message", e)
        }
    }

    private fun handleServiceResponse(jsonObj: JsonObject) {
        val id = jsonObj["id"]?.jsonPrimitive?.contentOrNull ?: return
        val handler = topicHandlers.remove(id)
        if (handler != null) {
            handler.invoke(jsonObj.toString())
            Logger.d("RosBridgeViewModel", "Handled service response for id $id")
        } else {
            Logger.w("RosBridgeViewModel", "No handler found for service response id $id")
        }
    }
}
