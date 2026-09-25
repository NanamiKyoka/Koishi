package com.nanami.koishi.feature.tools.decision_maker

import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionMode
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionOption
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionPhase
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionMakerContractTest {

    private val topic = DecisionTopic(
        id = "topic_test",
        title = "今天吃什么",
        options = listOf(
            DecisionOption("a", "麻辣烫", 2),
            DecisionOption("b", "火锅", 5),
            DecisionOption("c", "   ", 1)
        )
    )

    @Test
    fun defaultUiState_hasExpectedDefaults() {
        val state = DecisionMakerUiState()

        assertEquals(DecisionMode.WHEEL, state.mode)
        assertEquals(DecisionPhase.IDLE, state.phase)
        assertEquals(0f, state.shakeEnergy, 0.001f)
        assertNull(state.currentTopic)
        assertNull(state.result)
        assertEquals(0L, state.animationToken)
        assertTrue(state.hapticsEnabled)
        assertNull(state.userMessageRes)
        assertEquals(0, state.playableOptionCount)
        assertFalse(state.canPlay)
        assertFalse(state.isBusy)
        assertFalse(state.hasResult)
    }

    @Test
    fun playableOptionCount_ignoresBlankOptions() {
        val state = DecisionMakerUiState(currentTopic = topic)

        assertEquals(2, state.playableOptionCount)
        assertTrue(state.canPlay)
    }

    @Test
    fun canPlay_requiresTwoPlayableCandidates() {
        val singleOptionTopic = DecisionTopic(
            id = "solo",
            title = "唯一",
            options = listOf(DecisionOption("x", "回家"))
        )
        val blankOnlyTopic = DecisionTopic(
            id = "empty",
            title = "空",
            options = listOf(DecisionOption("y", "  "))
        )

        assertFalse(DecisionMakerUiState(currentTopic = singleOptionTopic).canPlay)
        assertFalse(DecisionMakerUiState(currentTopic = blankOnlyTopic).canPlay)
    }

    @Test
    fun busyAndResultFlags_followPhaseAndResult() {
        val spinning = DecisionMakerUiState(
            currentTopic = topic,
            phase = DecisionPhase.SPINNING,
            result = topic.options[0]
        )
        assertTrue(spinning.isBusy)
        assertFalse(spinning.hasResult)

        val settled = spinning.copy(phase = DecisionPhase.RESULT)
        assertTrue(settled.hasResult)
        assertFalse(settled.isBusy)

        val settledWithoutResult = spinning.copy(phase = DecisionPhase.RESULT, result = null)
        assertFalse(settledWithoutResult.hasResult)

        val shaking = spinning.copy(phase = DecisionPhase.SHAKING, result = null)
        assertFalse(shaking.isBusy)
        assertFalse(shaking.hasResult)
    }
}
