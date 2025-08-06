package com.examples.testros2jsbridge.presentation.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examples.testros2jsbridge.presentation.state.SettingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.examples.testros2jsbridge.domain.repository.ConfigurationRepository
import com.examples.testros2jsbridge.domain.model.AppConfiguration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configurationRepository: ConfigurationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingUiState())
    val uiState: StateFlow<SettingUiState> = _uiState

    fun setTheme(theme: String) {
        _uiState.value = _uiState.value.copy(theme = theme)
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
                    notificationsEnabled = _uiState.value.notificationsEnabled
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