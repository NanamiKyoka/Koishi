package com.nanami.koishi.core.designsystem.theme

import android.app.Activity
import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nanami.koishi.core.data.preferences.ThemePreferences
import com.nanami.koishi.core.designsystem.KoishiShapes
import com.nanami.koishi.core.designsystem.KoishiTypography
import com.nanami.koishi.core.designsystem.theme.colorscheme.BaseColorScheme
import com.nanami.koishi.core.designsystem.theme.colorscheme.CatppuccinColorScheme
import com.nanami.koishi.core.designsystem.theme.colorscheme.EverforestColorScheme
import com.nanami.koishi.core.designsystem.theme.colorscheme.GruvboxColorScheme
import com.nanami.koishi.core.designsystem.theme.colorscheme.KoishiColorScheme
import com.nanami.koishi.core.designsystem.theme.colorscheme.LavenderColorScheme
import com.nanami.koishi.core.designsystem.theme.colorscheme.MonetColorScheme
import com.nanami.koishi.core.designsystem.theme.colorscheme.MonochromeColorScheme
import com.nanami.koishi.core.designsystem.theme.colorscheme.NordColorScheme
import com.nanami.koishi.core.designsystem.theme.colorscheme.RosePineColorScheme
import com.nanami.koishi.core.designsystem.theme.colorscheme.TokyoNightColorScheme

@Composable
fun KoishiTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    BaseKoishiTheme(
        appTheme = appTheme ?: ThemePreferences.appTheme(context),
        isDark = darkTheme ?: ThemePreferences.themeMode(context).isDarkTheme,
        isAmoled = amoled ?: ThemePreferences.isAmoled(context),
        content = content
    )
}

@Composable
private fun BaseKoishiTheme(
    appTheme: AppTheme,
    isDark: Boolean,
    isAmoled: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = remember(appTheme, isDark, isAmoled, context) {
        getThemeColorScheme(
            context = context,
            appTheme = appTheme,
            isDark = isDark,
            isAmoled = isAmoled
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = KoishiShapes,
        typography = KoishiTypography,
        content = content
    )
}

private fun getThemeColorScheme(
    context: Context,
    appTheme: AppTheme,
    isDark: Boolean,
    isAmoled: Boolean
): ColorScheme {
    val colorScheme = if (appTheme == AppTheme.MONET) {
        MonetColorScheme(context)
    } else {
        colorSchemes.getOrDefault(appTheme, KoishiColorScheme)
    }
    return colorScheme.getColorScheme(
        isDark = isDark,
        isAmoled = isAmoled,
        overrideDarkSurfaceContainers = appTheme != AppTheme.MONET
    )
}

private val colorSchemes: Map<AppTheme, BaseColorScheme> = mapOf(
    AppTheme.KOISHI to KoishiColorScheme,
    AppTheme.CATPPUCCIN to CatppuccinColorScheme,
    AppTheme.NORD to NordColorScheme,
    AppTheme.TOKYO_NIGHT to TokyoNightColorScheme,
    AppTheme.GRUVBOX to GruvboxColorScheme,
    AppTheme.ROSE_PINE to RosePineColorScheme,
    AppTheme.EVERFOREST to EverforestColorScheme,
    AppTheme.LAVENDER to LavenderColorScheme,
    AppTheme.MONOCHROME to MonochromeColorScheme
)
