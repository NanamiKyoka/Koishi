package com.nanami.koishi.feature.tools.prism_tank

import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankFill
import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankParams
import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankRevealConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrismTankContractTest {

    @Test
    fun defaultUiState_hasExpectedDefaults() {
        val state = PrismTankUiState()

        assertEquals(PrismTankPanel.CREATE, state.panel)
        assertNull(state.innerImage)
        assertNull(state.coverImage)
        assertNull(state.resultBitmap)
        assertNull(state.sourceImage)
        assertNull(state.revealedInner)
        assertNull(state.revealedCover)
        assertNull(state.userMessage)
        assertFalse(state.hasBothImages)
        assertFalse(state.canSave)
        assertFalse(state.canReveal)
        assertFalse(state.canSaveRevealed)
        assertFalse(state.isProcessing)
        assertFalse(state.isSaving)

        assertEquals(PrismTankParams(), state.params)
        assertEquals(PrismTankRevealConfig(), state.revealConfig)
        assertEquals(1f, state.exposureGain, 0.001f)
    }


    @Test
    fun defaultParams_keepInnerBandBelowCoverBand() {
        val params = PrismTankParams()

        assertTrue(params.usableInnerThreshold < params.usableCoverThreshold)
        assertEquals(PrismTankFill.NEIGHBOR_AVG, PrismTankRevealConfig().fill)
        assertTrue(PrismTankRevealConfig().usableLower < PrismTankRevealConfig().usableUpper)
    }

    @Test
    fun revealConfig_clampsReversedThresholds() {
        val config = PrismTankRevealConfig(lowerThreshold = 200, higherThreshold = 10)

        assertTrue(config.usableLower < config.usableUpper)
        assertTrue(config.usableUpper <= PrismTankRevealConfig.MAX_THRESHOLD)
    }
}