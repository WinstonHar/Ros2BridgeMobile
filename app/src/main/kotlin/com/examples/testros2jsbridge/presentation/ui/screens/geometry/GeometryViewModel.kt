package com.examples.testros2jsbridge.presentation.ui.screens.geometry

import androidx.lifecycle.ViewModel
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.presentation.state.GeometryUiState
import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class GeometryViewModel @Inject constructor(
    private val rosMessageRepository: RosMessageRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GeometryUiState())
    val uiState: StateFlow<GeometryUiState> = _uiState

    init {
        // On ViewModel init, load messages for the current topic if set
        viewModelScope.launch {
            val topicInput = _uiState.value.topicInput
            if (topicInput.isNotBlank()) {
                val topic = RosId(topicInput)
                val messageDtos = rosMessageRepository.getMessagesByTopic(topic)
                val messages = messageDtos.map { dto ->
                    RosMessage(
                        id = dto.id,
                        topic = dto.topic,
                        type = dto.type ?: "",
                        content = dto.msg?.let {
                            val jsonElement = Json.encodeToJsonElement(
                                MapSerializer(String.serializer(), String.serializer()),
                                it
                            )
                            jsonElement.toString()
                        } ?: "",
                        timestamp = System.currentTimeMillis(),
                        label = null,
                        sender = null,
                        isPublished = true,
                        op = dto.op ?: "",
                        latch = dto.latch,
                        queue_size = dto.queue_size
                    )
                }
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
    }

    fun updateNameInput(name: String) {
        _uiState.value = _uiState.value.copy(nameInput = name)
    }
    fun updateTopicInput(topic: String) {
        _uiState.value = _uiState.value.copy(topicInput = topic)
    }
    fun updateTypeInput(type: String) {
        _uiState.value = _uiState.value.copy(typeInput = type)
    }
    fun updateMessageContentInput(content: String) {
        _uiState.value = _uiState.value.copy(messageContentInput = content)
    }

    fun selectMessage(message: RosMessage) {
        _uiState.value = _uiState.value.copy(
            selectedMessage = message,
            nameInput = message.label ?: "",
            topicInput = message.topic.toString(),
            typeInput = message.type,
            messageContentInput = message.content
        )
    }

    fun saveMessage() {
        val newMessage = RosMessage(
            topic = RosId(_uiState.value.topicInput),
            type = _uiState.value.typeInput,
            content = _uiState.value.messageContentInput,
            label = _uiState.value.nameInput.ifBlank { null },
            id = null,
            timestamp = System.currentTimeMillis(),
            sender = null,
            isPublished = true,
            op = "publish",
            latch = null,
            queue_size = null
        )
        // Add to UI state list (simulate save)
        val updated = _uiState.value.messages + newMessage
        _uiState.value = _uiState.value.copy(messages = updated)
    }

    fun publishMessage() {
        val msg = _uiState.value.selectedMessage ?: return
        rosMessageRepository.publishMessage(msg)
    }



    fun deleteMessage(message: RosMessage) {
        // Not implemented
    }

    fun showSavedButtons(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSavedButtons = show)
    }

    fun dismissErrorDialog() {
        _uiState.value = _uiState.value.copy(showErrorDialog = false, errorMessage = null)
    }
}