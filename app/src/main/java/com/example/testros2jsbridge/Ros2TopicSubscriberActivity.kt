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

/*
    Ros2TopicSubscriberActivity provides a UI for subscribing to ROS2 topics via rosbridge, supporting both dynamic topic discovery and manual subscription.
    Handles connection management, topic/type selection, message display, and auto-refresh of available topics.
*/

class Ros2TopicSubscriberActivity : AppCompatActivity() {
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
    // Supported message types for dynamic subscription
    private val supportedTypes = setOf(
        "std_msgs/msg/String", "std_msgs/msg/Int32", "geometry_msgs/msg/Twist", "geometry_msgs/msg/PoseStamped",
        "sensor_msgs/msg/BatteryState", "sensor_msgs/msg/CameraInfo", "sensor_msgs/msg/ChannelFloat32", "sensor_msgs/msg/CompressedImage", "sensor_msgs/msg/FluidPressure", "sensor_msgs/msg/Illuminance", "sensor_msgs/msg/Image", "sensor_msgs/msg/Imu", "sensor_msgs/msg/JointState", "sensor_msgs/msg/Joy", "sensor_msgs/msg/JoyFeedback", "sensor_msgs/msg/JoyFeedbackArray", "sensor_msgs/msg/LaserEcho", "sensor_msgs/msg/LaserScan", "sensor_msgs/msg/MagneticField", "sensor_msgs/msg/MultiDOFJointState", "sensor_msgs/msg/MultiEchoLaserScan", "sensor_msgs/msg/NavSatFix", "sensor_msgs/msg/NavSatStatus", "sensor_msgs/msg/PointCloud", "sensor_msgs/msg/PointCloud2", "sensor_msgs/msg/PointField", "sensor_msgs/msg/Range", "sensor_msgs/msg/RegionOfInterest", "sensor_msgs/msg/RelativeHumidity", "sensor_msgs/msg/Temperature", "sensor_msgs/msg/TimeReference",
        "geometry_msgs/msg/Accel", "geometry_msgs/msg/AccelStamped", "geometry_msgs/msg/AccelWithCovariance", "geometry_msgs/msg/AccelWithCovarianceStamped", "geometry_msgs/msg/Inertia", "geometry_msgs/msg/InertiaStamped", "geometry_msgs/msg/Point", "geometry_msgs/msg/Point32", "geometry_msgs/msg/PointStamped", "geometry_msgs/msg/Polygon", "geometry_msgs/msg/PolygonInstance", "geometry_msgs/msg/PolygonInstanceStamped", "geometry_msgs/msg/PolygonStamped", "geometry_msgs/msg/Pose", "geometry_msgs/msg/Pose2D", "geometry_msgs/msg/PoseArray", "geometry_msgs/msg/PoseStamped", "geometry_msgs/msg/PoseWithCovariance", "geometry_msgs/msg/PoseWithCovarianceStamped", "geometry_msgs/msg/Quaternion", "geometry_msgs/msg/QuaternionStamped", "geometry_msgs/msg/Transform", "geometry_msgs/msg/TransformStamped", "geometry_msgs/msg/Twist", "geometry_msgs/msg/TwistStamped", "geometry_msgs/msg/TwistWithCovariance", "geometry_msgs/msg/TwistWithCovarianceStamped", "geometry_msgs/msg/Vector3", "geometry_msgs/msg/Vector3Stamped", "geometry_msgs/msg/VelocityStamped", "geometry_msgs/msg/Wrench", "geometry_msgs/msg/WrenchStamped",
        "std_msgs/msg/Bool", "std_msgs/msg/Byte", "std_msgs/msg/ByteMultiArray", "std_msgs/msg/Char", "std_msgs/msg/ColorRGBA", "std_msgs/msg/Empty", "std_msgs/msg/Float32", "std_msgs/msg/Float32MultiArray", "std_msgs/msg/Float64", "std_msgs/msg/Float64MultiArray", "std_msgs/msg/Header", "std_msgs/msg/Int16", "std_msgs/msg/Int16MultiArray", "std_msgs/msg/Int32MultiArray", "std_msgs/msg/Int64", "std_msgs/msg/Int64MultiArray", "std_msgs/msg/Int8", "std_msgs/msg/Int8MultiArray", "std_msgs/msg/MultiArrayDimension", "std_msgs/msg/MultiArrayLayout", "std_msgs/msg/String", "std_msgs/msg/UInt16", "std_msgs/msg/UInt16MultiArray", "std_msgs/msg/UInt32", "std_msgs/msg/UInt32MultiArray", "std_msgs/msg/UInt64", "std_msgs/msg/UInt64MultiArray", "std_msgs/msg/UInt8", "std_msgs/msg/UInt8MultiArray"
    )
    // List of discovered topics and types
    private var discoveredTopics: List<Pair<String, String>> = emptyList()
    private var selectedTopic: Pair<String, String>? = null

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
        val spinnerTopics = findViewById<android.widget.Spinner>(R.id.spinner_topics)
        val subscribeButton: Button = findViewById(R.id.button_subscribe_topic)

        // --- Auto-refresh polling logic ---
        var autoRefreshJob: Job? = null
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
            // Validate topic
            if (topic.isEmpty()) {
                manualTopicEditText.error = "Topic required"
                logView.text = "Manual topic required.\n" + logView.text
                return@setOnClickListener
            }
            // Validate type
            if (type.isEmpty()) {
                manualTypeEditText.error = "Type required"
                logView.text = "Manual type required.\n" + logView.text
                return@setOnClickListener
            }
            // Validate type format: should be like pkg/msg/Type
            val typePattern = Regex("^[a-zA-Z0-9_]+/msg/[a-zA-Z0-9_]+$")
            if (!typePattern.matches(type)) {
                manualTypeEditText.error = "Type must be in the form pkg/msg/Type (e.g. std_msgs/msg/String)"
                logView.text = "Type format invalid. Use pkg/msg/Type (e.g. std_msgs/msg/String).\n" + logView.text
                return@setOnClickListener
            }
            // Optionally, check if type is in supportedTypes
            if (!supportedTypes.contains(type)) {
                manualTypeEditText.error = "Type not in supported list"
                logView.text = "Warning: Type '$type' is not in supported types.\n" + logView.text
                // Allow to proceed, but warn
            }
            val subscribeMsg = JSONObject()
            subscribeMsg.put("op", "subscribe")
            subscribeMsg.put("topic", topic)
            subscribeMsg.put("type", type)
            RosbridgeConnectionManager.send(subscribeMsg)
            logView.text = "Manually subscribed to $topic ($type)\n" + logView.text
        }

        // SharedPreferences for IP/Port sync
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

        // Fetch topics button logic
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

        // Spinner selection logic
        spinnerTopics.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < discoveredTopics.size) {
                    selectedTopic = discoveredTopics[position]
                } else {
                    selectedTopic = null
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                selectedTopic = null
            }
        }

        // Subscribe button logic
        subscribeButton.setOnClickListener {
            if (!RosbridgeConnectionManager.isConnected) {
                logView.text = "Not connected to rosbridge.\n" + logView.text
                return@setOnClickListener
            }
            val topicPair = selectedTopic
            if (topicPair == null) {
                logView.text = "No topic selected.\n" + logView.text
                return@setOnClickListener
            }
            val (topic, type) = topicPair
            val subscribeMsg = JSONObject()
            subscribeMsg.put("op", "subscribe")
            subscribeMsg.put("topic", topic)
            subscribeMsg.put("type", type)
            RosbridgeConnectionManager.send(subscribeMsg)
            logView.text = "Subscribed to $topic ($type)\n" + logView.text
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
                // Handle rosapi topics_and_raw_types response
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
                            val topicNames = filtered.map { "${it.first}  [${it.second}]" }
                            val adapter = android.widget.ArrayAdapter(this@Ros2TopicSubscriberActivity, android.R.layout.simple_spinner_item, topicNames)
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            runOnUiThread {
                                spinnerTopics.adapter = adapter
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
                runOnUiThread {
                    // Try to parse as a published topic message
                    try {
                        val json = JSONObject(text)
                        if (json.optString("op") == "publish") {
                            val topic = json.optString("topic", "?")
                            val msg = json.optJSONObject("msg")
                            logView.text = "RECEIVED: [${topic}] ${msg}\n" + logView.text
                        } else {
                            logView.text = "INCOMING JSON: ${'$'}text\n" + logView.text
                        }
                    } catch (e: Exception) {
                        logView.text = "INCOMING JSON: ${'$'}text\n" + logView.text
                    }
                    logScrollView.post {
                        logScrollView.fullScroll(android.view.View.FOCUS_UP)
                    }
                }
            }
            override fun onError(error: String) {
                runOnUiThread {
                    logView.text = "WebSocket error: ${'$'}error\n" + logView.text
                    updateButtonStates(false)
                }
            }
        }
        RosbridgeConnectionManager.addListener(rosbridgeListener!!)
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
        rosbridgeListener?.let { RosbridgeConnectionManager.removeListener(it) }
        super.onDestroy()
    }
}
