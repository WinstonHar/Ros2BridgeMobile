package com.examples.testros2jsbridge.domain.usecase.controller

import android.content.Context
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.model.ControllerPreset
import com.examples.testros2jsbridge.domain.repository.AppActionRepository
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoadAllControllerConfigsUseCase @Inject constructor(
    private val controllerRepository: ControllerRepository,
    private val appActionRepository: AppActionRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun load(): List<ControllerConfig> {
        return withContext(Dispatchers.IO) {
            val controllers = controllerRepository.getControllers().first()
            val allActions = appActionRepository.getCustomAppActions(context).first()

            fun getActionById(id: String): AppAction? {
                return allActions.find { it.id == id }
            }

            controllers.map { controller ->
                ControllerConfig(
                    name = controller.name,
                    controllerPresets = controller.presets.map {
                        ControllerPreset(
                            name = it.name,
                            topic = null,
                            buttonAssignments = it.buttonMaps.mapNotNull { buttonMap ->
                                getActionById(buttonMap.mappedActionId)?.let {
                                    buttonMap.inputType to it
                                }
                            }.toMap(),
                            joystickMappings = emptyList()
                        )
                    },
                    buttonAssignments = controller.fixedButtonMaps.mapNotNull { buttonMap ->
                        getActionById(buttonMap.mappedActionId)?.let {
                            buttonMap.inputType to it
                        }
                    }.toMap(),
                    joystickMappings = emptyList()
                )
            }
        }
    }
}
