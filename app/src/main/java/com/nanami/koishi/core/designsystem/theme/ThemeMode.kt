package com.nanami.koishi.core.designsystem.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.nanami.koishi.R

enum class ThemeMode(@StringRes val titleRes: Int) {
    SYSTEM(R.string.settings_theme_system),
    LIGHT(R.string.settings_theme_light),
    DARK(R.string.settings_theme_dark)
}

val ThemeMode.isDarkTheme: Boolean
    @Composable
    get() {
        return when (this) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    }
