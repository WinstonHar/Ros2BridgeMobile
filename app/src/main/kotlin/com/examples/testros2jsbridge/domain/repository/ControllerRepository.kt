package com.examples.testros2jsbridge.domain.repository

/*
Controller logic interface
 */

import com.examples.testros2jsbridge.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ControllerRepository {
    val controller: Flow<ControllerConfig>

    suspend fun saveController(controller: ControllerConfig)
    suspend fun getController(): ControllerConfig
    suspend fun getAllControllerConfigs(): List<ControllerConfig>
    suspend fun getSelectedConfigName(selectedConfigKey: String): String?
    suspend fun saveSelectedConfigName(configName: String)
}