package com.examples.testros2jsbridge.presentation.ui.screens.subscriber

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.core.error.ErrorHandler
import com.examples.testros2jsbridge.core.network.ConnectionManager
import com.examples.testros2jsbridge.data.local.database.dao.ConnectionDao
import com.examples.testros2jsbridge.presentation.state.SubscriberUiState
import com.examples.testros2jsbridge.domain.model.Subscriber
import com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.repository.RosActionRepository
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository
import com.examples.testros2jsbridge.domain.repository.RosServiceRepository
import com.examples.testros2jsbridge.domain.repository.RosTopicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * ViewModel for managing ROS2 topic subscriptions, topic discovery, and message updates.
 * Replicates legacy Ros2TopicSubscriberActivity functionality using modular codebase components.
 */

@HiltViewModel
class SubscriberViewModel @Inject constructor(
    private val subscriberDao: SubscriberDao,
    private val connectionDao: ConnectionDao,
    private val rosMessageRepository: RosMessageRepository,
    private val rosTopicRepository: RosTopicRepository,
    private val rosServiceRepository: RosServiceRepository,
    private val rosActionRepository: RosActionRepository,
    private val connectionManager: ConnectionManager,
    private val subscriberRepository: com.examples.testros2jsbridge.domain.repository.SubscriberRepository,
    private val errorHandler: ErrorHandler
) : ViewModel() {
    private val _uiState = MutableStateFlow(SubscriberUiState())
    val uiState: StateFlow<SubscriberUiState> get() = _uiState

    private val _availableTopics = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val availableTopics: StateFlow<List<Pair<String, String>>> get() = _availableTopics

    private var autoRefreshJob: Job? = null

    init {
        // Observe subscribers from DAO
        viewModelScope.launch {
            subscriberDao.getAllSubscribersFlow().collect { entities ->
                val subs = entities.map { entity ->
                    Subscriber(
                        id = entity.id,
                        topic = RosId(entity.topic),
                        type = entity.type,
                        isActive = entity.isActive,
                        lastMessage = entity.lastMessage,
                        label = entity.label,
                        isEnabled = entity.isEnabled,
                        group = entity.group,
                        timestamp = entity.timestamp,
                        messageHistory = emptyList<String>() // or appropriate default
                    )
                }
                _uiState.update { it.copy(subscribers = subs) }
            }
        }
        // Observe available topics from repository
        viewModelScope.launch {
            rosTopicRepository.subscribedTopics.collect { topics ->
                _availableTopics.value = topics.map { it.name to it.type}
            }
        }
        startAutoRefreshTopics()
    }

    fun updateTopicInput(topic: String) {
        _uiState.update { it.copy(topicInput = topic) }
    }

    fun updateTypeInput(type: String) {
        _uiState.update { it.copy(typeInput = type) }
    }

    fun updateLabelInput(label: String) {
        _uiState.update { it.copy(labelInput = label) }
    }

    fun showAddSubscriberDialog(show: Boolean) {
        _uiState.update { it.copy(showAddSubscriberDialog = show) }
    }

    fun selectSubscriber(subscriber: Subscriber?) {
        _uiState.update { it.copy(selectedSubscriber = subscriber) }
    }

    fun subscribeToTopic() {
        val topic = _uiState.value.topicInput.trim()
        val type = _uiState.value.typeInput.trim()
        val label = _uiState.value.labelInput.trim().ifEmpty { topic }
        if (topic.isEmpty() || type.isEmpty()) {
            showError("Topic and type are required.")
            return
        }
        _uiState.update { it.copy(isSubscribing = true) }
        viewModelScope.launch {
            try {
                subscriberRepository.subscribeToTopic(topic, type, label) { receivedMsg ->
                    onMessageReceived(topic, receivedMsg)
                }
                _uiState.update { it.copy(isSubscribing = false, showAddSubscriberDialog = false, topicInput = "", typeInput = "", labelInput = "") }
            } catch (e: Exception) {
                showError("Failed to subscribe: ${e.message}")
                _uiState.update { it.copy(isSubscribing = false) }
            }
        }
    }

    fun unsubscribeFromTopic(subscriber: Subscriber) {
        viewModelScope.launch {
            try {
                subscriberRepository.unsubscribeFromTopic(subscriber.topic.value)
            } catch (e: Exception) {
                showError("Failed to unsubscribe: ${e.message}")
            }
        }
    }

    fun fetchAvailableTopics() {
        viewModelScope.launch {
            try {
                rosTopicRepository.getTopics()
            } catch (e: Exception) {
                showError("Failed to fetch topics: ${e.message}")
            }
        }
    }

    private fun startAutoRefreshTopics() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                fetchAvailableTopics()
                delay(5000)
            }
        }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(showErrorDialog = true, errorMessage = message) }
    }

    fun dismissErrorDialog() {
        _uiState.update { it.copy(showErrorDialog = false, errorMessage = null) }
    }

    fun onMessageReceived(topic: String, message: String) {
        val subs = _uiState.value.subscribers
        val updatedSubs = subs.map {
            if (it.topic.value == topic) it.copy(lastMessage = message) else it
        }
        val history = _uiState.value.messageHistory + "[$topic] $message"
        val maxLogLines = 300
        val trimmedHistory = if (history.size > maxLogLines) history.takeLast(maxLogLines) else history
        _uiState.update { it.copy(subscribers = updatedSubs, messageHistory = trimmedHistory) }
    }

    fun showSubscriberHistory(show: Boolean) {
        _uiState.update { it.copy(showSubscriberHistory = show) }
    }
}