package com.nanami.koishi.feature.tools.grid_split

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class GridSplitContractTest {

    @Test
    fun defaultUiState_hasExpectedDefaults() {
        val state = GridSplitUiState()

        assertEquals(GridSplitMode.SQUARE, state.mode)
        assertEquals(3, state.squareGridN)
        assertEquals(2, state.customRows)
        assertEquals(2, state.customCols)
        assertEquals(9, state.totalSquareSlices)
        assertEquals(4, state.totalCustomSlices)
        assertFalse(state.isProcessing)
        assertNull(state.selectedUri)
        assertNull(state.previewBitmap)
        assertNull(state.userMessage)
    }

    @Test
    fun totalSlicesCalculation_reflectsParameterChanges() {
        val state4x4 = GridSplitUiState(squareGridN = 4, customRows = 3, customCols = 5)
        assertEquals(16, state4x4.totalSquareSlices)
        assertEquals(15, state4x4.totalCustomSlices)

        val state8x8 = GridSplitUiState(squareGridN = 8, customRows = 8, customCols = 8)
        assertEquals(64, state8x8.totalSquareSlices)
        assertEquals(64, state8x8.totalCustomSlices)
    }
}
