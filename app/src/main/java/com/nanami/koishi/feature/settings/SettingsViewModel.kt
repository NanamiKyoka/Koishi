package com.nanami.koishi.feature.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("koishi_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(loadSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun loadSettings(): SettingsUiState {
        val themeOrdinal = prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)
        val themeMode = ThemeMode.entries.getOrElse(themeOrdinal) { ThemeMode.SYSTEM }
        val dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        val langOrdinal = prefs.getInt(KEY_LANGUAGE, AppLanguage.SYSTEM.ordinal)
        val language = AppLanguage.entries.getOrElse(langOrdinal) { AppLanguage.SYSTEM }

        return SettingsUiState(
            themeMode = themeMode,
            dynamicColor = dynamicColor,
            language = language
        )
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.OnThemeModeSelected -> {
                prefs.edit().putInt(KEY_THEME_MODE, event.mode.ordinal).apply()
                _uiState.update { it.copy(themeMode = event.mode, showThemeDialog = false) }
            }
            is SettingsUiEvent.OnDynamicColorToggled -> {
                prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, event.enabled).apply()
                _uiState.update { it.copy(dynamicColor = event.enabled) }
            }
            is SettingsUiEvent.OnLanguageSelected -> {
                prefs.edit().putInt(KEY_LANGUAGE, event.language.ordinal).apply()
                _uiState.update { it.copy(language = event.language, showLanguageDialog = false) }
            }
            is SettingsUiEvent.OnShowThemeDialog -> {
                _uiState.update { it.copy(showThemeDialog = event.show) }
            }
            is SettingsUiEvent.OnShowLanguageDialog -> {
                _uiState.update { it.copy(showLanguageDialog = event.show) }
            }
        }
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_LANGUAGE = "language"
    }
}
