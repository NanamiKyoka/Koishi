package com.nanami.koishi.core.designsystem.adaptive

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomBarVisibilityStateTest {

    @Test
    fun upwardScroll_hidesAfterThreshold() {
        val state = BottomBarVisibilityState(hideThresholdPx = 16f, showThresholdPx = 10f)

        state.onScroll(15f)
        assertTrue(state.isVisible)

        state.onScroll(1f)
        assertFalse(state.isVisible)
    }

    @Test
    fun downwardScroll_showsAfterThreshold() {
        val state = BottomBarVisibilityState(
            hideThresholdPx = 16f,
            showThresholdPx = 10f,
            initiallyVisible = false
        )

        state.onScroll(-9f)
        assertFalse(state.isVisible)

        state.onScroll(-1f)
        assertTrue(state.isVisible)
    }

    @Test
    fun smallDirectionChanges_doNotToggleVisibility() {
        val state = BottomBarVisibilityState(hideThresholdPx = 16f, showThresholdPx = 10f)

        state.onScroll(6f)
        state.onScroll(-4f)
        state.onScroll(5f)

        assertTrue(state.isVisible)
    }

    @Test
    fun directionReversal_resetsAccumulatedDelta() {
        val state = BottomBarVisibilityState(hideThresholdPx = 16f, showThresholdPx = 10f)

        state.onScroll(10f)
        state.onScroll(-10f)
        state.onScroll(15f)

        assertTrue(state.isVisible)
    }

    @Test
    fun show_resetsStateAndAccumulatedDelta() {
        val state = BottomBarVisibilityState(hideThresholdPx = 16f, showThresholdPx = 10f)

        state.onScroll(20f)
        state.show()
        state.onScroll(15f)

        assertTrue(state.isVisible)
    }
}