package com.nanami.koishi.core.designsystem.adaptive

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

internal class BottomBarVisibilityState(
    hideThresholdPx: Float,
    showThresholdPx: Float,
    initiallyVisible: Boolean = true
) {
    private val hideThresholdPx = hideThresholdPx.coerceAtLeast(0f)
    private val showThresholdPx = showThresholdPx.coerceAtLeast(0f)
    private var accumulatedDelta = 0f

    var isVisible: Boolean = initiallyVisible
        private set

    fun onScroll(delta: Float) {
        if (delta == 0f) return

        val directionChanged = accumulatedDelta != 0f &&
            (delta > 0f) != (accumulatedDelta > 0f)
        accumulatedDelta = if (directionChanged) delta else accumulatedDelta + delta

        when {
            isVisible && accumulatedDelta >= hideThresholdPx -> {
                isVisible = false
                accumulatedDelta = 0f
            }

            !isVisible && accumulatedDelta <= -showThresholdPx -> {
                isVisible = true
                accumulatedDelta = 0f
            }
        }
    }

    fun show() {
        isVisible = true
        accumulatedDelta = 0f
    }
}

@Stable
class BottomBarVisibilityController internal constructor(
    hideThresholdPx: Float = Float.MAX_VALUE,
    showThresholdPx: Float = Float.MAX_VALUE
) {
    private val state = BottomBarVisibilityState(
        hideThresholdPx = hideThresholdPx,
        showThresholdPx = showThresholdPx
    )

    var isVisible by mutableStateOf(state.isVisible)
        private set

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (source == NestedScrollSource.UserInput) {
                val upwardDelta = -(consumed.y.takeIf { it != 0f } ?: available.y)
                onScroll(upwardDelta)
            }
            return Offset.Zero
        }
    }

    fun onScroll(delta: Float) {
        state.onScroll(delta)
        isVisible = state.isVisible
    }

    fun show() {
        state.show()
        isVisible = true
    }
}

val LocalBottomBarVisibilityController = staticCompositionLocalOf {
    BottomBarVisibilityController()
}