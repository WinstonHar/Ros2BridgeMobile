package com.examples.testros2jsbridge.domain.usecase.controller

import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.JoystickMapping
import com.examples.testros2jsbridge.domain.repository.AppActionRepository
import javax.inject.Inject

class HandleControllerInputUseCase @Inject constructor(
    private val appActionRepository: AppActionRepository
) {
    fun handleKeyEvent(keyCode: Int, assignments: Map<String, com.examples.testros2jsbridge.domain.model.AppAction>): com.examples.testros2jsbridge.domain.model.AppAction? {
        val buttonName = com.examples.testros2jsbridge.presentation.ui.screens.controller.keyCodeToButtonName(keyCode)
        return assignments[buttonName]
    }

    fun handleJoystickInput(x: Float, y: Float, mapping: JoystickMapping): Pair<Float, Float> {
        // Apply deadzone
        val deadzone = mapping.deadzone ?: 0.1f
        val effectiveX = if (kotlin.math.abs(x) > deadzone) x else 0f
        val effectiveY = if (kotlin.math.abs(y) > deadzone) y else 0f

        // Apply sensitivity and step
        val sensitivity = mapping.max ?: 1.0f
        val step = mapping.step ?: 0.1f
        val scaledX = (effectiveX * sensitivity / step).toInt() * step
        val scaledY = (effectiveY * sensitivity / step).toInt() * step

        return Pair(scaledX, scaledY)
    }

    fun triggerAppAction(action: com.examples.testros2jsbridge.domain.model.AppAction) {
        //TODO call repository to trigger action
    }
}
