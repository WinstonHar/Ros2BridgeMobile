package com.examples.testros2jsbridge.domain.usecase.action

import com.examples.testros2jsbridge.domain.model.RosMessage
import com.examples.testros2jsbridge.domain.repository.AppActionRepository
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetActionStatusUseCase @Inject constructor(
    private val appActionRepository: AppActionRepository
) {

    suspend fun getActionStatus(message: RosMessage) {
        withContext(Dispatchers.IO) {
            // TODO: Implement status retrieval logic
        }
    }
}