package com.example.testros2jsbridge

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.UUID
import android.util.Log
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.io.OutputStream
import androidx.core.content.edit

/* 
RosViewModel is the main Android ViewModel for managing ROS2 communication and state in the app. 
Handles subscribing, publishing, and service/action calls via rosbridge, including robust queuing and status tracking. 
Provides helper data classes and functions for ROS2 message serialization, UUID conversion, and action/service topic management. 
Maintains UI state flows for connection status, custom messages, publishers, protocol actions, and message history. 
Integrates with RosbridgeConnectionManager for network operations and implements listener callbacks for connection events and incoming messages. 
*/

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
data class RosBridgeUnsubscribe(
    val op: String = "unsubscribe",
    val topic: String
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

/*
    input:    uuid - String
    output:   String (byte array string for ROS UUID)
    remarks:  Converts a UUID string to a ROS-compatible byte array string representation.
*/
private fun uuidToByteArrayString(uuid: String): String {
    return try {
        val u = UUID.fromString(uuid)
        val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
        bb.putLong(u.mostSignificantBits)
        bb.putLong(u.leastSignificantBits)
        bb.array().joinToString(",", prefix = "[", postfix = "]") { it.toUByte().toString() }
    } catch (e: Exception) {
        Log.w("RosViewModel", "Failed to parse UUID '$uuid'. Defaulting to zero array.", e)
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

/*
    input:    actionName - String, actionType - String
    output:   RosActionNames? (null if invalid type)
    remarks:  Parses action type and name to generate all related ROS action topic/type names.
*/
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

/*
    input:    logContext - String, operation - () -> String
    output:   None
    remarks:  Extension function for RosViewModel to send a JSON string to rosbridge with logging and error handling.
*/
private fun RosViewModel.sendToBridge(logContext: String, operation: () -> String) {
    viewModelScope.launch {
        if (!RosbridgeConnectionManager.isConnected) {
            Log.w("RosViewModel", "Cannot perform '$logContext': Not connected.")
            return@launch
        }
        try {
            val jsonString = operation()
            RosbridgeConnectionManager.sendRaw(jsonString)
            Log.d("RosViewModel", "Sent $logContext: $jsonString")
        } catch (e: Exception) {
            Log.e("RosViewModel", "Error during '$logContext'", e)
        }
    }
}

class RosViewModel(application: Application) : AndroidViewModel(application), RosbridgeConnectionManager.Listener {
    init {
        RosbridgeConnectionManager.addListener(this)
    }
    val latestBitmap: MutableStateFlow<android.graphics.Bitmap?> = MutableStateFlow(null)
    private val _subscribedTopics = MutableStateFlow<Set<Pair<String, String>>>(emptySet())
    val subscribedTopics: StateFlow<Set<Pair<String, String>>> = _subscribedTopics.asStateFlow()

    /*
        input:    topic - String, type - String
        output:   None
        remarks:  Adds a topic subscription and subscribes immediately with a handler that appends to log.
    */
    fun addSubscribedTopic(topic: String, type: String) {
        val newSet = _subscribedTopics.value + (topic to type)
        _subscribedTopics.value = newSet
    }

    /*
        input:    topic - String, type - String
        output:   None
        remarks:  Removed a topic subscription and unsubscribes immediately with a handler that appends to log.
    */
    fun removeSubscribedTopic(topic: String, type: String) {
        val newSet = _subscribedTopics.value - (topic to type)
        _subscribedTopics.value = newSet
    }

    /*
        input:    msg - String
        output:   None
        remarks:  Appends a message to the custom message history for Compose log view.
    */
    fun appendToMessageHistory(msg: String) {
        val truncated = if (msg.length > 300) msg.take(300) + "... [truncated]" else msg
        _customMessageHistory.update { currentHistory -> (currentHistory + truncated).takeLast(25) }
    }

    private val lastServiceRequestIdMap = mutableMapOf<String, String>()
    private val lastServiceBusyMap = mutableMapOf<String, Boolean>()
    private val pendingServiceRequestMap = mutableMapOf<String, Triple<String, String, (String) -> Unit>>()

    /*
        input:    None
        output:   None
        remarks:  Force clears the busy lock and queue for all services.
    */
    fun forceClearAllServiceBusyLocks() {
        lastServiceRequestIdMap.clear()
        lastServiceBusyMap.clear()
        pendingServiceRequestMap.clear()
    }

    /*
        input:    serviceName - String, serviceType - String, requestJson - String, onResult - (String) -> Unit
        output:   None
        remarks:  Robustly sends or queues a service request, queues if previous is active.
    */
    private fun sendOrQueueServiceRequest(
        serviceName: String,
        serviceType: String,
        requestJson: String,
        onResult: (String) -> Unit
    ) {
        val busy = lastServiceBusyMap[serviceName] ?: false
        if (busy) {
            pendingServiceRequestMap[serviceName] = Triple(serviceType, requestJson, onResult)
            Log.d("RosViewModel", "sendOrQueueServiceRequest: Service $serviceName busy, queuing new request.")
            return
        }
        lastServiceBusyMap[serviceName] = true
        val id = "service_call_${System.currentTimeMillis()}"
        lastServiceRequestIdMap[serviceName] = id
        topicHandlers[id] = { response ->
            lastServiceBusyMap[serviceName] = false
            onResult(response)
            if (pendingServiceRequestMap.containsKey(serviceName)) {
                val (nextType, nextJson, nextCallback) = pendingServiceRequestMap.remove(serviceName)!!
                sendOrQueueServiceRequest(serviceName, nextType, nextJson, nextCallback)
            }
        }
        sendToBridge("Service Call '$serviceName'") {
            Json.encodeToString(
                RosBridgeServiceCall(
                    op = "call_service",
                    id = id,
                    service = serviceName,
                    type = serviceType,
                    args = Json.parseToJsonElement(requestJson)
                )
            )
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(10000L)
            if (lastServiceBusyMap[serviceName] == true && lastServiceRequestIdMap[serviceName] == id) {
                Log.w("RosViewModel", "[SERVICE] Timeout waiting for response for $serviceName, clearing busy lock.")
                lastServiceBusyMap[serviceName] = false
                if (pendingServiceRequestMap.containsKey(serviceName)) {
                    val (nextType, nextJson, nextCallback) = pendingServiceRequestMap.remove(serviceName)!!
                    sendOrQueueServiceRequest(serviceName, nextType, nextJson, nextCallback)
                }
            }
        }
    }

    /*
        input:    None
        output:   None
        remarks:  Force clears the busy lock and queue for all actions and resets action state.
    */
    fun forceClearAllActionBusyLocks() {
        lastGoalIdMap.clear()
        lastGoalStatusMap.clear()
        pendingGoalMap.clear()
    }

    /*
        input:    actionName - String, actionType - String, goalUuid - String, onResult - (String) -> Unit
        output:   None
        remarks:  Requests the result of an action goal via the get_result service.
    */
    private fun getActionResultViaService(
        actionName: String,
        actionType: String,
        goalUuid: String,
        onResult: (String) -> Unit
    ) {
        val names = getActionNames(actionName, actionType) ?: return
        val uuidBytes = uuidToByteArrayString(goalUuid)
        val getResultRequest = buildJsonObject {
            put("goal_id", buildJsonObject { put("uuid", Json.parseToJsonElement(uuidBytes)) })
        }
        val id = "service_call_${System.currentTimeMillis()}"
        topicHandlers[id] = onResult
        sendToBridge("GetResult Service Call '$actionName'") {
            Json.encodeToString(
                RosBridgeServiceCall(
                    op = "call_service",
                    id = id,
                    service = names.getResultService,
                    type = names.getResultType,
                    args = getResultRequest
                )
            )
        }
    }

    /*
        input:    actionName - String, actionType - String, goalFields - JsonObject, uuid - String
        output:   String? (UUID if sent, null if failed)
        remarks:  Sends an action goal via ROS2 service, returns the UUID if successful.
    */
    private fun sendActionGoalViaService(actionName: String, actionType: String, goalFields: JsonObject, uuid: String): String? {
        if (!RosbridgeConnectionManager.isConnected) {
            Log.w("RosViewModel", "Cannot send action goal: Not connected.")
            return null
        }
        val names = getActionNames(actionName, actionType) ?: return null
        val uuidBytes = uuidToByteArrayString(uuid)
        try {
            val sendGoalRequest = buildJsonObject {
                put("goal_id", buildJsonObject { put("uuid", Json.parseToJsonElement(uuidBytes)) })
                put("goal", goalFields)
            }
            val op = RosBridgeServiceCall(
                op = "call_service",
                id = "service_call_${System.currentTimeMillis()}",
                service = names.sendGoalService,
                type = names.sendGoalType,
                args = sendGoalRequest
            )
            val jsonString = Json.encodeToString(op)
            RosbridgeConnectionManager.sendRaw(jsonString)
            Log.d("RosViewModel", "Sent SendGoal service call to '${names.sendGoalService}': $sendGoalRequest")
            return uuid
        } catch (e: Exception) {
            Log.e("RosViewModel", "Failed to construct or send action goal (service)", e)
            return null
        }
    }

    private val lastGoalIdMap = mutableMapOf<String, String>()
    private val lastGoalStatusMap = mutableMapOf<String, String>()
    private val pendingGoalMap = mutableMapOf<String, Triple<String, String, JsonObject>>()
    private val actionStatusFlows = mutableMapOf<String, MutableSharedFlow<Map<String, String>>>()

    /*
        input:    actionName - String, actionType - String, goalFields - JsonObject, goalUuid - String (optional), onResult - ((String) -> Unit)? (optional)
        output:   None
        remarks:  Robustly sends or queues an action goal, cancels previous if active, and handles result.
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
            val jsonElem = Json.parseToJsonElement(resultJson)
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
            val timeoutMillis = 10000L
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
            viewModelScope.launch {
                kotlinx.coroutines.delay(timeoutMillis)
                if (!terminalHandled) {
                    terminalHandled = true
                    Log.w("RosViewModel", "[ACTION] Timeout waiting for terminal state for $actionName, fetching result anyway.")
                    getActionResultViaService(actionName, actionType, lastGoalIdMap[actionName] ?: "") { resultJson ->
                        handleTerminal(resultJson)
                    }
                    forceClearAllActionBusyLocks()
                }
            }
        }
    }

    /*
        input:    statusMsg - String, actionName - String, actionType - String
        output:   None
        remarks:  Handles status updates for an action goal and sends pending goal if previous is done/canceled.
    */
    private fun handleStatusUpdate(statusMsg: String, actionName: String, actionType: String) {
        Log.d("RosViewModel", "handleStatusUpdate called for actionName=$actionName, statusMsg=$statusMsg")
        val lastGoalId = lastGoalIdMap[actionName]
        Log.d("RosViewModel", "handleStatusUpdate: lastGoalId=$lastGoalId")
        val status = extractStatusForGoal(statusMsg, lastGoalId)
        Log.d("RosViewModel", "handleStatusUpdate: extracted status for goalId=$lastGoalId is $status")
        lastGoalStatusMap[actionName] = status ?: "UNKNOWN"
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

    /*
        input:    actionName - String
        output:   SharedFlow<Map<String, String>>
        remarks:  Returns a SharedFlow of status updates for a given action topic for coroutine-based waiting.
    */
    fun actionStatusFlow(actionName: String): SharedFlow<Map<String, String>> {
        return actionStatusFlows.getOrPut(actionName) { MutableSharedFlow(extraBufferCapacity = 8) }
    }

    /*
        input:    statusMsg - String, goalId - String?
        output:   String? (status string or null)
        remarks:  Extracts the status for a specific goalId from the GoalStatusArray message.
    */
    private fun extractStatusForGoal(statusMsg: String, goalId: String?): String? {
        if (goalId == null) return null
        try {
            val jsonElem = Json.parseToJsonElement(statusMsg)
            val statusArr = jsonElem.jsonObject["status_list"]?.jsonArray ?: return null
            for (statusEntry in statusArr) {
                val entryObj = statusEntry.jsonObject
                val entryGoalId = entryObj["goal_info"]?.jsonObject?.get("goal_id")?.jsonObject?.get("uuid")?.toString()
                if (entryGoalId != null && goalId == entryGoalId) {
                    val statusCode = entryObj["status"]?.jsonPrimitive?.intOrNull
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

    var onRosbridgeDisconnected: (() -> Unit)? = null

    /*
        input:    serviceName - String, serviceType - String
        output:   None
        remarks:  Advertises a ROS2 service via rosbridge_server.
    */
    fun advertiseService(serviceName: String, serviceType: String) {
        sendToBridge("Advertise Service '$serviceName'") {
            val op = RosBridgeAdvertiseService(
                op = "advertise_service",
                id = "advertise_service_${System.currentTimeMillis()}",
                service = serviceName,
                type = serviceType
            )
            Json.encodeToString(op)
        }
    }

    /*
        input:    serviceName - String, serviceType - String, requestJson - String, onResult - ((String) -> Unit)? (optional)
        output:   None
        remarks:  Calls a custom ROS2 service, optionally handling the result with a callback.
    */
    fun callCustomService(serviceName: String, serviceType: String, requestJson: String, onResult: ((String) -> Unit)? = null) {
        Log.d("RosViewModel", "callCustomService called: serviceName=$serviceName, serviceType=$serviceType, requestJson=$requestJson")
        if (onResult != null) {
            sendOrQueueServiceRequest(serviceName, serviceType, requestJson, onResult)
        } else {
            sendToBridge("Call Service '$serviceName'") {
                Json.encodeToString(
                    RosBridgeServiceCall(
                        op = "call_service",
                        id = "service_call_${System.currentTimeMillis()}",
                        service = serviceName,
                        type = serviceType,
                        args = Json.parseToJsonElement(requestJson)
                    )
                )
            }
        }
    }

    /*
        input:    actionName - String, actionType - String, onMessage - (String) -> Unit
        output:   None
        remarks:  Subscribes to the feedback topic for a ROS2 action and routes messages to the callback.
    */
    fun subscribeToActionFeedback(actionName: String, actionType: String, onMessage: (String) -> Unit) {
        val names = getActionNames(actionName, actionType) ?: return
    }

    /*
        input:    actionName - String, onMessage - (String) -> Unit
        output:   None
        remarks:  Subscribes to the status topic for a ROS2 action and routes messages to the callback.
    */
    fun subscribeToActionStatus(actionName: String, onMessage: (String) -> Unit) {
        val statusTopic = "$actionName/status"
        val statusType = "action_msgs/msg/GoalStatusArray"
    }

    /*
        input:    actionName - String, actionType - String, uuid - String
        output:   None
        remarks:  Cancels a ROS2 action goal by publishing a cancel message to the appropriate topic.
    */
    private fun cancelActionGoal(actionName: String, actionType: String, uuid: String) {
        val names = getActionNames(actionName, actionType) ?: return
        val cancelTopic = names.cancelTopic
        val cancelType = "action_msgs/msg/GoalInfo"

        advertiseTopic(cancelTopic, cancelType)

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
        }.toString()

        publishCustomRawMessage(cancelTopic, cancelType, cancelMsgJson)
        Log.d("RosViewModel", "Sent cancel to $cancelTopic: $cancelMsgJson")
    }

    /*
        input:    topicName - String, messageType - String, rawJson - String
        output:   None
        remarks:  Publishes a raw message to a topic.
    */
    fun publishCustomRawMessage(topicName: String, messageType: String, rawJson: String) {
        sendToBridge("Publish '$topicName'") {
            val msgField = when (messageType) {
                "std_msgs/msg/String" -> buildJsonObject { put("data", rawJson) }
                "std_msgs/msg/Bool" -> {
                    val asJson = try { Json.parseToJsonElement(rawJson) } catch (_: Exception) { null }
                    val boolVal = if (asJson is JsonObject && asJson["data"] != null) {
                        val dataVal = asJson["data"]
                        if (dataVal is JsonPrimitive) dataVal.booleanOrNull ?: false else false
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
            jsonObject.toString()
        }
        _customMessageHistory.update { currentHistory -> (currentHistory + rawJson).takeLast(25) }
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
        _customPublishers.value += publisher
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
        remarks:  Clears the custom message field.
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

    /*
        input:    newMessage - String
        output:   None
        remarks:  Updates the custom message text field value.
    */
    fun onCustomMessageChange(newMessage: String) {
        _customMessage.value = newMessage
    }

    private val _selectedDropdownIndex = MutableStateFlow(0)
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
        input:    topicName - String, messageType - String
        output:   None
        remarks:  Advertises a topic with the given type to rosbridge.
    */
    fun advertiseTopic(topicName: String, messageType: String) {
        sendToBridge("Advertise '$topicName'") {
            Json.encodeToString(RosBridgeAdvertise(op = "advertise", topic = topicName, type = messageType))
        }
    }

    /*
        input:    topicName - String, message - T, serializer - KSerializer<T>
        output:   None
        remarks:  Publishes a strongly-typed message to the given topic using the provided serializer.
    */
    private fun <T> publishMessage(topicName: String, message: T, serializer: KSerializer<T>) {
        sendToBridge("Publish '$topicName'") {
            val jsonObject = buildJsonObject {
                put("op", "publish")
                put("id", "publish_${topicName.replace("/", "_")}_${System.currentTimeMillis()}")
                put("topic", topicName)
                put("msg", Json.encodeToJsonElement(serializer, message))
                put("latch", false)
            }
            jsonObject.toString()
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
        val asJson = try {
            Json.parseToJsonElement(messageToSend)
        } catch (_: Exception) {
            null
        }
        val jsonString = asJson?.toString() ?: messageToSend
        _customMessageHistory.update { currentHistory -> (currentHistory + jsonString).takeLast(25) }
        publishStringMessage(topicName, messageToSend)
    }

    data class CustomProtocolAction(
        val label: String,
        val proto: CustomProtocolsViewModel.ProtocolFile,
        val fieldValues: Map<String, String>
    )

    private val _customProtocolActions = MutableStateFlow<List<CustomProtocolAction>>(emptyList())
    val customProtocolActions: StateFlow<List<CustomProtocolAction>> = _customProtocolActions

    /*
        input:    action - CustomProtocolAction
        output:   None
        remarks:  Adds a custom protocol action and saves to preferences.
    */
    fun addCustomProtocolAction(action: CustomProtocolAction) {
        _customProtocolActions.value += action
        saveCustomProtocolActionsToPrefs()
    }
    /*
        input:    index - Int
        output:   None
        remarks:  Removes a custom protocol action by index and saves to preferences.
    */
    fun removeCustomProtocolAction(index: Int) {
        val list = _customProtocolActions.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _customProtocolActions.value = list
            saveCustomProtocolActionsToPrefs()
        }
    }
    /*
        input:    list - List<CustomProtocolAction>
        output:   None
        remarks:  Sets the list of custom protocol actions and saves to preferences.
    */
    fun setCustomProtocolActions(list: List<CustomProtocolAction>) {
        _customProtocolActions.value = list
        saveCustomProtocolActionsToPrefs()
    }

    private val CUSTOM_PROTOCOL_ACTIONS_PREFS = "custom_protocol_actions"
    /*
        input:    None
        output:   None
        remarks:  Saves custom protocol actions to SharedPreferences.
    */
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
        prefs.edit { putString("custom_protocol_actions", arr.toString()) }
    }
    /*
        input:    None
        output:   None
        remarks:  Loads custom protocol actions from SharedPreferences.
    */
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

    fun exportAppActivitiesToYaml(outputStream: OutputStream) {
        val yaml = Yaml()
        val context = getApplication<Application>().applicationContext

        try {
            // Gather all relevant app activities
            val sliderPrefs = context.getSharedPreferences("slider_buttons_prefs", android.content.Context.MODE_PRIVATE)
            val geometryPrefs = context.getSharedPreferences("geometry_reusable_buttons", android.content.Context.MODE_PRIVATE)
            val customPubPrefs = context.getSharedPreferences("custom_publishers_prefs", android.content.Context.MODE_PRIVATE)
            val customProtocolPrefs = context.getSharedPreferences(CUSTOM_PROTOCOL_ACTIONS_PREFS, android.content.Context.MODE_PRIVATE)

            val configMap = mapOf(
                "customPublishers" to customPublishers.value.map {
                    mapOf("topic" to it.topic, "messageType" to it.messageType, "message" to it.message)
                },
                "customProtocolActions" to customProtocolActions.value.map {
                    mapOf(
                        "label" to it.label,
                        "proto" to mapOf(
                            "name" to it.proto.name,
                            "importPath" to it.proto.importPath,
                            "type" to it.proto.type.name
                        ),
                        "fieldValues" to it.fieldValues
                    )
                },
                "customMessageHistory" to customMessageHistory.value,
                "sliderButtons" to (sliderPrefs.getString("saved_slider_buttons", null) ?: ""),
                "geometryButtons" to (geometryPrefs.getString("geometry_buttons", null) ?: ""),
                "customPublishersPrefs" to (customPubPrefs.getString("custom_publishers", null) ?: ""),
                "customProtocolActionsPrefs" to (customProtocolPrefs.getString("custom_protocol_actions", null) ?: "")
            )
            Log.d("RosViewModel", "Export configMap: $configMap")
            // Check if configMap is empty or all values are empty
            val allEmpty = configMap.values.all {
                when (it) {
                    is List<*> -> it.isEmpty()
                    is String -> it.isEmpty()
                    else -> false
                }
            }
            if (allEmpty) {
                Log.w("RosViewModel", "Exported config is empty! No activities to save.")
            }
            // Write and flush to ensure data is written
            outputStream.writer().use { writer ->
                val yamlString = yaml.dump(configMap)
                Log.d("RosViewModel", "YAML output: $yamlString")
                writer.write(yamlString)
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e("RosViewModel", "Error exporting config to YAML: ${e.message}", e)
        }
    }

    fun importAppActivitiesFromYaml(inputStream: InputStream) {
        val yaml = Yaml()
        val context = getApplication<Application>().applicationContext
        val configMap = yaml.load<Map<String, Any>>(inputStream.reader())
        Log.d("RosViewModel", "Import configMap: $configMap")

        if (configMap == null || configMap.isEmpty()) {
            Log.w("RosViewModel", "Imported config is empty or null!")
            return
        }

        // Restore custom publishers
        (configMap["customPublishers"] as? List<Map<String, Any>>)?.let { list ->
            val publishers = list.map {
                CustomPublisher(
                    topic = it["topic"] as? String ?: "",
                    messageType = it["messageType"] as? String ?: "",
                    message = it["message"] as? String ?: ""
                )
            }
            setCustomPublishers(publishers)
        }

        // Restore custom protocol actions
        (configMap["customProtocolActions"] as? List<Map<String, Any>>)?.let { list ->
            val actions = list.map {
                val label = it["label"] as? String ?: ""
                val protoMap = it["proto"] as? Map<String, Any> ?: emptyMap()
                val proto = CustomProtocolsViewModel.ProtocolFile(
                    protoMap["name"] as? String ?: "",
                    protoMap["importPath"] as? String ?: "",
                    CustomProtocolsViewModel.ProtocolType.valueOf(protoMap["type"] as? String ?: "MSG")
                )
                val fieldValues = (it["fieldValues"] as? Map<String, String>) ?: emptyMap()
                CustomProtocolAction(label, proto, fieldValues)
            }
            setCustomProtocolActions(actions)
        }

        // Restore custom message history
        (configMap["customMessageHistory"] as? List<String>)?.let { set ->
            _customMessageHistory.value = set
        }

        // Restore slider buttons
        (configMap["sliderButtons"] as? String)?.let {
            val sliderPrefs = context.getSharedPreferences("slider_buttons_prefs", android.content.Context.MODE_PRIVATE)
            sliderPrefs.edit { putString("saved_slider_buttons", it) }
        }

        // Restore geometry buttons
        (configMap["geometryButtons"] as? String)?.let {
            val geometryPrefs = context.getSharedPreferences("geometry_reusable_buttons", android.content.Context.MODE_PRIVATE)
            geometryPrefs.edit { putString("geometry_buttons", it) }
        }

        // Restore custom publishers prefs (raw JSON)
        (configMap["customPublishersPrefs"] as? String)?.let {
            val customPubPrefs = context.getSharedPreferences("custom_publishers_prefs", android.content.Context.MODE_PRIVATE)
            customPubPrefs.edit { putString("custom_publishers", it) }
        }

        // Restore custom protocol actions prefs (raw JSON)
        (configMap["customProtocolActionsPrefs"] as? String)?.let {
            val customProtocolPrefs = context.getSharedPreferences(CUSTOM_PROTOCOL_ACTIONS_PREFS, android.content.Context.MODE_PRIVATE)
            customProtocolPrefs.edit { putString("custom_protocol_actions", it) }
            loadCustomProtocolActionsFromPrefs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        RosbridgeConnectionManager.removeListener(this)
    }

    override fun onConnected() {
        _connectionStatus.value = "Connected"
    }

    override fun onDisconnected() {
        _connectionStatus.value = "Disconnected"
        onRosbridgeDisconnected?.invoke()
    }

    override fun onError(error: String) {
        _connectionStatus.value = "Error: $error"
    }

    override fun onMessage(text: String) {
        try {
            if (!text.contains("\"id\":")) {
                return
            }

            val json = Json.parseToJsonElement(text).jsonObject
            val op = json["op"]?.jsonPrimitive?.contentOrNull

            if (op == "service_response") {
                val id = json["id"]?.jsonPrimitive?.contentOrNull
                if (id != null && topicHandlers.containsKey(id)) {
                    topicHandlers[id]?.invoke(text)
                    topicHandlers.remove(id)
                }
            }
        } catch (e: Exception) {
            Log.w("RosViewModel", "Error in ViewModel.onMessage: ${e.message}")
        }
    }
}