package com.nanami.koishi.core.designsystem.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Tokyo Night 主题，主色取自 #7AA2F7 */
internal object TokyoNightColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFAEC6FF),
        onPrimary = Color(0xFF072E67),
        primaryContainer = Color(0xFF26457F),
        onPrimaryContainer = Color(0xFFD8E2FF),
        inversePrimary = Color(0xFF405D99),
        secondary = Color(0xFFB7C9D9),
        onSecondary = Color(0xFF22323F),
        secondaryContainer = Color(0xFF384956),
        onSecondaryContainer = Color(0xFFD3E5F5),
        tertiary = Color(0xFFDBBAF5),
        onTertiary = Color(0xFF3E2555),
        tertiaryContainer = Color(0xFF563B6D),
        onTertiaryContainer = Color(0xFFF1DAFF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0D1419),
        onBackground = Color(0xFFDDE3EA),
        surface = Color(0xFF0D1419),
        onSurface = Color(0xFFDDE3EA),
        surfaceVariant = Color(0xFF3C4851),
        onSurfaceVariant = Color(0xFFBBC8D2),
        outline = Color(0xFF86929C),
        outlineVariant = Color(0xFF3C4851),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFDDE3EA),
        inverseOnSurface = Color(0xFF2A3136),
        surfaceDim = Color(0xFF0D1419),
        surfaceBright = Color(0xFF333A3F),
        surfaceContainerLowest = Color(0xFF080F13),
        surfaceContainerLow = Color(0xFF161C21),
        surfaceContainer = Color(0xFF1A2025),
        surfaceContainerHigh = Color(0xFF242B30),
        surfaceContainerHighest = Color(0xFF2F363B)
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF405D99),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD8E2FF),
        onPrimaryContainer = Color(0xFF001A43),
        inversePrimary = Color(0xFFAEC6FF),
        secondary = Color(0xFF50606E),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD3E5F5),
        onSecondaryContainer = Color(0xFF0C1D29),
        tertiary = Color(0xFF6F5386),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF1DAFF),
        onTertiaryContainer = Color(0xFF280E3E),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFBFCFF),
        onBackground = Color(0xFF161C21),
        surface = Color(0xFFFBFCFF),
        onSurface = Color(0xFF161C21),
        surfaceVariant = Color(0xFFD7E4EF),
        onSurfaceVariant = Color(0xFF3C4851),
        outline = Color(0xFF6C7982),
        outlineVariant = Color(0xFFBBC8D2),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2A3136),
        inverseOnSurface = Color(0xFFEBF2F8),
        surfaceDim = Color(0xFFD4DBE1),
        surfaceBright = Color(0xFFF5FAFF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFEEF4FB),
        surfaceContainer = Color(0xFFE8EFF5),
        surfaceContainerHigh = Color(0xFFE2E9EF),
        surfaceContainerHighest = Color(0xFFDDE3EA)
    )
}
