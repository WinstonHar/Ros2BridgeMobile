
package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosBridgeServiceCall
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.UUID
import android.util.Log

class RosActionRepositoryImpl(
    private val rosbridgeClient: RosbridgeClient
) : RosActionRepository {

    override fun forceClearAllActionBusyLocks() {
        lastGoalIdMap.clear()
        lastGoalStatusMap.clear()
        pendingGoalMap.clear()
    }

    override fun sendOrQueueActionGoal(
        actionName: String,
        actionType: String,
        goalFields: JsonObject,
        goalUuid: String,
        onResult: ((String) -> Unit)?
    ) {
        val prevGoalId = lastGoalIdMap[actionName]
        val prevGoalStatus = lastGoalStatusMap[actionName]
        val handleTerminal: (String) -> Unit = { resultJson ->
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
            pendingGoalMap[actionName] = Triple(actionName, actionType, goalFields)
            cancelActionGoal(actionName, actionType, prevGoalId)
        } else {
            val newGoalId = sendActionGoalViaService(actionName, actionType, goalFields, goalUuid)
            lastGoalIdMap[actionName] = newGoalId ?: goalUuid
            lastGoalStatusMap[actionName] = "PENDING"
            var terminalHandled = false
            val timeoutMillis = 10000L
            val statusHandler: (String) -> Unit = { statusMsg ->
                val status = extractStatusForGoal(statusMsg, lastGoalIdMap[actionName])
                if (!terminalHandled && (status == "SUCCEEDED" || status == "CANCELED" || status == "ABORTED" || status == "REJECTED")) {
                    terminalHandled = true
                    getActionResultViaService(actionName, actionType, lastGoalIdMap[actionName] ?: "") { resultJson ->
                        handleTerminal(resultJson)
                    }
                }
                handleStatusUpdate(statusMsg, actionName, actionType)
            }
            subscribeToActionStatus(actionName, statusHandler)
            scope.launch {
                delay(timeoutMillis)
                if (!terminalHandled) {
                    terminalHandled = true
                    getActionResultViaService(actionName, actionType, lastGoalIdMap[actionName] ?: "") { resultJson ->
                        handleTerminal(resultJson)
                    }
                    forceClearAllActionBusyLocks()
                }
            }
        }
    }

    private fun sendActionGoalViaService(actionName: String, actionType: String, goalFields: JsonObject, uuid: String): String? {
        val names = getActionNames(actionName, actionType) ?: return null
        val uuidBytes = uuidToByteArrayString(uuid)
        return try {
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

    private val serviceResultHandlers = mutableMapOf<String, (String) -> Unit>()
    private fun getActionResultViaService(actionName: String, actionType: String, goalUuid: String, onResult: (String) -> Unit) {
        val names = getActionNames(actionName, actionType) ?: return
        val uuidBytes = uuidToByteArrayString(goalUuid)
        val getResultRequest = buildJsonObject {
            put("goal_id", buildJsonObject { put("uuid", Json.parseToJsonElement(uuidBytes)) })
        }
        val id = "service_call_${System.currentTimeMillis()}"
        serviceResultHandlers[id] = onResult
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

    override fun subscribeToActionFeedback(actionName: String, actionType: String, onMessage: (String) -> Unit) {
        val names = getActionNames(actionName, actionType) ?: return
        val feedbackTopic = names.feedbackTopic
        val feedbackType = names.feedbackType
        rosbridgeClient.subscribe(feedbackTopic, feedbackType, onMessage)
    }

    override fun subscribeToActionStatus(actionName: String, onMessage: (String) -> Unit) {
        val statusTopic = "$actionName/status"
        val statusType = "action_msgs/msg/GoalStatusArray"
        rosbridgeClient.subscribe(statusTopic, statusType, onMessage)
    }
    // This should be called by RosbridgeClient when a service response arrives
    fun handleServiceResponse(id: String, response: String) {
        serviceResultHandlers.remove(id)?.invoke(response)
    }

    override fun actionStatusFlow(actionName: String): SharedFlow<Map<String, String>> {
        return actionStatusFlows.getOrPut(actionName) { MutableSharedFlow(extraBufferCapacity = 8) }
    }

    // Helper functions
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