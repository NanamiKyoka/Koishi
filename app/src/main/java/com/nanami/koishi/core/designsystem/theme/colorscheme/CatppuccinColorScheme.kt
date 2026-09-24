package com.nanami.koishi.core.designsystem.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Catppuccin 主题，主色取自 #CBA6F7 */
internal object CatppuccinColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFD9B9FF),
        onPrimary = Color(0xFF3E2260),
        primaryContainer = Color(0xFF553978),
        onPrimaryContainer = Color(0xFFEEDBFF),
        inversePrimary = Color(0xFF6E5192),
        secondary = Color(0xFFC7C4DD),
        onSecondary = Color(0xFF2F2E42),
        secondaryContainer = Color(0xFF464559),
        onSecondaryContainer = Color(0xFFE3E0F9),
        tertiary = Color(0xFFF8B2D7),
        onTertiary = Color(0xFF4F1F3D),
        tertiaryContainer = Color(0xFF6A3554),
        onTertiaryContainer = Color(0xFFFFD8EA),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF101417),
        onBackground = Color(0xFFDFE3E8),
        surface = Color(0xFF101417),
        onSurface = Color(0xFFDFE3E8),
        surfaceVariant = Color(0xFF3F484F),
        onSurfaceVariant = Color(0xFFBFC8D0),
        outline = Color(0xFF89929A),
        outlineVariant = Color(0xFF3F484F),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFDFE3E8),
        inverseOnSurface = Color(0xFF2D3135),
        surfaceDim = Color(0xFF101417),
        surfaceBright = Color(0xFF353A3E),
        surfaceContainerLowest = Color(0xFF0A0F12),
        surfaceContainerLow = Color(0xFF181C20),
        surfaceContainer = Color(0xFF1C2024),
        surfaceContainerHigh = Color(0xFF262A2E),
        surfaceContainerHighest = Color(0xFF313539)
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF6E5192),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEEDBFF),
        onPrimaryContainer = Color(0xFF28094A),
        inversePrimary = Color(0xFFD9B9FF),
        secondary = Color(0xFF5E5C71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE3E0F9),
        onSecondaryContainer = Color(0xFF1A1A2C),
        tertiary = Color(0xFF854C6C),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8EA),
        onTertiaryContainer = Color(0xFF360927),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFBFCFF),
        onBackground = Color(0xFF181C20),
        surface = Color(0xFFFBFCFF),
        onSurface = Color(0xFF181C20),
        surfaceVariant = Color(0xFFDBE4ED),
        onSurfaceVariant = Color(0xFF3F484F),
        outline = Color(0xFF6F7880),
        outlineVariant = Color(0xFFBFC8D0),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2D3135),
        inverseOnSurface = Color(0xFFEEF1F6),
        surfaceDim = Color(0xFFD7DADF),
        surfaceBright = Color(0xFFF6F9FE),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF1F4F9),
        surfaceContainer = Color(0xFFEBEEF3),
        surfaceContainerHigh = Color(0xFFE5E8ED),
        surfaceContainerHighest = Color(0xFFDFE3E8)
    )
}
