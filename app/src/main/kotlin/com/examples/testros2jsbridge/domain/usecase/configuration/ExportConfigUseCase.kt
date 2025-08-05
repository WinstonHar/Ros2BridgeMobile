package com.examples.testros2jsbridge.domain.usecase.configuration

import com.examples.testros2jsbridge.domain.model.AppConfiguration
import com.examples.testros2jsbridge.domain.repository.ConfigurationRepository
import kotlinx.coroutines.withContext

class ExportConfigUseCase(
    private val configurationRepository: ConfigurationRepository
) : ExportConfigUseCase {

    override suspend fun exportConfig(): AppConfiguration {
        return withContext(Dispatchers.IO) {
            configurationRepository.get()
        }
    }
}