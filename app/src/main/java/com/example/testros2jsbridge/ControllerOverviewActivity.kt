package com.example.testros2jsbridge

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.testros2jsbridge.ControllerSupportFragment.AppAction
import com.example.testros2jsbridge.ControllerSupportFragment.JoystickMapping

class ControllerOverviewActivity : AppCompatActivity() {
    private var overlayHideRunnable: Runnable? = null
    private val overlayHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private lateinit var controllerFragment: ControllerSupportFragment

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

    // Intercept controller events before UI consumes them
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

    override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
        if (::controllerFragment.isInitialized && controllerFragment.isAdded) {
            val handled = controllerFragment.handleGenericMotionEvent(event)
            if (handled) return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    // Forward key and motion events to fragment using custom handler methods
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (::controllerFragment.isInitialized && controllerFragment.isAdded) {
            controllerFragment.handleKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (::controllerFragment.isInitialized && controllerFragment.isAdded) {
            controllerFragment.handleKeyUp(keyCode, event)
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: android.view.MotionEvent?): Boolean {
        if (::controllerFragment.isInitialized && controllerFragment.isAdded) {
            controllerFragment.handleGenericMotionEvent(event)
        }
        return super.onGenericMotionEvent(event)
    }

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
            controllerFragment.retainInstance = true
            fragMgr.beginTransaction()
                .add(controllerFragment, "ControllerSupportFragment")
                .commitNow()
        }
        // Set the callback so fragment can notify activity
        controllerFragment.presetOverlayCallback = presetOverlayCallback

        setupUi()
    }

    private fun setupUi() {
        // ABXY assignments (now TextViews)
        val assignments = controllerFragment.loadButtonAssignments(controllerFragment.getControllerButtonList())
        val abxyLabels = mapOf(
            "Y" to findViewById<TextView>(R.id.label_y),
            "X" to findViewById<TextView>(R.id.label_x),
            "B" to findViewById<TextView>(R.id.label_b),
            "A" to findViewById<TextView>(R.id.label_a)
        )
        abxyLabels.forEach { (key, tv) ->
            val action = assignments["Button $key"]
            tv.text = key
            tv.setOnClickListener {
                showActionDetails(action, tv)
            }
        }
        // Populate ABXY assignment and message fields
        val abxyAssignmentIds = mapOf(
            "Y" to R.id.assignment_y,
            "X" to R.id.assignment_x,
            "B" to R.id.assignment_b,
            "A" to R.id.assignment_a
        )
        val abxyMsgIds = mapOf(
            "Y" to R.id.msg_y,
            "X" to R.id.msg_x,
            "B" to R.id.msg_b,
            "A" to R.id.msg_a
        )
        abxyAssignmentIds.forEach { (key, id) ->
            val tv = findViewById<TextView>(id)
            val action = assignments["Button $key"]
            tv.text = action?.displayName ?: "<none>"
        }
        abxyMsgIds.forEach { (key, id) ->
            val tv = findViewById<TextView>(id)
            val action = assignments["Button $key"]
            tv.text = action?.msg ?: ""
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
        // Populate joystick info fields
        val leftInfoTv = findViewById<TextView>(R.id.info_left_joystick)
        val rightInfoTv = findViewById<TextView>(R.id.info_right_joystick)
        leftInfoTv.text = leftJoystick?.let {
            "Topic: ${it.topic}\nType: ${it.type}\nMax: ${it.max}\nStep: ${it.step}\nDeadzone: ${it.deadzone}"
        } ?: ""
        rightInfoTv.text = rightJoystick?.let {
            "Topic: ${it.topic}\nType: ${it.type}\nMax: ${it.max}\nStep: ${it.step}\nDeadzone: ${it.deadzone}"
        } ?: ""

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
                presetView.setBackgroundResource(R.color.purple_200)
            }
            overlayList.addView(presetView)
        }
        // Hide overlay by default
        overlayContainer.visibility = View.GONE
    }

    // Call this to show the overlay (e.g., when cycling presets)
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

    // Call this to hide the overlay
    private fun hidePresetsOverlay() {
        val overlayContainer = findViewById<View>(R.id.presets_overlay_container)
        overlayContainer.visibility = View.GONE
    }

    private fun setAssignmentLabel(id: Int, action: AppAction?) {
        val tv = findViewById<TextView>(id)
        tv.text = action?.displayName ?: "<none>"
        tv.setOnClickListener {
            showActionDetails(action, tv)
        }
    }

    private fun setJoystickAssignment(id: Int, mapping: JoystickMapping?) {
        val tv = findViewById<TextView>(id)
        tv.text = mapping?.displayName ?: "<none>"
        tv.setOnClickListener {
            showJoystickDetails(mapping, tv)
        }
    }

    private fun getSelectedPresetIdx(): Int {
        val prefs = this@ControllerOverviewActivity.getSharedPreferences("controller_presets", android.content.Context.MODE_PRIVATE)
        return prefs.getInt("selected_preset_idx", 0)
    }

    private fun showActionDetails(action: AppAction?, anchor: View) {
        if (action == null) return
        val dialog = android.app.AlertDialog.Builder(this@ControllerOverviewActivity)
            .setTitle(action.displayName)
            .setMessage("Type: ${action.type}\nTopic: ${action.topic}\nSource: ${action.source}\nMsg: ${action.msg}")
            .setPositiveButton("OK", null)
            .create()
        dialog.show()
    }

    private fun showJoystickDetails(mapping: JoystickMapping?, anchor: View) {
        if (mapping == null) return
        val dialog = android.app.AlertDialog.Builder(this@ControllerOverviewActivity)
            .setTitle(mapping.displayName)
            .setMessage("Topic: ${mapping.topic}\nType: ${mapping.type}\nMax: ${mapping.max}\nStep: ${mapping.step}\nDeadzone: ${mapping.deadzone}")
            .setPositiveButton("OK", null)
            .create()
        dialog.show()
    }
}
