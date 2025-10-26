package com.examples.testros2jsbridge.data.repository

import android.content.Context
import com.examples.testros2jsbridge.data.local.database.dao.AppActionDao
import com.examples.testros2jsbridge.data.mapper.toDomain
import com.examples.testros2jsbridge.data.mapper.toEntity
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.CustomProtocol
import com.examples.testros2jsbridge.domain.repository.ProtocolRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtocolRepositoryImpl @Inject constructor(
    private val appActionDao: AppActionDao
) : ProtocolRepository {
    override suspend fun getMessageFiles(context: Context): List<CustomProtocol> {
        // TODO: Implement message file loading
        return emptyList()
    }

    override suspend fun getServiceFiles(context: Context): List<CustomProtocol> {
        // TODO: Implement service file loading
        return emptyList()
    }

    override suspend fun getActionFiles(context: Context): List<CustomProtocol> {
        // TODO: Implement action file loading
        return emptyList()
    }

    override suspend fun importProtocols(context: Context, selected: Set<String>): List<CustomProtocol> {
        // TODO: Implement protocol import
        return emptyList()
    }

    override suspend fun saveCustomAppAction(action: AppAction, context: Context) {
        appActionDao.insertAppAction(action.toEntity())
    }

    override suspend fun getCustomAppActions(context: Context): List<AppAction> {
        return appActionDao.getAllAppActions().first().map { it.toDomain() }
    }

    override suspend fun deleteCustomAppAction(actionId: String, context: Context) {
        appActionDao.getAppActionById(actionId).first()?.let {
            appActionDao.deleteAppAction(it)
        }
    }

    override suspend fun getAvailablePackages(context: Context): List<String> {
        // TODO: Implement available packages loading
        return emptyList()
    }
}
