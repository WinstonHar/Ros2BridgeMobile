package com.example.testros2jsbridge

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testros2jsbridge.RosbridgeConnectionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/*
    RosViewModel manages ROS2 connection state, message publishing, topic advertisement, and UI state for the app.
    Acts as the main ViewModel for all ROS2-related operations, including message history and dropdown selection.
*/

class RosViewModel : ViewModel(), RosbridgeConnectionManager.Listener {
    /*
        input:    topicName - String, messageType - String, rawJson - String
        output:   None
        remarks:  Publishes a raw JSON message to the given topic and type, with special handling for std_msgs/String and std_msgs/Bool.
    */
    fun publishCustomRawMessage(topicName: String, messageType: String, rawJson: String) {
        viewModelScope.launch {
            if (!RosbridgeConnectionManager.isConnected) {
                Log.w("RosViewModel", "Cannot publish: Not connected.")
                return@launch
            }
            try {
                val msgField = when {
                    messageType == "std_msgs/msg/String" -> buildJsonObject { put("data", rawJson) }
                    messageType == "std_msgs/msg/Bool" -> {
                        val asJson = try { Json.parseToJsonElement(rawJson) } catch (_: Exception) { null }
                        val boolVal = if (asJson is kotlinx.serialization.json.JsonObject && asJson["data"] != null) {
                            val dataVal = asJson["data"]
                            if (dataVal is kotlinx.serialization.json.JsonPrimitive) dataVal.booleanOrNull ?: false else false
                        } else false
                        buildJsonObject { put("data", boolVal) }
                    }
                    else -> Json.parseToJsonElement(rawJson)
                }
                val jsonObject = buildJsonObject {
                    put("op", "publish")
                    put("id", "publish_${topicName.replace("/", "_")}_${System.currentTimeMillis()}")
                    put("topic", topicName)
                    put("msg", msgField)
                    put("latch", false)
                }
                val jsonString = jsonObject.toString()
                RosbridgeConnectionManager.sendRaw(jsonString)
                Log.d("RosViewModel", "Sent Publish to '$topicName': $jsonString")
                _customMessageHistory.update { currentHistory -> (currentHistory + jsonString).takeLast(100) }
            } catch (e: Exception) {
                Log.e("RosViewModel", "Publish error for topic '$topicName' (raw)", e)
            }
        }
    }

    data class CustomPublisher(val topic: String, val messageType: String, val message: String)

    private val _customPublishers = MutableStateFlow<List<CustomPublisher>>(emptyList())
    val customPublishers: StateFlow<List<CustomPublisher>> = _customPublishers

    /*
        input:    publisher - CustomPublisher
        output:   None
        remarks:  Adds a custom publisher to the list.
    */
    fun addCustomPublisher(publisher: CustomPublisher) {
        _customPublishers.value = _customPublishers.value + publisher
    }

    /*
        input:    list - List<CustomPublisher>
        output:   None
        remarks:  Sets the list of custom publishers.
    */
    fun setCustomPublishers(list: List<CustomPublisher>) {
        _customPublishers.value = list
    }

    private val _rosMessages = MutableSharedFlow<String>()
    val rosMessages: SharedFlow<String> = _rosMessages.asSharedFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _customMessage = MutableStateFlow("Default message")
    val customMessage: StateFlow<String> = _customMessage.asStateFlow()

    /*
        input:    None
        output:   None
        remarks:  Clears the current custom message text field.
    */
    fun clearCustomMessage() {
        _customMessage.value = ""
    }

    private val _customMessageHistory = MutableStateFlow<List<String>>(emptyList())
    val customMessageHistory: StateFlow<List<String>> = _customMessageHistory.asStateFlow()

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    data class RosBridgeAdvertise(
        val op: String = "advertise",
        val id: String = "advertise_${System.currentTimeMillis()}",
        val topic: String,
        val type: String,
        val latch: Boolean? = false,
        val queue_size: Int? = 100
    )

    @Serializable
    data class StdString(val data: String)

    /*
        input:    newMessage - String
        output:   None
        remarks:  Updates the custom message text field value.
    */
    fun onCustomMessageChange(newMessage: String) {
        _customMessage.value = newMessage
    }

    /*
        input:    ipAddress - String, port - Int (default 9090)
        output:   None
        remarks:  Initiates connection to rosbridge and updates connection status.
    */
    fun connect(ipAddress: String, port: Int = 9090) {
        _connectionStatus.value = "Connecting..."
        RosbridgeConnectionManager.addListener(this)
        RosbridgeConnectionManager.connect(ipAddress, port)
    }

    private val _selectedDropdownIndex = MutableStateFlow(0) // Default to first item
        val selectedDropdownIndex: StateFlow<Int> = _selectedDropdownIndex

    /*
        input:    index - Int
        output:   None
        remarks:  Sets the selected dropdown index for UI navigation.
    */
    fun selectDropdownIndex(index: Int) {
        _selectedDropdownIndex.value = index
    }

    /*
        input:    None
        output:   None
        remarks:  Disconnects from rosbridge and updates connection status.
    */
    fun disconnect() {
        RosbridgeConnectionManager.disconnect()
        _connectionStatus.value = "Disconnected (Client)"
        RosbridgeConnectionManager.removeListener(this)
    }

    /*
        input:    topicName - String, messageType - String
        output:   None
        remarks:  Advertises a topic with the given type to rosbridge.
    */
    fun advertiseTopic(topicName: String, messageType: String) {
        viewModelScope.launch {
            if (!RosbridgeConnectionManager.isConnected) {
                Log.w("RosViewModel", "Cannot advertise: Not connected.")
                return@launch
            }
            try {
                val advertiseMessage = RosBridgeAdvertise(topic = topicName, type = messageType)
                val jsonString = json.encodeToString(advertiseMessage)
                Log.d("RosViewModel", "Sent Advertise: $jsonString for topic '$topicName' type '$messageType'")
                RosbridgeConnectionManager.sendRaw(jsonString)
            } catch (e: Exception) {
                Log.e("RosViewModel", "Advertise error for topic '$topicName'", e)
            }
        }
    }

    /*
        input:    topicName - String, message - T, serializer - KSerializer<T>
        output:   None
        remarks:  Publishes a strongly-typed message to the given topic using the provided serializer.
    */
    private fun <T> publishMessage(topicName: String, message: T, serializer: KSerializer<T>) {
        viewModelScope.launch {
            if (!RosbridgeConnectionManager.isConnected) {
                Log.w("RosViewModel", "Cannot publish: Not connected.")
                return@launch
            }
            try {
                val jsonObject = buildJsonObject {
                    put("op", "publish")
                    put("id", "publish_${topicName.replace("/", "_")}_${System.currentTimeMillis()}")
                    put("topic", topicName)
                    put("msg", Json.encodeToJsonElement(serializer, message))
                    put("latch", false)
                }
                val jsonString = jsonObject.toString()
                RosbridgeConnectionManager.sendRaw(jsonString)
                Log.d("RosViewModel", "Sent Publish to '$topicName': $jsonString")
            } catch (e: Exception) {
                Log.e("RosViewModel", "Publish error for topic '$topicName'", e)
            }
        }
    }

    /*
        input:    topicName - String, text - String
        output:   None
        remarks:  Publishes a std_msgs/String message to the given topic.
    */
    private fun publishStringMessage(topicName: String, text: String) {
        publishMessage(topicName, StdString(text), StdString.serializer())
    }

    /*
        input:    topicName - String
        output:   None
        remarks:  Publishes the current custom message from the text field to the given topic, updating message history.
    */
    fun publishCustomMessageFromTextField(topicName: String) {
        val messageToSend = _customMessage.value
        // Try to parse as JSON, fallback to string
        val asJson = try {
            Json.parseToJsonElement(messageToSend)
        } catch (_: Exception) {
            null
        }
        // --- Update message history: store the JSON string of the sent message ---
        val jsonString = if (asJson != null) asJson.toString() else messageToSend
        _customMessageHistory.update { currentHistory -> (currentHistory + jsonString).takeLast(100) }
        // --- End message history update ---
        publishStringMessage(topicName, messageToSend)
    }

    /*
        input:    None
        output:   None
        remarks:  Called when the ViewModel is cleared; disconnects and removes listener.
    */
    override fun onCleared() {
        super.onCleared()
        Log.d("RosViewModel", "onCleared called, disconnecting if active.")
        disconnect()
        RosbridgeConnectionManager.removeListener(this)
    }

    // RosbridgeConnectionManager.Listener implementation

    /*
        input:    None
        output:   None
        remarks:  Called when rosbridge connection is established; updates connection status.
    */
    override fun onConnected() {
        _connectionStatus.value = "Connected"
    }

    /*
        input:    None
        output:   None
        remarks:  Called when rosbridge connection is lost; updates connection status.
    */
    override fun onDisconnected() {
        _connectionStatus.value = "Disconnected"
    }

    /*
        input:    text - String
        output:   None
        remarks:  Called when a message is received from rosbridge; emits to rosMessages flow.
    */
    override fun onMessage(text: String) {
        viewModelScope.launch {
            _rosMessages.emit(text)
        }
    }

    /*
        input:    error - String
        output:   None
        remarks:  Called when an error occurs on rosbridge connection; updates connection status.
    */
    override fun onError(error: String) {
        _connectionStatus.value = "Error: $error"
    }
}