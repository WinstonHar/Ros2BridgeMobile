package com.examples.testros2jsbridge.domain.usecase.connection

import com.examples.testros2jsbridge.domain.repository.RosConnectionRepository
import com.examples.testros2jsbridge.domain.model.RosConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConnectToRosUseCase(
    private val connectionRepository: RosConnectionRepository
) {
    suspend fun connect(ip: String, port: Int): Result<RosConnection> {
        // Basic validation (expand as needed)
        val ipPattern = Regex("""^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$""")
        if (!ipPattern.matches(ip)) {
            return Result.failure(IllegalArgumentException("Invalid IP address"))
        }
        if (port !in 1..65535) {
            return Result.failure(IllegalArgumentException("Invalid port number"))
        }
        return withContext(Dispatchers.IO) {
            try {
                val connection = RosConnection(
                    ipAddress = ip,
                    port = port,
                    isConnected = true // Assume connection success for now
                )
                connectionRepository.saveConnection(connection)
                Result.success(connection)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}