package com.examples.testros2jsbridge.domain.usecase.service

import com.examples.testros2jsbridge.domain.model.RosService
import com.examples.testros2jsbridge.domain.repository.RosServiceRepository
import javax.inject.Inject

class AdvertiseServiceUseCase @Inject constructor(private val rosServiceRepository: RosServiceRepository) {

    suspend fun advertise(service: RosService) {
        rosServiceRepository.saveService(service)
    }
}