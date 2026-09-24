package com.nanami.koishi.core.designsystem.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Monochrome 主题，纯灰阶配色 */
internal object MonochromeColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFC6C6C6),
        onPrimary = Color(0xFF303030),
        primaryContainer = Color(0xFF474747),
        onPrimaryContainer = Color(0xFFE2E2E2),
        inversePrimary = Color(0xFF5E5E5E),
        secondary = Color(0xFFC6C6C6),
        onSecondary = Color(0xFF303030),
        secondaryContainer = Color(0xFF474747),
        onSecondaryContainer = Color(0xFFE2E2E2),
        tertiary = Color(0xFFC6C6C6),
        onTertiary = Color(0xFF303030),
        tertiaryContainer = Color(0xFF474747),
        onTertiaryContainer = Color(0xFFE2E2E2),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF131313),
        onBackground = Color(0xFFE2E2E2),
        surface = Color(0xFF131313),
        onSurface = Color(0xFFE2E2E2),
        surfaceVariant = Color(0xFF474747),
        onSurfaceVariant = Color(0xFFC6C6C6),
        outline = Color(0xFF919191),
        outlineVariant = Color(0xFF474747),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE2E2E2),
        inverseOnSurface = Color(0xFF303030),
        surfaceDim = Color(0xFF131313),
        surfaceBright = Color(0xFF393939),
        surfaceContainerLowest = Color(0xFF0E0E0E),
        surfaceContainerLow = Color(0xFF1B1B1B),
        surfaceContainer = Color(0xFF1F1F1F),
        surfaceContainerHigh = Color(0xFF2A2A2A),
        surfaceContainerHighest = Color(0xFF353535)
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF5E5E5E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE2E2E2),
        onPrimaryContainer = Color(0xFF1B1B1B),
        inversePrimary = Color(0xFFC6C6C6),
        secondary = Color(0xFF5E5E5E),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE2E2E2),
        onSecondaryContainer = Color(0xFF1B1B1B),
        tertiary = Color(0xFF5E5E5E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE2E2E2),
        onTertiaryContainer = Color(0xFF1B1B1B),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFCFCFC),
        onBackground = Color(0xFF1B1B1B),
        surface = Color(0xFFFCFCFC),
        onSurface = Color(0xFF1B1B1B),
        surfaceVariant = Color(0xFFE2E2E2),
        onSurfaceVariant = Color(0xFF474747),
        outline = Color(0xFF777777),
        outlineVariant = Color(0xFFC6C6C6),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF303030),
        inverseOnSurface = Color(0xFFF1F1F1),
        surfaceDim = Color(0xFFDADADA),
        surfaceBright = Color(0xFFF9F9F9),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF3F3F3),
        surfaceContainer = Color(0xFFEEEEEE),
        surfaceContainerHigh = Color(0xFFE8E8E8),
        surfaceContainerHighest = Color(0xFFE2E2E2)
    )
}
