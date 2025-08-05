package com.examples.testros2jsbridge.domain.repository

/*
Interface for connection abstraction
 */

import com.examples.testros2jsbridge.domain.model.*
import kotlinx.coroutines.flow.Flow

interface RosConnectionRepository : com.examples.testros2jsbridge.core.base.RosRepository {
    val connections: Flow<RosConnection>

    suspend fun saveConnection(connection: RosConnection)
    suspend fun getConnection(connectionId: RosId): RosConnection
}