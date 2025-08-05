package com.examples.testros2jsbridge.domain.usecase.service

import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.repository.RosServiceRepository

class CallServiceUseCase(private val rosServiceRepository: RosServiceRepository) {

    suspend fun call(serviceId: RosId) {
        rosServiceRepository.getService(serviceId) // Retrieve service before calling
    }
}