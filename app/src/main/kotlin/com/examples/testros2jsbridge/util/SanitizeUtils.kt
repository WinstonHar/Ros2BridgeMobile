package com.examples.testros2jsbridge.util

/**
 * Utility function for consistent config name handling across the app.
 */
fun sanitizeConfigName(name: String): String = name.trim().replace(" ", "").uppercase()
