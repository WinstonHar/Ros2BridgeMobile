package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosServiceDto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.util.Log

class RosServiceRepositoryImpl(
    private val rosbridgeClient: RosbridgeClient
) : RosServiceRepository {

    // Use domain models for service state and requests
    private val subscriberDao = com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao()

    override suspend fun getServices(): List<com.examples.testros2jsbridge.domain.model.RosService> {
        // Collect all services from subscribers
        val allSubscribers = subscriberDao.subscribers.value
        val services = mutableListOf<com.examples.testros2jsbridge.domain.model.RosService>()
        allSubscribers.forEach { sub ->
            if (sub.type.contains("Service", ignoreCase = true)) {
                if (sub.serviceInfo != null) {
                    services.add(sub.serviceInfo)
                } else {
                    services.add(
                        com.examples.testros2jsbridge.domain.model.RosService(
                            serviceName = sub.topic.id,
                            requestType = sub.type,
                            responseType = sub.type,
                            request = "",
                            response = null,
                            status = "unknown",
                            errorMessage = null,
                            caller = null,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        return services.distinctBy { it.serviceName }
    }

    override fun forceClearAllServiceBusyLocks() {
        lastServiceRequestIdMap.clear()
        lastServiceBusyMap.clear()
        pendingServiceRequestMap.clear()
    }

    override fun sendOrQueueServiceRequest(
        serviceName: String,
        serviceType: String,
        requestJson: String,
        onResult: (String) -> Unit
    ) {
        // Use domain model for service request and response
        val requestId = "service_${serviceName}_${System.currentTimeMillis()}"
        val serviceRequest = com.examples.testros2jsbridge.domain.model.RosService(
            serviceName = serviceName,
            requestType = serviceType,
            responseType = serviceType,
            request = requestJson,
            response = null,
            status = "pending",
            errorMessage = null,
            caller = null,
            timestamp = System.currentTimeMillis()
        )
        // Queue or send the request depending on busy state
        if (lastServiceBusyMap[serviceName] == true) {
            pendingServiceRequestMap.getOrPut(serviceName) { mutableListOf() }.add(Pair(serviceRequest, onResult))
        } else {
            lastServiceBusyMap[serviceName] = true
            lastServiceRequestIdMap[serviceName] = requestId
            topicHandlers[requestId] = onResult
            val jsonObject = kotlinx.serialization.json.buildJsonObject {
                put("op", "call_service")
                put("service", serviceName)
                put("type", serviceType)
                put("args", kotlinx.serialization.json.Json.parseToJsonElement(requestJson))
                put("id", requestId)
            }
            rosbridgeClient.send(jsonObject.toString())
        }
    }

    fun onServiceResponse(text: String) {
        try {
            val json = Json.parseToJsonElement(text).jsonObject
            val op = json["op"]?.jsonPrimitive?.contentOrNull
            if (op == "service_response") {
                val id = json["id"]?.jsonPrimitive?.contentOrNull
                if (id != null && topicHandlers.containsKey(id)) {
                    topicHandlers[id]?.invoke(text)
                    topicHandlers.remove(id)
                }
            }
        } catch (e: Exception) {
            Log.w("RosServiceRepo", "Error in onServiceResponse: ${e.message}")
        }
    }
}