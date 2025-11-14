package com.examples.testros2jsbridge.presentation.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.domain.model.AppConfiguration
import com.examples.testros2jsbridge.domain.repository.ConfigurationRepository
import com.examples.testros2jsbridge.presentation.state.SettingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configurationRepository: ConfigurationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingUiState())
    val uiState: StateFlow<SettingUiState> = _uiState

    init {
        // Load config from repository on startup
        viewModelScope.launch {
            configurationRepository.config.collect { config ->
                config?.let {
                    _uiState.value = _uiState.value.copy(
                        theme = it.theme,
                        language = it.language,
                        notificationsEnabled = it.notificationsEnabled,
                        reconnectOnFailure = it.reconnectOnFailure
                    )
                }
            }
        }
    }

    fun setTheme(theme: String) {
        _uiState.value = _uiState.value.copy(theme = theme)
        saveSettings() // Persist immediately for instant theme switching
    }

    fun setLanguage(language: String) {
        _uiState.value = _uiState.value.copy(language = language)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
    }

    fun setReconnectOnFailure(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(reconnectOnFailure = enabled)
    }

    fun saveSettings() {
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val config = AppConfiguration(
                    theme = _uiState.value.theme,
                    language = _uiState.value.language,
                    notificationsEnabled = _uiState.value.notificationsEnabled,
                    reconnectOnFailure = _uiState.value.reconnectOnFailure
                )
                configurationRepository.saveConfig(config)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, showErrorDialog = true, errorMessage = e.message)
            }
        }
    }

    fun dismissErrorDialog() {
        _uiState.value = _uiState.value.copy(showErrorDialog = false, errorMessage = null)
    }
}