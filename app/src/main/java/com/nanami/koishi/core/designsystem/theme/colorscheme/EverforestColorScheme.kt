package com.nanami.koishi.core.designsystem.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Everforest 主题，主色取自 #A7C080 */
internal object EverforestColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFB5D08B),
        onPrimary = Color(0xFF223602),
        primaryContainer = Color(0xFF384D17),
        onPrimaryContainer = Color(0xFFD1ECA5),
        inversePrimary = Color(0xFF4F662D),
        secondary = Color(0xFFCDC7A7),
        onSecondary = Color(0xFF34311B),
        secondaryContainer = Color(0xFF4B472F),
        onSecondaryContainer = Color(0xFFE9E3C2),
        tertiary = Color(0xFF99CFDE),
        onTertiary = Color(0xFF003640),
        tertiaryContainer = Color(0xFF104E5A),
        onTertiaryContainer = Color(0xFFB5EBFB),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF101411),
        onBackground = Color(0xFFE0E3DE),
        surface = Color(0xFF101411),
        onSurface = Color(0xFFE0E3DE),
        surfaceVariant = Color(0xFF3F4942),
        onSurfaceVariant = Color(0xFFBFC9BF),
        outline = Color(0xFF89938A),
        outlineVariant = Color(0xFF3F4942),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE0E3DE),
        inverseOnSurface = Color(0xFF2D312E),
        surfaceDim = Color(0xFF101411),
        surfaceBright = Color(0xFF363A36),
        surfaceContainerLowest = Color(0xFF0B0F0C),
        surfaceContainerLow = Color(0xFF181D19),
        surfaceContainer = Color(0xFF1C211D),
        surfaceContainerHigh = Color(0xFF272B27),
        surfaceContainerHighest = Color(0xFF323632)
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF4F662D),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD1ECA5),
        onPrimaryContainer = Color(0xFF121F00),
        inversePrimary = Color(0xFFB5D08B),
        secondary = Color(0xFF635F45),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE9E3C2),
        onSecondaryContainer = Color(0xFF1E1C08),
        tertiary = Color(0xFF2F6673),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFB5EBFB),
        onTertiaryContainer = Color(0xFF001F26),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFAFDF7),
        onBackground = Color(0xFF181D19),
        surface = Color(0xFFFAFDF7),
        onSurface = Color(0xFF181D19),
        surfaceVariant = Color(0xFFDBE5DB),
        onSurfaceVariant = Color(0xFF3F4942),
        outline = Color(0xFF6F7A71),
        outlineVariant = Color(0xFFBFC9BF),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2D312E),
        inverseOnSurface = Color(0xFFEEF2EC),
        surfaceDim = Color(0xFFD7DBD5),
        surfaceBright = Color(0xFFF7FAF4),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF1F5EF),
        surfaceContainer = Color(0xFFEBEFE9),
        surfaceContainerHigh = Color(0xFFE6E9E3),
        surfaceContainerHighest = Color(0xFFE0E3DE)
    )
}
