package com.examples.testros2jsbridge.domain.repository

/*
Configuration management interface
 */

import com.examples.testros2jsbridge.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ConfigurationRepository : com.examples.testros2jsbridge.core.base.RosRepository {
    val configuration: Flow<AppConfiguration> // Use Flow for reactive updates

    suspend fun saveConfiguration(configuration: AppConfiguration)
    suspend fun getConfiguration(): AppConfiguration
}