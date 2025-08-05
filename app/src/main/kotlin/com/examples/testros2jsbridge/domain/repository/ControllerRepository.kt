package com.examples.testros2jsbridge.domain.repository

/*
Controller logic interface
 */

import com.examples.testros2jsbridge.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ControllerRepository : com.examples.testros2jsbridge.core.base.RosRepository {
    val controller: Flow<ControllerConfig>

    suspend fun saveController(controller: ControllerConfig)
    suspend fun getController(): ControllerConfig
}