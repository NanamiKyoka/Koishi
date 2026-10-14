package com.nanami.koishi.feature.tools.spirit_level

import androidx.annotation.StringRes
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.spirit_level.engine.LevelMode
import com.nanami.koishi.feature.tools.spirit_level.engine.SpiritLevelFrame
import kotlin.math.abs

enum class LevelStatus(@StringRes val labelRes: Int) {
    LEVEL(R.string.spirit_level_status_level),
    NEAR(R.string.spirit_level_status_near),
    OFF(R.string.spirit_level_status_off)
}

data class SpiritLevelUiState(
    val isSupported: Boolean = true,
    val isHeld: Boolean = false,
    val mode: LevelMode = LevelMode.SURFACE,
    val frame: SpiritLevelFrame = SpiritLevelFrame(),
    val hasCalibration: Boolean = false,
    @StringRes val errorRes: Int? = null
) {
    val status: LevelStatus
        get() = when {
            abs(frame.pitchDeg) <= 0.5 && abs(frame.rollDeg) <= 0.5 -> LevelStatus.LEVEL
            abs(frame.pitchDeg) <= 2.0 && abs(frame.rollDeg) <= 2.0 -> LevelStatus.NEAR
            else -> LevelStatus.OFF
        }

    val bubbleX: Float
        get() = (-frame.rollDeg / 12.0).toFloat().coerceIn(-1f, 1f)

    val bubbleY: Float
        get() = (-frame.pitchDeg / 12.0).toFloat().coerceIn(-1f, 1f)
}

sealed interface SpiritLevelUiEvent {
    data object OnStart : SpiritLevelUiEvent
    data object OnStop : SpiritLevelUiEvent
    data object OnToggleHold : SpiritLevelUiEvent
    data class OnModeChange(val mode: LevelMode) : SpiritLevelUiEvent
    data object OnCalibrate : SpiritLevelUiEvent
    data object OnResetCalibration : SpiritLevelUiEvent
    data object OnDismissError : SpiritLevelUiEvent
}
