package com.nanami.koishi.feature.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

import com.nanami.koishi.BuildConfig
import com.nanami.koishi.core.data.update.AppUpdateManager
import com.nanami.koishi.core.data.update.ReleaseAsset
import com.nanami.koishi.core.data.update.UpdateCheckResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import java.io.File

class SettingsViewModel(
    application: Application,
    private val syncManager: WebDavSyncManager,
    private val updateManager: AppUpdateManager
) : AndroidViewModel(application) {

    private var downloadJob: Job? = null

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
            is SettingsUiEvent.OnCheckForUpdate -> {
                checkForUpdate()
            }
            is SettingsUiEvent.OnDownloadUpdate -> {
                downloadUpdate(event.asset)
            }
            is SettingsUiEvent.OnCancelDownload -> {
                cancelDownload()
            }
            is SettingsUiEvent.OnDismissUpdateDialog -> {
                dismissUpdateDialog()
            }
            is SettingsUiEvent.OnInstallUpdate -> {
                installUpdate(event.file)
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
                        webDavSyncState = WebDavSyncUiState.Success(R.string.webdav_success_restore),
                        refreshKey = System.currentTimeMillis()
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

    fun canInstallPackages(): Boolean = updateManager.canInstallPackages(context)

    fun openInstallPermissionSettings() {
        updateManager.openInstallPermissionSettings(context)
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(updateState = UpdateUiState.Checking) }
            when (val result = updateManager.checkForUpdate(BuildConfig.VERSION_NAME)) {
                is UpdateCheckResult.NewVersion -> {
                    _uiState.update {
                        it.copy(
                            updateState = UpdateUiState.Available(
                                release = result.release,
                                matchedAsset = result.matchedAsset
                            )
                        )
                    }
                }
                is UpdateCheckResult.AlreadyLatest -> {
                    _uiState.update {
                        it.copy(updateState = UpdateUiState.Latest(result.currentVersion))
                    }
                }
                is UpdateCheckResult.Error -> {
                    _uiState.update {
                        it.copy(updateState = UpdateUiState.Error(message = result.message))
                    }
                }
            }
        }
    }

    private fun downloadUpdate(asset: ReleaseAsset) {
        val current = _uiState.value.updateState
        val release = (current as? UpdateUiState.Available)?.release ?: return

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updateState = UpdateUiState.Downloading(
                        release = release,
                        matchedAsset = asset,
                        progress = 0f,
                        bytesDownloaded = 0L,
                        totalBytes = asset.size
                    )
                )
            }

            val updateDir = File(context.cacheDir, "updates")
            val destinationFile = File(updateDir, asset.name.ifEmpty { "koishi-update.apk" })

            try {
                val file = updateManager.downloadApk(
                    downloadUrl = asset.downloadUrl,
                    destinationFile = destinationFile
                ) { bytesDownloaded, totalBytes ->
                    val progress = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
                    _uiState.update { state ->
                        val cur = state.updateState
                        if (cur is UpdateUiState.Downloading) {
                            state.copy(
                                updateState = cur.copy(
                                    progress = progress,
                                    bytesDownloaded = bytesDownloaded,
                                    totalBytes = totalBytes
                                )
                            )
                        } else state
                    }
                }

                _uiState.update {
                    it.copy(
                        updateState = UpdateUiState.ReadyToInstall(
                            release = release,
                            apkFile = file
                        )
                    )
                }
            } catch (e: CancellationException) {
                destinationFile.delete()
                throw e
            } catch (e: Exception) {
                destinationFile.delete()
                _uiState.update {
                    it.copy(
                        updateState = UpdateUiState.Error(
                            message = e.localizedMessage,
                            messageRes = R.string.update_dialog_download_error
                        )
                    )
                }
            }
        }
    }

    private fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update { it.copy(updateState = UpdateUiState.Idle) }
    }

    private fun dismissUpdateDialog() {
        if (_uiState.value.updateState is UpdateUiState.Downloading) {
            downloadJob?.cancel()
            downloadJob = null
        }
        _uiState.update { it.copy(updateState = UpdateUiState.Idle) }
    }

    private fun installUpdate(file: File) {
        updateManager.installApk(context, file)
    }

    companion object {
        private const val PREFERENCES_NAME = "koishi_settings"
        private const val KEY_LANGUAGE = "language"
    }
}
