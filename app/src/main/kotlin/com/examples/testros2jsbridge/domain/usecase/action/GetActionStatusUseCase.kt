package com.examples.testros2jsbridge.domain.usecase.action

import com.examples.testros2jsbridge.domain.model.RosAction
import com.examples.testros2jsbridge.domain.repository.RosActionRepository
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetActionStatusUseCase @Inject constructor(
    private val rosActionRepository: RosActionRepository
) {

    suspend fun getActionStatus(action: RosAction): RosAction {
        return withContext(Dispatchers.IO) {
            rosActionRepository.getAction(com.examples.testros2jsbridge.domain.model.RosId(action.actionName))
        }
    }
}