package com.examples.testros2jsbridge.domain.usecase.action

import com.examples.testros2jsbridge.domain.model.RosAction
import com.examples.testros2jsbridge.domain.repository.RosActionRepository
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class SendActionGoalUseCase @Inject constructor(
    private val rosActionRepository: RosActionRepository
) {

    suspend fun sendGoal(action: RosAction) {
        withContext(Dispatchers.IO) {
            rosActionRepository.saveAction(action)
        }
    }
}