package com.examples.testros2jsbridge.domain.usecase.configuration

import com.examples.testros2jsbridge.domain.model.AppConfiguration
import com.examples.testros2jsbridge.domain.repository.ConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ValidateConfigUseCase @Inject constructor(
    private val configurationRepository: ConfigurationRepository
) {

     suspend fun isValidConfig(config: AppConfiguration): Boolean {
        return withContext(Dispatchers.IO) {
            // Validate ROS Server URL
            val urlPattern = Regex("""^wss?://[^\s]+$""")
            val isValidServerUrl = config.rosServerUrl.value.isNotBlank() && urlPattern.matches(config.rosServerUrl.value)

            // Validate Port
            val isValidPort = config.connectionTimeoutMs > 0 && config.maxRetryCount > 0

            // Validate Topic Names
            val topicPattern = Regex("""^/[a-zA-Z0-9_/]+$""")
            val isValidPublishTopic = topicPattern.matches(config.defaultPublishTopic.value)
            val isValidSubscribeTopic = topicPattern.matches(config.defaultSubscribeTopic.value)

            // Validate User Name
            val isValidUserName = config.userName.isNotBlank()

            // Validate Language
            val supportedLanguages = setOf("en", "es", "de", "fr", "zh")
            val isValidLanguage = config.language in supportedLanguages

            // Validate Custom Button Presets
            val isValidButtonPresets = config.customButtonPresets.all { (k, v) -> k.isNotBlank() && v.isNotBlank() }

            // Validate File Paths (if provided)
            val isValidExportPath = config.exportProfilePath?.isNotBlank() ?: true
            val isValidImportPath = config.importProfilePath?.isNotBlank() ?: true

            // Combine all checks
            isValidServerUrl && isValidPort && isValidPublishTopic && isValidSubscribeTopic &&
            isValidUserName && isValidLanguage && isValidButtonPresets && isValidExportPath && isValidImportPath
        }
    }
}