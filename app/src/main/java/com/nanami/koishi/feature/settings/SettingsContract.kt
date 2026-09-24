package com.nanami.koishi.feature.settings

import androidx.annotation.StringRes
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.theme.AppTheme
import com.nanami.koishi.core.designsystem.theme.ThemeMode

enum class AppLanguage(@StringRes val titleRes: Int, val tag: String) {
    SYSTEM(R.string.settings_lang_system, ""),
    ZH(R.string.settings_lang_zh, "zh-CN"),
    EN(R.string.settings_lang_en, "en")
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appTheme: AppTheme = AppTheme.KOISHI,
    val amoled: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val showLanguageDialog: Boolean = false
)

sealed interface SettingsUiEvent {
    data class OnThemeModeSelected(val mode: ThemeMode) : SettingsUiEvent
    data class OnAppThemeSelected(val theme: AppTheme) : SettingsUiEvent
    data class OnAmoledToggled(val enabled: Boolean) : SettingsUiEvent
    data class OnLanguageSelected(val language: AppLanguage) : SettingsUiEvent
    data class OnShowLanguageDialog(val show: Boolean) : SettingsUiEvent
}
