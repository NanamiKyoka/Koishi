package com.nanami.koishi.feature.tools.decision_maker.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionOption
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionTopic
import com.nanami.koishi.feature.tools.decision_maker.engine.WeightedPicker
import com.nanami.koishi.feature.tools.decision_maker.engine.playableOptions
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val POINTER_ANGLE = -90f
private const val FULL_TURNS = 5
private const val SLICE_GAP_DEGREES = 0.6f
private const val MIN_LABEL_SWEEP_DEGREES = 8f

private val WHEEL_MAX_SIZE = 340.dp
private val POINTER_OVERFLOW = 26.dp
private val RIM_INSET = 7.dp
private val TEXT_SAFE_PADDING = 18.dp
private val HUB_SIZE = 92.dp
private val HUB_DISC_SIZE = 78.dp
private val HUB_RING_SIZE = 84.dp

private data class WheelSlice(
    val option: DecisionOption,
    val startAngle: Float,
    val sweep: Float,
    val tone: SliceTone
)

@Composable
fun DecisionWheel(
    topic: DecisionTopic,
    result: DecisionOption?,
    highlightResult: Boolean,
    animationToken: Long,
    enabled: Boolean,
    hapticsEnabled: Boolean,
    spinDurationMillis: Int,
    onSpin: () -> Unit,
    onSpinFinished: () -> Unit,
    onSliceChange: (DecisionOption?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val palette = rememberWheelPalette()

    val slices = remember(topic, palette) {
        val pool = topic.playableOptions
        val sweeps = WeightedPicker.sweepAngles(pool)
        var cursor = POINTER_ANGLE
        pool.mapIndexed { index, option ->
            val sweep = sweeps[index]
            WheelSlice(option, cursor, sweep, sliceTone(palette, index)).also { cursor += sweep }
        }
    }

    val rotation = remember { Animatable(0f) }
    val view = LocalView.current
    val textMeasurer = rememberTextMeasurer()

    // 记住指针当前指向的扇区，避免每帧重复回调
    val lastSliceIndex = remember { mutableIntStateOf(-1) }

    // 未转动时（初始 / 首次结果落定）也要让外部拿到指针指向项
    LaunchedEffect(slices, rotation.value, animationToken) {
        if (rotation.isRunning) return@LaunchedEffect
        val index = sliceIndexAt(slices, rotation.value)
        if (index != lastSliceIndex.intValue) {
            lastSliceIndex.intValue = index
            onSliceChange(slices.getOrNull(index)?.option)
        }
    }

    LaunchedEffect(animationToken) {
        if (animationToken <= 0L) return@LaunchedEffect
        val index = slices.indexOfFirst { it.option.id == result?.id }
        if (index < 0) return@LaunchedEffect

        val slice = slices[index]
        val target = nextTarget(rotation.value, POINTER_ANGLE - (slice.startAngle + slice.sweep / 2f))
        var lastTick = -1

        rotation.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = spinDurationMillis,
                easing = CubicBezierEasing(0.17f, 0.85f, 0.06f, 1f)
            )
        ) {
            val current = sliceIndexAt(slices, rotation.value)
            if (current != lastTick) {
                lastTick = current
                view.performClockTickHaptic(hapticsEnabled)
                if (current != lastSliceIndex.intValue) {
                    lastSliceIndex.intValue = current
                    onSliceChange(slices.getOrNull(current)?.option)
                }
            }
        }
        onSpinFinished()
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val wheelSize = minOf(
            maxWidth,
            (maxHeight - POINTER_OVERFLOW).coerceAtLeast(140.dp),
            WHEEL_MAX_SIZE
        )
        val canvasHeight = wheelSize + POINTER_OVERFLOW
        val radiusPx = with(density) { (wheelSize / 2).toPx() }
        val rimInsetPx = with(density) { RIM_INSET.toPx() }
        val textSafeOuterPx = radiusPx - rimInsetPx - with(density) { TEXT_SAFE_PADDING.toPx() }
        val hubRadiusPx = with(density) { (HUB_SIZE / 2).toPx() }
        val minLabelWidthPx = with(density) { 26.dp.toPx() }
        val maxLabelWidthPx =
            (textSafeOuterPx - hubRadiusPx - with(density) { 8.dp.toPx() })
                .coerceAtLeast(minLabelWidthPx)
                .toInt()

        val labels = remember(slices, maxLabelWidthPx, textMeasurer) {
            slices.map { slice ->
                textMeasurer.measure(
                    text = AnnotatedString(slice.option.text),
                    style = TextStyle(
                        fontSize = labelFontSize(slice.sweep, slice.option.text.length),
                        fontWeight = FontWeight.SemiBold,
                        color = slice.tone.content
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    constraints = Constraints(maxWidth = maxLabelWidthPx)
                )
            }
        }

        Canvas(
            modifier = Modifier
                .width(wheelSize)
                .height(canvasHeight)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            val sliceRadius = radius - rimInsetPx
            val arcTopLeft = Offset(center.x - sliceRadius, center.y - sliceRadius)
            val arcSize = Size(sliceRadius * 2f, sliceRadius * 2f)
            val strokeTopLeft = Offset(
                center.x - radius + 1.5.dp.toPx(),
                center.y - radius + 1.5.dp.toPx()
            )
            val strokeSize = Size(radius * 2f - 3.dp.toPx(), radius * 2f - 3.dp.toPx())

            drawCircle(color = palette.rimFill, radius = radius, center = center)

            rotate(degrees = rotation.value, pivot = center) {
                slices.forEach { slice ->
                    drawArc(
                        color = slice.tone.fill,
                        startAngle = slice.startAngle + SLICE_GAP_DEGREES / 2f,
                        sweepAngle = (slice.sweep - SLICE_GAP_DEGREES).coerceAtLeast(0.4f),
                        useCenter = true,
                        topLeft = arcTopLeft,
                        size = arcSize
                    )
                }

                slices.forEach { slice ->
                    val radians = slice.startAngle * PI.toFloat() / 180f
                    drawLine(
                        color = palette.divider,
                        start = center,
                        end = Offset(
                            center.x + cos(radians) * sliceRadius,
                            center.y + sin(radians) * sliceRadius
                        ),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }

                if (highlightResult) {
                    val index = slices.indexOfFirst { it.option.id == result?.id }
                    if (index >= 0) {
                        val slice = slices[index]
                        drawArc(
                            color = scheme.onSurface.copy(alpha = 0.14f),
                            startAngle = slice.startAngle + SLICE_GAP_DEGREES / 2f,
                            sweepAngle = (slice.sweep - SLICE_GAP_DEGREES).coerceAtLeast(0.4f),
                            useCenter = true,
                            topLeft = arcTopLeft,
                            size = arcSize
                        )
                        val inset = 3.dp.toPx()
                        drawArc(
                            color = scheme.onSurface,
                            startAngle = slice.startAngle + SLICE_GAP_DEGREES / 2f,
                            sweepAngle = (slice.sweep - SLICE_GAP_DEGREES).coerceAtLeast(0.4f),
                            useCenter = false,
                            topLeft = Offset(arcTopLeft.x + inset, arcTopLeft.y + inset),
                            size = Size(arcSize.width - inset * 2f, arcSize.height - inset * 2f),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }
                }

                slices.forEachIndexed { index, slice ->
                    if (slice.sweep < MIN_LABEL_SWEEP_DEGREES) return@forEachIndexed
                    val layout = labels[index]
                    val mid = slice.startAngle + slice.sweep / 2f
                    val flipped = mid > 90f && mid < 270f
                    rotate(degrees = if (flipped) mid + 180f else mid, pivot = center) {
                        val left = if (flipped) {
                            center.x - textSafeOuterPx
                        } else {
                            center.x + textSafeOuterPx - layout.size.width
                        }
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(left, center.y - layout.size.height / 2f)
                        )
                    }
                }
            }

            drawCircle(
                color = palette.rimStroke,
                radius = radius - 0.6.dp.toPx(),
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.gloss, Color.Transparent),
                    startY = center.y - radius,
                    endY = center.y
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = strokeTopLeft,
                size = strokeSize,
                style = Stroke(width = 2.5.dp.toPx())
            )
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, palette.innerShadow),
                    startY = center.y,
                    endY = center.y + radius
                ),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = strokeTopLeft,
                size = strokeSize,
                style = Stroke(width = 2.dp.toPx())
            )

            val pointerHalfWidth = 13.dp.toPx()
            val pointerHeight = 24.dp.toPx()
            val pointerTopY = center.y - radius - 8.dp.toPx()
            val pointerCorner = 5.dp.toPx()
            val pointerLeft = Offset(center.x - pointerHalfWidth, pointerTopY)
            val pointerRight = Offset(center.x + pointerHalfWidth, pointerTopY)
            val pointerTip = Offset(center.x, pointerTopY + pointerHeight)
            val shadowOffset = Offset(0f, 2.5.dp.toPx())

            drawPath(
                path = roundedTrianglePath(
                    pointerLeft + shadowOffset,
                    pointerRight + shadowOffset,
                    pointerTip + shadowOffset,
                    pointerCorner
                ),
                color = palette.hubShadow
            )
            val pointerPath = roundedTrianglePath(
                pointerLeft,
                pointerRight,
                pointerTip,
                pointerCorner
            )
            drawPath(
                path = pointerPath,
                brush = Brush.verticalGradient(
                    colors = listOf(scheme.primary, scheme.primary.copy(alpha = 0.82f)),
                    startY = pointerTopY,
                    endY = pointerTip.y
                )
            )
            drawPath(
                path = pointerPath,
                color = palette.pointerOutline,
                style = Stroke(width = 1.dp.toPx())
            )
            drawLine(
                color = Color.White.copy(alpha = 0.26f),
                start = Offset(center.x - pointerHalfWidth * 0.45f, pointerTopY + 5.dp.toPx()),
                end = Offset(center.x + pointerHalfWidth * 0.45f, pointerTopY + 5.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        WheelHub(
            enabled = enabled,
            palette = palette,
            onSpin = onSpin
        )
    }
}

@Composable
private fun WheelHub(enabled: Boolean, palette: WheelPalette, onSpin: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val discBrush = if (enabled) {
        Brush.verticalGradient(
            listOf(scheme.primary, lerp(scheme.primary, scheme.primaryContainer, 0.35f))
        )
    } else {
        Brush.verticalGradient(listOf(palette.hubBase, palette.hubBase))
    }
    val contentColor = if (enabled) scheme.onPrimary else scheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(HUB_SIZE)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onSpin),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(HUB_RING_SIZE)
                .offset(y = 3.dp)
                .clip(CircleShape)
                .background(palette.hubShadow)
        )
        Box(
            modifier = Modifier
                .size(HUB_RING_SIZE)
                .clip(CircleShape)
                .background(palette.hubBase)
                .border(width = 1.dp, color = palette.hubBaseStroke, shape = CircleShape)
        )
        Box(
            modifier = Modifier
                .size(HUB_DISC_SIZE)
                .clip(CircleShape)
                .background(discBrush)
        )
        Box(
            modifier = Modifier
                .size(HUB_DISC_SIZE - 16.dp)
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    color = if (enabled) {
                        scheme.onPrimary.copy(alpha = 0.18f)
                    } else {
                        palette.hubBaseStroke
                    },
                    shape = CircleShape
                )
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Casino,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(R.string.decision_spin),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

private fun labelFontSize(sweep: Float, charCount: Int): TextUnit = when {
    sweep < 12f -> 9.sp
    sweep < 18f -> 10.sp
    charCount > 6 -> 10.5.sp
    charCount > 4 -> 11.5.sp
    else -> 12.5.sp
}

private fun roundedTrianglePath(a: Offset, b: Offset, c: Offset, radius: Float): Path {
    val corners = listOf(a, b, c)
    val path = Path()
    corners.forEachIndexed { index, corner ->
        val next = corners[(index + 1) % corners.size]
        val previous = corners[(index + 2) % corners.size]
        val entry = corner + unitVector(next - corner) * radius
        val exit = corner + unitVector(previous - corner) * radius
        if (index == 0) {
            path.moveTo(entry.x, entry.y)
        } else {
            path.lineTo(entry.x, entry.y)
        }
        path.quadraticTo(corner.x, corner.y, exit.x, exit.y)
    }
    path.close()
    return path
}

private fun unitVector(vector: Offset): Offset {
    val distance = vector.getDistance()
    return if (distance == 0f) Offset.Zero else vector / distance
}

private fun normalizeDegrees(value: Float): Float {
    val mod = value % 360f
    return if (mod < 0f) mod + 360f else mod
}

private fun nextTarget(currentRotation: Float, desiredAngle: Float): Float {
    var delta = normalizeDegrees(desiredAngle) - normalizeDegrees(currentRotation)
    if (delta <= 0f) delta += 360f
    return currentRotation + delta + 360f * FULL_TURNS
}

private fun sliceIndexAt(slices: List<WheelSlice>, rotation: Float): Int {
    val local = normalizeDegrees(POINTER_ANGLE - rotation)
    slices.forEachIndexed { index, slice ->
        if (normalizeDegrees(local - slice.startAngle) < slice.sweep) return index
    }
    return -1
}
