package com.examples.testros2jsbridge.core.base

sealed class BaseUseCase {
    suspend fun execute(): Any? {
        return ""
    }
}