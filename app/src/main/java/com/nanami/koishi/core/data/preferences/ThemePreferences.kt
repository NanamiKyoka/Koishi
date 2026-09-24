package com.nanami.koishi.core.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.nanami.koishi.core.designsystem.theme.AppTheme
import com.nanami.koishi.core.designsystem.theme.ThemeMode

object ThemePreferences {

    private const val PREFERENCES_NAME = "koishi_settings"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_APP_THEME = "app_theme"
    private const val KEY_AMOLED = "theme_dark_amoled"

    fun themeMode(context: Context): ThemeMode {
        val ordinal = preferences(context).getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)
        return ThemeMode.entries.getOrElse(ordinal) { ThemeMode.SYSTEM }
    }

    fun appTheme(context: Context): AppTheme {
        val name = preferences(context).getString(KEY_APP_THEME, null) ?: return AppTheme.KOISHI
        return AppTheme.entries.firstOrNull { it.name == name } ?: AppTheme.KOISHI
    }

    fun isAmoled(context: Context): Boolean = preferences(context).getBoolean(KEY_AMOLED, false)

    fun setThemeMode(context: Context, mode: ThemeMode) {
        preferences(context).edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
    }

    fun setAppTheme(context: Context, theme: AppTheme) {
        preferences(context).edit().putString(KEY_APP_THEME, theme.name).apply()
    }

    fun setAmoled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_AMOLED, enabled).apply()
    }

    private fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
