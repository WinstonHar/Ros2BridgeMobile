package com.examples.testros2jsbridge.core.error

sealed class RosException : Exception

class RosConnectionException(message: String) : RosException(message)
class RosMessageException(message: String) : RosException(message)