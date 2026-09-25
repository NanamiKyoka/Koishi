package com.nanami.koishi.feature.tools.image_sketch

import com.nanami.koishi.feature.tools.image_sketch.engine.ImageSketchEngine
import com.nanami.koishi.feature.tools.image_sketch.engine.ImageSketchParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageSketchContractTest {

    @Test
    fun defaultUiState_hasExpectedDefaults() {
        val state = ImageSketchUiState()

        assertNull(state.sourceUri)
        assertNull(state.sourceBitmap)
        assertNull(state.previewSourceBitmap)
        assertNull(state.previewBitmap)
        assertNull(state.userMessage)
        assertFalse(state.isProcessing)
        assertFalse(state.isSaving)
        assertFalse(state.isHelpDialogOpen)
        assertFalse(state.hasImage)
        assertFalse(state.canSave)
        assertEquals(ImageSketchEngine.DEFAULT_RADIUS, state.params.radius)
        assertEquals(15, state.effectiveRadius)
    }

    @Test
    fun params_defaultRadiusAndEffectiveOddConversion() {
        val defaultParams = ImageSketchParams()
        assertEquals(15, defaultParams.radius)
        assertEquals(15, defaultParams.effectiveRadius)

        assertEquals(17, defaultParams.copy(radius = 16).effectiveRadius)
        assertEquals(15, defaultParams.copy(radius = 15).effectiveRadius)
        assertEquals(1, defaultParams.copy(radius = 1).effectiveRadius)
        assertEquals(51, defaultParams.copy(radius = 50).effectiveRadius)
        assertEquals(101, defaultParams.copy(radius = 100).effectiveRadius)
    }

    @Test
    fun uiState_effectiveRadiusFollowsParams() {
        val state = ImageSketchUiState(params = ImageSketchParams(radius = 24))
        assertEquals(25, state.effectiveRadius)
    }

    @Test
    fun uiState_canSaveRequiresPreviewAndIdleState() {
        val busy = ImageSketchUiState(isProcessing = true)
        assertFalse(busy.canSave)

        val saving = ImageSketchUiState(isSaving = true)
        assertFalse(saving.canSave)

        val hasImageOnly = ImageSketchUiState(sourceBitmap = null)
        assertFalse(hasImageOnly.hasImage)
        assertFalse(hasImageOnly.canSave)
    }

    @Test
    fun events_areDistinguishableByType() {
        val events = listOf(
            ImageSketchUiEvent.OnClearImage,
            ImageSketchUiEvent.OnResetParams,
            ImageSketchUiEvent.OnSaveResult,
            ImageSketchUiEvent.OnDismissMessage,
            ImageSketchUiEvent.OnRadiusChanged(20),
            ImageSketchUiEvent.OnToggleHelpDialog(true)
        )

        assertTrue(events[0] is ImageSketchUiEvent.OnClearImage)
        assertEquals(20, (events[4] as ImageSketchUiEvent.OnRadiusChanged).radius)
        assertTrue((events[5] as ImageSketchUiEvent.OnToggleHelpDialog).open)
    }
}
