package com.examples.testros2jsbridge.presentation.ui.screens.geometry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.toDto
import com.examples.testros2jsbridge.domain.geometry.GeometryMessageBuilder
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository
import com.examples.testros2jsbridge.presentation.state.GeometryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.examples.testros2jsbridge.core.util.Logger

@HiltViewModel
class GeometryViewModel @Inject constructor(
    private val rosMessageRepository: RosMessageRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        GeometryUiState(
            fieldValues = emptyMap(),
            typeInput = com.examples.testros2jsbridge.domain.geometry.geometryTypes.firstOrNull() ?: ""
        )
    )
    val uiState: StateFlow<GeometryUiState> = _uiState

    init {
        // On ViewModel init, observe all saved geometry messages from repository for persistence
        viewModelScope.launch {
            rosMessageRepository.messages.collect { messageDtos ->
                val messages = messageDtos.map { dto ->
                    RosMessage(
                        id = dto.id,
                        topic = dto.topic,
                        type = dto.type ?: "",
                        content = dto.content ?: "",
                        timestamp = dto.timestamp ?: System.currentTimeMillis(),
                        label = dto.label,
                        sender = dto.sender,
                        isPublished = dto.isPublished ?: true,
                        op = dto.op,
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
    fun updateFieldValue(tag: String, value: String) {
        _uiState.value = _uiState.value.copy(fieldValues = _uiState.value.fieldValues.toMutableMap().apply { put(tag, value) })
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


    // saveMessage now delegates to GeometryMessageBuilder in the domain layer.
    fun saveMessage() {
        Logger.d("GeometryViewModel", "Saving geometry message")
        Logger.d("GeometryViewModel", "typeInput: ${_uiState.value.typeInput} fieldValues: ${_uiState.value.fieldValues}")
        val typeInput = _uiState.value.typeInput
        if (typeInput.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                showErrorDialog = true,
                errorMessage = "You must select a geometry message type before saving."
            )
            Logger.d("GeometryViewModel", "Error: No type selected.")
            return
        }
        val rosType = "geometry_msgs/msg/$typeInput"
        val msgJson = GeometryMessageBuilder.build(typeInput, _uiState.value.fieldValues)
        val newMessage = RosMessage(
            topic = RosId(_uiState.value.topicInput),
            type = rosType,
            content = msgJson,
            label = _uiState.value.nameInput.ifBlank { null },
            id = null,
            timestamp = System.currentTimeMillis(),
            sender = null,
            isPublished = true,
            op = "publish",
            latch = null,
            queue_size = null
        )
        val updated = _uiState.value.messages + newMessage
        _uiState.value = _uiState.value.copy(messages = updated)
        // Persist the new message
        viewModelScope.launch {
            rosMessageRepository.saveMessage(newMessage.toDto())
            Logger.d("GeometryViewModel", "Geometry message saved: $newMessage")
        }
    }

    fun publishMessage() {
        val msg = _uiState.value.selectedMessage ?: return
        rosMessageRepository.publishMessage(msg)
    }



    fun deleteMessage(message: RosMessage) {
        // Remove from UI state
        val updated = _uiState.value.messages.filterNot {
            it.timestamp == message.timestamp && it.topic == message.topic && it.type == message.type && it.content == message.content
        }
        _uiState.value = _uiState.value.copy(messages = updated)
        // Remove from repository for persistence
        viewModelScope.launch {
            rosMessageRepository.deleteMessage(message.toDto())
        }
    }

    fun showSavedButtons(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSavedButtons = show)
    }

    fun dismissErrorDialog() {
        _uiState.value = _uiState.value.copy(showErrorDialog = false, errorMessage = null)
    }
}