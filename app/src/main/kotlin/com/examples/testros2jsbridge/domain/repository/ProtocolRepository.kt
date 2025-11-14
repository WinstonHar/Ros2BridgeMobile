package com.examples.testros2jsbridge.domain.repository

import android.content.Context
import com.examples.testros2jsbridge.domain.model.CustomProtocol

interface ProtocolRepository {
    suspend fun getMessageFiles(context: Context): List<CustomProtocol>
    suspend fun getServiceFiles(context: Context): List<CustomProtocol>
    suspend fun getActionFiles(context: Context): List<CustomProtocol>
    suspend fun importProtocols(context: Context, selected: Set<String>): List<CustomProtocol>
    suspend fun saveCustomAppAction(action: com.examples.testros2jsbridge.domain.model.AppAction, context: Context)
    suspend fun getCustomAppActions(context: Context): List<com.examples.testros2jsbridge.domain.model.AppAction>
    suspend fun deleteCustomAppAction(actionId: String, context: Context)
    suspend fun getAvailablePackages(context: Context): List<String>
}

