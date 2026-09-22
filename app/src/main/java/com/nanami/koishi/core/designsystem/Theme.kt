package com.nanami.koishi.core.designsystem

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun isDarkThemeFromSettings(): Boolean {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("koishi_settings", Context.MODE_PRIVATE) }
    val themeOrdinal = prefs.getInt("theme_mode", 0)
    return when (themeOrdinal) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
}

@Composable
fun isDynamicColorFromSettings(): Boolean {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("koishi_settings", Context.MODE_PRIVATE) }
    return prefs.getBoolean("dynamic_color", false)
}

@Composable
fun KoishiTheme(
    darkTheme: Boolean = isDarkThemeFromSettings(),
    dynamicColor: Boolean = isDynamicColorFromSettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> KoishiDarkColorScheme
        else -> KoishiLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = KoishiShapes,
        typography = KoishiTypography,
        content = content
    )
}
