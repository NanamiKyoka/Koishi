package com.nanami.koishi.feature.tools.mirage_tank

import android.net.Uri
import com.nanami.koishi.feature.tools.mirage_tank.engine.MirageTankMode
import com.nanami.koishi.feature.tools.mirage_tank.engine.MirageTankParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MirageTankContractTest {

    @Test
    fun defaultUiState_hasExpectedDefaults() {
        val state = MirageTankUiState()

        assertNull(state.frontImage)
        assertNull(state.backImage)
        assertNull(state.resultBitmap)
        assertNull(state.userMessage)
        assertFalse(state.isProcessing)
        assertFalse(state.isSaving)
        assertFalse(state.isHelpDialogOpen)
        assertFalse(state.hasBothImages)
        assertFalse(state.canSave)

        assertEquals(MirageTankMode.GRAYSCALE, state.params.mode)
        assertEquals(PreviewBackgroundMode.WHITE, state.previewBackgroundMode)
        assertEquals(1.0f, state.currentBackgroundRatio, 0.001f)
    }

    @Test
    fun currentBackgroundRatio_reflectsPreviewMode() {
        val whiteState = MirageTankUiState(previewBackgroundMode = PreviewBackgroundMode.WHITE)
        assertEquals(1.0f, whiteState.currentBackgroundRatio, 0.001f)

        val blackState = MirageTankUiState(previewBackgroundMode = PreviewBackgroundMode.BLACK)
        assertEquals(0.0f, blackState.currentBackgroundRatio, 0.001f)

        val customState = MirageTankUiState(
            previewBackgroundMode = PreviewBackgroundMode.CUSTOM,
            customBackgroundRatio = 0.65f
        )
        assertEquals(0.65f, customState.currentBackgroundRatio, 0.001f)
    }

    @Test
    fun params_defaultAndMutation() {
        val defaultParams = MirageTankParams()
        assertEquals(MirageTankMode.GRAYSCALE, defaultParams.mode)
        assertEquals(1.0f, defaultParams.frontLightness, 0.001f)
        assertEquals(1.0f, defaultParams.frontContrast, 0.001f)
        assertEquals(1.0f, defaultParams.backLightness, 0.001f)
        assertEquals(1.0f, defaultParams.backContrast, 0.001f)
        assertFalse(defaultParams.enableCheckerboard)
        assertEquals(0.15f, defaultParams.checkerboardStrength, 0.001f)

        val customParams = defaultParams.copy(
            mode = MirageTankMode.COLOR,
            frontLightness = 1.2f,
            enableCheckerboard = true,
            checkerboardStrength = 0.25f
        )
        assertEquals(MirageTankMode.COLOR, customParams.mode)
        assertEquals(1.2f, customParams.frontLightness, 0.001f)
        assertTrue(customParams.enableCheckerboard)
        assertEquals(0.25f, customParams.checkerboardStrength, 0.001f)
    }
}
