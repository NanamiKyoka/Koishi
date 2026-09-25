package com.nanami.koishi.feature.tools.decision_maker.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

internal data class SliceTone(
    val fill: Color,
    val content: Color
)

internal data class WheelPalette(
    val tones: List<SliceTone>,
    val isDark: Boolean,
    val rimFill: Color,
    val rimStroke: Color,
    val gloss: Color,
    val innerShadow: Color,
    val divider: Color,
    val hubShadow: Color,
    val hubBase: Color,
    val hubBaseStroke: Color,
    val pointerOutline: Color
)

private val SLICE_HUES = floatArrayOf(348f, 16f, 40f, 96f, 168f, 208f, 262f, 318f)
private val CONTENT_LIGHT = Color(0xFFFBF8FD)
private val CONTENT_DARK = Color(0xFF221E25)
private const val MIN_CONTRAST_RATIO = 4.8f

/**
 * 扇区配色取自低饱和莫兰迪 / 马卡龙色环，文字取色按 WCAG 对比度自动择深择浅，
 * 若仍不达标则微调明度直到满足可读性下限
 */
private fun toneForFill(fill: Color): SliceTone {
    var current = fill
    repeat(MAX_LIGHTNESS_STEPS) {
        val content = contentColorFor(current)
        if (contrastRatio(current, content) >= MIN_CONTRAST_RATIO) {
            return SliceTone(current, content)
        }
        current = if (content == CONTENT_LIGHT) {
            lerp(current, Color.Black, LIGHTNESS_STEP)
        } else {
            lerp(current, Color.White, LIGHTNESS_STEP)
        }
    }
    return SliceTone(current, contentColorFor(current))
}

private fun toneFor(hue: Float, isDark: Boolean): SliceTone = toneForFill(
    if (isDark) {
        Color.hsl(hue = hue, saturation = 0.32f, lightness = 0.47f)
    } else {
        Color.hsl(hue = hue, saturation = 0.62f, lightness = 0.84f)
    }
)

private fun contentColorFor(fill: Color): Color =
    if (contrastRatio(fill, CONTENT_LIGHT) >= contrastRatio(fill, CONTENT_DARK)) {
        CONTENT_LIGHT
    } else {
        CONTENT_DARK
    }

private fun contrastRatio(first: Color, second: Color): Float {
    val a = first.luminance()
    val b = second.luminance()
    val lighter = maxOf(a, b)
    val darker = minOf(a, b)
    return (lighter + 0.05f) / (darker + 0.05f)
}

internal fun sliceTone(palette: WheelPalette, index: Int): SliceTone {
    val base = palette.tones[index % palette.tones.size]
    val cycle = index / palette.tones.size
    if (cycle == 0) return base
    val shifted = if (cycle % 2 == 1) {
        lerp(base.fill, Color.Black, 0.14f)
    } else {
        lerp(base.fill, Color.White, 0.16f)
    }
    return toneForFill(shifted)
}

@Composable
internal fun rememberWheelPalette(): WheelPalette {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        val isDark = scheme.surface.luminance() < 0.5f
        WheelPalette(
            tones = SLICE_HUES.map { hue -> toneFor(hue, isDark) },
            isDark = isDark,
            rimFill = scheme.surfaceContainerHigh,
            rimStroke = scheme.outlineVariant.copy(alpha = 0.55f),
            gloss = Color.White.copy(alpha = if (isDark) 0.14f else 0.5f),
            innerShadow = Color.Black.copy(alpha = if (isDark) 0.28f else 0.12f),
            divider = Color.Black.copy(alpha = if (isDark) 0.26f else 0.18f),
            hubShadow = Color.Black.copy(alpha = if (isDark) 0.42f else 0.16f),
            hubBase = scheme.surfaceContainerHighest,
            hubBaseStroke = scheme.outlineVariant.copy(alpha = 0.6f),
            pointerOutline = scheme.surface.copy(alpha = 0.55f)
        )
    }
}

private const val LIGHTNESS_STEP = 0.035f
private const val MAX_LIGHTNESS_STEPS = 14
