package com.examples.testros2jsbridge.core.ros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
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
            Logger.d("RosBridgeViewModel", "Publish called: topic=$topic, type=$type, rawJson=$rawJson")
            if (!isConnected()) {
                Logger.w("RosBridgeViewModel", "Cannot publish: Not connected.")
                return@launch
            }
            // Advertise if not already advertised
            val key = topic to type
            if (!advertisedTopics.contains(key)) {
                try {
                    val advertiseMsg = RosBridgeAdvertise(
                        op = "advertise",
                        topic = topic,
                        type = type
                    )
                    val advertiseJson = Json.encodeToString(RosBridgeAdvertise.serializer(), advertiseMsg)
                    rosbridgeClient.send(advertiseJson)
                    Logger.d("RosBridgeViewModel", "Advertised: topic=$topic, type=$type")
                    advertisedTopics.add(key)
                } catch (e: Exception) {
                    Logger.e("RosBridgeViewModel", "Error advertising topic: topic=$topic, type=$type", e)
                }
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
                Logger.e("RosBridgeViewModel", "Error publishing message: topic=$topic, type=$type, rawJson=$rawJson", e)
            }
        }
    }

    // --- Service Advertisement ---
    fun advertiseService(serviceName: String, serviceType: String) {
        val key = serviceName to serviceType
        if (advertisedServices.contains(key)) return
        viewModelScope.launch {
            try {
                val msg = buildJsonObject {
                    put("op", JsonPrimitive("advertise_service"))
                    put("id", JsonPrimitive("advertise_service_${System.currentTimeMillis()}"))
                    put("service", JsonPrimitive(serviceName))
                    put("type", JsonPrimitive(serviceType))
                }
                rosbridgeClient.send(msg.toString())
                Logger.d("RosBridgeViewModel", "Advertised service: $serviceName, type=$serviceType")
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
            if (!advertisedTopics.contains(topic to type)) {
                viewModelScope.launch {
                    try {
                        val advertiseMsg = RosBridgeAdvertise(
                            op = "advertise",
                            topic = topic,
                            type = type
                        )
                        val advertiseJson = Json.encodeToString(RosBridgeAdvertise.serializer(), advertiseMsg)
                        rosbridgeClient.send(advertiseJson)
                        Logger.d("RosBridgeViewModel", "Advertised action topic: $topic, type=$type")
                        advertisedTopics.add(topic to type)
                    } catch (e: Exception) {
                        Logger.e("RosBridgeViewModel", "Error advertising action topic", e)
                    }
                }
            }
        }
        // Advertise send_goal and get_result services
        advertiseService(names.sendGoalService, names.sendGoalType)
        advertiseService(names.getResultService, names.getResultType)
        advertisedActions.add(key)
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
                return@launch
            }
            try {
                val msg = buildJsonObject {
                    put("op", JsonPrimitive("call_service"))
                    put("id", JsonPrimitive(id))
                    put("service", JsonPrimitive(serviceName))
                    put("type", JsonPrimitive(serviceType))
                    put("args", Json.parseToJsonElement(requestJson))
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

    fun sendOrQueueActionGoal(
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

        sendOrQueueServiceRequest(names.sendGoalService, names.sendGoalType, sendGoalMsg.toString()) { response ->
            Logger.d("RosBridgeViewModel", "Sent action goal for $actionName. Response: $response")
            subscribeToActionStatus(actionName, actionType)
        }
    }

    private fun getActionResult(actionName: String, actionType: String, goalUuid: String, onResult: (String) -> Unit) {
        val names = getActionNames(actionName, actionType) ?: return
        val uuidBytes = uuidToByteArrayString(goalUuid)
        val getResultMsg = buildJsonObject {
            put("goal_id", buildJsonObject { put("uuid", Json.parseToJsonElement(uuidBytes)) })
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
        publishMessage(cancelTopic, cancelType, cancelMsgJson.toString())
    }

    private fun handleStatusUpdate(actionName: String, actionType: String, statusList: JsonArray) {
        val goalId = lastGoalIdMap[actionName] ?: return
        val currentStatus = statusList.firstOrNull {
            it.jsonObject["goal_info"]?.jsonObject?.get("goal_id")?.jsonObject?.get("uuid")?.jsonArray?.let { uuidArray ->
                val uuidString = uuidArray.map { it.jsonPrimitive.int }.joinToString(",", prefix = "[", postfix = "]")
                uuidToByteArrayString(goalId) == uuidString
            } ?: false
        }?.jsonObject?.get("status")?.jsonPrimitive?.int ?: -1

        val statusStr = when(currentStatus) {
            1 -> "PENDING"
            2 -> "ACTIVE"
            3 -> "SUCCEEDED"
            4 -> "ABORTED"
            5 -> "CANCELED"
            else -> "UNKNOWN"
        }

        lastGoalStatusMap[actionName] = statusStr

        if (statusStr in setOf("SUCCEEDED", "ABORTED", "CANCELED")) {
            val onResult = topicHandlers.remove("action_goal_${actionName}")
            if (onResult != null) {
                getActionResult(actionName, actionType, goalId, onResult)
            }

            if (pendingGoalMap.containsKey(actionName)) {
                val (nextType, nextUuid, nextGoal) = pendingGoalMap.remove(actionName)!!
                sendOrQueueActionGoal(actionName, nextType, nextGoal, nextUuid, onResult)
            }
        }
    }

    // Make isConnected a private member function
    private fun isConnected(): Boolean {
        return rosbridgeClient.isConnected()
    }

    private fun handleRosbridgeMessage(message: String) {
        try {
            val json = Json.parseToJsonElement(message).jsonObject
            when (json["op"]?.jsonPrimitive?.content) {
                "service_response" -> handleServiceResponse(json)
                "publish" -> {
                    val topic = json["topic"]?.jsonPrimitive?.content ?: return
                    if (topic.endsWith("/status")) {
                        val actionName = topic.removeSuffix("/status")
                        val actionType = advertisedActions.firstOrNull { it.first == actionName }?.second ?: return
                        val statusList = json["msg"]?.jsonObject?.get("status_list")?.jsonArray ?: return
                        handleStatusUpdate(actionName, actionType, statusList)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("RosBridgeViewModel", "Error handling rosbridge message", e)
        }
    }

    private fun handleServiceResponse(json: JsonObject) {
        val service = json["service"]?.jsonPrimitive?.content ?: return
        val id = json["id"]?.jsonPrimitive?.content ?: return
        val values = json["values"]?.toString() ?: "{}"

        val handler = topicHandlers.remove(id)
        if (handler != null) {
            handler(values)
        } else {
            Logger.w("RosBridgeViewModel", "No handler for service response: $service, id=$id")
        }

        if (pendingServiceRequestMap.containsKey(service)) {
            val (nextType, nextJson, nextCb) = pendingServiceRequestMap.remove(service)!!
            sendOrQueueServiceRequest(service, nextType, nextJson, nextCb)
        }
    }

    // --- Topic Advertisement (for use by other ViewModels) ---
    fun advertiseTopic(topic: String, type: String) {
        val key = topic to type
        if (advertisedTopics.contains(key)) return
        viewModelScope.launch {
            Logger.d("RosBridgeViewModel", "Advertise called: topic=${topic}, type=${type}")
            Logger.d("RosBridgeViewModel", "Current advertisedTopics: $advertisedTopics")
            try {
                val advertiseMsg = RosBridgeAdvertise(
                    op = "advertise",
                    topic = topic,
                    type = type
                )
                val advertiseJson = Json.encodeToString(RosBridgeAdvertise.serializer(), advertiseMsg)
                rosbridgeClient.send(advertiseJson)
                Logger.d("RosBridgeViewModel", "Advertised: topic=$topic, type=$type")
                advertisedTopics.add(key)
            } catch (e: Exception) {
                Logger.e("RosBridgeViewModel", "Error advertising topic", e)
            }
        }
    }
}
