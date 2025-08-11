package com.examples.testros2jsbridge.presentation.ui.screens.publisher

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.core.ros.RosBridgeViewModel
import com.examples.testros2jsbridge.core.util.Logger
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.Publisher
import com.examples.testros2jsbridge.presentation.state.PublisherUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PublisherViewModel @Inject constructor(
    private val rosMessageRepository: com.examples.testros2jsbridge.domain.repository.RosMessageRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PublisherUiState())
    val uiState: StateFlow<PublisherUiState> = _uiState

    init {
        viewModelScope.launch {
            rosMessageRepository.messages.collect { messages ->
                // Only show messages with a label (i.e., user-saved publishers or geometry messages)
                _uiState.value = _uiState.value.copy(publishers = messages.map { it.toPublisher() })
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
        val rosMsg = RosMessageDto(
            op = "publish",
            topic = RosId(topic),
            type = type,
            content = content,
            label = topic,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            rosMessageRepository.saveMessage(rosMsg)
            _uiState.value = _uiState.value.copy(isSaving = true)
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    fun createPublisher() {
        savePublisher()
    }

    fun deletePublisher(publisher: com.examples.testros2jsbridge.domain.model.Publisher) {
        viewModelScope.launch {
            val msg = rosMessageRepository.messages.value.find {
                // Prefer unique ID if available
                (publisher.id != null && it.id == publisher.id.value) ||
                // Fallback: match all key fields
                (it.topic.value == publisher.topic.value &&
                 it.type == publisher.messageType &&
                 it.content == publisher.message &&
                 it.label == publisher.label)
            }
            if (msg != null) rosMessageRepository.deleteMessage(msg)
        }
    }

    fun publishMessage(rosBridgeViewModel: com.examples.testros2jsbridge.core.ros.RosBridgeViewModel) {
        val publisher = _uiState.value.selectedPublisher ?: return
        // ADVERTISE before publish
        rosBridgeViewModel.advertiseTopic(publisher.topic.value, publisher.messageType)
        // Delegate to RosBridgeViewModel for actual network publish
        rosBridgeViewModel.publishMessage(
            topic = publisher.topic.value,
            type = publisher.messageType,
            rawJson = publisher.message
        )
        appendToMessageHistory("Published to ${publisher.topic.value}: ${publisher.message}")
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
                "Char" -> {
                    val charValue = userInput.firstOrNull()?.code ?: 0
                    "{\"data\": $charValue}"
                }
                "Byte", "Int8", "UInt8" -> {
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
        val rosMsg = RosMessageDto(
            op = "publish",
            topic = RosId(topic),
            type = "std_msgs/msg/$type",
            content = jsonMsg,
            label = topic,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            rosMessageRepository.saveMessage(rosMsg)
            _uiState.value = _uiState.value.copy(selectedPublisher = rosMsg.toPublisher())
        }
    }

    // Helper to map RosMessageDto to Publisher
    private fun RosMessageDto.toPublisher(): com.examples.testros2jsbridge.domain.model.Publisher {
        return com.examples.testros2jsbridge.domain.model.Publisher(
            id = this.id?.let { RosId(it) },
            topic = this.topic,
            messageType = this.type ?: "",
            message = this.content ?: "",
            label = this.label,
            isEnabled = true,
            lastPublishedTimestamp = this.timestamp
        )
    }

    fun updatePublisher(updated: Publisher) {
        viewModelScope.launch {
            // Always use the original id for lookup, even if topic/label changed
            val orig = rosMessageRepository.messages.value.find {
                it.id != null && it.id == updated.id?.value
            }
            if (orig != null) {
                // Overwrite with updated fields
                val newMsg = orig.copy(
                    topic = updated.topic,
                    type = updated.messageType,
                    content = updated.message,
                    label = updated.label,
                    timestamp = System.currentTimeMillis()
                )
                rosMessageRepository.saveMessage(newMsg)
            }
        }
    }
}