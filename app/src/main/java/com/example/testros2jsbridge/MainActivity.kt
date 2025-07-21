package com.example.testros2jsbridge
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.*
import androidx.compose.material3.*
import com.google.android.material.button.MaterialButton
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import android.text.Editable
import android.text.TextWatcher
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.fragment.app.Fragment
import androidx.compose.ui.res.colorResource

/*
    MainActivity provides the main entry point for the app UI, handling connection to rosbridge, fragment switching, and message history display.
    It manages user input for IP/port, connection state, and navigation between different ROS2-related fragments.
    Also includes a Jetpack Compose tab for displaying sent message history.
*/

class MainActivity : AppCompatActivity() {
    // File picker launchers for export/import config
    private val exportConfigLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/x-yaml")) { uri: Uri? ->
        uri?.let {
            val frag = supportFragmentManager.findFragmentByTag("ControllerSupportFragment") as? ControllerSupportFragment
            if (frag != null && frag.isVisible) {
                contentResolver.openOutputStream(uri)?.let { out ->
                    frag.exportConfigToStream(out)
                }
            } else {
                android.widget.Toast.makeText(this, "ControllerSupportFragment must be visible to export config!", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private val importConfigLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val frag = supportFragmentManager.findFragmentByTag("ControllerSupportFragment") as? ControllerSupportFragment
            if (frag != null && frag.isVisible) {
                contentResolver.openInputStream(uri)?.let { inp ->
                    frag.importConfigFromStream(inp)
                }
            } else {
                android.widget.Toast.makeText(this, "ControllerSupportFragment must be visible to import config!", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    private lateinit var openRos2SubscriberButton: MaterialButton

    /*
        input:    None
        output:   None
        remarks:  Called when the activity resumes; resets the dropdown adapter to clear filter state after rotation.
    */
    override fun onResume() {
        super.onResume()
        // Reset the adapter after rotation to clear any stuck filter state
        dropdown.post {
            val currentText = dropdown.text.toString()
            dropdown.setAdapter(null)
            dropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, actions))
            dropdown.setText(currentText, false)
        }
        // Ensure all topic subscriptions are (re-)registered to append to log
        rosViewModel.resubscribeAllTopicsToLog()
    }

    private val rosViewModel: RosViewModel by lazy {
        val app = application as MyApp
        androidx.lifecycle.ViewModelProvider(
            app.appViewModelStore,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        ).get(RosViewModel::class.java)
    }

    private lateinit var ipAddressEditText: TextInputEditText
    private lateinit var portEditText: TextInputEditText
    private lateinit var connectButton: Button
    private lateinit var statusTextView: TextView
    private var rosbridgeListener: RosbridgeConnectionManager.Listener? = null

    private lateinit var messageHistoryComposeView: ComposeView

    private lateinit var actions: List<String>
    private lateinit var dropdown: AutoCompleteTextView
    private lateinit var disconnectButton: Button

    private lateinit var ipPortContainer: android.view.View
    private lateinit var collapseIpPortButton: MaterialButton
    private lateinit var expandIpPortButton: MaterialButton

    /*
        input:    savedInstanceState - Bundle?
        output:   None
        remarks:  Initializes the UI, sets up listeners, restores preferences, and configures fragments and message history tab.
    */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Always register rosViewModel as a listener for rosbridge connection events
        RosbridgeConnectionManager.addListener(rosViewModel)

        ipAddressEditText = findViewById(R.id.edittext_ip_address)
        portEditText = findViewById(R.id.edittext_port)

        // SharedPreferences for IP/Port sync
        val prefs = getSharedPreferences("ros2_prefs", Context.MODE_PRIVATE)
        val savedIp = prefs.getString("ip_address", "")
        val savedPort = prefs.getString("port", "")
        if (!savedIp.isNullOrEmpty()) {
            ipAddressEditText.setText(savedIp)
        }
        if (!savedPort.isNullOrEmpty()) {
            portEditText.setText(savedPort)
        } else {
            portEditText.setText("9090")
            prefs.edit().putString("port", "9090").apply()
        }

        ipAddressEditText.addTextChangedListener(object : TextWatcher {
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
        connectButton = findViewById(R.id.button_connect)
        statusTextView = findViewById(R.id.textview_status)

        dropdown = findViewById(R.id.dropdown_action)
        disconnectButton = findViewById(R.id.button_disconnect)

        ipPortContainer = findViewById(R.id.layout_ip_port_container)
        collapseIpPortButton = findViewById<MaterialButton>(R.id.button_collapse_ip_port)
        expandIpPortButton = findViewById<MaterialButton>(R.id.button_expand_ip_port)

        // Collapsible logic
        collapseIpPortButton.setOnClickListener {
            ipPortContainer.visibility = android.view.View.GONE
            expandIpPortButton.visibility = android.view.View.VISIBLE
        }
        expandIpPortButton.setOnClickListener {
            ipPortContainer.visibility = android.view.View.VISIBLE
            expandIpPortButton.visibility = android.view.View.GONE
        }
        collapseIpPortButton.setOnClickListener {
            ipPortContainer.visibility = android.view.View.GONE
            expandIpPortButton.visibility = android.view.View.VISIBLE
        }
        expandIpPortButton.setOnClickListener {
            ipPortContainer.visibility = android.view.View.VISIBLE
            expandIpPortButton.visibility = android.view.View.GONE
        }

        actions = listOf(
            getString(R.string.dropdown_default_activity),
            getString(R.string.dropdown_custom_publisher),
            "Geometry Standard Messages",
            getString(R.string.dropdown_slider_buttons),
            "Controller Input Device",
            "Import Custom Protocols"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, actions)
        dropdown.setAdapter(adapter)

        // Observe the selected index from ViewModel and set the dropdown and fragment
        rosViewModel.selectedDropdownIndex
            .onEach { index ->
                if (index >= 0 && index < actions.size) {
                    // Only update dropdown text if needed to avoid unnecessary events
                    if (dropdown.text.toString() != actions[index]) {
                        dropdown.setText(actions[index], false)
                        dropdown.dismissDropDown()
                    }
                    showFragmentForIndex(index)
                }
            }
            .launchIn(lifecycleScope)

        dropdown.setOnClickListener {
            dropdown.showDropDown()
        }
        dropdown.setOnItemClickListener { _, _, position, _ ->
            rosViewModel.selectDropdownIndex(position)
        }

        messageHistoryComposeView = findViewById(R.id.compose_view_message_history)
        messageHistoryComposeView.setContent {
            MaterialTheme {
                Surface {
                    CollapsibleMessageHistoryTab(rosViewModel = rosViewModel)
                }
            }
        }

        setupClickListeners()
        observeViewModel()

        openRos2SubscriberButton = findViewById(R.id.button_open_ros2_subscriber)
        openRos2SubscriberButton.setOnClickListener {
            val intent = android.content.Intent(this, Ros2TopicSubscriberActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btn_export_config).setOnClickListener {
            exportConfigLauncher.launch("configs.yaml")
        }
        findViewById<Button>(R.id.btn_import_config).setOnClickListener {
            // Allow user to select any file type, but filter for YAML and text files
            importConfigLauncher.launch(arrayOf("application/x-yaml", "text/yaml", "text/plain", "application/octet-stream", "*/*"))
        }
    }

    /*
        input:    index - Int
        output:   None
        remarks:  Switches the main fragment based on the selected dropdown index.
    */
    private fun showFragmentForIndex(index: Int) {
        when (index) {
            0 -> showFragment(DefaultFragment())
            1 -> showFragment(CustomPublisherFragment())
            2 -> showFragment(GeometryStdMsgFragment())
            3 -> showFragment(SliderButtonFragment())
            4 -> showFragment(ControllerSupportFragment())
            5 -> showFragment(ImportCustomProtocolsFragment())
        }
    }

    /*
        input:    fragment - Fragment
        output:   None
        remarks:  Replaces the current fragment in the container with the given fragment.
    */
    private fun showFragment(fragment: Fragment) {
        val tag = when (fragment) {
            is ControllerSupportFragment -> "ControllerSupportFragment"
            else -> fragment::class.java.simpleName
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.layout_container, fragment, tag)
            .commit()
    }

    /*
        input:    keyCode - Int, event - KeyEvent
        output:   Boolean
        remarks:  Forwards key down events to ControllerSupportFragment if visible, otherwise default handling.
    */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val frag = supportFragmentManager.findFragmentByTag("ControllerSupportFragment")
        if (frag is ControllerSupportFragment && frag.isVisible) {
            if (frag.onControllerKeyEvent(event)) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    /*
        input:    keyCode - Int, event - KeyEvent
        output:   Boolean
        remarks:  Forwards key up events to ControllerSupportFragment if visible, otherwise default handling.
    */
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val frag = supportFragmentManager.findFragmentByTag("ControllerSupportFragment")
        if (frag is ControllerSupportFragment && frag.isVisible) {
            if (frag.onControllerKeyEvent(event)) return true
        }
        return super.onKeyUp(keyCode, event)
    }

    /*
        input:    event - MotionEvent
        output:   Boolean
        remarks:  Forwards generic motion events to ControllerSupportFragment if visible, otherwise default handling.
    */
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val frag = supportFragmentManager.findFragmentByTag("ControllerSupportFragment")
        if (frag is ControllerSupportFragment && frag.isVisible) {
            if (frag.onControllerMotionEvent(event)) return true
        }
        return super.onGenericMotionEvent(event)
    }

    /*
        input:    None
        output:   None
        remarks:  Sets up click listeners for connect and disconnect buttons, including validation and connection logic.
    */
    private fun setupClickListeners() {
        connectButton.setOnClickListener {
            Log.d("MainActivity", "Connect button pressed")
            val ipAddress = ipAddressEditText.text.toString().trim()
            val portText = portEditText.text.toString().trim()
            val port = portText.toIntOrNull() ?: 9090
            if (ipAddress.isEmpty()) {
                ipAddressEditText.error = "IP Address cannot be empty"
                Log.w("MainActivity", "IP Address is empty.")
                return@setOnClickListener
            }
            if (portText.isEmpty()) {
                portEditText.error = "Port cannot be empty"
                Log.w("MainActivity", "Port is empty.")
                return@setOnClickListener
            }
            Log.d("MainActivity", "Attempting to connect to $ipAddress:$port")
            RosbridgeConnectionManager.connect(ipAddress, port)
        }

        disconnectButton.setOnClickListener {
            Log.d("MainActivity", "Disconnect button pressed")
            RosbridgeConnectionManager.disconnect()
        }
    }

    /*
        input:    None
        output:   None
        remarks:  Observes ViewModel and rosbridge connection state, updating UI on connection events.
    */
    private fun observeViewModel() {
        rosbridgeListener = object : RosbridgeConnectionManager.Listener {
            override fun onConnected() {
                Log.d("MainActivity", "Connection to rosbridge successful!")
                runOnUiThread {
                    statusTextView.text = getString(R.string.status_label, "Connected")
                    ipAddressEditText.isEnabled = false
                    portEditText.isEnabled = false
                    connectButton.isEnabled = false
                    disconnectButton.isEnabled = true
                    // After connecting, (re-)subscribe to all topics for log updates
                    rosViewModel.resubscribeAllTopicsToLog()
                }
            }
            override fun onDisconnected() {
                runOnUiThread {
                    statusTextView.text = getString(R.string.status_label, "Disconnected")
                    ipAddressEditText.isEnabled = true
                    portEditText.isEnabled = true
                    connectButton.isEnabled = true
                    disconnectButton.isEnabled = false
                }
            }
            override fun onMessage(text: String) { /* No-op for main */ }
            override fun onError(error: String) {
                runOnUiThread {
                    statusTextView.text = getString(R.string.status_label, "Error: $error")
                    ipAddressEditText.isEnabled = true
                    portEditText.isEnabled = true
                    connectButton.isEnabled = true
                    disconnectButton.isEnabled = false
                }
            }
        }
        RosbridgeConnectionManager.addListener(rosbridgeListener!!)

        lifecycleScope.launch {
            rosViewModel.rosMessages.collect { /* No-op */ }
        }
    }
    /*
        input:    None
        output:   None
        remarks:  Cleans up rosbridge listener on activity destroy.
    */
    override fun onDestroy() {
        rosbridgeListener?.let { RosbridgeConnectionManager.removeListener(it) }
        // Remove rosViewModel as a listener to avoid leaks
        RosbridgeConnectionManager.removeListener(rosViewModel)
        super.onDestroy()
    }
}

/*
    input:    rosViewModel - RosViewModel
    output:   None
    remarks:  Jetpack Compose tab for displaying and toggling sent message history. Expands/collapses and shows messages in reverse order.
*/
@Composable
fun CollapsibleMessageHistoryTab(rosViewModel: RosViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val messageHistory by rosViewModel.customMessageHistory.collectAsState()
    // Reverse the message list so new messages appear at the top
    val reversedHistory = remember(messageHistory) { messageHistory.asReversed() }

    Surface(
        color = Color.Transparent,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(colorResource(id = R.color.background_container))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Tab bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sent Message History...",
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.Remove else Icons.Filled.Add,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            // Expandable content
            if (expanded) {
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp)
                        .padding(8.dp)
                ) {
                    items(reversedHistory) { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
