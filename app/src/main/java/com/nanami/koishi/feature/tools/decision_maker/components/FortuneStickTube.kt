package com.nanami.koishi.feature.tools.decision_maker.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionOption
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionPhase
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionTopic
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private val STAGE_HEIGHT = 424.dp
private val STAGE_WIDTH = 300.dp
private val TUBE_WIDTH = 132.dp
private val TUBE_BODY_HEIGHT = 160.dp
private val TUBE_CAP_HALF_HEIGHT = 9.dp
private val TUBE_PLINTH_HEIGHT = 14.dp
private val TUBE_BOTTOM_INSET = 16.dp
private const val TUBE_PLINTH_SCALE = 1.1f
private const val SLIT_WIDTH_RATIO = 0.34f

private val SLIT_HEIGHT = 11.dp
private val NORMAL_STICK_WIDTH = 11.dp
private val TARGET_STICK_WIDTH = 26.dp
private val STICK_HEIGHT = 210.dp
private val STICK_TIP_HEIGHT = 7.dp
private val STICK_SPACING = 6.1.dp
private val STICK_TIP_PEEK = 4.dp
private val MAX_LIFT = 160.dp
private val STICK_TOP_CLEARANCE = 12.dp
private const val STICK_COUNT = 7

private const val MAX_TILT_DEGREES = 15f
private const val DRAG_ENERGY_DIVISOR_DP = 900f
private const val DRAG_TICK_DISTANCE_DP = 34f
private const val MAX_PULL_DP = 26f

private val STICK_SHAPE = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)

private data class StickSlot(
    val offsetX: Dp,
    val tipPeek: Dp,
    val rotation: Float
)

@Composable
fun FortuneStickTube(
    topic: DecisionTopic,
    result: DecisionOption?,
    phase: DecisionPhase,
    energy: Float,
    hapticsEnabled: Boolean,
    onShakeProgress: (Float) -> Unit,
    onDrawFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberTubePalette()
    val density = LocalDensity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val slots = remember {
        val random = Random(STICK_COUNT * 7919)
        List(STICK_COUNT) { index ->
            StickSlot(
                offsetX = ((index - (STICK_COUNT - 1) / 2f) * STICK_SPACING.value).dp,
                tipPeek = random.nextInt(0, 7).dp,
                rotation = random.nextInt(-3, 4).toFloat()
            )
        }
    }
    val targetIndex = STICK_COUNT / 2

    val tilt = remember { Animatable(0f) }
    val pull = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    val targetWidth = remember { Animatable(NORMAL_STICK_WIDTH.value) }
    val settleSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val wobble = rememberInfiniteTransition(label = "omikujiWobble")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(820, easing = LinearEasing)),
            label = "omikujiWobblePhase"
        )

    val interactive = rememberUpdatedState(
        phase == DecisionPhase.IDLE || phase == DecisionPhase.SHAKING || phase == DecisionPhase.RESULT
    )
    val hapticsState = rememberUpdatedState(hapticsEnabled)
    val progressState = rememberUpdatedState(onShakeProgress)
    val drawn = phase == DecisionPhase.DRAWING || phase == DecisionPhase.RESULT

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val stageHeight = minOf(maxHeight, STAGE_HEIGHT)
        val stageWidth = minOf(maxWidth, STAGE_WIDTH)

        val tubeWidthPx = with(density) { TUBE_WIDTH.toPx() }
        val bodyHeightPx = with(density) { TUBE_BODY_HEIGHT.toPx() }
        val plinthHeightPx = with(density) { TUBE_PLINTH_HEIGHT.toPx() }
        val capHalfPx = with(density) { TUBE_CAP_HALF_HEIGHT.toPx() }
        val slitHeightPx = with(density) { SLIT_HEIGHT.toPx() }
        val stageWidthPx = with(density) { stageWidth.toPx() }
        val bottomAnchorPx = with(density) { (stageHeight - TUBE_BOTTOM_INSET).toPx() }
        val capFrontY = bottomAnchorPx - plinthHeightPx - bodyHeightPx

        val geometry = HexTubeGeometry(
            centerX = stageWidthPx / 2f,
            width = tubeWidthPx,
            rimCenterY = capFrontY - capHalfPx,
            capHalfHeight = capHalfPx,
            bodyHeight = bodyHeightPx,
            plinthHeight = plinthHeightPx,
            plinthScale = TUBE_PLINTH_SCALE,
            slitWidth = tubeWidthPx * SLIT_WIDTH_RATIO,
            slitHeight = slitHeightPx
        )

        val slitLeftDp = with(density) { geometry.slitLeftX.toDp() }
        val slitWidthDp = with(density) { geometry.slitWidth.toDp() }
        val slitBottomDp = with(density) { geometry.slitBottomY.toDp() }
        val slitTopDp = with(density) { geometry.slitTopY.toDp() }
        val tubeCenterDp = stageWidth / 2

        val stickTopDp = slitTopDp - STICK_TIP_PEEK
        val liftLimit = (stickTopDp - STICK_TOP_CLEARANCE).coerceIn(0.dp, MAX_LIFT)

        LaunchedEffect(phase) {
            when (phase) {
                DecisionPhase.DRAWING -> {
                    view.performConfirmHaptic(hapticsEnabled)
                    coroutineScope {
                        launch {
                            lift.animateTo(
                                targetValue = -with(density) { liftLimit.toPx() },
                                animationSpec = spring(
                                    dampingRatio = 0.62f,
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = 0.5f
                                )
                            )
                        }
                        launch {
                            targetWidth.animateTo(
                                targetValue = TARGET_STICK_WIDTH.value,
                                animationSpec = spring(
                                    dampingRatio = 0.7f,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                    onDrawFinished()
                }
                DecisionPhase.IDLE, DecisionPhase.SHAKING -> {
                    coroutineScope {
                        launch { lift.animateTo(0f, settleSpec) }
                        launch { targetWidth.animateTo(NORMAL_STICK_WIDTH.value, settleSpec) }
                    }
                }
                else -> Unit
            }
        }

        Box(
            modifier = Modifier
                .size(stageWidth, stageHeight)
                .graphicsLayer {
                    rotationZ = tilt.value + sin(wobble.value * PI.toFloat() / 180f) * energy * 1.8f
                    translationY = pull.value + cos(wobble.value * PI.toFloat() / 180f) * energy * 2.4f
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .pointerInput(topic.id) {
                    var tickAccumulator = 0f
                    val maxPullPx = MAX_PULL_DP * density.density
                    val release = {
                        scope.launch {
                            tilt.animateTo(0f, settleSpec)
                            pull.animateTo(0f, settleSpec)
                        }
                        Unit
                    }
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (interactive.value) {
                                val dxDp = dragAmount.x / density.density
                                val dyDp = dragAmount.y / density.density
                                scope.launch {
                                    tilt.snapTo(
                                        (tilt.value + dxDp * 0.9f)
                                            .coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)
                                    )
                                    pull.snapTo(
                                        (pull.value + dragAmount.y * 0.4f)
                                            .coerceIn(-maxPullPx, maxPullPx)
                                    )
                                }
                                val distanceDp = hypot(dxDp, dyDp)
                                tickAccumulator += distanceDp
                                if (tickAccumulator >= DRAG_TICK_DISTANCE_DP) {
                                    tickAccumulator %= DRAG_TICK_DISTANCE_DP
                                    view.performClockTickHaptic(hapticsState.value)
                                }
                                progressState.value(distanceDp / DRAG_ENERGY_DIVISOR_DP)
                            }
                        },
                        onDragEnd = { release() },
                        onDragCancel = { release() }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawOmikujiTube(geometry, palette)
            }

            Box(
                modifier = Modifier
                    .offset(x = slitLeftDp, y = 0.dp)
                    .size(slitWidthDp, slitBottomDp)
                    .clipToBounds()
            ) {
                slots.forEachIndexed { index, slot ->
                    val isTarget = index == targetIndex
                    val stickWidthDp = if (isTarget) targetWidth.value else NORMAL_STICK_WIDTH.value
                    val centerOffset = tubeCenterDp.value - slitLeftDp.value + slot.offsetX.value
                    val jitterAmplitudePx = with(density) { 3.2.dp.toPx() }
                    val hopAmplitudePx = with(density) { 1.8.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .offset(
                                x = (centerOffset - stickWidthDp / 2f).dp,
                                y = stickTopDp - slot.tipPeek
                            )
                            .width(stickWidthDp.dp)
                            .height(STICK_HEIGHT)
                            .graphicsLayer {
                                translationX =
                                    sin((wobble.value + index * 53f) * PI.toFloat() / 180f) *
                                        energy * jitterAmplitudePx
                                translationY =
                                    cos((wobble.value * 1.4f + index * 37f) * PI.toFloat() / 180f) *
                                        energy * hopAmplitudePx +
                                        if (isTarget) lift.value else 0f
                                rotationZ = if (isTarget) 0f else slot.rotation * energy
                                transformOrigin = TransformOrigin(0.5f, 0f)
                            }
                            .clip(STICK_SHAPE)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        palette.stickBody,
                                        palette.stickBody.copy(alpha = 0.94f)
                                    )
                                )
                            )
                            .border(width = 1.dp, color = palette.stickEdge, shape = STICK_SHAPE)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(STICK_TIP_HEIGHT)
                                    .background(palette.stickTip)
                            )
                            if (isTarget && drawn && result != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    result.text.forEach { char ->
                                        Text(
                                            text = char.toString(),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontSize = 15.sp,
                                                lineHeight = 19.sp
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = palette.stickInk
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
