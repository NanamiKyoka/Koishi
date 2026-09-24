package com.nanami.koishi.core.designsystem.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Koishi 默认主题，以水绿、紫罗兰与缎带金为主色 */
internal object KoishiColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFF8CD5A6),
        onPrimary = Color(0xFF003820),
        primaryContainer = Color(0xFF005231),
        onPrimaryContainer = Color(0xFFA6F2C1),
        inversePrimary = Color(0xFF1E6A44),
        secondary = Color(0xFFC8BFEE),
        onSecondary = Color(0xFF302B49),
        secondaryContainer = Color(0xFF474160),
        onSecondaryContainer = Color(0xFFE5DEFF),
        tertiary = Color(0xFFECC34A),
        onTertiary = Color(0xFF3E2E00),
        tertiaryContainer = Color(0xFF594400),
        onTertiaryContainer = Color(0xFFFFE08B),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0F1511),
        onBackground = Color(0xFFDEE4DD),
        surface = Color(0xFF0F1511),
        onSurface = Color(0xFFDEE4DD),
        surfaceVariant = Color(0xFF404942),
        onSurfaceVariant = Color(0xFFC0C9C0),
        outline = Color(0xFF8A938B),
        outlineVariant = Color(0xFF404942),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFDEE4DD),
        inverseOnSurface = Color(0xFF2D322C),
        surfaceDim = Color(0xFF0F1511),
        surfaceBright = Color(0xFF363A35),
        surfaceContainerLowest = Color(0xFF0A0F0C),
        surfaceContainerLow = Color(0xFF171D19),
        surfaceContainer = Color(0xFF1B211D),
        surfaceContainerHigh = Color(0xFF252C27),
        surfaceContainerHighest = Color(0xFF303732)
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF1E6A44),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFA6F2C1),
        onPrimaryContainer = Color(0xFF002111),
        inversePrimary = Color(0xFF8CD5A6),
        secondary = Color(0xFF5F5979),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE5DEFF),
        onSecondaryContainer = Color(0xFF1C1733),
        tertiary = Color(0xFF755C00),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFE08B),
        onTertiaryContainer = Color(0xFF241A00),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF6FAF4),
        onBackground = Color(0xFF181D19),
        surface = Color(0xFFF6FAF4),
        onSurface = Color(0xFF181D19),
        surfaceVariant = Color(0xFFDCE5DC),
        onSurfaceVariant = Color(0xFF404942),
        outline = Color(0xFF717A72),
        outlineVariant = Color(0xFFC0C9C0),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2D322C),
        inverseOnSurface = Color(0xFFEEF2E9),
        surfaceDim = Color(0xFFD7DBD3),
        surfaceBright = Color(0xFFF7FBF2),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF0F5EE),
        surfaceContainer = Color(0xFFEBEFE9),
        surfaceContainerHigh = Color(0xFFE5EAE3),
        surfaceContainerHighest = Color(0xFFDFE4DE)
    )
}
