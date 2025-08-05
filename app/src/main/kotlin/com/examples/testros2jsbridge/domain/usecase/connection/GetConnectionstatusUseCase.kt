package com.examples.testros2jsbridge.domain.usecase.connection

import com.examples.testros2jsbridge.domain.repository.RosConnectionRepository
import com.examples.testros2jsbridge.domain.model.RosConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetConnectionStatusUseCase(
    private val connectionRepository: RosConnectionRepository
) {
    suspend fun getStatus(connectionId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val connection = connectionRepository.getConnection(com.examples.testros2jsbridge.domain.model.RosId(connectionId))
                Result.success(connection.isConnected)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}