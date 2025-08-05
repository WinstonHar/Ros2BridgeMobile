package com.examples.testros2jsbridge.presentation.ui.screens.subscriber

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.presentation.state.SubscriberUiState
import com.examples.testros2jsbridge.domain.model.Subscriber
import com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao
import com.examples.testros2jsbridge.presentation.mapper.SubscriberUiMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ViewModel for managing ROS2 topic subscriptions, topic discovery, and message updates.
 * Replicates legacy Ros2TopicSubscriberActivity functionality using modular codebase components.
 */
class SubscriberViewModel(
    private val subscriberDao: SubscriberDao,
    private val publisherDao: PublisherDao,
    private val controllerDao: ControllerDao,
    private val rosMessageRepository: RosMessageRepository,
    private val rosTopicRepository: RosTopicRepository,
    private val rosServiceRepository: RosServiceRepository,
    private val rosActionRepository: RosActionRepository,
    private val connectionManager: ConnectionManager,
    private val errorHandler: ErrorHandler
) : ViewModel() {
    private val _uiState = MutableStateFlow(SubscriberUiState())
    val uiState: StateFlow<SubscriberUiState> get() = _uiState

    // For topic discovery and available topics
    private val _availableTopics = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val availableTopics: StateFlow<List<Pair<String, String>>> get() = _availableTopics

    // For auto-refresh job
    private var autoRefreshJob: kotlinx.coroutines.Job? = null

    // For IP/port persistence
    private val prefsKeyIp = "rosbridge_ip"
    private val prefsKeyPort = "rosbridge_port"

    // For image topic display
    private val _imageBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val imageBitmap: StateFlow<android.graphics.Bitmap?> get() = _imageBitmap

    init {
        // Observe subscribers from DAO
        viewModelScope.launch {
            subscriberDao.subscribers.collect { subs ->
                _uiState.update { it.copy(subscribers = subs) }
            }
        }
        // Observe available topics from repository
        viewModelScope.launch {
            rosTopicRepository.availableTopics.collect { topics ->
                _availableTopics.value = topics
            }
        }
        // Optionally, start auto-refresh for topic discovery
        startAutoRefreshTopics()
    }

    fun updateIp(ip: String) {
        _uiState.update { it.copy(ipInput = ip) }
        saveIpToPrefs(ip)
    }

    fun updatePort(port: String) {
        _uiState.update { it.copy(portInput = port) }
        savePortToPrefs(port)
    }

    private fun saveIpToPrefs(ip: String) {
        // Use modular persistence (e.g., via repository or context)
        // ...existing code...
    }

    private fun savePortToPrefs(port: String) {
        // Use modular persistence (e.g., via repository or context)
        // ...existing code...
    }

    fun loadIpPortFromPrefs() {
        // Use modular persistence (e.g., via repository or context)
        // ...existing code...
    }

    fun fetchAvailableTopics() {
        viewModelScope.launch {
            rosTopicRepository.refreshTopics()
        }
    }

    private fun startAutoRefreshTopics() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                fetchAvailableTopics()
                kotlinx.coroutines.delay(3000)
                autoUnsubscribeMissingTopics()
            }
        }
    }

    private fun autoUnsubscribeMissingTopics() {
        val currentTopics = _availableTopics.value.map { it.first }
        val subs = _uiState.value.subscribers
        val missing = subs.filter { it.topic.value !in currentTopics }
        missing.forEach { unsubscribeFromTopic(it) }
    }

    fun onTopicCheckboxChanged(topic: String, type: String, isChecked: Boolean) {
        if (isChecked) {
            _uiState.update { it.copy(topicInput = topic, typeInput = type) }
            subscribeToTopic()
        } else {
            val sub = _uiState.value.subscribers.find { it.topic.value == topic }
            if (sub != null) unsubscribeFromTopic(sub)
        }
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
                // Build and save subscriber
                val subscriber = Subscriber(
                    topic = com.examples.testros2jsbridge.domain.model.RosId(topic),
                    type = type,
                    label = label,
                    lastMessage = "",
                    timestamp = System.currentTimeMillis()
                )
                subscriberDao.insert(subscriber)
                rosMessageRepository.subscribeToTopic(topic, type) { msg ->
                    onMessageReceived(topic, msg)
                }
                _uiState.update { it.copy(isSubscribing = false, showAddSubscriberDialog = false, topicInput = "", typeInput = "", labelInput = "") }
            } catch (e: Exception) {
                showError(e.message ?: "Failed to subscribe.")
                _uiState.update { it.copy(isSubscribing = false) }
            }
        }
    }

    fun unsubscribeFromTopic(subscriber: Subscriber) {
        viewModelScope.launch {
            try {
                rosMessageRepository.unsubscribeFromTopic(subscriber.topic.value)
                subscriberDao.delete(subscriber)
            } catch (e: Exception) {
                showError(e.message ?: "Failed to unsubscribe.")
            }
        }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message, showErrorDialog = true) }
    }

    fun dismissErrorDialog() {
        _uiState.update { it.copy(showErrorDialog = false) }
    }

    // Listener for incoming messages (to be registered with ConnectionManager)
    fun onMessageReceived(topic: String, message: String) {
        // Update lastMessage for the subscriber
        val subs = _uiState.value.subscribers
        val updatedSubs = subs.map {
            if (it.topic.value == topic) it.copy(lastMessage = message, timestamp = System.currentTimeMillis()) else it
        }
        // Update message history log using modular repository
        val history = _uiState.value.messageHistory.toMutableList()
        val formattedMsg = "[$topic] $message"
        history.add(formattedMsg)
        // Limit log size for performance (similar to legacy maxLogLines)
        val maxLogLines = 300
        val trimmedHistory = if (history.size > maxLogLines) history.takeLast(maxLogLines) else history
        _uiState.update { it.copy(subscribers = updatedSubs, messageHistory = trimmedHistory) }

        // If image topic, decode and update bitmap using repository utility
        if (isImageTopic(topic)) {
            val bitmap = decodeImageMessage(message)
            _imageBitmap.value = bitmap
        }
        // Scroll log to top (handled in UI via StateFlow update)
    }

    private fun isImageTopic(topic: String): Boolean {
        // Use modular topic/type detection if available
        return topic.contains("/image") || topic.contains("/camera")
    }

    private fun decodeImageMessage(message: String): android.graphics.Bitmap? {
        // Use modular image decoding from RosMessageRepository or RosViewModel if available
        // Example: decode base64 or raw bytes from message
        return rosMessageRepository.decodeImageMessage(message)
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
                // Build and save subscriber
                val subscriber = Subscriber(
                    topic = com.examples.testros2jsbridge.domain.model.RosId(topic),
                    type = type,
                    label = label,
                    isActive = true,
                    isEnabled = true
                )
                subscriberDao.saveSubscriber(subscriber)
                // Use modular connection manager to subscribe
                connectionManager.subscribe(topic, type) { receivedMsg ->
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
                connectionManager.unsubscribe(subscriber.topic.value)
                subscriberDao.deleteSubscriber(subscriber.topic)
            } catch (e: Exception) {
                showError("Failed to unsubscribe: ${e.message}")
            }
        }
    }

    fun fetchAvailableTopics() {
        viewModelScope.launch {
            try {
                rosTopicRepository.refreshTopics()
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
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(showErrorDialog = true, errorMessage = message) }
    }

    fun dismissErrorDialog() {
        _uiState.update { it.copy(showErrorDialog = false, errorMessage = null) }
    }

    // Listener for incoming messages (to be registered with ConnectionManager)
    fun onMessageReceived(topic: String, message: String) {
        // Update lastMessage for the subscriber
        val subs = _uiState.value.subscribers
        val updatedSubs = subs.map {
            if (it.topic.value == topic) it.copy(lastMessage = message, timestamp = System.currentTimeMillis()) else it
        }
        // Update message history log using modular repository
        val history = _uiState.value.messageHistory.toMutableList()
        val formattedMsg = "[$topic] $message"
        history.add(formattedMsg)
        // Limit log size for performance (similar to legacy maxLogLines)
        val maxLogLines = 300
        val trimmedHistory = if (history.size > maxLogLines) history.takeLast(maxLogLines) else history
        _uiState.update { it.copy(subscribers = updatedSubs, messageHistory = trimmedHistory) }
    }

    // For showing subscriber history
    fun showSubscriberHistory(show: Boolean) {
        _uiState.update { it.copy(showSubscriberHistory = show) }
    }
}