package com.nanami.koishi.core.designsystem.adaptive

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowLayoutMode {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

fun windowLayoutModeForWidth(width: Dp): WindowLayoutMode = when {
    width < 600.dp -> WindowLayoutMode.COMPACT
    width < 840.dp -> WindowLayoutMode.MEDIUM
    else -> WindowLayoutMode.EXPANDED
}

val LocalWindowLayoutMode = staticCompositionLocalOf { WindowLayoutMode.COMPACT }
