package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosServiceDto
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.RosService
import com.examples.testros2jsbridge.domain.repository.RosServiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RosServiceRepositoryImpl @Inject constructor() : RosServiceRepository {
    private val _services = MutableStateFlow<List<RosServiceDto>>(emptyList())
    override val services: StateFlow<List<RosServiceDto>> = _services.asStateFlow()

    private val savedServices = mutableMapOf<String, RosService>()

    override suspend fun getServices(): List<RosServiceDto> {
        // TODO: Implement logic to fetch available services from ROS bridge
        return _services.value
    }

    override fun forceClearAllServiceBusyLocks() {
        // Clear all busy locks for services
        // Implementation can be added based on how busy locks are managed
    }

    override fun sendOrQueueServiceRequest(
        serviceName: String,
        serviceType: String,
        requestDto: RosServiceDto,
        onResult: (RosServiceDto) -> Unit
    ) {
        // TODO: Implement service request sending logic
        // This would typically interact with a ROS bridge service
    }

    override fun onServiceResponse(responseDto: RosServiceDto) {
        // TODO: Handle service response
        // Update the service list with the response
    }

    override fun saveService(service: RosService) {
        service.id?.let { id ->
            savedServices[id] = service
        }
    }

    override fun getService(serviceId: RosId) {
        savedServices[serviceId.value]
            ?: throw NoSuchElementException("Service with id ${serviceId.value} not found")
    }
}
