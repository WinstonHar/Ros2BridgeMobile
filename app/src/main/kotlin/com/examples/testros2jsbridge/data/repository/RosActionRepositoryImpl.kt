package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.domain.model.RosAction
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.repository.RosActionRepository
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosActionDto
import com.examples.testros2jsbridge.data.remote.rosbridge.dto.ActionFieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RosActionRepositoryImpl @Inject constructor() : RosActionRepository {
    private val _actions = MutableStateFlow<List<RosAction>>(emptyList())
    override val actions: MutableStateFlow<List<RosAction>> = _actions

    private val actionStatusFlows = mutableMapOf<String, MutableSharedFlow<Map<String, String>>>()

    override fun forceClearAllActionBusyLocks() {
        // Clear all busy locks for actions
        // Implementation can be added based on how busy locks are managed
    }

    override fun sendOrQueueActionGoal(
        actionName: String,
        actionType: String,
        goalFields: Map<String, ActionFieldValue>,
        goalUuid: String,
        onResult: ((RosActionDto) -> Unit)?
    ) {
        // TODO: Implement action goal sending logic
        // This would typically interact with a ROS bridge service
    }

    override fun cancelActionGoal(actionName: String, actionType: String, uuid: String) {
        // TODO: Implement action goal cancellation logic
    }

    override fun subscribeToActionFeedback(
        actionName: String,
        actionType: String,
        onMessage: (RosActionDto) -> Unit
    ) {
        // TODO: Implement action feedback subscription logic
    }

    override fun subscribeToActionStatus(
        actionName: String,
        onMessage: (String) -> Unit
    ) {
        // TODO: Implement action status subscription logic
    }

    override fun actionStatusFlow(actionName: String): SharedFlow<Map<String, String>> {
        return actionStatusFlows.getOrPut(actionName) {
            MutableSharedFlow()
        }.asSharedFlow()
    }

    override suspend fun saveAction(action: RosAction) {
        val currentList = _actions.value.toMutableList()
        val index = currentList.indexOfFirst { it.actionName == action.actionName && it.goalId == action.goalId }
        if (index >= 0) {
            currentList[index] = action
        } else {
            currentList.add(action)
        }
        _actions.value = currentList
    }

    override suspend fun getAction(actionId: RosId): RosAction {
        return _actions.value.firstOrNull { it.goalId == actionId.value }
            ?: throw NoSuchElementException("Action with id ${actionId.value} not found")
    }

    override fun publishRaw(json: String) {
        // TODO: Implement raw JSON publishing to rosbridge
        // This would typically interact with a WebSocket connection
    }
}
