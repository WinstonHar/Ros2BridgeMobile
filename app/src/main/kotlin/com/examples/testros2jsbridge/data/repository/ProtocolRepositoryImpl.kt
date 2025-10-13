package com.examples.testros2jsbridge.data.repository

import android.content.Context
import com.examples.testros2jsbridge.domain.model.AppAction
import com.examples.testros2jsbridge.domain.model.CustomProtocol
import com.examples.testros2jsbridge.domain.repository.ProtocolRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtocolRepositoryImpl @Inject constructor() : ProtocolRepository {
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
        // TODO: Implement custom app action save
    }

    override suspend fun getCustomAppActions(context: Context): List<AppAction> {
        // TODO: Implement custom app action loading
        return emptyList()
    }

    override suspend fun deleteCustomAppAction(actionId: String, context: Context) {
        // TODO: Implement custom app action deletion
    }

    override suspend fun getAvailablePackages(context: Context): List<String> {
        // TODO: Implement available packages loading
        return emptyList()
    }
}
