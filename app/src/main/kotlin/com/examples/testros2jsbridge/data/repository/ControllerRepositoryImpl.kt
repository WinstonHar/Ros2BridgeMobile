package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.local.database.dao.AppActionDao
import com.examples.testros2jsbridge.data.local.database.dao.ControllerConfigDao
import com.examples.testros2jsbridge.data.local.database.dao.SelectedConfigDao
import com.examples.testros2jsbridge.data.local.database.entities.SelectedConfig
import com.examples.testros2jsbridge.data.mapper.toDomain
import com.examples.testros2jsbridge.data.mapper.toEntity
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControllerRepositoryImpl @Inject constructor(
    private val appActionDao: AppActionDao,
    private val selectedConfigDao: SelectedConfigDao,
    private val controllerConfigDao: ControllerConfigDao
) : ControllerRepository {

    override val controller: Flow<ControllerConfig> = flow { emit(getController()) }

    override suspend fun saveController(controller: ControllerConfig) {
        controllerConfigDao.insert(controller.toEntity())
    }

    override suspend fun getController(): ControllerConfig {
        val selectedConfigName = selectedConfigDao.getSelectedConfig()?.selectedConfigName
        return if (selectedConfigName != null) {
            val allAppActions = appActionDao.getAllAppActions().first().map { it.toDomain() }
            controllerConfigDao.getControllerConfig(selectedConfigName)?.toDomain(allAppActions) ?: ControllerConfig()
        } else {
            ControllerConfig()
        }
    }

    override suspend fun getAllControllerConfigs(): List<ControllerConfig> {
        val allAppActions = appActionDao.getAllAppActions().first().map { it.toDomain() }
        return controllerConfigDao.getAllControllerConfigs().map { it.toDomain(allAppActions) }
    }

    override suspend fun getSelectedConfigName(selectedConfigKey: String): String? {
        return selectedConfigDao.getSelectedConfig()?.selectedConfigName
    }

    override suspend fun saveSelectedConfigName(configName: String) {
        selectedConfigDao.setSelectedConfig(SelectedConfig(selectedConfigName = configName))
    }
}
