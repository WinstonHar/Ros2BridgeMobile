package com.examples.testros2jsbridge.domain.repository

import com.examples.testros2jsbridge.domain.model.AppConfiguration
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

interface ConfigurationRepository {
    val config: Flow<AppConfiguration?>

    suspend fun saveConfig(config: AppConfiguration)
    suspend fun getConfig(): AppConfiguration

    fun exportConfigToStream(out: OutputStream)
    fun importConfigFromStream(inp: InputStream)

    suspend fun getAllControllerConfigs(): List<ControllerConfig>

    suspend fun getSelectedConfigName(id: String): String?

    suspend fun saveSelectedConfigName(name: String)
}