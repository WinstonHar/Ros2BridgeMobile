package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.local.database.dao.AppActionDao
import com.examples.testros2jsbridge.data.local.database.dao.ControllerDao
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControllerRepositoryImpl @Inject constructor(
    private val controllerDao: ControllerDao,
    private val appActionDao: AppActionDao
) : ControllerRepository {

    private val _controller = MutableStateFlow(ControllerConfig())
    override val controller: Flow<ControllerConfig> = _controller

    override suspend fun saveController(controller: ControllerConfig) {
        // TODO: Implement save controller to database
    }

    override suspend fun getController(): ControllerConfig {
        // TODO: Implement get controller from database
        return _controller.value
    }

    override fun getSelectedConfigName(selectedConfigKey: String): String? {
        // TODO: This should be handled differently, perhaps by storing the selected config ID in SharedPreferences
        return null
    }
}
