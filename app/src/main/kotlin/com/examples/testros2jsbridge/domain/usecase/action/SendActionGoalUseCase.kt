package com.examples.testros2jsbridge.domain.usecase.action

import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.repository.AppActionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SendActionGoalUseCase @Inject constructor(
    private val appActionRepository: AppActionRepository
) {

    suspend fun sendGoal(message: RosMessage) {
        withContext(Dispatchers.IO) {
            appActionRepository.publishMessage(message)
        }
    }
}