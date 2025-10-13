package com.examples.testros2jsbridge.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.examples.testros2jsbridge.domain.model.ControllerConfig
import com.examples.testros2jsbridge.domain.model.ControllerPreset
import com.examples.testros2jsbridge.domain.model.fromMap
import com.examples.testros2jsbridge.domain.model.toMap
import com.examples.testros2jsbridge.domain.repository.ControllerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControllerRepositoryImpl @Inject constructor(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) : ControllerRepository {

    private val _controller = MutableStateFlow(ControllerConfig())
    override val controller: Flow<ControllerConfig> = _controller

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        loadController()
    }

    private fun loadController() {
        val configJson = sharedPreferences.getString("controller_config", null)
        if (configJson != null) {
            try {
                val yaml = Yaml()
                val map = yaml.load<Map<String, Any?>>(configJson)
                _controller.value = ControllerConfig.fromMap(map)
            } catch (e: Exception) {
                _controller.value = ControllerConfig()
            }
        } else {
            _controller.value = ControllerConfig()
        }
    }

    override suspend fun saveController(controller: ControllerConfig) {
        val yaml = Yaml()
        val yamlString = yaml.dump(controller.toMap())
        sharedPreferences.edit().putString("controller_config", yamlString).apply()
        _controller.value = controller
    }

    override suspend fun getController(): ControllerConfig {
        return _controller.value
    }

    override fun getSelectedConfigName(selectedConfigKey: String): String? {
        return sharedPreferences.getString(selectedConfigKey, null)
    }

    fun loadControllerPresets(): List<ControllerPreset> {
        val presetsJson = sharedPreferences.getString("controller_presets", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<ControllerPreset>>(presetsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveControllerPresets(presets: List<ControllerPreset>) {
        val presetsJson = json.encodeToString(presets)
        sharedPreferences.edit().putString("controller_presets", presetsJson).apply()
    }

    fun loadControllerConfigs(): List<ControllerConfig> {
        val configsJson = sharedPreferences.getString("controller_configs", null) ?: return listOf(ControllerConfig())
        return try {
            val yaml = Yaml()
            val configsList = yaml.load<List<Map<String, Any?>>>(configsJson)
            configsList.map { ControllerConfig.fromMap(it) }
        } catch (e: Exception) {
            listOf(ControllerConfig())
        }
    }

    fun saveControllerConfigs(configs: List<ControllerConfig>) {
        val yaml = Yaml()
        val configsList = configs.map { it.toMap() }
        val yamlString = yaml.dump(configsList)
        sharedPreferences.edit().putString("controller_configs", yamlString).apply()
    }

    fun exportConfigToStream(outputStream: OutputStream) {
        val config = _controller.value
        val yaml = Yaml()
        val yamlString = yaml.dump(config.toMap())
        outputStream.write(yamlString.toByteArray())
    }

    fun importConfigFromStream(inputStream: InputStream) {
        val yaml = Yaml()
        val yamlString = inputStream.bufferedReader().readText()
        val map = yaml.load<Map<String, Any?>>(yamlString)
        val config = ControllerConfig.fromMap(map)
        _controller.value = config
    }

    fun initialize() {
        // Called from ViewModels if needed to ensure initialization
    }
}
