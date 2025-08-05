package com.examples.testros2jsbridge.data.local.database.dao

import com.examples.testros2jsbridge.domain.model.AppConfiguration
import com.examples.testros2jsbridge.domain.model.RosId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Centralized config DAO for managing AppConfiguration.
 */
class ConfigurationDao {
    private val _configuration = MutableStateFlow<AppConfiguration?>(null)
    val configuration: StateFlow<AppConfiguration?> get() = _configuration

    fun saveConfiguration(config: AppConfiguration) {
        _configuration.value = config
    }

    fun getConfiguration(): AppConfiguration? {
        return _configuration.value
    }

    fun clearConfiguration() {
        _configuration.value = null
    }
}