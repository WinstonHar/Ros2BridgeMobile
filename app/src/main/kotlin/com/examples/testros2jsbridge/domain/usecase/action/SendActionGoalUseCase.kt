package com.examples.testros2jsbridge.domain.usecase.action

import com.examples.testros2jsbridge.domain.model.RosAction
import com.examples.testros2jsbridge.domain.repository.RosActionRepository
import kotlinx.coroutines.withContext

class SendActionGoalUseCase(
    private val rosActionRepository: RosActionRepository
) : SendActionGoalUseCase {

    override suspend fun sendGoal(action: RosAction) {
        withContext(Dispatchers.IO) {
            rosActionRepository.save(action)
        }
    }
}