package com.nanami.koishi.core.designsystem.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Nord 主题，主色取自 #88C0D0 */
internal object NordColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFF90D0E3),
        onPrimary = Color(0xFF003640),
        primaryContainer = Color(0xFF004E5C),
        onPrimaryContainer = Color(0xFFACECFF),
        inversePrimary = Color(0xFF1F6777),
        secondary = Color(0xFFB4CBCA),
        onSecondary = Color(0xFF1F3434),
        secondaryContainer = Color(0xFF364A4A),
        onSecondaryContainer = Color(0xFFD0E7E6),
        tertiary = Color(0xFFDEBBE3),
        onTertiary = Color(0xFF402747),
        tertiaryContainer = Color(0xFF573D5E),
        onTertiaryContainer = Color(0xFFFAD7FF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF101416),
        onBackground = Color(0xFFDFE3E5),
        surface = Color(0xFF101416),
        onSurface = Color(0xFFDFE3E5),
        surfaceVariant = Color(0xFF3E484C),
        onSurfaceVariant = Color(0xFFBEC8CC),
        outline = Color(0xFF889296),
        outlineVariant = Color(0xFF3E484C),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFDFE3E5),
        inverseOnSurface = Color(0xFF2D3133),
        surfaceDim = Color(0xFF101416),
        surfaceBright = Color(0xFF353A3C),
        surfaceContainerLowest = Color(0xFF0B0F10),
        surfaceContainerLow = Color(0xFF181C1E),
        surfaceContainer = Color(0xFF1C2022),
        surfaceContainerHigh = Color(0xFF262B2C),
        surfaceContainerHighest = Color(0xFF313537)
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF1F6777),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFACECFF),
        onPrimaryContainer = Color(0xFF001F26),
        inversePrimary = Color(0xFF90D0E3),
        secondary = Color(0xFF4D6262),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD0E7E6),
        onSecondaryContainer = Color(0xFF091F1F),
        tertiary = Color(0xFF715577),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFAD7FF),
        onTertiaryContainer = Color(0xFF291231),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF9FDFF),
        onBackground = Color(0xFF181C1E),
        surface = Color(0xFFF9FDFF),
        onSurface = Color(0xFF181C1E),
        surfaceVariant = Color(0xFFDAE4E9),
        onSurfaceVariant = Color(0xFF3E484C),
        outline = Color(0xFF6E797D),
        outlineVariant = Color(0xFFBEC8CC),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2D3133),
        inverseOnSurface = Color(0xFFEEF1F3),
        surfaceDim = Color(0xFFD7DADC),
        surfaceBright = Color(0xFFF6FAFC),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF1F4F6),
        surfaceContainer = Color(0xFFEBEEF0),
        surfaceContainerHigh = Color(0xFFE5E9EB),
        surfaceContainerHighest = Color(0xFFDFE3E5)
    )
}
