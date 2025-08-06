package com.examples.testros2jsbridge.core.base

import kotlinx.coroutines.flow.Flow

sealed class BaseUseCase {
    suspend fun execute(): Any? {
        return ""
    }
}