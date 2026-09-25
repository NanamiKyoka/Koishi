package com.nanami.koishi.feature.tools.decision_maker

import android.net.Uri
import androidx.annotation.StringRes
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionMode
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionOption
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionPhase
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionTopic

data class DecisionMakerUiState(
    val topics: List<DecisionTopic> = emptyList(),
    val currentTopic: DecisionTopic? = null,
    val mode: DecisionMode = DecisionMode.WHEEL,
    val phase: DecisionPhase = DecisionPhase.IDLE,
    val shakeEnergy: Float = 0f,
    val result: DecisionOption? = null,
    val animationToken: Long = 0L,
    val hapticsEnabled: Boolean = true,
    val showTopicManager: Boolean = false,
    val showOptionEditor: Boolean = false,
    val showHelpDialog: Boolean = false,
    @StringRes val userMessageRes: Int? = null,
    val userMessageArg: String? = null
) {
    val playableOptionCount: Int
        get() = currentTopic?.options?.count { it.text.isNotBlank() } ?: 0

    val canPlay: Boolean
        get() = playableOptionCount >= MIN_PLAYABLE_OPTIONS

    val isBusy: Boolean
        get() = phase == DecisionPhase.SPINNING || phase == DecisionPhase.DRAWING

    val hasResult: Boolean
        get() = phase == DecisionPhase.RESULT && result != null

    companion object {
        const val MIN_PLAYABLE_OPTIONS = 2
    }
}

sealed interface DecisionMakerUiEvent {
    data class OnSelectMode(val mode: DecisionMode) : DecisionMakerUiEvent
    data object OnStartSpin : DecisionMakerUiEvent
    data object OnSpinFinished : DecisionMakerUiEvent
    data class OnShakeProgress(val delta: Float) : DecisionMakerUiEvent
    data object OnShakeImpulse : DecisionMakerUiEvent
    data object OnDrawFinished : DecisionMakerUiEvent
    data object OnResetRound : DecisionMakerUiEvent
    data class OnSelectTopic(val topicId: String) : DecisionMakerUiEvent
    data class OnShowTopicManager(val show: Boolean) : DecisionMakerUiEvent
    data class OnShowOptionEditor(val show: Boolean) : DecisionMakerUiEvent
    data class OnShowHelpDialog(val show: Boolean) : DecisionMakerUiEvent
    data class OnSaveTopic(val topic: DecisionTopic) : DecisionMakerUiEvent
    data class OnDeleteTopic(val topicId: String) : DecisionMakerUiEvent
    data object OnRestoreBuiltInPresets : DecisionMakerUiEvent
    data object OnCreateTopic : DecisionMakerUiEvent
    data class OnImportFrom(val uri: Uri) : DecisionMakerUiEvent
    data class OnExportTo(val uri: Uri) : DecisionMakerUiEvent
    data class OnHapticsEnabledChange(val enabled: Boolean) : DecisionMakerUiEvent
    data object OnDismissMessage : DecisionMakerUiEvent
}
