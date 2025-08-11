package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.usecase.publisher.CreatePublisherUseCase
import com.examples.testros2jsbridge.domain.usecase.publisher.DeletePublisherUseCase
import com.examples.testros2jsbridge.domain.usecase.publisher.GetPublisherUseCase
import com.examples.testros2jsbridge.domain.usecase.publisher.SavePublisherUseCase
import com.examples.testros2jsbridge.presentation.state.PublisherUiState
import com.examples.testros2jsbridge.presentation.mapper.PublisherUiMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PublisherViewModel @Inject constructor(
    private val publisherRepository: com.examples.testros2jsbridge.domain.repository.PublisherRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PublisherUiState())
    val uiState: StateFlow<PublisherUiState> = _uiState

    init {
        viewModelScope.launch {
            publisherRepository.publishers.collect { publishers ->
                _uiState.value = _uiState.value.copy(publishers = publishers)
            }
        }
    }

    /**
     * Appends a message to the message history in the UI state.
     */
    fun appendToMessageHistory(msg: String) {
        val truncated = if (msg.length > 300) msg.take(300) + "... [truncated]" else msg
        val newHistory = (_uiState.value.messageHistory + truncated).takeLast(25)
        _uiState.value = _uiState.value.copy(messageHistory = newHistory)
    }

    fun selectPublisher(publisher: Publisher) {
        _uiState.value = _uiState.value.copy(selectedPublisher = publisher)
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

    fun showAddPublisherDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddPublisherDialog = show)
    }

    fun showEditPublisherDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showEditPublisherDialog = show)
    }

    fun savePublisher() {
        val topic = _uiState.value.topicInput
        val type = _uiState.value.typeInput
        val content = _uiState.value.messageContentInput
        val publisher = Publisher(
            topic = RosId(topic),
            messageType = type,
            message = content,
            label = topic,
            isEnabled = true,
            lastPublishedTimestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            publisherRepository.savePublisher(publisher)
            _uiState.value = _uiState.value.copy(isSaving = true)
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    fun createPublisher() {
        val topic = _uiState.value.topicInput
        val type = _uiState.value.typeInput
        val content = _uiState.value.messageContentInput
        val publisher = Publisher(
            topic = RosId(topic),
            messageType = type,
            message = content,
            label = topic,
            isEnabled = true,
            lastPublishedTimestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            publisherRepository.createPublisher(publisher)
        }
    }

    fun deletePublisher(publisher: Publisher) {
        viewModelScope.launch {
            publisherRepository.deletePublisher(publisher.topic)
        }
    }

    fun publishMessage() {
        val publisher = _uiState.value.selectedPublisher ?: return
        viewModelScope.launch {
            val updatedPublisher = publisher.copy(lastPublishedTimestamp = System.currentTimeMillis())
            publisherRepository.savePublisher(updatedPublisher)
            appendToMessageHistory("Published to ${publisher.topic.value}: ${publisher.message}")
        }
    }

    fun showErrorDialog(message: String) {
        _uiState.value = _uiState.value.copy(showErrorDialog = true, errorMessage = message)
    }

    fun dismissErrorDialog() {
        _uiState.value = _uiState.value.copy(showErrorDialog = false, errorMessage = null)
    }

    fun clearPublisherInputFields() {
        _uiState.value = _uiState.value.copy(
            topicInput = "",
            typeInput = "",
            messageContentInput = ""
        )
    }
}