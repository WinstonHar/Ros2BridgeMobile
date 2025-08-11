package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.presentation.state.PublisherUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
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

        private fun buildStdMsgJson(messageType: String, userInput: String): String {
            return when (messageType) {
                "Bool" -> {
                    val boolValue = when (userInput.trim().lowercase()) {
                        "true", "1" -> true
                        "false", "0" -> false
                        else -> false
                    }
                    "{\"data\": $boolValue}"
                }
                "Byte", "Char", "Int8", "UInt8" -> {
                    val num = userInput.toIntOrNull() ?: 0
                    "{\"data\": $num}"
                }
                "Int16", "UInt16" -> {
                    val num = userInput.toIntOrNull() ?: 0
                    "{\"data\": $num}"
                }
                "Int32", "UInt32" -> {
                    val num = userInput.toLongOrNull() ?: 0L
                    "{\"data\": $num}"
                }
                "Int64", "UInt64" -> {
                    val num = userInput.toLongOrNull() ?: 0L
                    "{\"data\": $num}"
                }
                "Float32", "Float64" -> {
                    val num = userInput.toDoubleOrNull() ?: 0.0
                    "{\"data\": $num}"
                }
                "String" -> {
                    "{\"data\": \"${userInput.replace("\"", "\\\"")}\"}"
                }
                "ColorRGBA" -> {
                    val parts = userInput.split(",").map { it.trim().toFloatOrNull() ?: 0f }
                    val (r, g, b, a) = (parts + List(4) { 0f }).take(4)
                    "{\"r\":$r,\"g\":$g,\"b\":$b,\"a\":$a}"
                }
                "Empty" -> {
                    "{}"
                }
                else -> {
                    "{\"data\": \"${userInput.replace("\"", "\\\"")}\"}"
                }
            }
    }

    fun createStandardPublisher() {
        val topic = _uiState.value.topicInput
        val type = _uiState.value.typeInput
        val userInput = _uiState.value.messageContentInput
        val jsonMsg = buildStdMsgJson(type, userInput)
        val newId = UUID.randomUUID().toString()
        val publisher = com.examples.testros2jsbridge.domain.model.Publisher(
            id = com.examples.testros2jsbridge.domain.model.RosId(newId),
            topic = com.examples.testros2jsbridge.domain.model.RosId(topic),
            messageType = "std_msgs/msg/$type",
            message = jsonMsg,
            label = topic,
            isEnabled = true,
            lastPublishedTimestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            val exists = _uiState.value.publishers.any { it.id?.value == newId }
            if (exists) {
                Logger.e("PublisherViewModel", "Duplicate publisher ID generated: $newId")
                try {
                    val app = publisherRepository as? Application
                    Toast.makeText(app, "Duplicate publisher ID generated. Not saving.", Toast.LENGTH_LONG).show()
                } catch (_: Exception) {}
                return@launch
            }
            publisherRepository.createPublisher(publisher)
            _uiState.value = _uiState.value.copy(selectedPublisher = publisher)
        }
    }
}