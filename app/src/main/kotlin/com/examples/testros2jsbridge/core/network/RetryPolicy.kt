package com.examples.testros2jsbridge.core.network

object RetryPolicy {
    var maxRetries: Int = 5
    var initialDelay: Int = 1000 // Initial delay in milliseconds
}