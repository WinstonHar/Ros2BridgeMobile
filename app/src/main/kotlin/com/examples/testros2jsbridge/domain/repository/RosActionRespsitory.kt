package com.examples.testros2jsbridge.domain.repository

/*
Action handling interface
 */

import com.examples.testros2jsbridge.domain.model.*
import kotlinx.coroutines.flow.Flow

interface RosActionRepository : com.examples.testros2jsbridge.core.base.RosRepository {
    val actions: Flow<RosAction>

    suspend fun saveAction(action: RosAction)
    suspend fun getAction(actionId: RosId): RosAction
}