package com.example.testros2jsbridge

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/*
    Ros2TopicSubscriberActivity provides a UI for subscribing to ROS2 topics via rosbridge, supporting both dynamic topic discovery and manual subscription.
    Handles connection management, topic/type selection, message display, and auto-refresh of available topics.
*/

class Ros2TopicSubscriberActivity : AppCompatActivity() {
    // Stop image decode coroutine and drain channel
    private fun stopImageProcessing() {
        imageProcessorJob?.cancel()
        imageProcessorJob = null
        while (imageDecodeChannel.tryReceive().isSuccess) { /* discard */ }
    }
    private var imageLogCounter = 0
    private val imageDecodeChannel = Channel<String>(Channel.UNLIMITED)
    private var imageProcessorJob: Job? = null
    /*
        input:    subs - List<Pair<String, String>>
        output:   Boolean
        remarks:  Checks if any image topic is subscribed.
    */
    private fun isImageTopicSubscribed(subs: List<Pair<String, String>>): Boolean {
        return subs.any { it.second == "sensor_msgs/msg/Image" || it.second == "sensor_msgs/msg/CompressedImage" }
    }
    
    private var autoRefreshJob: Job? = null
    private val rosViewModel: RosViewModel by lazy {
        val app = application as MyApp
        androidx.lifecycle.ViewModelProvider(
            app.appViewModelStore,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )[RosViewModel::class.java]
    }
    private val logBuffer = StringBuilder()
    private var logDirty = false
    private val maxLogLines = 300
    private var logUpdateJob: Job? = null
    private var rosbridgeListener: RosbridgeConnectionManager.Listener? = null
    private lateinit var logView: TextView
    private lateinit var logScrollView: ScrollView
    private lateinit var backToMainButton: Button
    private lateinit var ipEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var autoRefreshCheckBox: android.widget.CheckBox
    private lateinit var manualTopicEditText: EditText
    private lateinit var manualTypeEditText: EditText
    private lateinit var subscribeManualButton: Button
    // Compose image state (bitmap now in ViewModel)
    private val showImageState = androidx.compose.runtime.mutableStateOf(true)

    private val supportedTypes = setOf(
        "std_msgs/msg/String", "std_msgs/msg/Int32", "geometry_msgs/msg/Twist", "geometry_msgs/msg/PoseStamped",
        "sensor_msgs/msg/BatteryState", "sensor_msgs/msg/CameraInfo", "sensor_msgs/msg/ChannelFloat32", "sensor_msgs/msg/CompressedImage", "sensor_msgs/msg/FluidPressure", "sensor_msgs/msg/Illuminance", "sensor_msgs/msg/Image", "sensor_msgs/msg/Imu", "sensor_msgs/msg/JointState", "sensor_msgs/msg/Joy", "sensor_msgs/msg/JoyFeedback", "sensor_msgs/msg/JoyFeedbackArray", "sensor_msgs/msg/LaserEcho", "sensor_msgs/msg/LaserScan", "sensor_msgs/msg/MagneticField", "sensor_msgs/msg/MultiDOFJointState", "sensor_msgs/msg/MultiEchoLaserScan", "sensor_msgs/msg/NavSatFix", "sensor_msgs/msg/NavSatStatus", "sensor_msgs/msg/PointCloud", "sensor_msgs/msg/PointCloud2", "sensor_msgs/msg/PointField", "sensor_msgs/msg/Range", "sensor_msgs/msg/RegionOfInterest", "sensor_msgs/msg/RelativeHumidity", "sensor_msgs/msg/Temperature", "sensor_msgs/msg/TimeReference",
        "geometry_msgs/msg/Accel", "geometry_msgs/msg/AccelStamped", "geometry_msgs/msg/AccelWithCovariance", "geometry_msgs/msg/AccelWithCovarianceStamped", "geometry_msgs/msg/Inertia", "geometry_msgs/msg/InertiaStamped", "geometry_msgs/msg/Point", "geometry_msgs/msg/Point32", "geometry_msgs/msg/PointStamped", "geometry_msgs/msg/Polygon", "geometry_msgs/msg/PolygonInstance", "geometry_msgs/msg/PolygonInstanceStamped", "geometry_msgs/msg/PolygonStamped", "geometry_msgs/msg/Pose", "geometry_msgs/msg/Pose2D", "geometry_msgs/msg/PoseArray", "geometry_msgs/msg/PoseStamped", "geometry_msgs/msg/PoseWithCovariance", "geometry_msgs/msg/PoseWithCovarianceStamped", "geometry_msgs/msg/Quaternion", "geometry_msgs/msg/QuaternionStamped", "geometry_msgs/msg/Transform", "geometry_msgs/msg/TransformStamped", "geometry_msgs/msg/Twist", "geometry_msgs/msg/TwistStamped", "geometry_msgs/msg/TwistWithCovariance", "geometry_msgs/msg/TwistWithCovarianceStamped", "geometry_msgs/msg/Vector3", "geometry_msgs/msg/Vector3Stamped", "geometry_msgs/msg/VelocityStamped", "geometry_msgs/msg/Wrench", "geometry_msgs/msg/WrenchStamped",
        "std_msgs/msg/Bool", "std_msgs/msg/Byte", "std_msgs/msg/ByteMultiArray", "std_msgs/msg/Char", "std_msgs/msg/ColorRGBA", "std_msgs/msg/Empty", "std_msgs/msg/Float32", "std_msgs/msg/Float32MultiArray", "std_msgs/msg/Float64", "std_msgs/msg/Float64MultiArray", "std_msgs/msg/Header", "std_msgs/msg/Int16", "std_msgs/msg/Int16MultiArray", "std_msgs/msg/Int32MultiArray", "std_msgs/msg/Int64", "std_msgs/msg/Int64MultiArray", "std_msgs/msg/Int8", "std_msgs/msg/Int8MultiArray", "std_msgs/msg/MultiArrayDimension", "std_msgs/msg/MultiArrayLayout", "std_msgs/msg/String", "std_msgs/msg/UInt16", "std_msgs/msg/UInt16MultiArray", "std_msgs/msg/UInt32", "std_msgs/msg/UInt32MultiArray", "std_msgs/msg/UInt64", "std_msgs/msg/UInt64MultiArray", "std_msgs/msg/UInt8", "std_msgs/msg/UInt8MultiArray"
    )
    private var discoveredTopics: List<Pair<String, String>> = emptyList()
    private var topicAdapter: TopicCheckboxAdapter? = null

    /*
        input:    data - ByteArray, width - Int, height - Int
        output:   Bitmap
        remarks:  Decodes BGR8 image data to a Bitmap.
    */
    private fun decodeBgr8Base64ToBitmap(base64: String, width: Int, height: Int): android.graphics.Bitmap {
        val bitmap = createBitmap(width, height)
        ImageUtils.bgrBase64ToArgb(base64, bitmap, width, height)
        return bitmap
    }

    /*
        input:    savedInstanceState - Bundle?
        output:   None
        remarks:  Initializes the UI, sets up listeners, manages connection, topic discovery, and subscription logic.
    */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchImageProcessor()
        setContentView(R.layout.activity_ros2_topic_subscriber)

        logView = findViewById(R.id.logView)
        logScrollView = findViewById(R.id.logScrollView)
        // imageView removed, now handled by ComposeView
        backToMainButton = findViewById(R.id.button_back_to_main)
        ipEditText = findViewById(R.id.edittext_ip_address_sub)
        portEditText = findViewById(R.id.edittext_port_sub)
        connectButton = findViewById(R.id.button_connect_sub)
        disconnectButton = findViewById(R.id.button_disconnect_sub)

        autoRefreshCheckBox = findViewById(R.id.checkbox_auto_refresh)
        manualTopicEditText = findViewById(R.id.edittext_manual_topic)
        manualTypeEditText = findViewById(R.id.edittext_manual_type)
        subscribeManualButton = findViewById(R.id.button_subscribe_manual)

        val fetchTopicsButton: Button = findViewById(R.id.button_fetch_topics)
        val recyclerTopics = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_topics)

        // Compose image state: subscribe to topic changes and update ComposeView
        val imageComposeView = findViewById<androidx.compose.ui.platform.ComposeView>(R.id.image_compose_view)
        imageComposeView.setContent {
            val subsState = rosViewModel.subscribedTopics.collectAsState()
            val hasImage = isImageTopicSubscribed(subsState.value.toList())
            val bitmap by rosViewModel.latestBitmap.collectAsState()
            androidx.compose.runtime.LaunchedEffect(hasImage) {
                if (hasImage) showImageState.value = true
            }

            if (hasImage) {
                androidx.compose.foundation.layout.Column {
                    if (showImageState.value) {
                        androidx.compose.material3.Button(onClick = { showImageState.value = false }) {
                            androidx.compose.material3.Text("Hide Image")
                        }
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                android.widget.ImageView(ctx).apply {
                                    adjustViewBounds = true
                                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                }
                            },
                            update = { imageView ->
                                if (bitmap != null) {
                                    imageView.setImageBitmap(bitmap)
                                } else {
                                    imageView.setImageDrawable(null)
                                }
                            }
                        )
                        if (bitmap == null) {
                            androidx.compose.material3.Text("Waiting for image...", color = androidx.compose.ui.graphics.Color.Gray)
                        }

                    } else {
                        androidx.compose.material3.Button(onClick = { showImageState.value = true }) {
                            androidx.compose.material3.Text("Show Image")
                        }
                    }
                }
            }
        }
        logUpdateJob = lifecycleScope.launch {
            while (true) {
                if (logDirty) {
                    logView.text = logBuffer.toString()
                    logDirty = false
                    logScrollView.post {
                        logScrollView.fullScroll(android.view.View.FOCUS_UP)
                    }
                }
                delay(200)
            }
        }

        fun sendUnsubscribeToRosbridge(topic: String, type: String) {
            val obj = JSONObject()
            obj.put("op", "unsubscribe")
            obj.put("topic", topic)
            obj.put("type", type)
            RosbridgeConnectionManager.send(obj)
        }

        fun sendSubscribeToRosbridge(topic: String, type: String) {
            val subId = "subscribe_${topic.replace("/", "_")}_${System.currentTimeMillis()}"
            val obj = JSONObject()
            obj.put("op", "subscribe")
            obj.put("id", subId)
            obj.put("topic", topic)
            obj.put("type", type)
            obj.put("latch", false)
            obj.put("queue_size", 1)
            RosbridgeConnectionManager.send(obj)
        }

        /*
                    input:    topics - List<Pair<String, String>>, subscribed - List<Pair<String, String>>
                    output:   None
                    remarks:  Updates the topic adapter with current subscriptions.
                */
        fun updateTopicAdapterWithSubscriptions(topics: List<Pair<String, String>>, subscribed: List<Pair<String, String>>) {
            val subscribedSet = subscribed.map { it.first }.toSet()
            if (topicAdapter == null) {
                topicAdapter = TopicCheckboxAdapter(
                    topics = topics,
                    subscribedTopics = subscribedSet,
                    onCheckedChange = { topic, type, isChecked ->
                        if (isChecked) {
                            rosViewModel.addSubscribedTopic(topic, type)
                            sendSubscribeToRosbridge(topic, type)

                            logView.text = "Subscribed to $topic ($type)\n" + logView.text
                            if (type == "sensor_msgs/msg/Image" || type == "sensor_msgs/msg/CompressedImage") {
                                if (imageProcessorJob == null || !imageProcessorJob!!.isActive) {
                                    rosViewModel.latestBitmap.value = null 
                                    launchImageProcessor()
                                }
                            }
                        } else {
                            rosViewModel.removeSubscribedTopic(topic, type)
                            sendUnsubscribeToRosbridge(topic, type)

                            logView.text = "Unsubscribed from $topic\n" + logView.text
                            if (type == "sensor_msgs/msg/Image" || type == "sensor_msgs/msg/CompressedImage") {
                                stopImageProcessing()
                                rosViewModel.latestBitmap.value = null 
                            }
                        }
                    }
                )
                recyclerTopics.adapter = topicAdapter
                recyclerTopics.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            } else {
                topicAdapter!!.updateTopicsAndSubscribed(topics, subscribedSet)
            }
        }
        // Initial adapter setup
        updateTopicAdapterWithSubscriptions(discoveredTopics, rosViewModel.subscribedTopics.value.toList())

        // --- Auto-fetch topics and enable auto-refresh if connected on activity start ---
        if (RosbridgeConnectionManager.isConnected) {
            autoRefreshCheckBox.isChecked = true
            if (autoRefreshJob == null) {
                autoRefreshJob = lifecycleScope.launch {
                    while (autoRefreshCheckBox.isChecked) {
                        runOnUiThread { fetchTopicsButton.performClick() }
                        delay(1500)
                    }
                }
            }
            fetchTopicsButton.performClick()
        }

        lifecycleScope.launch {
            rosViewModel.subscribedTopics.collect { subs ->
                updateTopicAdapterWithSubscriptions(discoveredTopics, subs.toList())
            }
        }

        // --- Auto-refresh polling logic ---
        autoRefreshCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                autoRefreshJob?.cancel()
                autoRefreshJob = lifecycleScope.launch {
                    while (autoRefreshCheckBox.isChecked) {
                        runOnUiThread { fetchTopicsButton.performClick() }
                        delay(1500)
                    }
                }
                logView.text = "Auto-refresh enabled.\n" + logView.text
            } else {
                autoRefreshJob?.cancel()
                autoRefreshJob = null
                logView.text = "Auto-refresh disabled.\n" + logView.text
            }
        }

        // --- Manual subscribe logic ---
        subscribeManualButton.setOnClickListener {
            if (!RosbridgeConnectionManager.isConnected) {
                logView.text = "Not connected to rosbridge.\n" + logView.text
                return@setOnClickListener
            }
            val topic = manualTopicEditText.text.toString().trim()
            val type = manualTypeEditText.text.toString().trim()
            if (topic.isEmpty()) {
                manualTopicEditText.error = "Topic required"
                logView.text = "Manual topic required.\n" + logView.text
                return@setOnClickListener
            }
            if (type.isEmpty()) {
                manualTypeEditText.error = "Type required"
                logView.text = "Manual type required.\n" + logView.text
                return@setOnClickListener
            }
            val typePattern = Regex("^[a-zA-Z0-9_]+/msg/[a-zA-Z0-9_]+$")
            if (!typePattern.matches(type)) {
                manualTypeEditText.error = "Type must be in the form pkg/msg/Type (e.g. std_msgs/msg/String)"
                logView.text = "Type format invalid. Use pkg/msg/Type (e.g. std_msgs/msg/String).\n" + logView.text
                return@setOnClickListener
            }
            if (!supportedTypes.contains(type)) {
                manualTypeEditText.error = "Type not in supported list"
                logView.text = "Warning: Type '$type' is not in supported types.\n" + logView.text
            }
            rosViewModel.addSubscribedTopic(topic, type)
            logView.text = "Manually subscribed to $topic ($type)\n" + logView.text
        }

        val prefs = getSharedPreferences("ros2_prefs", Context.MODE_PRIVATE)
        val savedIp = prefs.getString("ip_address", "")
        val savedPort = prefs.getString("port", "")
        if (!savedIp.isNullOrEmpty()) {
            ipEditText.setText(savedIp)
        }
        if (!savedPort.isNullOrEmpty()) {
            portEditText.setText(savedPort)
        } else {
            portEditText.setText("9090")
            prefs.edit { putString("port", "9090") }
        }

        ipEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                prefs.edit { putString("ip_address", s?.toString() ?: "") }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        portEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                prefs.edit { putString("port", s?.toString() ?: "") }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        connectButton.setOnClickListener {
            val ip = ipEditText.text.toString().trim()
            val port = portEditText.text.toString().trim().toIntOrNull() ?: 9090
            RosbridgeConnectionManager.connect(ip, port)
        }
        disconnectButton.setOnClickListener {
            RosbridgeConnectionManager.disconnect()
        }
        backToMainButton.setOnClickListener {
            finish()
        }

        updateButtonStates(RosbridgeConnectionManager.isConnected)

        fetchTopicsButton.setOnClickListener {
            if (!RosbridgeConnectionManager.isConnected) {
                logView.text = "Not connected to rosbridge.\n" + logView.text
                return@setOnClickListener
            }
            val request = JSONObject()
            request.put("op", "call_service")
            request.put("service", "/rosapi/topics_and_raw_types")
            request.put("id", "topics_and_types_request")
            RosbridgeConnectionManager.send(request)
        }

        rosbridgeListener = object : RosbridgeConnectionManager.Listener {
            override fun onConnected() {
                runOnUiThread {
                    logView.text = "Connected to rosbridge\n" + logView.text
                    updateButtonStates(true)
                }
            }

            override fun onDisconnected() {
                runOnUiThread {
                    logView.text = "Disconnected\n" + logView.text
                    updateButtonStates(false)
                }
            }
            
            override fun onMessage(text: String) {
                try {
                    val json = JSONObject(text)
                    val op = json.optString("op")
                    val topic = json.optString("topic", "")
                    val msgType = discoveredTopics.find { it.first == topic }?.second

                    if (op == "publish" && (msgType == "sensor_msgs/msg/Image" || msgType == "sensor_msgs/msg/CompressedImage")) {
                        imageDecodeChannel.trySend(text)
                        return
                    }

                    when (op) {
                        "publish" -> {
                            val topic = json.optString("topic", "?")
                            val msg = json.optJSONObject("msg")
                            appendLogLine("RECEIVED: [${topic}] $msg")

                        }
                        "service_response" -> {
                            if (json.optString("service") == "/rosapi/topics_and_raw_types") {
                                val values = json.optJSONObject("values")
                                val topics = values?.optJSONArray("topics")
                                val types = values?.optJSONArray("types")
                                if (topics != null && types != null && topics.length() == types.length()) {
                                    val filtered = mutableListOf<Pair<String, String>>()
                                    for (i in 0 until topics.length()) {
                                        val topic = topics.getString(i)
                                        val type = types.getString(i)
                                        if (supportedTypes.contains(type)) {
                                            filtered.add(Pair(topic, type))
                                        }
                                    }
                                    discoveredTopics = filtered
                                    val availableTopicsSet = filtered.map { it.first }.toSet()
                                    val currentSubs = rosViewModel.subscribedTopics.value.toList()
                                    val toRemove = currentSubs.filter { !availableTopicsSet.contains(it.first) }
                                    for ((topic, type) in toRemove) {
                                        runOnUiThread {
                                            logView.text = "Auto-unsubscribed from $topic (no longer exists)\n" + logView.text
                                        }
                                    }
                                    runOnUiThread {
                                        updateTopicAdapterWithSubscriptions(filtered, rosViewModel.subscribedTopics.value.toList())
                                        if (filtered.isEmpty()) {
                                            logView.text = "No supported topics found.\n" + logView.text
                                        } else {
                                            logView.text = "Fetched ${filtered.size} supported topics.\n" + logView.text
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    appendLogLine("Error processing message: ${e.message}")
                }
            }
            
            private fun appendLogLine(line: String) {
                val truncated = if (line.length > 300) line.take(300) + "... [truncated]" else line
                synchronized(logBuffer) {
                    logBuffer.insert(0, truncated + "\n")
                    var lines = 0
                    var idx = 0
                    while (idx < logBuffer.length && lines < maxLogLines) {
                        if (logBuffer[idx] == '\n') lines++
                        idx++
                    }
                    if (idx < logBuffer.length) {
                        logBuffer.setLength(idx)
                    }
                    logDirty = true
                }
                rosViewModel.appendToMessageHistory(line)
            }

            override fun onError(error: String) {
                runOnUiThread {
                    logView.text = "WebSocket error: $error\n" + logView.text
                    updateButtonStates(false)
                }
            }

        }
        RosbridgeConnectionManager.addListener(rosbridgeListener!!)
    }

    /*
        input:    None
        output:   None
        remarks:  Called when the activity resumes; updates topic adapter and auto-refresh job.
    */
    override fun onResume() {
        super.onResume()
        if (discoveredTopics.isNotEmpty() && topicAdapter != null) {
            val subscribedSet = rosViewModel.subscribedTopics.value.map { it.first }.toSet()
            topicAdapter!!.updateTopicsAndSubscribed(discoveredTopics, subscribedSet)
        }
        if (RosbridgeConnectionManager.isConnected && autoRefreshCheckBox.isChecked && autoRefreshJob == null) {
            autoRefreshJob = lifecycleScope.launch {
                val fetchTopicsButton: Button = findViewById(R.id.button_fetch_topics)
                while (autoRefreshCheckBox.isChecked) {
                    runOnUiThread { fetchTopicsButton.performClick() }
                    delay(1500)
                }
            }
        }
    }

    /*
        input:    None
        output:   None
        remarks:  Called when the activity pauses; cancels auto-refresh job.
    */
    override fun onPause() {
        super.onPause()
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    /*
        input:    isConnected - Boolean
        output:   None
        remarks:  Enables/disables UI controls based on rosbridge connection state.
    */
    fun updateButtonStates(isConnected: Boolean) {
        connectButton.isEnabled = !isConnected
        disconnectButton.isEnabled = isConnected
        ipEditText.isEnabled = !isConnected
        portEditText.isEnabled = !isConnected
    }

    /*
        input:    None
        output:   None
        remarks:  Cleans up rosbridge listener on activity destroy.
    */
    override fun onDestroy() {
        imageProcessorJob?.cancel()
        logUpdateJob?.cancel()
        rosbridgeListener?.let { RosbridgeConnectionManager.removeListener(it) }
        super.onDestroy()
    }

    private fun launchImageProcessor() {
        imageProcessorJob?.cancel()
        imageProcessorJob = lifecycleScope.launch(Dispatchers.Default) {
            var droppedCount = 0
            var processedCount = 0
            
            while (isActive) {
                // Get the first message
                var jsonText = imageDecodeChannel.receive()
                
                // Aggressively drop all queued messages, keep only the latest
                var dropped = 0
                while (true) {
                    val latest = imageDecodeChannel.tryReceive().getOrNull() ?: break
                    jsonText = latest
                    dropped++
                }
                
                if (dropped > 0) {
                    droppedCount += dropped
                    // Log every 50 processed frames to avoid spam
                    if (processedCount % 50 == 0) {
                        android.util.Log.d("ImageProcessor", "Dropped $dropped frames (total dropped: $droppedCount, processed: $processedCount)")
                    }
                }
                
                try {
                    val decodeStart = System.currentTimeMillis()

                    val json = JSONObject(jsonText)
                    val topic = json.optString("topic", "")
                    val msgType = discoveredTopics.find { it.first == topic }?.second
                    val msg = json.getJSONObject("msg")

                    val bitmap = when (msgType) {
                        "sensor_msgs/msg/Image" -> {
                            val width = msg.getInt("width")
                            val height = msg.getInt("height")
                            val base64Data = msg.getString("data")
                            decodeBgr8Base64ToBitmap(base64Data, width, height)
                        }
                        "sensor_msgs/msg/CompressedImage" -> {
                            val base64Data = msg.getString("data")
                            decodeCompressedBase64ToBitmap(base64Data)
                        }
                        else -> null
                    }

                    val header = msg.optJSONObject("header")
                    val stamp = header?.optJSONObject("stamp")
                    val seconds = stamp?.optLong("sec", 0) ?: 0
                    val nanoseconds = stamp?.optLong("nanosec", 0) ?: 0
                    val receivedTimestamp = (seconds * 1000) + (nanoseconds / 1_000_000)

                    val decodeEnd = System.currentTimeMillis()
                    processedCount++

                    withContext(Dispatchers.Main) {
                        rosViewModel.latestBitmap.value = bitmap as Bitmap?
                        android.util.Log.d("Ros2ImagePerf", "Image decode+display latency: ${decodeEnd - decodeStart} ms, total from ROS publish: ${decodeEnd - receivedTimestamp} ms")
                    }

                } catch (e: Exception) {
                    android.util.Log.e("ImageProcessor", "Failed to process image JSON: ${e.message}")
                }
            }
        }
    }

    private fun decodeCompressedBase64ToBitmap(base64: String): android.graphics.Bitmap? {
        return try {
            val imageBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}