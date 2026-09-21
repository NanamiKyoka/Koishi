package com.nanami.koishi.feature.settings

import androidx.annotation.StringRes
import com.nanami.koishi.R

enum class ThemeMode(@StringRes val titleRes: Int) {
    SYSTEM(R.string.settings_theme_system),
    LIGHT(R.string.settings_theme_light),
    DARK(R.string.settings_theme_dark)
}

enum class AppLanguage(@StringRes val titleRes: Int, val tag: String) {
    SYSTEM(R.string.settings_lang_system, ""),
    ZH(R.string.settings_lang_zh, "zh-CN"),
    EN(R.string.settings_lang_en, "en")
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val showThemeDialog: Boolean = false,
    val showLanguageDialog: Boolean = false
)

sealed interface SettingsUiEvent {
    data class OnThemeModeSelected(val mode: ThemeMode) : SettingsUiEvent
    data class OnDynamicColorToggled(val enabled: Boolean) : SettingsUiEvent
    data class OnLanguageSelected(val language: AppLanguage) : SettingsUiEvent
    data class OnShowThemeDialog(val show: Boolean) : SettingsUiEvent
    data class OnShowLanguageDialog(val show: Boolean) : SettingsUiEvent
}
