package com.examples.testros2jsbridge.domain.usecase.configuration

import com.examples.testros2jsbridge.domain.model.AppConfiguration
import com.examples.testros2jsbridge.domain.repository.ConfigurationRepository
import kotlinx.coroutines.withContext

class ImportConfigUseCase(
    private val configurationRepository: ConfigurationRepository
) : ImportConfigUseCase {

    override suspend fun importConfig(config: AppConfiguration) {
        withContext(Dispatchers.IO) {
            configurationRepository.save(config)
        }
    }
}