package com.nanami.koishi.core.designsystem.adaptive

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowLayoutModeTest {

    @Test
    fun compactBelowMediumBreakpoint() {
        assertEquals(WindowLayoutMode.COMPACT, windowLayoutModeForWidth(599.dp))
    }

    @Test
    fun mediumFromSixHundredDp() {
        assertEquals(WindowLayoutMode.MEDIUM, windowLayoutModeForWidth(600.dp))
        assertEquals(WindowLayoutMode.MEDIUM, windowLayoutModeForWidth(839.dp))
    }

    @Test
    fun expandedFromEightHundredFortyDp() {
        assertEquals(WindowLayoutMode.EXPANDED, windowLayoutModeForWidth(840.dp))
    }
}
