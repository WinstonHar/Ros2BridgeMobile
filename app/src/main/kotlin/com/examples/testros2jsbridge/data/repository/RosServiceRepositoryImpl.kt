package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosServiceDto
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.RosService
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import com.examples.testros2jsbridge.domain.repository.RosServiceRepository
import javax.inject.Inject

class RosServiceRepositoryImpl @Inject constructor(
    private val rosbridgeClient: RosbridgeClient
) : RosServiceRepository {

    private val lastServiceRequestIdMap = mutableMapOf<String, String>()
    private val lastServiceBusyMap = mutableMapOf<String, Boolean>()
    private val pendingServiceRequestMap = mutableMapOf<String, MutableList<Pair<RosServiceDto, (RosServiceDto) -> Unit>>>()
    private val topicHandlers = mutableMapOf<String, (RosServiceDto) -> Unit>()

    private val _services = MutableStateFlow<List<RosServiceDto>>(emptyList())
    override val services: StateFlow<List<RosServiceDto>> get() = _services

    private val _selectedService = MutableStateFlow<RosServiceDto?>(null)
    val selectedService: StateFlow<RosServiceDto?> get() = _selectedService

    override suspend fun getServices(): List<RosServiceDto> {
        return _services.value
    }

    override fun forceClearAllServiceBusyLocks() {
        lastServiceRequestIdMap.clear()
        lastServiceBusyMap.clear()
        pendingServiceRequestMap.clear()
    }

    override fun sendOrQueueServiceRequest(
        serviceName: String,
        serviceType: String,
        requestDto: RosServiceDto,
        onResult: (RosServiceDto) -> Unit
    ) {
        val requestId = requestDto.id ?: "service_${serviceName}_${System.currentTimeMillis()}"
        // Queue or send the request depending on busy state
        if (lastServiceBusyMap[serviceName] == true) {
            pendingServiceRequestMap.getOrPut(serviceName) { mutableListOf() }.add(Pair(requestDto, onResult))
        } else {
            lastServiceBusyMap[serviceName] = true
            lastServiceRequestIdMap[serviceName] = requestId
            topicHandlers[requestId] = onResult
            val jsonString = Json.encodeToString(RosServiceDto.serializer(), requestDto.copy(id = requestId))
            rosbridgeClient.send(jsonString)
        }
    }

    override fun onServiceResponse(responseDto: RosServiceDto) {
        val id = responseDto.id
        if (id != null && topicHandlers.containsKey(id)) {
            topicHandlers[id]?.invoke(responseDto)
            topicHandlers.remove(id)
        }
        // Optionally update local service list
        _services.value = _services.value + responseDto
    }

    override fun saveService(service: RosService) {
        // Convert domain RosService to DTO and add to the list
        val dto = RosServiceDto(
            op = "call_service", // or "advertise_service" if needed
            service = service.serviceName,
            args = null, // You may want to parse service.request JSON to a Map<String, ActionFieldValue>
            id = service.id,
            result = null,
            values = null,
            error = service.errorMessage,
            isSystem = false,
            dataType = null,
            requestDataType = service.requestType,
            responseDataType = service.responseType
        )
        _services.value = _services.value.filter { it.id != dto.id } + dto
    }

    override fun getService(serviceId: RosId) {
        val found = _services.value.find { it.id == serviceId.value }
        _selectedService.value = found
    }
}