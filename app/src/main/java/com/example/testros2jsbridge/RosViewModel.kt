package com.example.testros2jsbridge

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.intOrNull
import java.util.UUID
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import android.util.Log
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.testros2jsbridge.RosbridgeConnectionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/*
    RosViewModel manages ROS2 connection state, message publishing, topic advertisement, and UI state for the app.
    Acts as the main ViewModel for all ROS2-related operations, including message history and dropdown selection.
*/

class RosViewModel(application: Application) : AndroidViewModel(application), RosbridgeConnectionManager.Listener {
    // --- Persistent set of currently subscribed topics (topic to type) ---
    private val _subscribedTopics = MutableStateFlow<Set<Pair<String, String>>>(emptySet())
    val subscribedTopics: StateFlow<Set<Pair<String, String>>> = _subscribedTopics.asStateFlow()

    /**
     * Add a topic subscription (topic, type). Call this from the subscriber activity when user checks a topic.
     * This will subscribe and keep the topic in the persistent set.
     */
    fun addSubscribedTopic(topic: String, type: String) {
        val newSet = _subscribedTopics.value + (topic to type)
        _subscribedTopics.value = newSet
        // Subscribe immediately with a handler that appends to log
        subscribeToTopic(topic, type) { msg -> appendToMessageHistory(msg) }
    }

    /**
     * Remove a topic subscription (topic, type). Call this from the subscriber activity when user unchecks a topic.
     * This will remove the topic from the persistent set. (Unsubscribe logic can be added if needed)
     */
    fun removeSubscribedTopic(topic: String, type: String) {
        val newSet = _subscribedTopics.value - (topic to type)
        _subscribedTopics.value = newSet
=        val unsubscribeJson = """
            {"op": "unsubscribe", "topic": "$topic"}
        """.trimIndent()
        RosbridgeConnectionManager.sendRaw(unsubscribeJson)
        Log.d("RosViewModel", "Unsubscribed from topic: $topic ($type)")
    }

    /**
     * (Re-)subscribe to all topics in the persistent set, with a handler that appends to the log.
     * Call this from MainActivity after connecting or resuming.
     */
    fun resubscribeAllTopicsToLog() {
        for ((topic, type) in _subscribedTopics.value) {
            subscribeToTopic(topic, type) { msg -> appendToMessageHistory(msg) }
        }
    }
    /**
     * Appends a message to the custom message history (for Compose log view).
     * Call this from other activities to display received messages in the Compose log.
     */
    fun appendToMessageHistory(msg: String) {
        // Truncate each log line to 300 characters to prevent UI lag
        val truncated = if (msg.length > 300) msg.take(300) + "... [truncated]" else msg
        _customMessageHistory.update { currentHistory -> (currentHistory + truncated).takeLast(25) }
    }
    // --- Robust Service Request Management ---
    private val lastServiceRequestIdMap = mutableMapOf<String, String>() // serviceName -> last request id
    private val lastServiceBusyMap = mutableMapOf<String, Boolean>() // serviceName -> busy
    private val pendingServiceRequestMap = mutableMapOf<String, Triple<String, String, (String) -> Unit>>() // serviceName -> (serviceType, requestJson, onResult)

    /**
     * Force clear the busy lock and queue for all services. This allows new service requests to be sent even if any previous one is stuck.
     */
    fun forceClearAllServiceBusyLocks() {
        lastServiceRequestIdMap.clear()
        lastServiceBusyMap.clear()
        pendingServiceRequestMap.clear()
    }

    /**
     * Robustly send or queue a service request. If a previous request is active, queue the new one until the previous is done.
     * @param serviceName The ROS2 service name (e.g. /my_service)
     * @param serviceType The ROS2 service type (e.g. my_pkg/srv/MyService)
     * @param requestJson The request arguments as a JSON string (e.g. {"a": 1, "b": 2})
     * @param onResult Callback with the result JSON string
     */
    fun sendOrQueueServiceRequest(
        serviceName: String,
        serviceType: String,
        requestJson: String,
        onResult: (String) -> Unit
    ) {
        val busy = lastServiceBusyMap[serviceName] ?: false
        if (busy) {
            // Queue the request
            pendingServiceRequestMap[serviceName] = Triple(serviceType, requestJson, onResult)
            Log.d("RosViewModel", "sendOrQueueServiceRequest: Service $serviceName busy, queuing new request.")
            return
        }
        // Mark as busy
        lastServiceBusyMap[serviceName] = true
        val id = "service_call_${System.currentTimeMillis()}"
        lastServiceRequestIdMap[serviceName] = id
        // Register a one-time handler for the service response
        topicHandlers[id] = { response ->
            lastServiceBusyMap[serviceName] = false
            onResult(response)
            // If a pending request exists, send it now
            if (pendingServiceRequestMap.containsKey(serviceName)) {
                val (nextType, nextJson, nextCallback) = pendingServiceRequestMap.remove(serviceName)!!
                sendOrQueueServiceRequest(serviceName, nextType, nextJson, nextCallback)
            }
        }
        val jsonString = """
            {"op": "call_service", "id": "$id", "service": "$serviceName", "type": "$serviceType", "args": $requestJson}
        """.trimIndent()
        RosbridgeConnectionManager.sendRaw(jsonString)
        Log.d("RosViewModel", "Sent robust service call to '$serviceName': $requestJson")
        // Timeout fallback: if no response in 10s, clear busy and try next
        viewModelScope.launch {
            kotlinx.coroutines.delay(10000L)
            if (lastServiceBusyMap[serviceName] == true && lastServiceRequestIdMap[serviceName] == id) {
                Log.w("RosViewModel", "[SERVICE] Timeout waiting for response for $serviceName, clearing busy lock.")
                lastServiceBusyMap[serviceName] = false
                // If a pending request exists, send it now
                if (pendingServiceRequestMap.containsKey(serviceName)) {
                    val (nextType, nextJson, nextCallback) = pendingServiceRequestMap.remove(serviceName)!!
                    sendOrQueueServiceRequest(serviceName, nextType, nextJson, nextCallback)
                }
            }
        }
    }
    /**
     * Force clear the busy lock and queue for all actions. This allows new actions to be sent even if any previous one is stuck.
     * Also clears lastGoalIdMap and lastGoalStatusMap to fully reset action state.
     */
    fun forceClearAllActionBusyLocks() {
        lastGoalIdMap.clear()
        lastGoalStatusMap.clear()
        pendingGoalMap.clear()
    }
    
    /**
     * Request the result of an action goal via the get_result service.
     * @param actionName The base action name, e.g. /pose_server/run_trajectory
     * @param actionType The action type, e.g. ryan_msgs/action/RunPose
     * @param goalUuid The UUID of the goal as a string
     * @param onResult Callback with the result JSON string
     */
    fun getActionResultViaService(
        actionName: String,
        actionType: String,
        goalUuid: String,
        onResult: (String) -> Unit
    ) {
        val parts = actionType.split('/')
        val pkg = parts[0]
        val actionNameBase = parts[2]
        val getResultService = "$actionName/_action/get_result"
        val getResultType = "$pkg/action/${actionNameBase}_GetResult"
        // Convert UUID string to 16-byte array for unique_identifier_msgs/UUID
        val uuidBytes = try {
            val u = UUID.fromString(goalUuid)
            val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
            bb.putLong(u.mostSignificantBits)
            bb.putLong(u.leastSignificantBits)
            bb.array().joinToString(",", prefix = "[", postfix = "]") { it.toUByte().toString() }
        } catch (e: Exception) {
            (0..15).joinToString(",", prefix = "[", postfix = "]") { "0" }
        }
        val getResultRequest = buildJsonObject {
            put("goal_id", buildJsonObject { put("uuid", Json.parseToJsonElement(uuidBytes)) })
        }
        // Register a one-time handler for the service response
        val id = "service_call_${System.currentTimeMillis()}"
        topicHandlers[id] = onResult
        val jsonString = """
            {"op": "call_service", "id": "$id", "service": "$getResultService", "type": "$getResultType", "args": $getResultRequest}
        """.trimIndent()
        RosbridgeConnectionManager.sendRaw(jsonString)
        Log.d("RosViewModel", "Sent get_result service call to '$getResultService': $getResultRequest")
    }

    /**
     * Main entry point: Robustly send an action goal. If a previous goal is active, cancel and queue the new goal until the previous is done.
     * Use this method from UI or other logic to send action goals.
     * @param actionName The base action name, e.g. /pose_server/run_trajectory
     * @param actionType The action type, e.g. ryan_msgs/action/RunPose
     * @param goalFields The goal fields as a JsonObject (e.g. {"names": [...]})
     * @param goalUuid The goal UUID as a string (optional, will be generated if not provided)
     */

    // Internal: Send an action goal using the ROS2 action protocol (SendGoal service call via rosbridge)
    private fun sendActionGoalViaService(actionName: String, actionType: String, goalFields: JsonObject, uuid: String): String? {
        Log.d("RosViewModel", "sendActionGoalViaService called with actionName=$actionName, actionType=$actionType, uuid=$uuid, goalFields=$goalFields")
        if (!RosbridgeConnectionManager.isConnected) {
            Log.w("RosViewModel", "Cannot send action goal: Not connected.")
            return null
        }
        try {
            val parts = actionType.split('/')
            val pkg = parts[0]
            val actionNameBase = parts[2]
            val sendGoalService = "$actionName/_action/send_goal"
            val sendGoalType = "$pkg/action/${actionNameBase}_SendGoal"
            // Convert UUID string to 16-byte array for unique_identifier_msgs/UUID
            val uuidBytes = try {
                val u = UUID.fromString(uuid)
                val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
                bb.putLong(u.mostSignificantBits)
                bb.putLong(u.leastSignificantBits)
                bb.array().joinToString(",", prefix = "[", postfix = "]") { it.toUByte().toString() }
            } catch (e: Exception) {
                (0..15).joinToString(",", prefix = "[", postfix = "]") { "0" }
            }
            val sendGoalRequest = buildJsonObject {
                put("goal_id", buildJsonObject { put("uuid", Json.parseToJsonElement(uuidBytes)) })
                put("goal", goalFields)
            }
            Log.d("RosViewModel", "Calling callCustomService for SendGoal: service=$sendGoalService, type=$sendGoalType, request=$sendGoalRequest")
            callCustomService(sendGoalService, sendGoalType, sendGoalRequest.toString())
            Log.d("RosViewModel", "Sent SendGoal service call to '$sendGoalService': $sendGoalRequest")
            return uuid
        } catch (e: Exception) {
            Log.e("RosViewModel", "Failed to construct or send action goal (service)", e)
            return null
        }
    }
    // Per-topic action goal management for robust cancel/send
    private val lastGoalIdMap = mutableMapOf<String, String>() // topic -> last goal UUID
    private val lastGoalStatusMap = mutableMapOf<String, String>() // topic -> last goal status
    private val pendingGoalMap = mutableMapOf<String, Triple<String, String, JsonObject>>() // topic -> (actionName, actionType, goalFields)
    private val actionStatusFlows = mutableMapOf<String, MutableSharedFlow<Map<String, String>>>() // topic -> status flow

    /**
     * Robust: Call this with explicit goal UUID. If previous goal exists, cancel and wait for terminal state before sending new goal.
     */
    /**
     * Send or queue an action goal. When the goal reaches a terminal state, call getActionResultViaService and invoke the provided onResult callback.
     */
    fun sendOrQueueActionGoal(
        actionName: String,
        actionType: String,
        goalFields: JsonObject,
        goalUuid: String = UUID.randomUUID().toString(),
        onResult: ((String) -> Unit)? = null
    ) {
        val prevGoalId = lastGoalIdMap[actionName]
        val prevGoalStatus = lastGoalStatusMap[actionName]
        val handleTerminal: (String) -> Unit = { resultJson ->
            Log.d("RosViewModel", "[ACTION] handleTerminal called for $actionName, result: $resultJson")
            val jsonElem = kotlinx.serialization.json.Json.parseToJsonElement(resultJson)
            val obj = jsonElem.jsonObject
            val statusCode = obj["values"]!!.jsonObject["status"]!!.jsonPrimitive.intOrNull ?: -1
            val statusString = when (statusCode) {
                2 -> "ACTIVE"
                3 -> "PREEMPTED"
                4 -> "SUCCEEDED"
                5 -> "ABORTED"
                6 -> "REJECTED"
                7 -> "PREEMPTING"
                8 -> "RECALLING"
                9 -> "RECALLED"
                10 -> "LOST"
                0 -> "PENDING"
                1 -> "CANCELED"
                else -> "UNKNOWN"
            }
            lastGoalStatusMap[actionName] = statusString
            onResult?.invoke(resultJson)
        }
        if (prevGoalId != null && prevGoalStatus != null && prevGoalStatus != "SUCCEEDED" && prevGoalStatus != "CANCELED" && prevGoalStatus != "ABORTED" && prevGoalStatus != "REJECTED") {
            Log.d("RosViewModel", "sendOrQueueActionGoal: Previous goal active, sending cancel for prevGoalId=$prevGoalId and queuing new goal.")
            pendingGoalMap[actionName] = Triple(actionName, actionType, goalFields)
            cancelActionGoal(actionName, actionType, prevGoalId)
        } else {
            Log.d("RosViewModel", "sendOrQueueActionGoal: No active goal, sending new goal immediately.")
            val newGoalId = sendActionGoalViaService(actionName, actionType, goalFields, goalUuid)
            lastGoalIdMap[actionName] = newGoalId ?: goalUuid
            lastGoalStatusMap[actionName] = "PENDING"
            var terminalHandled = false
            val timeoutMillis = 10000L // 10 seconds
            val statusHandler: (String) -> Unit = { statusMsg ->
                val status = extractStatusForGoal(statusMsg, lastGoalIdMap[actionName])
                Log.d("RosViewModel", "[ACTION] Status update for $actionName: $status (msg: $statusMsg)")
                if (!terminalHandled && (status == "SUCCEEDED" || status == "CANCELED" || status == "ABORTED" || status == "REJECTED")) {
                    terminalHandled = true
                    Log.d("RosViewModel", "[ACTION] Terminal state reached for $actionName: $status, fetching result.")
                    getActionResultViaService(actionName, actionType, lastGoalIdMap[actionName] ?: "") { resultJson ->
                        handleTerminal(resultJson)
                    }
                }
                handleStatusUpdate(statusMsg, actionName, actionType)
            }
            subscribeToActionStatus(actionName, statusHandler)
            // Timeout fallback: if no terminal state in 10s, fetch result anyway
            viewModelScope.launch {
                kotlinx.coroutines.delay(timeoutMillis)
                if (!terminalHandled) {
                    terminalHandled = true
                    Log.w("RosViewModel", "[ACTION] Timeout waiting for terminal state for $actionName, fetching result anyway.")
                    getActionResultViaService(actionName, actionType, lastGoalIdMap[actionName] ?: "") { resultJson ->
                        handleTerminal(resultJson)
                    }
                    // Mark as TIMEOUT to clear all busy locks and pending goals globally
                    forceClearAllActionBusyLocks()
                }
            }
        }
    }

    // Handles status updates and sends pending goal if previous is done/canceled
    private fun handleStatusUpdate(statusMsg: String, actionName: String, actionType: String) {
        Log.d("RosViewModel", "handleStatusUpdate called for actionName=$actionName, statusMsg=$statusMsg")
        val lastGoalId = lastGoalIdMap[actionName]
        Log.d("RosViewModel", "handleStatusUpdate: lastGoalId=$lastGoalId")
        val status = extractStatusForGoal(statusMsg, lastGoalId)
        Log.d("RosViewModel", "handleStatusUpdate: extracted status for goalId=$lastGoalId is $status")
        lastGoalStatusMap[actionName] = status ?: "UNKNOWN"
        // Emit to flow for coroutine-based waiting
        if (status != null) {
            val flow = actionStatusFlows.getOrPut(actionName) { MutableSharedFlow(extraBufferCapacity = 8) }
            flow.tryEmit(mapOf("goal_id" to (lastGoalId ?: ""), "status" to status))
        }
        if ((status == "CANCELED" || status == "SUCCEEDED" || status == "ABORTED" || status == "REJECTED") && pendingGoalMap.containsKey(actionName)) {
            Log.d("RosViewModel", "handleStatusUpdate: Terminal state ($status) reached for $actionName, sending pending goal if any.")
            val (nextActionName, nextActionType, nextGoalFields) = pendingGoalMap[actionName]!!
            val newGoalUuid = UUID.randomUUID().toString()
            val newGoalId = sendActionGoalViaService(nextActionName, nextActionType, nextGoalFields, newGoalUuid)
            lastGoalIdMap[actionName] = newGoalId ?: newGoalUuid
            lastGoalStatusMap[actionName] = "PENDING"
            pendingGoalMap.remove(actionName)
            subscribeToActionStatus(nextActionName) { statusMsg2 ->
                handleStatusUpdate(statusMsg2, nextActionName, nextActionType)
            }
        }
    }

    /**
     * Returns a SharedFlow of status updates for a given action topic, for coroutine-based waiting.
     */
    fun actionStatusFlow(actionName: String): SharedFlow<Map<String, String>> {
        return actionStatusFlows.getOrPut(actionName) { MutableSharedFlow(extraBufferCapacity = 8) }
    }

    // Extracts the status for a specific goalId from the GoalStatusArray message
    private fun extractStatusForGoal(statusMsg: String, goalId: String?): String? {
        if (goalId == null) return null
        try {
            val jsonElem = Json.parseToJsonElement(statusMsg)
            val statusArr = jsonElem.jsonObject["status_list"]?.jsonArray ?: return null
            for (statusEntry in statusArr) {
                val entryObj = statusEntry.jsonObject
                val entryGoalId = entryObj["goal_info"]?.jsonObject?.get("goal_id")?.jsonObject?.get("uuid")?.toString()
                // UUID is a byte array, so compare as string
                if (entryGoalId != null && goalId == entryGoalId) {
                    val statusCode = entryObj["status"]?.jsonPrimitive?.intOrNull
                    // Map status code to string
                    return when (statusCode) {
                        2 -> "ACTIVE"
                        3 -> "PREEMPTED"
                        4 -> "SUCCEEDED"
                        5 -> "ABORTED"
                        6 -> "REJECTED"
                        7 -> "PREEMPTING"
                        8 -> "RECALLING"
                        9 -> "RECALLED"
                        10 -> "LOST"
                        0 -> "PENDING"
                        1 -> "CANCELED"
                        else -> null
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    // Callbacks for connection events (to be set by Fragment/Activity)
    var onRosbridgeDisconnected: (() -> Unit)? = null
    // Map to store topic handlers for message routing
    private val topicHandlers = mutableMapOf<String, (String) -> Unit>()
    /**
     * Advertise a ROS2 service via rosbridge_server.
     * @param serviceName The ROS2 service name (e.g. /my_service)
     * @param serviceType The ROS2 service type (e.g. my_pkg/srv/MyService)
     */
    fun advertiseService(serviceName: String, serviceType: String) {
        viewModelScope.launch {
            if (!RosbridgeConnectionManager.isConnected) {
                Log.w("RosViewModel", "Cannot advertise service: Not connected.")
                return@launch
            }
            try {
                val id = "advertise_service_${System.currentTimeMillis()}"
                val jsonString = """
                    {"op": "advertise_service", "id": "$id", "service": "$serviceName", "type": "$serviceType"}
                """.trimIndent()
                RosbridgeConnectionManager.sendRaw(jsonString)
                Log.d("RosViewModel", "Sent advertise_service: $jsonString")
            } catch (e: Exception) {
                Log.e("RosViewModel", "advertiseService error for service '$serviceName'", e)
            }
        }
    }

    /**
     * Call a ROS2 service via rosbridge_server.
     * @param serviceName The ROS2 service name (e.g. /my_service)
     * @param serviceType The ROS2 service type (e.g. my_pkg/srv/MyService)
     * @param requestJson The request arguments as a JSON string (e.g. {"a": 1, "b": 2})
     */
    /**
     * Robustly call a ROS2 service via rosbridge_server, using the robust queue/busy logic.
     * @param serviceName The ROS2 service name (e.g. /my_service)
     * @param serviceType The ROS2 service type (e.g. my_pkg/srv/MyService)
     * @param requestJson The request arguments as a JSON string (e.g. {"a": 1, "b": 2})
     * @param onResult Optional callback with the result JSON string
     */
    fun callCustomService(serviceName: String, serviceType: String, requestJson: String, onResult: ((String) -> Unit)? = null) {
        Log.d("RosViewModel", "callCustomService called: serviceName=$serviceName, serviceType=$serviceType, requestJson=$requestJson")
        if (onResult != null) {
            sendOrQueueServiceRequest(serviceName, serviceType, requestJson, onResult)
        } else {
            // Fallback: fire and forget (legacy usage)
            viewModelScope.launch {
                if (!RosbridgeConnectionManager.isConnected) {
                    Log.w("RosViewModel", "Cannot call service: Not connected.")
                    return@launch
                }
                try {
                    val id = "service_call_${System.currentTimeMillis()}"
                    val jsonString = """
                        {"op": "call_service", "id": "$id", "service": "$serviceName", "type": "$serviceType", "args": $requestJson}
                    """.trimIndent()
                    RosbridgeConnectionManager.sendRaw(jsonString)
                    Log.d("RosViewModel", "Sent call_service: $jsonString")
                } catch (e: Exception) {
                    Log.e("RosViewModel", "callCustomService error for service '$serviceName'", e)
                }
            }
        }
    }

    // Removed sendActionGoalClassic overloads. Use sendOrQueueActionGoal as the main entry point for sending action goals.

    fun subscribeToActionFeedback(actionName: String, actionType: String, onMessage: (String) -> Unit) {
        val feedbackTopic = "$actionName/feedback"
        val parts = actionType.split('/')
        val pkg = parts[0]
        val actionNameBase = parts[2]
        val feedbackType = "$pkg/action/${actionNameBase}_Feedback"
        subscribeToTopic(feedbackTopic, feedbackType, onMessage)
    }

    fun subscribeToActionStatus(actionName: String, onMessage: (String) -> Unit) {
        val statusTopic = "$actionName/status"
        val statusType = "action_msgs/action/GoalStatusArray"
        subscribeToTopic(statusTopic, statusType, onMessage)
    }

    // Removed subscribeToActionResult. Use getActionResultViaService for result retrieval.

    fun cancelActionGoal(actionName: String, actionType: String, uuid: String) {
        val cancelTopic = "$actionName/cancel"
        val cancelType = "action_msgs/msg/GoalInfo"
        advertiseTopic(cancelTopic, cancelType)
        val now = System.currentTimeMillis()
        val sec = (now / 1000).toInt()
        val nanosec = ((now % 1000) * 1_000_000).toInt()
        // Convert UUID string to 16-byte array for unique_identifier_msgs/UUID
        val uuidBytes = try {
            val u = UUID.fromString(uuid)
            val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
            bb.putLong(u.mostSignificantBits)
            bb.putLong(u.leastSignificantBits)
            bb.array().joinToString(",", prefix = "[", postfix = "]") { it.toUByte().toString() }
        } catch (e: Exception) {
            (0..15).joinToString(",", prefix = "[", postfix = "]") { "0" }
        }
        val cancelMsg = """
            {"stamp": {"sec": $sec, "nanosec": $nanosec}, "goal_id": {"uuid": $uuidBytes}}
        """.trimIndent()
        publishCustomRawMessage(cancelTopic, cancelType, cancelMsg)
        Log.d("RosViewModel", "Sent cancel to $cancelTopic: $cancelMsg")
    }

    // Helper to subscribe to a topic and route messages
    fun subscribeToTopic(topic: String, type: String, onMessage: (String) -> Unit) {
        val id = "subscribe_${topic.replace("/", "_")}_${System.currentTimeMillis()}"
        val jsonString = """
            {"op": "subscribe", "id": "$id", "topic": "$topic", "type": "$type"}
        """.trimIndent()
        RosbridgeConnectionManager.sendRaw(jsonString)
        topicHandlers[topic] = onMessage
    }

    /*
        input:    topicName - String, messageType - String, rawJson - String
        output:   None
        remarks:  Publishes a raw JSON message to the given topic and type, with special handling for std_msgs/String and std_msgs/Bool.
    */
    fun publishCustomRawMessage(topicName: String, messageType: String, rawJson: String) {
        viewModelScope.launch {
            if (!RosbridgeConnectionManager.isConnected) {
                Log.w("RosViewModel", "Cannot publish: Not connected.")
                return@launch
            }
            try {
                val msgField = when {
                    messageType == "std_msgs/msg/String" -> buildJsonObject { put("data", rawJson) }
                    messageType == "std_msgs/msg/Bool" -> {
                        val asJson = try { Json.parseToJsonElement(rawJson) } catch (_: Exception) { null }
                        val boolVal = if (asJson is kotlinx.serialization.json.JsonObject && asJson["data"] != null) {
                            val dataVal = asJson["data"]
                            if (dataVal is kotlinx.serialization.json.JsonPrimitive) dataVal.booleanOrNull ?: false else false
                        } else false
                        buildJsonObject { put("data", boolVal) }
                    }
                    else -> Json.parseToJsonElement(rawJson)
                }
                val jsonObject = buildJsonObject {
                    put("op", "publish")
                    put("id", "publish_${topicName.replace("/", "_")}_${System.currentTimeMillis()}")
                    put("topic", topicName)
                    put("msg", msgField)
                    put("latch", false)
                }
                val jsonString = jsonObject.toString()
                RosbridgeConnectionManager.sendRaw(jsonString)
                Log.d("RosViewModel", "Sent Publish to '$topicName': $jsonString")
                _customMessageHistory.update { currentHistory -> (currentHistory + jsonString).takeLast(25) }
            } catch (e: Exception) {
                Log.e("RosViewModel", "Publish error for topic '$topicName' (raw)", e)
            }
        }
    }

    data class CustomPublisher(val topic: String, val messageType: String, val message: String)

    private val _customPublishers = MutableStateFlow<List<CustomPublisher>>(emptyList())
    val customPublishers: StateFlow<List<CustomPublisher>> = _customPublishers

    /*
        input:    publisher - CustomPublisher
        output:   None
        remarks:  Adds a custom publisher to the list.
    */
    fun addCustomPublisher(publisher: CustomPublisher) {
        _customPublishers.value = _customPublishers.value + publisher
    }

    /*
        input:    list - List<CustomPublisher>
        output:   None
        remarks:  Sets the list of custom publishers.
    */
    fun setCustomPublishers(list: List<CustomPublisher>) {
        _customPublishers.value = list
    }

    private val _rosMessages = MutableSharedFlow<String>()
    val rosMessages: SharedFlow<String> = _rosMessages.asSharedFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _customMessage = MutableStateFlow("Default message")
    val customMessage: StateFlow<String> = _customMessage.asStateFlow()

    /*
        input:    None
        output:   None
        remarks:  Clears the current custom message text field.
    */
    fun clearCustomMessage() {
        _customMessage.value = ""
    }

    private val _customMessageHistory = MutableStateFlow<List<String>>(emptyList())
    val customMessageHistory: StateFlow<List<String>> = _customMessageHistory.asStateFlow()

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    data class RosBridgeAdvertise(
        val op: String = "advertise",
        val id: String = "advertise_${System.currentTimeMillis()}",
        val topic: String,
        val type: String,
        val latch: Boolean? = false,
        val queue_size: Int? = 100
    )

    @Serializable
    data class StdString(val data: String)

    /*
        input:    newMessage - String
        output:   None
        remarks:  Updates the custom message text field value.
    */
    fun onCustomMessageChange(newMessage: String) {
        _customMessage.value = newMessage
    }

    /*
        input:    ipAddress - String, port - Int (required)
        output:   None
        remarks:  Initiates connection to rosbridge and updates connection status. Port must be specified by the caller.
    */
    fun connect(ipAddress: String, port: Int) {
        _connectionStatus.value = "Connecting..."
        RosbridgeConnectionManager.addListener(this)
        RosbridgeConnectionManager.connect(ipAddress, port)
    }

    private val _selectedDropdownIndex = MutableStateFlow(0) // Default to first item
        val selectedDropdownIndex: StateFlow<Int> = _selectedDropdownIndex

    /*
        input:    index - Int
        output:   None
        remarks:  Sets the selected dropdown index for UI navigation.
    */
    fun selectDropdownIndex(index: Int) {
        _selectedDropdownIndex.value = index
    }

    /*
        input:    None
        output:   None
        remarks:  Disconnects from rosbridge and updates connection status.
    */
    fun disconnect() {
        RosbridgeConnectionManager.disconnect()
        _connectionStatus.value = "Disconnected (Client)"
        RosbridgeConnectionManager.removeListener(this)
    }

    /*
        input:    topicName - String, messageType - String
        output:   None
        remarks:  Advertises a topic with the given type to rosbridge.
    */
    fun advertiseTopic(topicName: String, messageType: String) {
        viewModelScope.launch {
            if (!RosbridgeConnectionManager.isConnected) {
                Log.w("RosViewModel", "Cannot advertise: Not connected.")
                return@launch
            }
            try {
                val advertiseMessage = RosBridgeAdvertise(topic = topicName, type = messageType)
                val jsonString = json.encodeToString(advertiseMessage)
                Log.d("RosViewModel", "Sent Advertise: $jsonString for topic '$topicName' type '$messageType'")
                RosbridgeConnectionManager.sendRaw(jsonString)
            } catch (e: Exception) {
                Log.e("RosViewModel", "Advertise error for topic '$topicName'", e)
            }
        }
    }

    /*
        input:    topicName - String, message - T, serializer - KSerializer<T>
        output:   None
        remarks:  Publishes a strongly-typed message to the given topic using the provided serializer.
    */
    private fun <T> publishMessage(topicName: String, message: T, serializer: KSerializer<T>) {
        viewModelScope.launch {
            if (!RosbridgeConnectionManager.isConnected) {
                Log.w("RosViewModel", "Cannot publish: Not connected.")
                return@launch
            }
            try {
                val jsonObject = buildJsonObject {
                    put("op", "publish")
                    put("id", "publish_${topicName.replace("/", "_")}_${System.currentTimeMillis()}")
                    put("topic", topicName)
                    put("msg", Json.encodeToJsonElement(serializer, message))
                    put("latch", false)
                }
                val jsonString = jsonObject.toString()
                RosbridgeConnectionManager.sendRaw(jsonString)
                Log.d("RosViewModel", "Sent Publish to '$topicName': $jsonString")
            } catch (e: Exception) {
                Log.e("RosViewModel", "Publish error for topic '$topicName'", e)
            }
        }
    }

    /*
        input:    topicName - String, text - String
        output:   None
        remarks:  Publishes a std_msgs/String message to the given topic.
    */
    private fun publishStringMessage(topicName: String, text: String) {
        publishMessage(topicName, StdString(text), StdString.serializer())
    }

    /*
        input:    topicName - String
        output:   None
        remarks:  Publishes the current custom message from the text field to the given topic, updating message history.
    */
    fun publishCustomMessageFromTextField(topicName: String) {
        val messageToSend = _customMessage.value
        // Try to parse as JSON, fallback to string
        val asJson = try {
            Json.parseToJsonElement(messageToSend)
        } catch (_: Exception) {
            null
        }
        // --- Update message history: store the JSON string of the sent message ---
        val jsonString = if (asJson != null) asJson.toString() else messageToSend
        _customMessageHistory.update { currentHistory -> (currentHistory + jsonString).takeLast(25) }
        // --- End message history update ---
        publishStringMessage(topicName, messageToSend)
    }

    /*
        input:    None
        output:   None
        remarks:  Called when the ViewModel is cleared; disconnects and removes listener.
    */
    override fun onCleared() {
        super.onCleared()
        Log.d("RosViewModel", "onCleared called, disconnecting if active.")
        disconnect()
        RosbridgeConnectionManager.removeListener(this)
    }

    // Data class for custom protocol actions
    data class CustomProtocolAction(
        val label: String,
        val proto: CustomProtocolsViewModel.ProtocolFile,
        val fieldValues: Map<String, String>
    )

    private val _customProtocolActions = MutableStateFlow<List<CustomProtocolAction>>(emptyList())
    val customProtocolActions: StateFlow<List<CustomProtocolAction>> = _customProtocolActions

    fun addCustomProtocolAction(action: CustomProtocolAction) {
        _customProtocolActions.value = _customProtocolActions.value + action
        saveCustomProtocolActionsToPrefs()
    }
    fun removeCustomProtocolAction(index: Int) {
        val list = _customProtocolActions.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _customProtocolActions.value = list
            saveCustomProtocolActionsToPrefs()
        }
    }
    fun setCustomProtocolActions(list: List<CustomProtocolAction>) {
        _customProtocolActions.value = list
        saveCustomProtocolActionsToPrefs()
    }

    // Persistence for custom protocol actions
    private val CUSTOM_PROTOCOL_ACTIONS_PREFS = "custom_protocol_actions"
    private fun saveCustomProtocolActionsToPrefs() {
        val prefs = getApplication<Application>().getSharedPreferences(CUSTOM_PROTOCOL_ACTIONS_PREFS, android.content.Context.MODE_PRIVATE)
        val arr = org.json.JSONArray()
        for (action in _customProtocolActions.value) {
            val obj = org.json.JSONObject()
            obj.put("label", action.label)
            obj.put("proto_name", action.proto.name)
            obj.put("proto_type", action.proto.type.name)
            obj.put("proto_importPath", action.proto.importPath)
            val fieldsObj = org.json.JSONObject()
            for ((k, v) in action.fieldValues) fieldsObj.put(k, v)
            obj.put("fields", fieldsObj)
            arr.put(obj)
        }
        prefs.edit().putString("custom_protocol_actions", arr.toString()).apply()
    }
    fun loadCustomProtocolActionsFromPrefs() {
        val prefs = getApplication<Application>().getSharedPreferences(CUSTOM_PROTOCOL_ACTIONS_PREFS, android.content.Context.MODE_PRIVATE)
        val json = prefs.getString("custom_protocol_actions", null)
        if (json.isNullOrBlank()) return
        try {
            val arr = org.json.JSONArray(json)
            val loaded = mutableListOf<CustomProtocolAction>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val label = obj.optString("label", "")
                val protoName = obj.optString("proto_name", "")
                val protoType = obj.optString("proto_type", "MSG")
                val protoImportPath = obj.optString("proto_importPath", "")
                val fieldsObj = obj.optJSONObject("fields") ?: org.json.JSONObject()
                val fields = mutableMapOf<String, String>()
                val keys = fieldsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    fields[k] = fieldsObj.optString(k, "")
                }
                val proto = CustomProtocolsViewModel.ProtocolFile(protoName, protoImportPath, CustomProtocolsViewModel.ProtocolType.valueOf(protoType))
                loaded.add(CustomProtocolAction(label, proto, fields))
            }
            _customProtocolActions.value = loaded
        } catch (_: Exception) { }
    }

    // RosbridgeConnectionManager.Listener implementation

    /*
        input:    None
        output:   None
        remarks:  Called when rosbridge connection is established; updates connection status.
    */
    override fun onConnected() {
        _connectionStatus.value = "Connected"
    }

    /*
        input:    None
        output:   None
        remarks:  Called when rosbridge connection is lost; updates connection status.
    */
    override fun onDisconnected() {
        _connectionStatus.value = "Disconnected"
        // Notify listeners (e.g., Fragment) to clear advertised sets
        onRosbridgeDisconnected?.invoke()
    }

    /*
        input:    text - String
        output:   None
        remarks:  Called when a message is received from rosbridge; emits to rosMessages flow.
    */
    override fun onMessage(text: String) {
        // Try to parse the message and route to the correct handler (topic or service id)
        try {
            val jsonElem = Json.parseToJsonElement(text)
            val obj = jsonElem.jsonObject
            val topic = obj["topic"]?.jsonPrimitive?.let { jp -> try { jp.content } catch (_: Exception) { null } }
            val id = obj["id"]?.jsonPrimitive?.let { jp -> try { jp.content } catch (_: Exception) { null } }
            var handled = false
            if (topic != null && topicHandlers.containsKey(topic)) {
                topicHandlers[topic]?.invoke(text)
                handled = true
            }
            if (!handled && id != null && topicHandlers.containsKey(id)) {
                topicHandlers[id]?.invoke(text)
                topicHandlers.remove(id) // one-time handler for service response
                handled = true
            }
            if (!handled) {
                viewModelScope.launch { _rosMessages.emit(text) }
            }
        } catch (e: Exception) {
            viewModelScope.launch { _rosMessages.emit(text) }
        }
    }

    /*
        input:    error - String
        output:   None
        remarks:  Called when an error occurs on rosbridge connection; updates connection status.
    */
    override fun onError(error: String) {
        _connectionStatus.value = "Error: $error"
    }
}