package com.examples.testros2jsbridge.data.repository

import com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient
import com.examples.testros2jsbridge.domain.model.RosConnection
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.repository.RosConnectionRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class RosConnectionRepositoryImpl @Inject constructor(rosbridgeClient: RosbridgeClient) : RosConnectionRepository {
    private val _connections = MutableStateFlow<List<RosConnection>>(emptyList())
    override val connections: StateFlow<List<RosConnection>> get() = _connections

    override suspend fun saveConnection(connection: RosConnection?) {
        _connections.value = (_connections.value.filter { it.connectionId != connection?.connectionId } + connection) as List<RosConnection>
    }

    override suspend fun getConnection(connectionId: RosId): RosConnection? {
        return _connections.value.find { it.connectionId == connectionId.value }
    }

    suspend fun deleteConnection(connectionId: RosId) {
        _connections.value = _connections.value.filterNot { it.connectionId == connectionId.value }
    }
}