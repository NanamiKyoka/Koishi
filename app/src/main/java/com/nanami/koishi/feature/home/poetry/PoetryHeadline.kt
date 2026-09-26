package com.nanami.koishi.feature.home.poetry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp

private const val MAX_LINES = 2
private const val LINE_HEIGHT_RATIO = 1.28f
private const val FONT_STEP_SP = 1f
private const val MIN_FONT_SIZE_SP = 9f

private val MAX_TEXT_HEIGHT = 42.dp
private val FALLBACK_WIDTH = 180.dp

/**
 * 标题栏中的一言，字号随句子长度自适应：上限与标题一致，放不下时逐级缩小至最多两行
 */
@Composable
fun PoetryHeadline(
    sentence: Hitokoto?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (sentence == null) return

    val baseStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) {
            (maxWidth.takeIf { it.value.isFinite() } ?: FALLBACK_WIDTH).roundToPx()
        }.coerceAtLeast(1)
        val heightPx = with(density) {
            min(maxHeight, MAX_TEXT_HEIGHT).roundToPx()
        }.coerceAtLeast(1)

        val fontSize = remember(sentence.text, widthPx, heightPx, baseStyle, measurer) {
            fitFontSize(measurer, baseStyle, sentence.text, widthPx, heightPx)
        }

        Text(
            text = sentence.text,
            style = baseStyle.copy(
                fontSize = fontSize,
                lineHeight = (fontSize.value * LINE_HEIGHT_RATIO).sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

private fun fitFontSize(
    measurer: TextMeasurer,
    style: TextStyle,
    text: String,
    maxWidthPx: Int,
    maxHeightPx: Int
): TextUnit {
    var sizeSp = style.fontSize.value
    while (sizeSp > MIN_FONT_SIZE_SP) {
        val size = sizeSp.sp
        val layout = measurer.measure(
            text = AnnotatedString(text),
            style = style.copy(
                fontSize = size,
                lineHeight = (sizeSp * LINE_HEIGHT_RATIO).sp
            ),
            constraints = Constraints(maxWidth = maxWidthPx)
        )
        if (layout.lineCount <= MAX_LINES && layout.size.height <= maxHeightPx) return size
        sizeSp -= FONT_STEP_SP
    }
    return MIN_FONT_SIZE_SP.sp
}
