package com.nanami.koishi.feature.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.KoishiApp
import com.nanami.koishi.R
import com.nanami.koishi.core.data.preferences.ThemePreferences
import com.nanami.koishi.core.data.sync.WebDavAuthException
import com.nanami.koishi.core.data.sync.WebDavConfig
import com.nanami.koishi.core.data.sync.WebDavFileNotFoundException
import com.nanami.koishi.core.data.sync.WebDavPreferences
import com.nanami.koishi.core.data.sync.WebDavSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val syncManager = WebDavSyncManager(
        context = context,
        dao = (application as KoishiApp).toolStorageDao
    )

    private val _uiState = MutableStateFlow(loadSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun loadSettings(): SettingsUiState {
        val langOrdinal = prefs.getInt(KEY_LANGUAGE, AppLanguage.SYSTEM.ordinal)
        val language = AppLanguage.entries.getOrElse(langOrdinal) { AppLanguage.SYSTEM }

        return SettingsUiState(
            themeMode = ThemePreferences.themeMode(context),
            appTheme = ThemePreferences.appTheme(context),
            amoled = ThemePreferences.isAmoled(context),
            language = language,
            webDavConfig = WebDavPreferences.loadConfig(context)
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
            is SettingsUiEvent.OnShowWebDavDialog -> {
                _uiState.update {
                    it.copy(
                        showWebDavDialog = event.show,
                        webDavConfig = if (event.show) WebDavPreferences.loadConfig(context) else it.webDavConfig,
                        webDavSyncState = if (event.show) WebDavSyncUiState.Idle else it.webDavSyncState
                    )
                }
            }
            is SettingsUiEvent.OnUpdateWebDavConfig -> {
                WebDavPreferences.saveConfig(context, event.config)
                _uiState.update { it.copy(webDavConfig = event.config) }
            }
            is SettingsUiEvent.OnDismissWebDavStatus -> {
                _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Idle) }
            }
            is SettingsUiEvent.OnTestWebDavConnection -> {
                testWebDavConnection()
            }
            is SettingsUiEvent.OnWebDavBackup -> {
                performWebDavBackup()
            }
            is SettingsUiEvent.OnWebDavRestore -> {
                performWebDavRestore()
            }
        }
    }

    private fun testWebDavConnection() {
        val config = _uiState.value.webDavConfig
        if (!validateConfig(config)) return

        viewModelScope.launch {
            _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Testing) }
            try {
                syncManager.testConnection(config)
                _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Success(R.string.webdav_success_test)) }
            } catch (e: WebDavAuthException) {
                _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Error(messageRes = R.string.webdav_error_unauthorized)) }
            } catch (e: IOException) {
                _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Error(messageRes = R.string.webdav_error_network)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Error(message = e.localizedMessage)) }
            }
        }
    }

    private fun performWebDavBackup() {
        val config = _uiState.value.webDavConfig
        if (!validateConfig(config)) return

        viewModelScope.launch {
            _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.BackingUp) }
            try {
                syncManager.backup(config)
                val updatedConfig = WebDavPreferences.loadConfig(context)
                _uiState.update {
                    it.copy(
                        webDavConfig = updatedConfig,
                        webDavSyncState = WebDavSyncUiState.Success(R.string.webdav_success_backup)
                    )
                }
            } catch (e: WebDavAuthException) {
                _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Error(messageRes = R.string.webdav_error_unauthorized)) }
            } catch (e: IOException) {
                _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Error(messageRes = R.string.webdav_error_network)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Error(message = e.localizedMessage)) }
            }
        }
    }

    private fun performWebDavRestore() {
        val config = _uiState.value.webDavConfig
        if (!validateConfig(config)) return

        viewModelScope.launch {
            _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Restoring) }
            try {
                syncManager.restore(config)
                val updated = loadSettings()
                _uiState.update {
                    updated.copy(
                        showWebDavDialog = true,
                        webDavSyncState = WebDavSyncUiState.Success(R.string.webdav_success_restore)
                    )
                }
            } catch (e: WebDavAuthException) {
                _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Error(messageRes = R.string.webdav_error_unauthorized)) }
            } catch (e: WebDavFileNotFoundException) {
                _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Error(messageRes = R.string.webdav_error_not_found)) }
            } catch (e: IOException) {
                _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Error(messageRes = R.string.webdav_error_network)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Error(message = e.localizedMessage)) }
            }
        }
    }

    private fun validateConfig(config: WebDavConfig): Boolean {
        if (config.serverUrl.isBlank() || config.username.isBlank() || config.password.isBlank()) {
            _uiState.update { it.copy(webDavSyncState = WebDavSyncUiState.Error(messageRes = R.string.webdav_error_empty_fields)) }
            return false
        }
        return true
    }

    companion object {
        private const val PREFERENCES_NAME = "koishi_settings"
        private const val KEY_LANGUAGE = "language"
    }
}
