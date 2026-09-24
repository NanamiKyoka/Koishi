package com.nanami.koishi.core.designsystem.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Gruvbox 主题，主色取自 #D79921 */
internal object GruvboxColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFF6BD5C),
        onPrimary = Color(0xFF432C00),
        primaryContainer = Color(0xFF604100),
        onPrimaryContainer = Color(0xFFFFDEAD),
        inversePrimary = Color(0xFF7F5700),
        secondary = Color(0xFFDFC1A9),
        onSecondary = Color(0xFF3F2D1B),
        secondaryContainer = Color(0xFF574330),
        onSecondaryContainer = Color(0xFFFCDDC3),
        tertiary = Color(0xFFABD19F),
        onTertiary = Color(0xFF183714),
        tertiaryContainer = Color(0xFF2E4E28),
        onTertiaryContainer = Color(0xFFC6EDBA),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF1A120D),
        onBackground = Color(0xFFF0DFD7),
        surface = Color(0xFF1A120D),
        onSurface = Color(0xFFF0DFD7),
        surfaceVariant = Color(0xFF554339),
        onSurfaceVariant = Color(0xFFDBC2B4),
        outline = Color(0xFFA28C80),
        outlineVariant = Color(0xFF554339),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFF0DFD7),
        inverseOnSurface = Color(0xFF382E29),
        surfaceDim = Color(0xFF1A120D),
        surfaceBright = Color(0xFF413732),
        surfaceContainerLowest = Color(0xFF140D08),
        surfaceContainerLow = Color(0xFF221A15),
        surfaceContainer = Color(0xFF261E19),
        surfaceContainerHigh = Color(0xFF312823),
        surfaceContainerHighest = Color(0xFF3D332D)
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF7F5700),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDEAD),
        onPrimaryContainer = Color(0xFF281900),
        inversePrimary = Color(0xFFF6BD5C),
        secondary = Color(0xFF705A46),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFCDDC3),
        onSecondaryContainer = Color(0xFF281809),
        tertiary = Color(0xFF45673E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFC6EDBA),
        onTertiaryContainer = Color(0xFF032103),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF221A15),
        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF221A15),
        surfaceVariant = Color(0xFFF8DDD0),
        onSurfaceVariant = Color(0xFF554339),
        outline = Color(0xFF887368),
        outlineVariant = Color(0xFFDBC2B4),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF382E29),
        inverseOnSurface = Color(0xFFFFEDE5),
        surfaceDim = Color(0xFFE7D7CF),
        surfaceBright = Color(0xFFFFF8F5),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFFF1EA),
        surfaceContainer = Color(0xFFFCEAE2),
        surfaceContainerHigh = Color(0xFFF6E5DD),
        surfaceContainerHighest = Color(0xFFF0DFD7)
    )
}
