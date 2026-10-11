package com.nanami.koishi.feature.tools.noise_meter

import androidx.annotation.StringRes
import com.nanami.koishi.feature.tools.noise_meter.engine.FrequencyWeighting
import com.nanami.koishi.feature.tools.noise_meter.engine.NoiseConfig
import com.nanami.koishi.feature.tools.noise_meter.engine.NoiseLevel
import com.nanami.koishi.feature.tools.noise_meter.engine.TimeWeighting

data class NoiseMeterUiState(
    val hasPermission: Boolean = false,
    val isMeasuring: Boolean = false,
    val hasReading: Boolean = false,
    val levelDb: Double = 0.0,
    val peakDb: Double = 0.0,
    val minimumDb: Double = 0.0,
    val leqDb: Double = 0.0,
    val elapsedMs: Long = 0L,
    val level: NoiseLevel = NoiseLevel.VERY_QUIET,
    val frequencyWeighting: FrequencyWeighting = FrequencyWeighting.A,
    val timeWeighting: TimeWeighting = TimeWeighting.FAST,
    val calibrationOffsetDb: Double = NoiseConfig.DEFAULT_CALIBRATION_OFFSET_DB,
    @StringRes val errorRes: Int? = null
) {

    val gaugeFraction: Float
        get() = ((levelDb - MIN_DISPLAY_DB) / (MAX_DISPLAY_DB - MIN_DISPLAY_DB))
            .toFloat()
            .coerceIn(0f, 1f)

    companion object {
        const val MIN_DISPLAY_DB = 0.0
        const val MAX_DISPLAY_DB = 120.0
    }
}

sealed interface NoiseMeterUiEvent {
    data class OnPermissionResult(val granted: Boolean) : NoiseMeterUiEvent
    data object OnStart : NoiseMeterUiEvent
    data object OnStop : NoiseMeterUiEvent
    data object OnReset : NoiseMeterUiEvent
    data class OnCalibrationChange(val offsetDb: Double) : NoiseMeterUiEvent
    data class OnFrequencyWeightingChange(val weighting: FrequencyWeighting) : NoiseMeterUiEvent
    data class OnTimeWeightingChange(val weighting: TimeWeighting) : NoiseMeterUiEvent
    data object OnDismissError : NoiseMeterUiEvent
}