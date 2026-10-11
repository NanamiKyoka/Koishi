package com.nanami.koishi.feature.settings

import android.net.Uri
import androidx.annotation.StringRes
import com.nanami.koishi.R
import com.nanami.koishi.core.data.sync.WebDavConfig
import com.nanami.koishi.core.data.sync.model.KoishiBackup
import com.nanami.koishi.core.designsystem.theme.AppTheme
import com.nanami.koishi.core.designsystem.theme.ThemeMode

enum class AppLanguage(@StringRes val titleRes: Int, val tag: String) {
    SYSTEM(R.string.settings_lang_system, ""),
    ZH(R.string.settings_lang_zh, "zh-CN"),
    EN(R.string.settings_lang_en, "en")
}

sealed interface WebDavSyncUiState {
    data object Idle : WebDavSyncUiState
    data object Testing : WebDavSyncUiState
    data object BackingUp : WebDavSyncUiState
    data object Restoring : WebDavSyncUiState
    data class Success(@StringRes val messageRes: Int) : WebDavSyncUiState
    data class Error(val message: String? = null, @StringRes val messageRes: Int? = null) : WebDavSyncUiState
}

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data class Available(
        val release: com.nanami.koishi.core.data.update.AppRelease,
        val matchedAsset: com.nanami.koishi.core.data.update.ReleaseAsset?
    ) : UpdateUiState
    data class Downloading(
        val release: com.nanami.koishi.core.data.update.AppRelease,
        val matchedAsset: com.nanami.koishi.core.data.update.ReleaseAsset,
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateUiState
    data class ReadyToInstall(
        val release: com.nanami.koishi.core.data.update.AppRelease,
        val apkFile: java.io.File
    ) : UpdateUiState
    data class Error(val message: String? = null, @StringRes val messageRes: Int? = null) : UpdateUiState
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appTheme: AppTheme = AppTheme.KOISHI,
    val amoled: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val showLanguageDialog: Boolean = false,
    val showWebDavDialog: Boolean = false,
    val webDavConfig: WebDavConfig = WebDavConfig(),
    val webDavSyncState: WebDavSyncUiState = WebDavSyncUiState.Idle,
    val updateState: UpdateUiState = UpdateUiState.Idle,
    val isCheckingUpdate: Boolean = false,
    val userMessage: Int? = null,
    val cacheSizeBytes: Long = 0L,
    val dataSizeBytes: Long = 0L,
    val isClearingCache: Boolean = false,
    val showClearCacheDialog: Boolean = false,
    val showLocalBackupDialog: Boolean = false,
    val localBackupIncludeSensitive: Boolean = false,
    val isLocalBackupBusy: Boolean = false,
    val pendingImportBackup: KoishiBackup? = null,
    val refreshKey: Long = 0L
)

sealed interface SettingsUiEvent {
    data class OnThemeModeSelected(val mode: ThemeMode) : SettingsUiEvent
    data class OnAppThemeSelected(val theme: AppTheme) : SettingsUiEvent
    data class OnAmoledToggled(val enabled: Boolean) : SettingsUiEvent
    data class OnLanguageSelected(val language: AppLanguage) : SettingsUiEvent
    data class OnShowLanguageDialog(val show: Boolean) : SettingsUiEvent
    data class OnShowWebDavDialog(val show: Boolean) : SettingsUiEvent
    data class OnUpdateWebDavConfig(val config: WebDavConfig) : SettingsUiEvent
    data object OnTestWebDavConnection : SettingsUiEvent
    data object OnWebDavBackup : SettingsUiEvent
    data object OnWebDavRestore : SettingsUiEvent
    data object OnDismissWebDavStatus : SettingsUiEvent
    data object OnCheckForUpdate : SettingsUiEvent
    data class OnDownloadUpdate(val asset: com.nanami.koishi.core.data.update.ReleaseAsset) : SettingsUiEvent
    data class OnInstallUpdate(val file: java.io.File) : SettingsUiEvent
    data object OnCancelDownload : SettingsUiEvent
    data object OnDismissUpdateDialog : SettingsUiEvent
    data object OnDismissUserMessage : SettingsUiEvent
    data object OnRefreshStorageUsage : SettingsUiEvent
    data object OnClearCache : SettingsUiEvent
    data class OnShowClearCacheDialog(val show: Boolean) : SettingsUiEvent
    data class OnShowLocalBackupDialog(val show: Boolean) : SettingsUiEvent
    data class OnLocalBackupSensitiveChange(val include: Boolean) : SettingsUiEvent
    data class OnExportLocalBackup(val uri: Uri) : SettingsUiEvent
    data class OnImportLocalBackup(val uri: Uri) : SettingsUiEvent
    data object OnConfirmLocalImport : SettingsUiEvent
    data object OnCancelLocalImport : SettingsUiEvent
}
