package com.nanami.koishi.feature.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.nanami.koishi.core.data.preferences.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(loadSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun loadSettings(): SettingsUiState {
        val langOrdinal = prefs.getInt(KEY_LANGUAGE, AppLanguage.SYSTEM.ordinal)
        val language = AppLanguage.entries.getOrElse(langOrdinal) { AppLanguage.SYSTEM }

        return SettingsUiState(
            themeMode = ThemePreferences.themeMode(context),
            appTheme = ThemePreferences.appTheme(context),
            amoled = ThemePreferences.isAmoled(context),
            language = language
        )
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.OnThemeModeSelected -> {
                ThemePreferences.setThemeMode(context, event.mode)
                _uiState.update { it.copy(themeMode = event.mode) }
            }
            is SettingsUiEvent.OnAppThemeSelected -> {
                ThemePreferences.setAppTheme(context, event.theme)
                _uiState.update { it.copy(appTheme = event.theme) }
            }
            is SettingsUiEvent.OnAmoledToggled -> {
                ThemePreferences.setAmoled(context, event.enabled)
                _uiState.update { it.copy(amoled = event.enabled) }
            }
            is SettingsUiEvent.OnLanguageSelected -> {
                prefs.edit().putInt(KEY_LANGUAGE, event.language.ordinal).apply()
                _uiState.update { it.copy(language = event.language, showLanguageDialog = false) }
            }
            is SettingsUiEvent.OnShowLanguageDialog -> {
                _uiState.update { it.copy(showLanguageDialog = event.show) }
            }
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "koishi_settings"
        private const val KEY_LANGUAGE = "language"
    }
}
