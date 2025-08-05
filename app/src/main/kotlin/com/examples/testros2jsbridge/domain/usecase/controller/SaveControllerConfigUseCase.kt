package com.examples.testros2jsbridge.domain.usecase.controller

import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveControllerConfigUseCase(
    private val controllerRepository: ControllerRepository
) {
    suspend fun save(config: ControllerConfig) {
        withContext(Dispatchers.IO) {
            controllerRepository.saveController(config)
        }
    }
}