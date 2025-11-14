package com.examples.testros2jsbridge.domain.repository

/*
Service call interface
 */

import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosServiceDto
import com.examples.testros2jsbridge.domain.model.RosService
import kotlinx.coroutines.flow.StateFlow

interface RosServiceRepository {
    val services: StateFlow<List<RosServiceDto>>

    suspend fun getServices(): List<RosServiceDto>
    fun forceClearAllServiceBusyLocks()
    fun sendOrQueueServiceRequest(
        serviceName: String,
        serviceType: String,
        requestDto: RosServiceDto,
        onResult: (RosServiceDto) -> Unit
    )
    fun onServiceResponse(responseDto: RosServiceDto)
    fun saveService(service: RosService)
    fun getService(serviceId: com.examples.testros2jsbridge.domain.model.RosId)
}