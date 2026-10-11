package com.nanami.koishi.feature.tools.noise_meter.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.noise_meter.engine.NoiseLevel

@StringRes
fun NoiseLevel.labelRes(): Int = when (this) {
    NoiseLevel.VERY_QUIET -> R.string.noise_meter_level_very_quiet
    NoiseLevel.QUIET -> R.string.noise_meter_level_quiet
    NoiseLevel.MODERATE -> R.string.noise_meter_level_moderate
    NoiseLevel.NOISY -> R.string.noise_meter_level_noisy
    NoiseLevel.VERY_NOISY -> R.string.noise_meter_level_very_noisy
    NoiseLevel.DANGEROUS -> R.string.noise_meter_level_dangerous
    NoiseLevel.EXTREME -> R.string.noise_meter_level_extreme
}

@StringRes
fun NoiseLevel.descriptionRes(): Int = when (this) {
    NoiseLevel.VERY_QUIET -> R.string.noise_meter_desc_very_quiet
    NoiseLevel.QUIET -> R.string.noise_meter_desc_quiet
    NoiseLevel.MODERATE -> R.string.noise_meter_desc_moderate
    NoiseLevel.NOISY -> R.string.noise_meter_desc_noisy
    NoiseLevel.VERY_NOISY -> R.string.noise_meter_desc_very_noisy
    NoiseLevel.DANGEROUS -> R.string.noise_meter_desc_dangerous
    NoiseLevel.EXTREME -> R.string.noise_meter_desc_extreme
}

fun NoiseLevel.color(): Color = when (this) {
    NoiseLevel.VERY_QUIET -> Color(0xFF43A047)
    NoiseLevel.QUIET -> Color(0xFF7CB342)
    NoiseLevel.MODERATE -> Color(0xFFF9A825)
    NoiseLevel.NOISY -> Color(0xFFEF6C00)
    NoiseLevel.VERY_NOISY -> Color(0xFFE64A19)
    NoiseLevel.DANGEROUS -> Color(0xFFD32F2F)
    NoiseLevel.EXTREME -> Color(0xFF8E0000)
}

fun NoiseLevel.upperBoundDb(): Double = when (this) {
    NoiseLevel.VERY_QUIET -> 30.0
    NoiseLevel.QUIET -> 40.0
    NoiseLevel.MODERATE -> 55.0
    NoiseLevel.NOISY -> 70.0
    NoiseLevel.VERY_NOISY -> 85.0
    NoiseLevel.DANGEROUS -> 100.0
    NoiseLevel.EXTREME -> 120.0
}