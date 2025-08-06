package com.examples.testros2jsbridge.domain.usecase.configuration

import com.examples.testros2jsbridge.domain.model.AppConfiguration
import com.examples.testros2jsbridge.domain.repository.ConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExportConfigUseCase @Inject constructor(
    private val configurationRepository: ConfigurationRepository
) {

    suspend fun exportConfig(): AppConfiguration {
        return withContext(Dispatchers.IO) {
            configurationRepository.getConfig()
        }
    }
}