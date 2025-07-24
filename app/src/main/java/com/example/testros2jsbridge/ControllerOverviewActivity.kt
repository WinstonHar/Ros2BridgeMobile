package com.example.testros2jsbridge

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.testros2jsbridge.ControllerSupportFragment.AppAction
import com.example.testros2jsbridge.ControllerSupportFragment.JoystickMapping
import com.google.android.material.button.MaterialButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/*
New UI for end users to be able to interact with controller without having to use the UI of the controller fragment.
Allows for usage of controller fragment but with cleaner more logical descriptions for assigned buttons.
Allows for image background during controller usage.
 */

class ControllerOverviewActivity : AppCompatActivity() {
    private var overlayHideRunnable: Runnable? = null
    private val overlayHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private lateinit var controllerFragment: ControllerSupportFragment

    private val repeatIntervalMs = 100L // 100 ms
    private var repeatHandler: android.os.Handler? = null
    private var repeatRunnable: Runnable? = null

    /*
        input:    buttonID - Int, btnName - String
        output:   None
        remarks:  triggers simulateControllerButtonPress via repeat 100ms loops of button press
    */
    private fun setupRepeatButton(buttonId: Int, btnName: String) {
        val button = findViewById<MaterialButton>(buttonId)
        button.setOnTouchListener {_, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    simulateControllerButtonPress(btnName)
                    repeatHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    repeatRunnable = object : Runnable {
                        override fun run() {
                            simulateControllerButtonPress(btnName)
                            repeatHandler?.postDelayed(this, repeatIntervalMs)
                        }
                    }
                    repeatHandler?.postDelayed(repeatRunnable!!, repeatIntervalMs)
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    repeatHandler?.removeCallbacks(repeatRunnable!!)
                    repeatHandler = null
                    repeatRunnable = null
                    true
                }
                else -> false
            }
        }
    }
    
    // Callback for fragment to notify activity when cycling presets
    interface PresetOverlayCallback {
        fun onShowPresetsOverlay()
        fun onPresetCycled()
    }

    private val presetOverlayCallback = object : PresetOverlayCallback {
        override fun onShowPresetsOverlay() {
            showPresetsOverlay()
        }
        override fun onPresetCycled() {
            setupUi()
        }
    }

    /*
        input:    event (controller events ie button press etc) - Boolean
        output:   key event
        remarks:  Intercept controller events before UI consumes them
    */
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (::controllerFragment.isInitialized && controllerFragment.isAdded) {
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                val btnName = controllerFragment.keyCodeToButtonName(event.keyCode)
                val assignments = controllerFragment.loadButtonAssignments(controllerFragment.getControllerButtonList())
                val assignedAction = assignments[btnName]
                if (assignedAction != null) {
                    when (assignedAction.displayName) {
                        "Cycle Presets Forwards" -> {
                            controllerFragment.cyclePreset(next = true)
                            return true
                        }
                        "Cycle Presets Backwards" -> {
                            controllerFragment.cyclePreset(next = false)
                            return true
                        }
                    }
                }
            }
            val handled = when (event.action) {
                android.view.KeyEvent.ACTION_DOWN -> controllerFragment.handleKeyDown(event.keyCode, event)
                android.view.KeyEvent.ACTION_UP -> controllerFragment.handleKeyUp(event.keyCode, event)
                else -> false
            }
            if (handled) return true
        }
        return super.dispatchKeyEvent(event)
    }

    /*
        input:    keyCode - int, event - keyEvent
        output:   key event passthrough
        remarks:  Forward key and motion events to fragment using custom handler methods
    */
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (::controllerFragment.isInitialized && controllerFragment.isAdded) {
            controllerFragment.handleKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }

    /*
        input:    keyCode - Int, event - keyEvent
        output:   key Event passthrough
        remarks:  not currently used, but can be integrated for better responsiveness
    */
    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (::controllerFragment.isInitialized && controllerFragment.isAdded) {
            controllerFragment.handleKeyUp(keyCode, event)
        }
        return super.onKeyUp(keyCode, event)
    }

    /*
        input:    savedInstaceState - bundle (previous state of ui if existing)
        output:   None
        remarks:  Navigates to main activity upon button press, uses controller support fragment as callback for key event action passthrough, init ui, allows for background image if subscribed to
    */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controller_overview)

        findViewById<View>(R.id.button_back_to_main).setOnClickListener {
            val intent = android.content.Intent(this, MainActivity::class.java)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // Add ControllerSupportFragment as retained fragment (background)
        val fragMgr = supportFragmentManager
        val existing = fragMgr.findFragmentByTag("ControllerSupportFragment")
        if (existing is ControllerSupportFragment) {
            controllerFragment = existing
        } else {
            controllerFragment = ControllerSupportFragment()
            fragMgr.beginTransaction()
                .add(controllerFragment, "ControllerSupportFragment")
                .commitNow()
        }
        // Set the callback so fragment can notify activity
        controllerFragment.presetOverlayCallback = presetOverlayCallback

        setupRepeatButton(R.id.button_y, "Button Y")
        setupRepeatButton(R.id.button_x, "Button X")
        setupRepeatButton(R.id.button_b, "Button B")
        setupRepeatButton(R.id.button_a, "Button A")

        // Obtain RosViewModel from Application (same as in Ros2TopicSubscriberActivity)
        val app = application as MyApp
        val rosViewModel = androidx.lifecycle.ViewModelProvider(
            app.appViewModelStore,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )[RosViewModel::class.java]

        val backgroundImageView = findViewById<android.widget.ImageView>(R.id.backgroundImageView)
        lifecycleScope.launch {
            rosViewModel.latestBitmap.collect { bitmap ->
                if (bitmap != null) {
                    backgroundImageView.setImageBitmap(bitmap)
                    backgroundImageView.visibility = View.VISIBLE
                } else {
                    backgroundImageView.setImageDrawable(null)
                    backgroundImageView.visibility = View.GONE
                }
            }
        }
        

        setupUi()
    }

    /*
        input:    None
        output:   None
        remarks:  Does the bulk of the work connecting logic to UI for buttons
    */
    private fun setupUi() {
        // ABXY assignments (now TextViews)
        val assignments = controllerFragment.loadButtonAssignments(controllerFragment.getControllerButtonList())

        // Populate ABXY assignment and message fields
        val abxyAssignmentIds = mapOf(
            "Y" to R.id.assignment_y,
            "X" to R.id.assignment_x,
            "B" to R.id.assignment_b,
            "A" to R.id.assignment_a
        )
        
        abxyAssignmentIds.forEach { (key, id) ->
            val tv = findViewById<TextView>(id)
            val action = assignments["Button $key"]
            tv.text = action?.displayName ?: "<none>"
            tv.setOnClickListener {
                showActionDetails(action, tv)
            }
        }
        
        // Triggers
        setAssignmentLabel(R.id.assignment_l1, assignments["L1"])
        setAssignmentLabel(R.id.assignment_l2, assignments["L2"])
        setAssignmentLabel(R.id.assignment_r1, assignments["R1"])
        setAssignmentLabel(R.id.assignment_r2, assignments["R2"])

        // Joysticks
        val joystickMappings = controllerFragment.loadJoystickMappings()
        val leftJoystick = joystickMappings.getOrNull(0)
        val rightJoystick = joystickMappings.getOrNull(1)
        setJoystickAssignment(R.id.assignment_left_joystick, leftJoystick)
        setJoystickAssignment(R.id.assignment_right_joystick, rightJoystick)

        // Select/Start
        setAssignmentLabel(R.id.assignment_select, assignments["Select"])
        setAssignmentLabel(R.id.assignment_start, assignments["Start"])

        val presets = controllerFragment.loadControllerPresets()
        val selectedIdx = getSelectedPresetIdx()
        // Overlay: full presets list, vertical, initially hidden
        val overlayContainer = findViewById<View>(R.id.presets_overlay_container)
        val overlayList = findViewById<LinearLayout>(R.id.presets_overlay_list)
        overlayList.removeAllViews()
        presets.forEachIndexed { idx, preset ->
            val presetView = layoutInflater.inflate(R.layout.preset_item_view, overlayList, false)
            presetView.findViewById<TextView>(R.id.preset_name).text = preset.name
            // Show subactions horizontally (comma separated)
            val subActions = preset.abxy.values.filter { it.isNotEmpty() }.joinToString(", ")
            presetView.findViewById<TextView>(R.id.preset_subactions).text = subActions
            if (idx == selectedIdx) {
                presetView.setBackgroundColor(resources.getColor(R.color.preset_selected_bg, theme))
                presetView.findViewById<TextView>(R.id.preset_name).setTextColor(resources.getColor(R.color.preset_selected_text, theme))
                presetView.findViewById<TextView>(R.id.preset_subactions).setTextColor(resources.getColor(R.color.preset_selected_text, theme))
            }
            overlayList.addView(presetView)
        }
        val scrollView = findViewById<android.widget.HorizontalScrollView>(R.id.presets_overlay_container)
        val selectedView = overlayList.getChildAt(selectedIdx)
        selectedView?.let {
            scrollView.post {
                scrollView.smoothScrollTo(it.left, 0)
            }
        }
        // Hide overlay by default
        overlayContainer.visibility = View.GONE
    }

    /*
        input:    None
        output:   None
        remarks:  Call this to show the overlay (e.g., when cycling presets)
    */
    private fun showPresetsOverlay() {
        val overlayContainer = findViewById<View>(R.id.presets_overlay_container)
        overlayContainer.visibility = View.VISIBLE
        // Cancel previous hide requests
        overlayHideRunnable?.let { overlayHandler.removeCallbacks(it) }
        // Schedule new hide
        overlayHideRunnable = Runnable {
            overlayContainer.visibility = View.GONE
        }
        overlayHandler.postDelayed(overlayHideRunnable!!, 1500)
    }

    /*
        input:    None
        output:   None
        remarks:  Call to hide overlay
    */
    private fun hidePresetsOverlay() {
        val overlayContainer = findViewById<View>(R.id.presets_overlay_container)
        overlayContainer.visibility = View.GONE
    }

    /*
        input:    id - int, action - AppAction (custom framework for app action)
        output:   None
        remarks:  Setter for button app action label
    */
    private fun setAssignmentLabel(id: Int, action: AppAction?) {
        val tv = findViewById<TextView>(id)
        tv.text = action?.displayName ?: "<none>"
        tv.setOnClickListener {
            showActionDetails(action, tv)
        }
    }

    /*
        input:    id - int, mapping - joystick map
        output:   None
        remarks:  sets map of joystick, listener for details for joystick
    */
    private fun setJoystickAssignment(id: Int, mapping: JoystickMapping?) {
        val tv = findViewById<TextView>(id)
        tv.text = if (mapping != null) {
            mapping.type ?: "Unassigned"
        } else {
            "<none>"
        }
        tv.setOnClickListener {
            showJoystickDetails(mapping, tv)
        }
    }

    /*
        input:    event - motionEvent
        output:   Boolean
        remarks:  Consumes controller event for joystick
    */
    override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
        if (::controllerFragment.isInitialized && controllerFragment.isAdded) {
            if (controllerFragment.handleGenericMotionEvent(event)) {
                return true
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    /*
        input:    Int
        output:   perfs idx id - int
        remarks:  passes in from shared preferences ID of controller
    */
    private fun getSelectedPresetIdx(): Int {
        val prefs = this@ControllerOverviewActivity.getSharedPreferences("controller_presets", android.content.Context.MODE_PRIVATE)
        return prefs.getInt("selected_preset_idx", 0)
    }

    /*
        input:   action - AppAction, anchor - View 
        output:  None 
        remarks: logic for popup for more info listener for app actions 
    */
    private fun showActionDetails(action: AppAction?, anchor: View) {
        if (action == null) return
        val dialog = android.app.AlertDialog.Builder(this@ControllerOverviewActivity)
            .setTitle(action.displayName)
            .setMessage("Type: ${action.type}\nTopic: ${action.topic}\nSource: ${action.source}\nMsg: ${action.msg}")
            .setPositiveButton("OK", null)
            .create()
        dialog.show()
    }

    /*
        input:    mapping - JoystickMapping, anchor - View
        output:   None
        remarks:  creates pop up dialogue for joystick when pressed (backend logic)
    */
    private fun showJoystickDetails(mapping: JoystickMapping?, anchor: View) {
        if (mapping == null) return
        val dialog = android.app.AlertDialog.Builder(this@ControllerOverviewActivity)
            .setTitle(mapping.displayName)
            .setMessage("Topic: ${mapping.topic}\nType: ${mapping.type}\nMax: ${mapping.max}\nStep: ${mapping.step}\nDeadzone: ${mapping.deadzone}")
            .setPositiveButton("OK", null)
            .create()
        dialog.show()
    }

    /*
        input:    btnName - String
        output:   None
        remarks:  function that passes action in this activity to controller fragment for processing
    */
    private fun simulateControllerButtonPress(btnName: String) {
        val assignments = controllerFragment.loadButtonAssignments(controllerFragment.getControllerButtonList())
        val assignedAction = assignments[btnName]
        if (assignedAction != null) {
            controllerFragment.triggerAppAction(assignedAction)
        }
    }
}
