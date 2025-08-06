package com.examples.testros2jsbridge.domain.usecase.action

import com.examples.testros2jsbridge.domain.model.RosAction
import com.examples.testros2jsbridge.domain.repository.RosActionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CancelActionGoalUseCase @Inject constructor(
    private val rosActionRepository: RosActionRepository
) {

    suspend fun cancelGoal(action: RosAction, actionType: String) {
        withContext(Dispatchers.IO) {
            rosActionRepository.cancelActionGoal(
                action.actionName,
                actionType,
                action.goalId
            )
        }
    }
}