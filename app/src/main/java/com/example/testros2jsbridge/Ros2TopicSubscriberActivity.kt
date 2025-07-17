package com.example.testros2jsbridge

import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import android.widget.EditText
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import android.content.SharedPreferences
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.collectAsState

/*
    Ros2TopicSubscriberActivity provides a UI for subscribing to ROS2 topics via rosbridge, supporting both dynamic topic discovery and manual subscription.
    Handles connection management, topic/type selection, message display, and auto-refresh of available topics.
*/

class Ros2TopicSubscriberActivity : AppCompatActivity() {
    // Helper to check if any image topic is subscribed
    private fun isImageTopicSubscribed(subs: List<Pair<String, String>>): Boolean {
        return subs.any { it.second == "sensor_msgs/msg/Image" }
    }
    private var autoRefreshJob: Job? = null
    // Shared ViewModel for Compose log view (application-scoped)
    private val rosViewModel: RosViewModel by lazy {
        val app = application as MyApp
        androidx.lifecycle.ViewModelProvider(
            app.appViewModelStore,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        ).get(RosViewModel::class.java)
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
    // Compose image state
    private val latestBitmap = androidx.compose.runtime.mutableStateOf<android.graphics.Bitmap?>(null)
    private val showImageState = androidx.compose.runtime.mutableStateOf(true)

    private val supportedTypes = setOf(
        "std_msgs/msg/String", "std_msgs/msg/Int32", "geometry_msgs/msg/Twist", "geometry_msgs/msg/PoseStamped",
        "sensor_msgs/msg/BatteryState", "sensor_msgs/msg/CameraInfo", "sensor_msgs/msg/ChannelFloat32", "sensor_msgs/msg/CompressedImage", "sensor_msgs/msg/FluidPressure", "sensor_msgs/msg/Illuminance", "sensor_msgs/msg/Image", "sensor_msgs/msg/Imu", "sensor_msgs/msg/JointState", "sensor_msgs/msg/Joy", "sensor_msgs/msg/JoyFeedback", "sensor_msgs/msg/JoyFeedbackArray", "sensor_msgs/msg/LaserEcho", "sensor_msgs/msg/LaserScan", "sensor_msgs/msg/MagneticField", "sensor_msgs/msg/MultiDOFJointState", "sensor_msgs/msg/MultiEchoLaserScan", "sensor_msgs/msg/NavSatFix", "sensor_msgs/msg/NavSatStatus", "sensor_msgs/msg/PointCloud", "sensor_msgs/msg/PointCloud2", "sensor_msgs/msg/PointField", "sensor_msgs/msg/Range", "sensor_msgs/msg/RegionOfInterest", "sensor_msgs/msg/RelativeHumidity", "sensor_msgs/msg/Temperature", "sensor_msgs/msg/TimeReference",
        "geometry_msgs/msg/Accel", "geometry_msgs/msg/AccelStamped", "geometry_msgs/msg/AccelWithCovariance", "geometry_msgs/msg/AccelWithCovarianceStamped", "geometry_msgs/msg/Inertia", "geometry_msgs/msg/InertiaStamped", "geometry_msgs/msg/Point", "geometry_msgs/msg/Point32", "geometry_msgs/msg/PointStamped", "geometry_msgs/msg/Polygon", "geometry_msgs/msg/PolygonInstance", "geometry_msgs/msg/PolygonInstanceStamped", "geometry_msgs/msg/PolygonStamped", "geometry_msgs/msg/Pose", "geometry_msgs/msg/Pose2D", "geometry_msgs/msg/PoseArray", "geometry_msgs/msg/PoseStamped", "geometry_msgs/msg/PoseWithCovariance", "geometry_msgs/msg/PoseWithCovarianceStamped", "geometry_msgs/msg/Quaternion", "geometry_msgs/msg/QuaternionStamped", "geometry_msgs/msg/Transform", "geometry_msgs/msg/TransformStamped", "geometry_msgs/msg/Twist", "geometry_msgs/msg/TwistStamped", "geometry_msgs/msg/TwistWithCovariance", "geometry_msgs/msg/TwistWithCovarianceStamped", "geometry_msgs/msg/Vector3", "geometry_msgs/msg/Vector3Stamped", "geometry_msgs/msg/VelocityStamped", "geometry_msgs/msg/Wrench", "geometry_msgs/msg/WrenchStamped",
        "std_msgs/msg/Bool", "std_msgs/msg/Byte", "std_msgs/msg/ByteMultiArray", "std_msgs/msg/Char", "std_msgs/msg/ColorRGBA", "std_msgs/msg/Empty", "std_msgs/msg/Float32", "std_msgs/msg/Float32MultiArray", "std_msgs/msg/Float64", "std_msgs/msg/Float64MultiArray", "std_msgs/msg/Header", "std_msgs/msg/Int16", "std_msgs/msg/Int16MultiArray", "std_msgs/msg/Int32MultiArray", "std_msgs/msg/Int64", "std_msgs/msg/Int64MultiArray", "std_msgs/msg/Int8", "std_msgs/msg/Int8MultiArray", "std_msgs/msg/MultiArrayDimension", "std_msgs/msg/MultiArrayLayout", "std_msgs/msg/String", "std_msgs/msg/UInt16", "std_msgs/msg/UInt16MultiArray", "std_msgs/msg/UInt32", "std_msgs/msg/UInt32MultiArray", "std_msgs/msg/UInt64", "std_msgs/msg/UInt64MultiArray", "std_msgs/msg/UInt8", "std_msgs/msg/UInt8MultiArray"
    )
    private var discoveredTopics: List<Pair<String, String>> = emptyList()
    private var topicAdapter: TopicCheckboxAdapter? = null

    // --- Bitmap decoding helpers for sensor_msgs/msg/Image ---
    private fun decodeRgb8ToBitmap(data: ByteArray, width: Int, height: Int): android.graphics.Bitmap {
        val pixels = IntArray(width * height)
        var i = 0
        var j = 0
        while (i < data.size && j < pixels.size) {
            val r = data[i].toInt() and 0xFF
            val g = data[i + 1].toInt() and 0xFF
            val b = data[i + 2].toInt() and 0xFF
            pixels[j] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            i += 3
            j += 1
        }
        return android.graphics.Bitmap.createBitmap(pixels, width, height, android.graphics.Bitmap.Config.ARGB_8888)
    }

    private fun decodeBgr8ToBitmap(data: ByteArray, width: Int, height: Int): android.graphics.Bitmap {
        val pixels = IntArray(width * height)
        var i = 0
        var j = 0
        while (i < data.size && j < pixels.size) {
            val b = data[i].toInt() and 0xFF
            val g = data[i + 1].toInt() and 0xFF
            val r = data[i + 2].toInt() and 0xFF
            pixels[j] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            i += 3
            j += 1
        }
        return android.graphics.Bitmap.createBitmap(pixels, width, height, android.graphics.Bitmap.Config.ARGB_8888)
    }

    /*
        input:    savedInstanceState - Bundle?
        output:   None
        remarks:  Initializes the UI, sets up listeners, manages connection, topic discovery, and subscription logic.
    */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            val bitmap = latestBitmap.value
            // Reset showImageState if a new image topic is subscribed
            androidx.compose.runtime.LaunchedEffect(hasImage) {
                if (hasImage) showImageState.value = true
            }
            if (hasImage) {
                androidx.compose.foundation.layout.Column {
                    if (showImageState.value) {
                        androidx.compose.material3.Button(onClick = { showImageState.value = false }) {
                            androidx.compose.material3.Text("Hide Image")
                        }
                        if (bitmap != null) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    android.widget.ImageView(ctx).apply {
                                        setImageBitmap(bitmap)
                                        adjustViewBounds = true
                                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                    }
                                },
                                update = { it.setImageBitmap(bitmap) }
                            )
                        } else {
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

        fun updateTopicAdapterWithSubscriptions(topics: List<Pair<String, String>>, subscribed: List<Pair<String, String>>) {
        val subscribedSet = subscribed.map { it.first }.toSet()
            if (topicAdapter == null) {
                topicAdapter = TopicCheckboxAdapter(
                    topics = topics,
                    subscribedTopics = subscribedSet,
                    onCheckedChange = { topic, type, isChecked ->
                        if (isChecked) {
                            rosViewModel.addSubscribedTopic(topic, type)
                            logView.text = "Subscribed to $topic ($type)\n" + logView.text
                        } else {
                            rosViewModel.removeSubscribedTopic(topic, type)
                            logView.text = "Unsubscribed from $topic\n" + logView.text
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
            prefs.edit().putString("port", "9090").apply()
        }

        ipEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString("ip_address", s?.toString() ?: "").apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        portEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString("port", s?.toString() ?: "").apply()
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
                    if (json.optString("op") == "service_response" && json.optString("service") == "/rosapi/topics_and_raw_types") {
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
                                rosViewModel.removeSubscribedTopic(topic, type)
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
                        return
                    }
                } catch (_: Exception) {}
                try {
                    val json = JSONObject(text)
                    if (json.optString("op") == "publish") {
                        val topic = json.optString("topic", "?")
                        val msg = json.optJSONObject("msg")
                        val type = discoveredTopics.find { it.first == topic }?.second
            if (type == "sensor_msgs/msg/Image" && msg != null) {
                            val width = msg.optInt("width", -1)
                            val height = msg.optInt("height", -1)
                            val encoding = msg.optString("encoding", "")
                            val dataField = msg.opt("data")
                            var byteArray: ByteArray? = null
                            var dataPreview = ""
                            var dataLen = 0
                            if (dataField is String) {
                                try {
                                    byteArray = android.util.Base64.decode(dataField, android.util.Base64.DEFAULT)
                                    dataLen = byteArray.size
                                    val previewLen = minOf(8, dataLen)
                                    val previewBytes = byteArray.take(previewLen).joinToString(" ") { b ->
                                        (b.toInt() and 0xFF).toString(16).padStart(2, '0')
                                    }
                                    dataPreview = "data[0..${previewLen - 1}]=[$previewBytes] (base64)"
                                } catch (_: Exception) {
                                    dataPreview = "data=[base64 decode error]"
                                }
                            } else if (dataField is org.json.JSONArray) {
                                val dataArray = dataField
                                dataLen = dataArray.length()
                                val previewLen = minOf(8, dataLen)
                                val previewBytes = (0 until previewLen).joinToString(" ") { idx ->
                                    val v = dataArray.getInt(idx)
                                    v.toUByte().toString(16).padStart(2, '0')
                                }
                                dataPreview = "data[0..${previewLen - 1}]=[$previewBytes] (array)"
                                byteArray = ByteArray(dataLen)
                                for (i in 0 until dataLen) {
                                    byteArray[i] = dataArray.getInt(i).toByte()
                                }
                            } else {
                                dataPreview = "data=[]"
                            }
                if (width > 0 && height > 0 && byteArray != null && (encoding == "rgb8" || encoding == "bgr8")) {
                    try {
                        val bitmap = if (encoding == "rgb8") {
                            decodeRgb8ToBitmap(byteArray, width, height)
                        } else {
                            decodeBgr8ToBitmap(byteArray, width, height)
                        }
                        latestBitmap.value = bitmap
                        android.util.Log.d("Ros2TopicSubscriber", "Bitmap created: ${bitmap.width}x${bitmap.height}")
                    } catch (e: Exception) {
                        latestBitmap.value = null
                        android.util.Log.e("Ros2TopicSubscriber", "Bitmap decode error: ${e.message}")
                    }
                    // Only auto-show if currently hidden and a new image topic is subscribed
                    if (!showImageState.value) {
                        // Do not force show; keep hidden until user toggles
                    }
                }
                            appendLogLine("RECEIVED: [$topic] sensor_msgs/msg/Image: width=$width, height=$height, encoding=$encoding, $dataPreview, data.length=$dataLen")
                        } else {
                            appendLogLine("RECEIVED: [${topic}] ${msg}")
                        }
                    } else {
                        appendLogLine("INCOMING JSON: $text")
                    }
                } catch (e: Exception) {
                    appendLogLine("INCOMING JSON: $text")
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
        logUpdateJob?.cancel()
        rosbridgeListener?.let { RosbridgeConnectionManager.removeListener(it) }
        super.onDestroy()
    }
}
