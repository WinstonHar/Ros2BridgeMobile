package com.examples.testros2jsbridge.domain.usecase.connection

import com.examples.testros2jsbridge.domain.repository.RosConnectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DisconnectFromRosUseCase @Inject constructor(
    private val connectionRepository: RosConnectionRepository
) {
    suspend fun disconnect(connectionId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Mark connection as disconnected
                val connection = connectionRepository.getConnection(com.examples.testros2jsbridge.domain.model.RosId(connectionId))
                val updated = connection?.copy(isConnected = false)
                connectionRepository.saveConnection(updated)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}