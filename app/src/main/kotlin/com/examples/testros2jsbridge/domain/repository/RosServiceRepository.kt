package com.examples.testros2jsbridge.domain.repository

/*
Service call interface
 */

import com.examples.testros2jsbridge.domain.model.*
import kotlinx.coroutines.flow.Flow

interface RosServiceRepository : com.examples.testros2jsbridge.core.base.RosRepository {
    val services: Flow<RosService>

    suspend fun saveService(service: RosService)
    suspend fun getService(serviceId: RosId): RosService
}