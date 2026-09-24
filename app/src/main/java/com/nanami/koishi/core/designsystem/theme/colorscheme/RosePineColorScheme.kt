package com.nanami.koishi.core.designsystem.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Rosé Pine 主题，主色取自 #EBBCBA */
internal object RosePineColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFFFB3B1),
        onPrimary = Color(0xFF532020),
        primaryContainer = Color(0xFF6F3635),
        onPrimaryContainer = Color(0xFFFFDAD8),
        inversePrimary = Color(0xFF8B4D4B),
        secondary = Color(0xFFE9BEA6),
        onSecondary = Color(0xFF452A1A),
        secondaryContainer = Color(0xFF5E402E),
        onSecondaryContainer = Color(0xFFFFDBC8),
        tertiary = Color(0xFFBFC2FA),
        onTertiary = Color(0xFF282C5A),
        tertiaryContainer = Color(0xFF3E4372),
        onTertiaryContainer = Color(0xFFE0E0FF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF121318),
        onBackground = Color(0xFFE2E2E9),
        surface = Color(0xFF121318),
        onSurface = Color(0xFFE2E2E9),
        surfaceVariant = Color(0xFF444651),
        onSurfaceVariant = Color(0xFFC4C6D3),
        outline = Color(0xFF8E909C),
        outlineVariant = Color(0xFF444651),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE2E2E9),
        inverseOnSurface = Color(0xFF2F3036),
        surfaceDim = Color(0xFF121318),
        surfaceBright = Color(0xFF38393F),
        surfaceContainerLowest = Color(0xFF0D0E13),
        surfaceContainerLow = Color(0xFF1A1B21),
        surfaceContainer = Color(0xFF1E1F25),
        surfaceContainerHigh = Color(0xFF282A2F),
        surfaceContainerHighest = Color(0xFF33343A)
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF8B4D4B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDAD8),
        onPrimaryContainer = Color(0xFF380B0D),
        inversePrimary = Color(0xFFFFB3B1),
        secondary = Color(0xFF785744),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDBC8),
        onSecondaryContainer = Color(0xFF2D1607),
        tertiary = Color(0xFF565B8B),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE0E0FF),
        onTertiaryContainer = Color(0xFF121644),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFEFBFF),
        onBackground = Color(0xFF1A1B21),
        surface = Color(0xFFFEFBFF),
        onSurface = Color(0xFF1A1B21),
        surfaceVariant = Color(0xFFE0E2EF),
        onSurfaceVariant = Color(0xFF444651),
        outline = Color(0xFF747782),
        outlineVariant = Color(0xFFC4C6D3),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2F3036),
        inverseOnSurface = Color(0xFFF1F0F7),
        surfaceDim = Color(0xFFDAD9E0),
        surfaceBright = Color(0xFFFAF8FF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF4F3FA),
        surfaceContainer = Color(0xFFEEEDF4),
        surfaceContainerHigh = Color(0xFFE8E7EF),
        surfaceContainerHighest = Color(0xFFE2E2E9)
    )
}
