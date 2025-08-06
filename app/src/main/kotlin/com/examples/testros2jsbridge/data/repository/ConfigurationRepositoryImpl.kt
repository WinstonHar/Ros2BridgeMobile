package com.examples.testros2jsbridge.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.examples.testros2jsbridge.domain.model.AppConfiguration
import com.examples.testros2jsbridge.domain.model.fromMap
import com.examples.testros2jsbridge.domain.model.fromYaml
import com.examples.testros2jsbridge.domain.model.toMap
import com.examples.testros2jsbridge.domain.model.toYaml
import com.examples.testros2jsbridge.domain.repository.ConfigurationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject

/**
 * ConfigurationRepositoryImpl provides abstraction for configuration management,
 * including import/export of YAML config, SharedPreferences persistence, and domain mapping.
 * Follows Clean Architecture and repository pattern as per report.md.
 */
class ConfigurationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SharedPreferences
) : ConfigurationRepository {

    private val _config = MutableStateFlow<AppConfiguration?>(null)
    override val config: Flow<AppConfiguration?> = _config.asStateFlow()

    // --- SharedPreferences keys ---
    private val PREFS_CONFIG = "app_config"
    private val KEY_LAST_IMPORTED = "last_imported_config"


    override suspend fun saveConfig(config: AppConfiguration) {
        _config.value = config
        val prefs = context.getSharedPreferences(PREFS_CONFIG, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_LAST_IMPORTED, config.toYaml()) }
    }


    override suspend fun getConfig(): AppConfiguration {
        val prefs = context.getSharedPreferences(PREFS_CONFIG, Context.MODE_PRIVATE)
        val yamlString = prefs.getString(KEY_LAST_IMPORTED, null)
        val loadedConfig = if (yamlString != null) AppConfiguration.fromYaml(yamlString) else null
        _config.value = loadedConfig ?: AppConfiguration()
        return _config.value!!
    }

    /**
     * Export current configuration to YAML OutputStream
     */
    override fun exportConfigToStream(out: OutputStream) {
        val yaml = Yaml()
        val configMap = _config.value?.toMap()
        yaml.dump(configMap, OutputStreamWriter(out))
    }

    /**
     * Import configuration from YAML InputStream
     */
    override fun importConfigFromStream(inp: InputStream) {
        val yaml = Yaml()
        val configMap = yaml.load<Map<String, Any>>(InputStreamReader(inp))
        val importedConfig = AppConfiguration.fromMap(configMap)
        _config.value = importedConfig
        val prefs = context.getSharedPreferences(PREFS_CONFIG, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_LAST_IMPORTED, importedConfig.toYaml()) }
    }

}
