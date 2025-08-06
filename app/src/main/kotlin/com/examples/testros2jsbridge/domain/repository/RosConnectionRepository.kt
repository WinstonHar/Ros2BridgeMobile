package com.examples.testros2jsbridge.domain.repository

/*
Interface for connection abstraction
 */

import com.examples.testros2jsbridge.domain.model.*
import kotlinx.coroutines.flow.StateFlow

interface RosConnectionRepository {
    val connections: StateFlow<List<RosConnection>>

    suspend fun saveConnection(connection: RosConnection?)
    suspend fun getConnection(connectionId: RosId): RosConnection?
}