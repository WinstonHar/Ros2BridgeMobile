package com.examples.testros2jsbridge.core.error

import com.examples.testros2jsbridge.domain.model.RosId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ErrorHandler {
    private val _errorFlow = MutableSharedFlow<String>(replay = 0)
    val errorFlow = _errorFlow.asSharedFlow()

    suspend fun handle(exception: Throwable) {
        _errorFlow.emit(exception.message ?: "Unknown error")
    }
}