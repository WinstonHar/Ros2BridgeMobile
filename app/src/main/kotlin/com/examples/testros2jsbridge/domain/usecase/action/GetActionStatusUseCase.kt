package com.examples.testros2jsbridge.domain.usecase.action

import com.examples.testros2jsbridge.domain.model.RosAction
import com.examples.testros2jsbridge.domain.repository.RosActionRepository
import kotlinx.coroutines.withContext

class GetActionStatusUseCase(
    private val rosActionRepository: RosActionRepository
) : GetActionStatusUseCase {

    override suspend fun getActionStatus(actionId: RosAction) : RosAction {
        return withContext(Dispatchers.IO) {
            rosActionRepository.get(actionId.actionName)
        }
    }
}