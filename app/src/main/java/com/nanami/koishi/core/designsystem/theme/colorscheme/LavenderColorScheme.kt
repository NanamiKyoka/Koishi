package com.nanami.koishi.core.designsystem.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Lavender 主题，主色取自 #B57EDC */
internal object LavenderColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFE1B6FD),
        onPrimary = Color(0xFF43205C),
        primaryContainer = Color(0xFF5B3774),
        onPrimaryContainer = Color(0xFFF3DAFF),
        inversePrimary = Color(0xFF744F8E),
        secondary = Color(0xFFC8C4DC),
        onSecondary = Color(0xFF302E42),
        secondaryContainer = Color(0xFF464559),
        onSecondaryContainer = Color(0xFFE4DFF9),
        tertiary = Color(0xFFFFB2BD),
        onTertiary = Color(0xFF541E29),
        tertiaryContainer = Color(0xFF70343F),
        onTertiaryContainer = Color(0xFFFFD9DD),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF131318),
        onBackground = Color(0xFFE4E1E9),
        surface = Color(0xFF131318),
        onSurface = Color(0xFFE4E1E9),
        surfaceVariant = Color(0xFF464651),
        onSurfaceVariant = Color(0xFFC7C5D3),
        outline = Color(0xFF918F9C),
        outlineVariant = Color(0xFF464651),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE4E1E9),
        inverseOnSurface = Color(0xFF303036),
        surfaceDim = Color(0xFF131318),
        surfaceBright = Color(0xFF39383F),
        surfaceContainerLowest = Color(0xFF0E0E13),
        surfaceContainerLow = Color(0xFF1B1B21),
        surfaceContainer = Color(0xFF1F1F25),
        surfaceContainerHigh = Color(0xFF2A292F),
        surfaceContainerHighest = Color(0xFF35343A)
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF744F8E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFF3DAFF),
        onPrimaryContainer = Color(0xFF2C0746),
        inversePrimary = Color(0xFFE1B6FD),
        secondary = Color(0xFF5E5C71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE4DFF9),
        onSecondaryContainer = Color(0xFF1B1A2C),
        tertiary = Color(0xFF8C4B56),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD9DD),
        onTertiaryContainer = Color(0xFF390915),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF1B1B21),
        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF1B1B21),
        surfaceVariant = Color(0xFFE3E1EF),
        onSurfaceVariant = Color(0xFF464651),
        outline = Color(0xFF777682),
        outlineVariant = Color(0xFFC7C5D3),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF303036),
        inverseOnSurface = Color(0xFFF3EFF7),
        surfaceDim = Color(0xFFDCD9E0),
        surfaceBright = Color(0xFFFCF8FF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF6F2FA),
        surfaceContainer = Color(0xFFF0ECF4),
        surfaceContainerHigh = Color(0xFFEAE7EF),
        surfaceContainerHighest = Color(0xFFE4E1E9)
    )
}
