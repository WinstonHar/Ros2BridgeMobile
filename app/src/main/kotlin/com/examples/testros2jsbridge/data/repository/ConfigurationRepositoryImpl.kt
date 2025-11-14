package com.examples.testros2jsbridge.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.examples.testros2jsbridge.domain.model.AppConfiguration
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.model.fromMap
import com.examples.testros2jsbridge.domain.model.toMap
import com.examples.testros2jsbridge.domain.repository.ConfigurationRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigurationRepositoryImpl @Inject constructor(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) : ConfigurationRepository {

    private val _config = MutableStateFlow<AppConfiguration?>(null)
    override val config: Flow<AppConfiguration?> = _config

    init {
        loadConfig()
    }

    private fun loadConfig() {
        val rosServerUrl = sharedPreferences.getString("rosServerUrl", "10.0.0.0") ?: "10.0.0.0"
        val defaultPublishTopic = sharedPreferences.getString("defaultPublishTopic", "/default_topic") ?: "/default_topic"
        val defaultSubscribeTopic = sharedPreferences.getString("defaultSubscribeTopic", "/default_subscribe") ?: "/default_subscribe"
        val userName = sharedPreferences.getString("userName", "") ?: ""
        val enableLogging = sharedPreferences.getBoolean("enableLogging", false)
        val reconnectOnFailure = sharedPreferences.getBoolean("reconnectOnFailure", false)
        val theme = sharedPreferences.getString("theme", "system") ?: "system"
        val connectionTimeoutMs = sharedPreferences.getInt("connectionTimeoutMs", 10000)
        val maxRetryCount = sharedPreferences.getInt("maxRetryCount", 3)
        val lastUsedProtocolType = sharedPreferences.getString("lastUsedProtocolType", "msg") ?: "msg"
        val messageHistorySize = sharedPreferences.getInt("messageHistorySize", 25)
        val exportProfilePath = sharedPreferences.getString("exportProfilePath", null)
        val importProfilePath = sharedPreferences.getString("importProfilePath", null)
        val joystickAddressingMode = sharedPreferences.getString("joystickAddressingMode", "DIRECT") ?: "DIRECT"
        val language = sharedPreferences.getString("language", "en") ?: "en"
        val notificationsEnabled = sharedPreferences.getBoolean("notificationsEnabled", true)

        _config.value = AppConfiguration(
            rosServerUrl = RosId(rosServerUrl),
            defaultPublishTopic = RosId(defaultPublishTopic),
            defaultSubscribeTopic = RosId(defaultSubscribeTopic),
            userName = userName,
            enableLogging = enableLogging,
            reconnectOnFailure = reconnectOnFailure,
            theme = theme,
            connectionTimeoutMs = connectionTimeoutMs,
            maxRetryCount = maxRetryCount,
            lastUsedProtocolType = RosId(lastUsedProtocolType),
            messageHistorySize = messageHistorySize,
            exportProfilePath = exportProfilePath,
            importProfilePath = importProfilePath,
            joystickAddressingMode = RosId(joystickAddressingMode),
            language = language,
            notificationsEnabled = notificationsEnabled
        )
    }

    override suspend fun saveConfig(config: AppConfiguration) {
        with(sharedPreferences.edit()) {
            putString("rosServerUrl", config.rosServerUrl.value)
            putString("defaultPublishTopic", config.defaultPublishTopic.value)
            putString("defaultSubscribeTopic", config.defaultSubscribeTopic.value)
            putString("userName", config.userName)
            putBoolean("enableLogging", config.enableLogging)
            putBoolean("reconnectOnFailure", config.reconnectOnFailure)
            putString("theme", config.theme)
            putInt("connectionTimeoutMs", config.connectionTimeoutMs)
            putInt("maxRetryCount", config.maxRetryCount)
            putString("lastUsedProtocolType", config.lastUsedProtocolType.value)
            putInt("messageHistorySize", config.messageHistorySize)
            putString("exportProfilePath", config.exportProfilePath)
            putString("importProfilePath", config.importProfilePath)
            putString("joystickAddressingMode", config.joystickAddressingMode.value)
            putString("language", config.language)
            putBoolean("notificationsEnabled", config.notificationsEnabled)
            apply()
        }
        _config.value = config
    }

    override suspend fun getConfig(): AppConfiguration {
        return _config.value ?: AppConfiguration()
    }

    override fun exportConfigToStream(out: OutputStream) {
        val config = _config.value ?: AppConfiguration()
        val yaml = Yaml()
        val yamlString = yaml.dump(config.toMap())
        out.write(yamlString.toByteArray())
    }

    override fun importConfigFromStream(inp: InputStream) {
        val yaml = Yaml()
        val yamlString = inp.bufferedReader().readText()
        val map = yaml.load<Map<String, Any?>>(yamlString)
        val config = AppConfiguration.fromMap(map)
        _config.value = config
    }

    override suspend fun getAllControllerConfigs(): List<ControllerConfig> {
        val json = sharedPreferences.getString("controller_configs", null)
        return if (json != null) {
            val type = object : TypeToken<List<ControllerConfig>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    override suspend fun getSelectedConfigName(id: String): String? {
        return sharedPreferences.getString("selected_config_$id", null)
    }

    override suspend fun saveSelectedConfigName(name: String) {
        sharedPreferences.edit().putString("selected_config_name", name).apply()
    }
}
