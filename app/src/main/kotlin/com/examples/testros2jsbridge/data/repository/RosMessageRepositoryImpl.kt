package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.local.database.dao.ConnectionDao
import com.examples.testros2jsbridge.data.local.database.dao.PublisherDao
import com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.toDto
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.RosMessage
import javax.inject.Inject



class RosMessageRepositoryImpl @Inject constructor(
    private val rosbridgeClient: RosbridgeClient,
    private val connectionDao: ConnectionDao
) : RosMessageRepository {

    private val _messages: MutableStateFlow<List<RosMessageDto>> = MutableStateFlow<List<RosMessageDto>>(emptyList())
    override val messages: MutableStateFlow<List<RosMessageDto>> get() = _messages


    override suspend fun deleteMessage(message: RosMessageDto) {
        _messages.value = _messages.value.filterNot {
            it.id == message.id &&
            it.topic == message.topic &&
            it.type == message.type &&
            it.content == message.content
        }
    }

    override suspend fun getMessagesByTopic(topic: RosId): List<RosMessageDto> {
        return _messages.value.filter { it.topic.value == topic.value }
    }

    override suspend fun saveMessage(message: RosMessageDto) {
        _messages.value = _messages.value.filter { it.id != message.id } + message
    }

    override suspend fun getMessage(messageId: String): RosMessageDto? {
        return _messages.value.find { it.id == messageId }
    }

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun publishMessage(message: RosMessage) {
        val dto = message.toDto()
        val jsonString = Json.encodeToString(RosMessageDto.serializer(), dto)
        rosbridgeClient.send(jsonString)
        // Optionally update local message history
        _messages.value = (_messages.value + dto)
    }

    private val _customMessage = MutableStateFlow("")

    override fun clearCustomMessage() {
        _customMessage.value = ""
    }

    override fun onCustomMessageChange(newMessage: String) {
        _customMessage.value = newMessage
    }

    override fun updateConnectionStatus(status: String) {
        // Here, 'status' is used as the connectionId. Adjust as needed.
        connectionDao.updateStatus(
            status,
            isConnected = status == "connected"
        )
    }
}