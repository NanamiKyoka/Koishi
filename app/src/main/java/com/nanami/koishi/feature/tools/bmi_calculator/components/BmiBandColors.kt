package com.nanami.koishi.feature.tools.bmi_calculator.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiCategory

@Immutable
data class BmiBandColors(
    val underweight: Color,
    val normal: Color,
    val overweight: Color,
    val obese: Color
) {
    fun of(category: BmiCategory): Color = when (category) {
        BmiCategory.UNDERWEIGHT -> underweight
        BmiCategory.NORMAL -> normal
        BmiCategory.OVERWEIGHT -> overweight
        BmiCategory.OBESE -> obese
    }
}

@Composable
fun rememberBmiBandColors(): BmiBandColors {
    val onLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    return remember(onLightSurface) {
        if (onLightSurface) {
            BmiBandColors(
                underweight = Color(0xFF2563EB),
                normal = Color(0xFF15803D),
                overweight = Color(0xFFB45309),
                obese = Color(0xFFDC2626)
            )
        } else {
            BmiBandColors(
                underweight = Color(0xFF60A5FA),
                normal = Color(0xFF4ADE80),
                overweight = Color(0xFFFBBF24),
                obese = Color(0xFFF87171)
            )
        }
    }
}
