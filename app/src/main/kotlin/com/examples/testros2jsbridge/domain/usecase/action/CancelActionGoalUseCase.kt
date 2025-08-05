package com.examples.testros2jsbridge.domain.usecase.action

import com.examples.testros2jsbridge.domain.model.RosAction
import com.examples.testros2jsbridge.domain.repository.RosActionRepository
import kotlinx.coroutines.withContext

class CancelActionGoalUseCase(
    private val rosActionRepository: RosActionRepository
) : CancelActionGoalUseCase {

    override suspend fun cancelGoal(action: RosAction) {
        withContext(Dispatchers.IO) {
            rosActionRepository.deleteAction(action)
        }
    }
}