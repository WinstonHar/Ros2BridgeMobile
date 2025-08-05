package com.examples.testros2jsbridge.data.local.preferences

import android.content.Context
import android.content.SharedPreferences

class RosPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ros2_prefs", Context.MODE_PRIVATE)

    var ipAddress: String?
        get() = prefs.getString("ip_address", "")
        set(value) = prefs.edit().putString("ip_address", value).apply()

    var port: String?
        get() = prefs.getString("port", "9090")
        set(value) = prefs.edit().putString("port", value).apply()

    var lastConnected: Long
        get() = prefs.getLong("last_connected", 0L)
        set(value) = prefs.edit().putLong("last_connected", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}