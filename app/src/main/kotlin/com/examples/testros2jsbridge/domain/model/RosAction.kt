package com.examples.testros2jsbridge.domain.model

/*
Action goal/status managmenet
*/

data class RosAction(
    val actionName: String,
    val goalId: String,
    val goal: String,
    val status: String,
    val result: String? = null,
    val feedback: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)