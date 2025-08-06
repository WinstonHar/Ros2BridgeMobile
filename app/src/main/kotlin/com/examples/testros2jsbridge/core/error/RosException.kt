package com.examples.testros2jsbridge.core.error

sealed class RosException(message1: String) : Exception()

class RosConnectionException(message: String) : RosException(message)
class RosMessageException(message: String) : RosException(message)