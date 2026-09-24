package com.nanami.koishi.core.designsystem.theme.colorscheme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

internal abstract class BaseColorScheme {

    abstract val darkScheme: ColorScheme
    abstract val lightScheme: ColorScheme

    private val amoledSurfaceContainer = Color(0xFF0C0C0C)
    private val amoledSurfaceContainerHigh = Color(0xFF131313)
    private val amoledSurfaceContainerHighest = Color(0xFF1B1B1B)

    fun getColorScheme(
        isDark: Boolean,
        isAmoled: Boolean,
        overrideDarkSurfaceContainers: Boolean
    ): ColorScheme {
        if (!isDark) return lightScheme
        if (!isAmoled) return darkScheme

        val amoledScheme = darkScheme.copy(
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White
        )

        if (!overrideDarkSurfaceContainers) return amoledScheme

        return amoledScheme.copy(
            surfaceVariant = amoledSurfaceContainer,
            surfaceContainerLowest = amoledSurfaceContainer,
            surfaceContainerLow = amoledSurfaceContainer,
            surfaceContainer = amoledSurfaceContainer,
            surfaceContainerHigh = amoledSurfaceContainerHigh,
            surfaceContainerHighest = amoledSurfaceContainerHighest
        )
    }
}
