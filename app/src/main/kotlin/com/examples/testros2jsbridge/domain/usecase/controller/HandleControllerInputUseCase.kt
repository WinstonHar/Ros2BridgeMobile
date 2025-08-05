package com.examples.testros2jsbridge.domain.usecase.controller

import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.model.JoystickMapping

/**
 * Handles key and joystick input events.
 */
class HandleControllerInputUseCase {


    private val keyCodeToButtonMap: Map<Int, String> = mapOf(
        96 to "Button A",
        97 to "Button B",
        99 to "Button X",
        100 to "Button Y",
        102 to "L1",
        103 to "R1",
        104 to "L2",
        105 to "R2",
        108 to "Start",
        82 to "Select"
    )

    fun handleKeyEvent(keyCode: Int, assignments: Map<String, AppAction>): AppAction? {
        val buttonName = keyCodeToButtonMap[keyCode]
        return buttonName?.let { assignments[it] }
    }

    fun handleJoystickInput(
        x: Float,
        y: Float,
        mapping: JoystickMapping
    ): Pair<Float, Float> {
        val maxValue = mapping.max ?: 1.0f
        val stepValue = mapping.step ?: 0.2f
        val deadzoneValue = mapping.deadzone ?: 0.1f

        val quantX = Math.signum(x) * Math.ceil(((Math.abs(x) - deadzoneValue) / stepValue).toDouble()) + deadzoneValue
        val quantY = Math.signum(y) * Math.ceil(((Math.abs(y) - deadzoneValue) / stepValue).toDouble()) + deadzoneValue

        return clamped(quantX, maxValue) to clamped(quantY, maxValue)
    }

    private fun clamped(value: Float, maxValue: Float): Float {
        return Math.min(Math.max(value, -maxValue), maxValue)
    }
}