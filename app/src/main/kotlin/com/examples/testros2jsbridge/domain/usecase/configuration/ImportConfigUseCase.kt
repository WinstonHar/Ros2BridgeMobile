package com.examples.testros2jsbridge.domain.usecase.configuration

import com.examples.testros2jsbridge.domain.model.AppConfiguration
import com.examples.testros2jsbridge.domain.repository.ConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImportConfigUseCase @Inject constructor(
    private val configurationRepository: ConfigurationRepository
) {

    suspend fun importConfig(config: AppConfiguration) {
        withContext(Dispatchers.IO) {
            configurationRepository.saveConfig(config)
        }
    }
}