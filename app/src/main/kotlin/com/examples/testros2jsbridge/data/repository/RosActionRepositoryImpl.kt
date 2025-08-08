package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosBridgeServiceCall
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.ActionFieldValue
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosActionDto
import com.examples.testros2jsbridge.domain.model.RosAction
import com.examples.testros2jsbridge.domain.model.RosId
import kotlinx.serialization.InternalSerializationApi
import java.util.UUID
import com.examples.testros2jsbridge.domain.repository.RosActionRepository
import javax.inject.Inject

class RosActionRepositoryImpl @Inject constructor(
    private val rosbridgeClient: RosbridgeClient,
    override val actions: MutableStateFlow<List<RosAction>>
) : RosActionRepository {

    override fun publishRaw(json: String) {
        rosbridgeClient.send(json)
    }
    // Coroutine scope for launching jobs
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Track goal IDs and statuses for each action
    private val lastGoalIdMap = mutableMapOf<String, String>()
    private val lastGoalStatusMap = mutableMapOf<String, String>()
    private val pendingGoalMap = mutableMapOf<String, Triple<String, String, Map<String, ActionFieldValue>>>()
    private val actionStatusFlows = mutableMapOf<String, MutableSharedFlow<Map<String, String>>>()


     override fun forceClearAllActionBusyLocks() {
        lastGoalIdMap.clear()
        lastGoalStatusMap.clear()
        pendingGoalMap.clear()
    }

    override fun sendOrQueueActionGoal(
        actionName: String,
        actionType: String,
        goalFields: Map<String, ActionFieldValue>,
        goalUuid: String,
        onResult: ((RosActionDto) -> Unit)?
    ) {
        val prevGoalId = lastGoalIdMap[actionName]
        val prevGoalStatus = lastGoalStatusMap[actionName]
        val handleTerminal: (RosActionDto) -> Unit = { resultDto: RosActionDto ->
            val statusCode = resultDto.resultCode ?: -1
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
            onResult?.invoke(resultDto)
        }
        if (prevGoalId != null && prevGoalStatus != null && prevGoalStatus != "SUCCEEDED" && prevGoalStatus != "CANCELED" && prevGoalStatus != "ABORTED" && prevGoalStatus != "REJECTED") {
            pendingGoalMap[actionName] = Triple(actionName, actionType, goalFields)
            cancelActionGoal(actionName, actionType, prevGoalId)
        } else {
            val newGoalId = sendActionGoalViaService(actionName, actionType, goalFields, goalUuid)
            lastGoalIdMap[actionName] = newGoalId ?: goalUuid
            lastGoalStatusMap[actionName] = "PENDING"
            var terminalHandled = false
            val timeoutMillis = 10000L
            val statusHandler: (String) -> Unit = { statusMsg: String ->
                val status = extractStatusForGoal(statusMsg, lastGoalIdMap[actionName])
                if (!terminalHandled && (status == "SUCCEEDED" || status == "CANCELED" || status == "ABORTED" || status == "REJECTED")) {
                    terminalHandled = true
                    getActionResultViaService(actionName, actionType, lastGoalIdMap[actionName] ?: "") { resultDto: RosActionDto ->
                        handleTerminal(resultDto)
                    }
                }
                handleStatusUpdate(statusMsg, actionName, actionType)
            }
            subscribeToActionStatus(actionName, statusHandler)
            scope.launch {
                delay(timeoutMillis)
                if (!terminalHandled) {
                    terminalHandled = true
                    getActionResultViaService(actionName, actionType, lastGoalIdMap[actionName] ?: "") { resultDto ->
                        handleTerminal(resultDto)
                    }
                    forceClearAllActionBusyLocks()
                }
            }
        }
    }

    private val serviceResultHandlers = mutableMapOf<String, (String) -> Unit>()
    private fun getActionResultViaService(actionName: String, actionType: String, goalUuid: String, onResult: (RosActionDto) -> Unit) {
        val names = getActionNames(actionName, actionType) ?: return
        val uuidBytes = uuidToByteArrayString(goalUuid)
        val getResultRequest = buildJsonObject {
            put("goal_id", buildJsonObject { put("uuid", Json.parseToJsonElement(uuidBytes)) })
        }
        val id = "service_call_${System.currentTimeMillis()}"
        serviceResultHandlers[id] = { resultJson ->
            try {
                val dto = kotlinx.serialization.json.Json.decodeFromString(RosActionDto.serializer(), resultJson)
                onResult(dto)
            } catch (e: Exception) {
                // Optionally log or handle error
            }
        }
        rosbridgeClient.send(Json.encodeToString(
            RosBridgeServiceCall(
                op = "call_service",
                id = id,
                service = names.getResultService,
                type = names.getResultType,
                args = getResultRequest
            )
        ))
    }

    private fun handleStatusUpdate(statusMsg: String, actionName: String, actionType: String) {
        val lastGoalId = lastGoalIdMap[actionName]
        val status = extractStatusForGoal(statusMsg, lastGoalId)
        lastGoalStatusMap[actionName] = status ?: "UNKNOWN"
        if (status != null) {
            val flow = actionStatusFlows.getOrPut(actionName) { MutableSharedFlow(extraBufferCapacity = 8) }
            flow.tryEmit(mapOf("goal_id" to (lastGoalId ?: ""), "status" to status))
        }
        if ((status == "CANCELED" || status == "SUCCEEDED" || status == "ABORTED" || status == "REJECTED") && pendingGoalMap.containsKey(actionName)) {
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

    @OptIn(InternalSerializationApi::class)
    private fun sendActionGoalViaService(actionName: String, actionType: String, goalFields: Map<String, ActionFieldValue>, uuid: String): String? {
        val names = getActionNames(actionName, actionType) ?: return null
        val uuidBytes = uuidToByteArrayString(uuid)
        return try {
            val rosActionDto = RosActionDto(
                action = actionName,
                type = actionType,
                goal = goalFields,
                id = uuid,
                goalId = uuid,
                status = null,
                result = null,
                feedback = null,
                resultCode = null,
                resultText = null
            )
            // You may need to convert ActionFieldValue back to JsonObject for the actual service call
            val sendGoalRequest = buildJsonObject {
                put("goal_id", buildJsonObject { put("uuid", Json.parseToJsonElement(uuidBytes)) })
                put("goal", actionFieldValueMapToJsonObject(goalFields))
            }
            val op = RosBridgeServiceCall(
                op = "call_service",
                id = "service_call_${System.currentTimeMillis()}",
                service = names.sendGoalService,
                type = names.sendGoalType,
                args = sendGoalRequest
            )
            val jsonString = Json.encodeToString(op)
            rosbridgeClient.send(jsonString)
            uuid
        } catch (e: Exception) {
            null
        }
    }

     override fun cancelActionGoal(actionName: String, actionType: String, uuid: String) {
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
    }

    @OptIn(InternalSerializationApi::class)
    override fun subscribeToActionFeedback(
        actionName: String,
        actionType: String,
        onMessage: (RosActionDto) -> Unit
    ) {
        val names = getActionNames(actionName, actionType) ?: return
        val feedbackTopic = names.feedbackTopic
        val feedbackType = names.feedbackType
        rosbridgeClient.subscribe(feedbackTopic, feedbackType) { msgJson ->
            try {
                val dto = kotlinx.serialization.json.Json.decodeFromString(RosActionDto.serializer(), msgJson)
                onMessage(dto)
            } catch (e: Exception) {
                // Optionally log or handle error
            }
        }
    }

     override fun subscribeToActionStatus(actionName: String, onMessage: (String) -> Unit) {
        val statusTopic = "$actionName/status"
        val statusType = "action_msgs/msg/GoalStatusArray"
        rosbridgeClient.subscribe(statusTopic, statusType, onMessage)
    }

    override fun actionStatusFlow(actionName: String): SharedFlow<Map<String, String>> {
        TODO("Not yet implemented")
    }

    override suspend fun saveAction(action: RosAction) {
        TODO("Not yet implemented")
    }

    override suspend fun getAction(actionId: RosId): RosAction {
        TODO("Not yet implemented")
    }

    // Helper functions

    // Converts JsonObject to Map<String, ActionFieldValue>
    private fun jsonObjectToActionFieldValueMap(jsonObject: JsonObject): Map<String, ActionFieldValue> =
        jsonObject.mapValues { (_, v) -> jsonElementToActionFieldValue(v) }

    // Converts Map<String, ActionFieldValue> back to JsonObject
    private fun actionFieldValueMapToJsonObject(map: Map<String, ActionFieldValue>): JsonObject =
        buildJsonObject {
            map.forEach { (k, v) -> put(k, actionFieldValueToJsonElement(v)) }
        }

    // Recursively convert JsonElement to ActionFieldValue
    private fun jsonElementToActionFieldValue(element: JsonElement): ActionFieldValue = when {
        element is JsonPrimitive && element.isString -> ActionFieldValue.StringValue(element.content)
        element is JsonPrimitive && element.booleanOrNull != null -> ActionFieldValue.BoolValue(element.boolean)
        element is JsonPrimitive && element.intOrNull != null -> ActionFieldValue.IntValue(element.int)
        element is JsonPrimitive && element.doubleOrNull != null -> ActionFieldValue.DoubleValue(element.double)
        element is JsonObject -> ActionFieldValue.ObjectValue(
            element.mapValues { (_, v) -> jsonElementToActionFieldValue(v) }
        )
        element is JsonArray -> ActionFieldValue.ListValue(
            element.map { jsonElementToActionFieldValue(it) }
        )
        else -> ActionFieldValue.StringValue(element.toString())
    }

    // Recursively convert ActionFieldValue to JsonElement
    private fun actionFieldValueToJsonElement(value: ActionFieldValue): JsonElement = when (value) {
        is ActionFieldValue.StringValue -> JsonPrimitive(value.value)
        is ActionFieldValue.BoolValue -> JsonPrimitive(value.value)
        is ActionFieldValue.IntValue -> JsonPrimitive(value.value)
        is ActionFieldValue.DoubleValue -> JsonPrimitive(value.value)
        is ActionFieldValue.ObjectValue -> buildJsonObject {
            value.value.forEach { (k, v) -> put(k, actionFieldValueToJsonElement(v)) }
        }
        is ActionFieldValue.ListValue -> JsonArray(value.value.map { actionFieldValueToJsonElement(it) })
    }
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

    // Data class for action topic/type names
    private data class RosActionNames(
        val feedbackTopic: String,
        val statusTopic: String,
        val cancelTopic: String,
        val sendGoalService: String,
        val getResultService: String,
        val feedbackType: String,
        val sendGoalType: String,
        val getResultType: String
    )

    private fun uuidToByteArrayString(uuid: String): String {
        return try {
            val u = UUID.fromString(uuid)
            val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
            bb.putLong(u.mostSignificantBits)
            bb.putLong(u.leastSignificantBits)
            bb.array().joinToString(",", prefix = "[", postfix = "]") { it.toUByte().toString() }
        } catch (e: Exception) {
            (0..15).joinToString(",", prefix = "[", postfix = "]") { "0" }
        }
    }

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

    private fun advertiseTopic(topicName: String, messageType: String) {
        val advertiseMsg = Json.encodeToString(
            mapOf(
                "op" to "advertise",
                "topic" to topicName,
                "type" to messageType
            )
        )
        rosbridgeClient.send(advertiseMsg)
    }

    private fun publishCustomRawMessage(topicName: String, messageType: String, rawJson: String) {
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
        rosbridgeClient.send(jsonObject.toString())
    }
}