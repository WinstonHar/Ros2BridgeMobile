package com.examples.testros2jsbridge.core.network

sealed class NetworkResult<T> {
    data class Success(val data: T) : NetworkResult<T>()
    data class Failure(val message: String) : NetworkResult<Nothing>()
}