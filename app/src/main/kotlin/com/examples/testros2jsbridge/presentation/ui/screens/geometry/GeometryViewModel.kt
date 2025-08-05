package com.examples.testros2jsbridge.presentation.ui.screens.geometry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.presentation.state.GeometryUiState
import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GeometryViewModel(
    private val repository: RosMessageRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GeometryUiState())
    val uiState: StateFlow<GeometryUiState> = _uiState

    init {
        viewModelScope.launch {
            val messages = repository.getMessagesByTopic(/* geometry topic id, if needed */)
            _uiState.value = _uiState.value.copy(messages = messages)
        }
    }

    fun selectMessage(message: RosMessage) {
        _uiState.value = _uiState.value.copy(selectedMessage = message, topicInput = message.topic.toString(), typeInput = message.type, messageContentInput = message.content)
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

    fun saveMessage() {
        val newMessage = RosMessage(
            topic = com.examples.testros2jsbridge.domain.model.RosId(_uiState.value.topicInput),
            type = _uiState.value.typeInput,
            content = _uiState.value.messageContentInput,
            label = _uiState.value.selectedMessage?.label
        )
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            try {
                repository.saveMessage(newMessage)
                val updatedMessages = repository.getMessagesByTopic(newMessage.topic)
                _uiState.value = _uiState.value.copy(messages = updatedMessages, isSaving = false, selectedMessage = newMessage)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, showErrorDialog = true, errorMessage = e.message)
            }
        }
    }

    fun publishMessage() {
        val msg = _uiState.value.selectedMessage ?: return
        repository.publishCustomRawMessage(msg.topic.toString(), msg.type, msg.content)
    }

    fun deleteMessage(message: RosMessage) {
        //no-op not currently supported
    }

    fun showSavedButtons(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSavedButtons = show)
    }

    fun dismissErrorDialog() {
        _uiState.value = _uiState.value.copy(showErrorDialog = false, errorMessage = null)
    }
}