package com.examples.testros2jsbridge.domain.usecase.controller

import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoadControllerConfigUseCase @Inject constructor(
    private val controllerRepository: ControllerRepository
) {
    suspend fun load(): ControllerConfig {
        return withContext(Dispatchers.IO) {
            controllerRepository.getController()
        }
    }
}
