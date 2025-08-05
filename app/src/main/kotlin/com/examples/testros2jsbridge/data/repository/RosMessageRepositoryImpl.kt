package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository
import com.examples.testros2jsbridge.data.remote.protocol.RosProtocolHandler
import com.examples.testros2jsbridge.data.remote.protocol.MessageSerializer

class RosMessageRepositoryImpl(
    private val rosbridgeClient: RosbridgeClient
) : RosMessageRepository {

    private val subscriberDao = com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao()
    private val publisherDao = com.examples.testros2jsbridge.data.local.database.dao.PublisherDao()

    override suspend fun getMessagesByTopic(topicId: com.examples.testros2jsbridge.domain.model.RosId): List<com.examples.testros2jsbridge.domain.model.RosMessage> {
        // Collect messages from all subscribers for the given topic
        val allSubscribers = subscriberDao.subscribers.value
        val messages = mutableListOf<com.examples.testros2jsbridge.domain.model.RosMessage>()
        allSubscribers.forEach { sub ->
            if (sub.topic == topicId && sub.messageHistory != null) {
                messages.addAll(sub.messageHistory)
            }
        }
        // Also collect from publishers if needed
        val allPublishers = publisherDao.publishers.value
        allPublishers.forEach { pub ->
            if (pub.id == topicId && pub.messageHistory != null) {
                messages.addAll(pub.messageHistory)
            }
        }
        // Optionally, filter duplicates by message id
        return messages.distinctBy { it.id }
    }

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun publishCustomRawMessage(topicName: String, messageType: String, rawJson: String) {
        val msgField = when {
            messageType.startsWith("geometry_msgs/") -> {
                // Use protocol handler for geometry messages
                RosProtocolHandler.buildGeometryMessage(messageType, rawJson)
            }
            messageType.endsWith(".msg") || messageType.endsWith(".srv") || messageType.endsWith(".action") -> {
                // Use protocol handler for custom protocol messages
                RosProtocolHandler.buildCustomProtocolMessage(messageType, rawJson)
            }
            messageType == "std_msgs/msg/String" -> buildJsonObject { put("data", rawJson) }
            messageType == "std_msgs/msg/Bool" -> {
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
        _customMessageHistory.update { currentHistory -> (currentHistory + rawJson).takeLast(25) }
    }

    fun publishServiceCall(serviceName: String, serviceType: String, args: String) {
        val serviceMsg = RosProtocolHandler.buildServiceCall(serviceName, serviceType, args)
        rosbridgeClient.send(serviceMsg)
    }

    fun publishActionCall(actionName: String, actionType: String, goal: String) {
        val actionMsg = RosProtocolHandler.buildActionCall(actionName, actionType, goal)
        rosbridgeClient.send(actionMsg)
    }

    override fun clearCustomMessage() {
        _customMessage.value = ""
    }

    override fun onCustomMessageChange(newMessage: String) {
        _customMessage.value = newMessage
    }

    override fun updateConnectionStatus(status: String) {
        val connectionDao = com.examples.testros2jsbridge.data.local.database.dao.ConnectionDao.getInstance()
        connectionDao.updateStatus(status)
    }
}