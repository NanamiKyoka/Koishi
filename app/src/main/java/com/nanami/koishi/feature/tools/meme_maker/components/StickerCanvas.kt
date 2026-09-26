package com.nanami.koishi.feature.tools.meme_maker.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeTextFont
import com.nanami.koishi.feature.tools.meme_maker.engine.PlacedSticker
import com.nanami.koishi.feature.tools.meme_maker.engine.StickerKind
import com.nanami.koishi.feature.tools.meme_maker.engine.StickerTransform
import com.nanami.koishi.feature.tools.meme_maker.engine.TextStickerSpec
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 贴纸编辑画布。
 *
 * 单指拖动、双指缩放旋转，点击贴纸选中、点击空白处预览成图。
 * 手势过程中的变换值只在画布内部流转，抬手后才回写，避免拖动时整页重组。
 */
@Composable
fun StickerCanvas(
    background: Bitmap?,
    stickers: List<PlacedSticker>,
    stickerBitmaps: Map<String, Bitmap>,
    canvasWidth: Int,
    canvasHeight: Int,
    selectedStickerId: String?,
    onSelectSticker: (String?) -> Unit,
    onTransformSticker: (String, StickerTransform) -> Unit,
    onRequestPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()

        val availableWidth = constraints.maxWidth.toFloat()
        val availableHeight = constraints.maxHeight.toFloat()
        val canvasW = canvasWidth.coerceAtLeast(1).toFloat()
        val canvasH = canvasHeight.coerceAtLeast(1).toFloat()
        val widthScale = if (availableWidth > 0f) availableWidth / canvasW else Float.MAX_VALUE
        val heightScale = if (availableHeight > 0f) availableHeight / canvasH else Float.MAX_VALUE
        // 等比缩放到容器内并保持不放大，画布一旦溢出容器，贴纸的命中区域就会和绘制位置错位
        val fitScale = min(widthScale, heightScale)
            .takeIf { it.isFinite() && it > 0f }
            ?.coerceAtMost(1f)
            ?: 0f
        val drawWidth = canvasW * fitScale
        val drawHeight = canvasH * fitScale
        val shortSide = min(drawWidth, drawHeight)

        val currentStickers by rememberUpdatedState(stickers)
        val bitmaps by rememberUpdatedState(stickerBitmaps)
        val selectedId by rememberUpdatedState(selectedStickerId)
        val transformCallback by rememberUpdatedState(onTransformSticker)
        val selectCallback by rememberUpdatedState(onSelectSticker)
        val previewCallback by rememberUpdatedState(onRequestPreview)

        var gestureOverride by remember { mutableStateOf<GestureOverride?>(null) }

        LaunchedEffect(stickers, gestureOverride) {
            val pending = gestureOverride ?: return@LaunchedEffect
            val committed = stickers.find { it.id == pending.stickerId }?.transform
            if (committed == pending.transform) {
                gestureOverride = null
            }
        }

        fun effectiveTransform(sticker: PlacedSticker): StickerTransform =
            gestureOverride?.takeIf { it.stickerId == sticker.id }?.transform ?: sticker.transform

        fun stickerSize(sticker: PlacedSticker, transform: StickerTransform): Pair<Float, Float> =
            when (sticker.kind) {
                StickerKind.IMAGE -> {
                    val bitmap = sticker.source?.let { bitmaps[it.key] }
                    if (bitmap == null || bitmap.width <= 0 || bitmap.isRecycled) {
                        0f to 0f
                    } else {
                        val width = max(1f, transform.scale * shortSide)
                        width to (width * bitmap.height / bitmap.width)
                    }
                }

                StickerKind.TEXT -> {
                    val spec = sticker.text
                    val textSizePx = max(8f, transform.scale * shortSide)
                    if (spec == null || spec.text.isBlank()) {
                        // 文字被清空时仍保留占位尺寸，否则贴纸既画不出来也点不中，会变成删不掉的幽灵图层
                        textSizePx * EMPTY_TEXT_WIDTH_RATIO to textSizePx * EMPTY_TEXT_HEIGHT_RATIO
                    } else {
                        val layout = textMeasurer.measure(
                            text = AnnotatedString(spec.text),
                            style = previewTextStyle(spec, textSizePx, density),
                            constraints = Constraints()
                        )
                        layout.size.width.toFloat() to layout.size.height.toFloat()
                    }
                }
            }

        fun hitTest(position: Offset): String? {
            for (sticker in currentStickers.asReversed()) {
                val transform = effectiveTransform(sticker)
                val (width, height) = stickerSize(sticker, transform)
                if (width <= 0f || height <= 0f) continue

                val dx = position.x - transform.centerX * drawWidth
                val dy = position.y - transform.centerY * drawHeight
                val radians = Math.toRadians(-transform.rotation.toDouble())
                val localX = (dx * cos(radians) - dy * sin(radians)).toFloat()
                val localY = (dx * sin(radians) + dy * cos(radians)).toFloat()

                if (abs(localX) <= width / 2f + HIT_PADDING_PX && abs(localY) <= height / 2f + HIT_PADDING_PX) {
                    return sticker.id
                }
            }
            return null
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(
                    width = with(density) { drawWidth.toDp() },
                    height = with(density) { drawHeight.toDp() }
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .pointerInput(drawWidth, drawHeight, shortSide) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val hitId = hitTest(down.position)
                        val target = hitId?.let { id -> currentStickers.find { it.id == id } }

                        if (target == null) {
                            gestureOverride = null
                        }

                        val start = target?.let { sticker ->
                            gestureOverride?.takeIf { it.stickerId == sticker.id }?.transform ?: sticker.transform
                        }

                        var panTotal = Offset.Zero
                        var zoomTotal = 1f
                        var rotationTotal = 0f
                        var transformed = false

                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.isConsumed }) break

                            val pan = event.calculatePan()
                            val zoom = event.calculateZoom()
                            val rotation = event.calculateRotation()

                            if (start != null && (pan != Offset.Zero || zoom != 1f || rotation != 0f)) {
                                transformed = true
                            }

                            panTotal += pan
                            zoomTotal *= zoom
                            rotationTotal += rotation

                            if (transformed && target != null && start != null) {
                                gestureOverride = GestureOverride(
                                    stickerId = target.id,
                                    transform = start.copy(
                                        centerX = (start.centerX + panTotal.x / drawWidth)
                                            .coerceIn(MIN_CENTER_RATIO, MAX_CENTER_RATIO),
                                        centerY = (start.centerY + panTotal.y / drawHeight)
                                            .coerceIn(MIN_CENTER_RATIO, MAX_CENTER_RATIO),
                                        scale = (start.scale * zoomTotal)
                                            .coerceIn(MIN_STICKER_SCALE, MAX_STICKER_SCALE),
                                        rotation = start.rotation + rotationTotal
                                    )
                                )
                            }

                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                            if (event.changes.none { it.pressed }) break
                        }

                        when {
                            target != null && transformed -> {
                                val finalTransform = gestureOverride
                                    ?.takeIf { it.stickerId == target.id }
                                    ?.transform
                                if (finalTransform != null) {
                                    transformCallback(target.id, finalTransform)
                                }
                                selectCallback(target.id)
                            }

                            target != null -> selectCallback(target.id)

                            transformed -> selectCallback(null)

                            else -> previewCallback()
                        }
                    }
                }
        ) {
            background?.let { bitmap ->
                if (!bitmap.isRecycled) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            stickers.forEach { sticker ->
                val transform = effectiveTransform(sticker)
                val (width, height) = stickerSize(sticker, transform)
                if (width <= 0f || height <= 0f) return@forEach

                StickerItem(
                    sticker = sticker,
                    transform = transform,
                    bitmaps = bitmaps,
                    width = width,
                    height = height,
                    left = transform.centerX * drawWidth - width / 2f,
                    top = transform.centerY * drawHeight - height / 2f,
                    textSizePx = max(8f, transform.scale * shortSide),
                    density = density,
                    isSelected = sticker.id == selectedStickerId
                )
            }
        }
    }
}

@Composable
private fun StickerItem(
    sticker: PlacedSticker,
    transform: StickerTransform,
    bitmaps: Map<String, Bitmap>,
    width: Float,
    height: Float,
    left: Float,
    top: Float,
    textSizePx: Float,
    density: Density,
    isSelected: Boolean
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .graphicsLayer {
                rotationZ = transform.rotation
                alpha = transform.alpha
                scaleX = if (transform.flipHorizontal) -1f else 1f
            }
            .size(
                width = with(density) { width.toDp() },
                height = with(density) { height.toDp() }
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (sticker.kind) {
            StickerKind.IMAGE -> {
                val bitmap = sticker.source?.let { bitmaps[it.key] }
                if (bitmap != null && !bitmap.isRecycled) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            StickerKind.TEXT -> {
                val spec = sticker.text
                if (spec == null || spec.text.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.meme_text_placeholder),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                } else {
                    val baseStyle = previewTextStyle(spec, textSizePx, density)
                    if (spec.strokeEnabled) {
                        Text(
                            text = spec.text,
                            style = baseStyle.copy(
                                color = Color(spec.strokeColor),
                                drawStyle = Stroke(
                                    width = textSizePx * TEXT_STROKE_RATIO,
                                    join = StrokeJoin.Round,
                                    cap = StrokeCap.Round
                                )
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = spec.text,
                        style = baseStyle,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun previewTextStyle(spec: TextStickerSpec, textSizePx: Float, density: Density): TextStyle =
    TextStyle(
        color = Color(spec.color),
        fontSize = with(density) { textSizePx.toSp() },
        fontFamily = spec.font.fontFamily,
        fontWeight = spec.font.fontWeight,
        lineHeight = with(density) { (textSizePx * TEXT_LINE_SPACING).toSp() }
    )

private val MemeTextFont.fontFamily: FontFamily
    get() = when (this) {
        MemeTextFont.DEFAULT, MemeTextFont.BOLD -> FontFamily.Default
        MemeTextFont.SERIF -> FontFamily.Serif
        MemeTextFont.MONOSPACE -> FontFamily.Monospace
    }

private val MemeTextFont.fontWeight: FontWeight
    get() = if (this == MemeTextFont.BOLD) FontWeight.Bold else FontWeight.Normal

private data class GestureOverride(
    val stickerId: String,
    val transform: StickerTransform
)

private const val TEXT_STROKE_RATIO = 0.12f
private const val TEXT_LINE_SPACING = 1.15f
private const val EMPTY_TEXT_WIDTH_RATIO = 2.4f
private const val EMPTY_TEXT_HEIGHT_RATIO = 1.6f
private const val HIT_PADDING_PX = 16f
private const val MIN_CENTER_RATIO = -0.4f
private const val MAX_CENTER_RATIO = 1.4f
private const val MIN_STICKER_SCALE = 0.03f
private const val MAX_STICKER_SCALE = 3f
