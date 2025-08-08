package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.local.database.dao.ConnectionDao
import com.examples.testros2jsbridge.data.local.database.dao.GeometryMessageDao
import com.examples.testros2jsbridge.data.local.database.entities.toDto
import com.examples.testros2jsbridge.data.local.database.entities.toEntity
import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.toDto
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import javax.inject.Inject

class RosMessageRepositoryImpl @Inject constructor(
    private val rosbridgeClient: RosbridgeClient,
    private val connectionDao: ConnectionDao,
    private val geometryMessageDao: GeometryMessageDao
) : RosMessageRepository {


    private val _messages: MutableStateFlow<List<RosMessageDto>> = MutableStateFlow<List<RosMessageDto>>(emptyList())
    override val messages: MutableStateFlow<List<RosMessageDto>> get() = _messages

    init {
        // Load all geometry messages from Room on repository init
        kotlinx.coroutines.GlobalScope.launch {
            val entities = geometryMessageDao.getAll()
            _messages.value = entities.map { it.toDto() }
        }
    }



    override suspend fun deleteMessage(message: RosMessageDto) {
        // Remove from Room
        val all = geometryMessageDao.getAll()
        val entity = all.find {
            it.label == message.label &&
            it.topic == message.topic.value &&
            it.type == message.type &&
            it.content == message.content
        }
        if (entity != null) {
            geometryMessageDao.delete(entity)
        }
        // Update in-memory list
        val updated = geometryMessageDao.getAll().map { it.toDto() }
        _messages.value = updated
    }

    override suspend fun getMessagesByTopic(topic: RosId): List<RosMessageDto> {
        return _messages.value.filter { it.topic.value == topic.value }
    }


    override suspend fun saveMessage(message: RosMessageDto) {
        // Save to Room
        geometryMessageDao.insert(message.toEntity())
        // Update in-memory list
        val updated = geometryMessageDao.getAll().map { it.toDto() }
        _messages.value = updated
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
        // Optionally update local message history and persist
        kotlinx.coroutines.GlobalScope.launch {
            geometryMessageDao.insert(dto.toEntity())
            val updated = geometryMessageDao.getAll().map { it.toDto() }
            _messages.value = updated
        }
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