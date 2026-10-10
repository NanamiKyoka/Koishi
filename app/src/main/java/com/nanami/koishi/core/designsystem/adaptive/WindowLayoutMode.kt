package com.nanami.koishi.core.designsystem.adaptive

import androidx.compose.runtime.staticCompositionLocalOf

enum class WindowLayoutMode {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

val LocalWindowLayoutMode = staticCompositionLocalOf { WindowLayoutMode.COMPACT }
