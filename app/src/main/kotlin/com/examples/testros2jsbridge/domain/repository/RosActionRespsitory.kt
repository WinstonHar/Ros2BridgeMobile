package com.examples.testros2jsbridge.domain.repository

/*
Action handling interface
 */

import com.examples.testros2jsbridge.domain.model.*
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosActionDto
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.ActionFieldValue
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface RosActionRepository {
    val actions: MutableStateFlow<List<RosAction>>

    fun forceClearAllActionBusyLocks()
    fun sendOrQueueActionGoal(
        actionName: String,
        actionType: String,
        goalFields: Map<String, ActionFieldValue>,
        goalUuid: String,
        onResult: ((RosActionDto) -> Unit)?
    )
    fun cancelActionGoal(actionName: String, actionType: String, uuid: String)
    fun subscribeToActionFeedback(
        actionName: String,
        actionType: String,
        onMessage: (RosActionDto) -> Unit
    )
    fun subscribeToActionStatus(
        actionName: String,
        onMessage: (String) -> Unit
    )
    fun actionStatusFlow(actionName: String): SharedFlow<Map<String, String>>

    suspend fun saveAction(action: RosAction)
    suspend fun getAction(actionId: RosId): RosAction
}