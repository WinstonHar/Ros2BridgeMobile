package com.examples.testros2jsbridge.data.local.preferences

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var userName: String?
        get() = prefs.getString("user_name", "")
        set(value) = prefs.edit().putString("user_name", value).apply()

    var theme: String?
        get() = prefs.getString("theme", "light")
        set(value) = prefs.edit().putString("theme", value).apply()

    var language: String?
        get() = prefs.getString("language", "en")
        set(value) = prefs.edit().putString("language", value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}