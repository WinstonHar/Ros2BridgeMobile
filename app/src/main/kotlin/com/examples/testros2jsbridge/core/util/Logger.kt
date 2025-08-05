package com.examples.testros2jsbridge.core.util

import android.util.Log

object Logger {
    fun d(tag: String, message: String) = Log.d(tag, message)
    fun i(tag: String, message: String) = Log.i(tag, message)
    fun w(tag: String, message: String, throwable: Throwable? = null) = Log.w(tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = Log.e(tag, message, throwable)
}