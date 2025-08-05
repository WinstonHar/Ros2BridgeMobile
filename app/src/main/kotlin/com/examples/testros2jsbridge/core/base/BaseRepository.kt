package com.examples.testros2jsbridge.core.base

import kotlinx.coroutines.flow.Flow

sealed class RosRepository {
    abstract suspend fun save(item: Any)
    abstract suspend fun get(id: Any): Any?
    abstract fun observe(callback: (Any) -> Unit)
}