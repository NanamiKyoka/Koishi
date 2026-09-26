package com.nanami.koishi.feature.settings

import androidx.annotation.StringRes
import com.nanami.koishi.R
import com.nanami.koishi.core.data.sync.WebDavConfig
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

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appTheme: AppTheme = AppTheme.KOISHI,
    val amoled: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val showLanguageDialog: Boolean = false,
    val showWebDavDialog: Boolean = false,
    val webDavConfig: WebDavConfig = WebDavConfig(),
    val webDavSyncState: WebDavSyncUiState = WebDavSyncUiState.Idle
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
}
