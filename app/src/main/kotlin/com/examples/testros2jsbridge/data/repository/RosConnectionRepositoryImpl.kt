package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.domain.model.RosConnection
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.repository.RosConnectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RosConnectionRepositoryImpl @Inject constructor() : RosConnectionRepository {
    private val _connections = MutableStateFlow<List<RosConnection>>(emptyList())
    override val connections: StateFlow<List<RosConnection>> = _connections

    override suspend fun saveConnection(connection: RosConnection?) {
        if (connection != null) {
            val currentList = _connections.value.toMutableList()
            val index = currentList.indexOfFirst { it.connectionId == connection.connectionId }
            if (index >= 0) {
                currentList[index] = connection
            } else {
                currentList.add(connection)
            }
            _connections.value = currentList
        }
    }

    override suspend fun getConnection(connectionId: RosId): RosConnection? {
        return _connections.value.find { it.connectionId == connectionId.value }
    }
}
