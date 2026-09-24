package com.nanami.koishi.core.image.crop

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Rotate90DegreesCw
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

enum class CropToolTab(@StringRes val titleRes: Int, val icon: ImageVector) {
    ROTATE(R.string.crop_tab_rotate, Icons.AutoMirrored.Rounded.RotateRight),
    SCALE(R.string.crop_tab_scale, Icons.Rounded.Crop),
    BRIGHTNESS(R.string.crop_tab_brightness, Icons.Rounded.WbSunny),
    CONTRAST(R.string.crop_tab_contrast, Icons.Rounded.Contrast),
    SATURATION(R.string.crop_tab_saturation, Icons.Rounded.ColorLens)
}

private enum class CropDragHandle {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, TOP, RIGHT, BOTTOM, INSIDE
}

private fun detectCropHandle(touch: Offset, rect: RectF, touchRadius: Float): CropDragHandle {
    val tx = touch.x
    val ty = touch.y
    val r = touchRadius

    // 优先匹配 4 个角落
    val cornerRadius = r * 1.25f
    if (hypot(tx - rect.left, ty - rect.top) <= cornerRadius) return CropDragHandle.TOP_LEFT
    if (hypot(tx - rect.right, ty - rect.top) <= cornerRadius) return CropDragHandle.TOP_RIGHT
    if (hypot(tx - rect.left, ty - rect.bottom) <= cornerRadius) return CropDragHandle.BOTTOM_LEFT
    if (hypot(tx - rect.right, ty - rect.bottom) <= cornerRadius) return CropDragHandle.BOTTOM_RIGHT

    // 匹配 4 条边缘
    if (kotlin.math.abs(tx - rect.left) <= r && ty in (rect.top - r)..(rect.bottom + r)) return CropDragHandle.LEFT
    if (kotlin.math.abs(tx - rect.right) <= r && ty in (rect.top - r)..(rect.bottom + r)) return CropDragHandle.RIGHT
    if (kotlin.math.abs(ty - rect.top) <= r && tx in (rect.left - r)..(rect.right + r)) return CropDragHandle.TOP
    if (kotlin.math.abs(ty - rect.bottom) <= r && tx in (rect.left - r)..(rect.right + r)) return CropDragHandle.BOTTOM

    // 内部拖动移动整个框
    if (rect.contains(tx, ty)) return CropDragHandle.INSIDE

    return CropDragHandle.NONE
}

/**
 * 专业 MD3 图片裁剪与调整界面 (支持 1:1 正方形裁剪与全图自适应自由拉伸裁剪)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropScreen(
    sourceBitmap: Bitmap?,
    isSquare: Boolean,
    onCancel: () -> Unit,
    onConfirm: (
        cropRectOnScreen: RectF,
        viewportWidth: Float,
        viewportHeight: Float,
        scale: Float,
        rotationDegrees: Float,
        panX: Float,
        panY: Float,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        baseScale: Float
    ) -> Unit
) {
    if (sourceBitmap == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val marginPx = with(density) { 28.dp.toPx() }
    val touchRadius = with(density) { 30.dp.toPx() }
    val minSize = with(density) { 48.dp.toPx() }
    val boundPadding = with(density) { 16.dp.toPx() }
    val primaryColor = MaterialTheme.colorScheme.primary
    var activeTab by remember { mutableStateOf(CropToolTab.ROTATE) }

    // 持久化存储记忆的“限定在图片范围内”设置
    var constrainToImage by remember { mutableStateOf(CropPreferences.isConstrainToImage(context)) }

    // 变换参数状态
    var baseRotateSteps by remember { mutableFloatStateOf(0f) } // 0, 90, 180, 270
    var fineAngle by remember { mutableFloatStateOf(0f) }       // -45° .. +45°
    val totalRotation = (baseRotateSteps + fineAngle)

    var scaleFactor by remember { mutableFloatStateOf(1f) }     // 1.0f .. 3.0f
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // 图像色彩调节参数 (-100 .. +100)
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }

    // 视口与裁剪框几何参数记录
    var viewportSizePx by remember { mutableStateOf(Size.Zero) }
    var cropRectPx by remember { mutableStateOf(RectF()) }
    var freeCropRect by remember { mutableStateOf<RectF?>(null) }
    var currentBaseScale by remember { mutableFloatStateOf(1f) }

    val colorFilter = remember(brightness, contrast, saturation) {
        ImageAdjustmentEngine.createComposeColorFilter(brightness, contrast, saturation)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. 顶部操作栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.dialog_cancel),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = stringResource(R.string.crop_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                androidx.compose.material3.FilledIconButton(
                    onClick = {
                        onConfirm(
                            cropRectPx,
                            viewportSizePx.width,
                            viewportSizePx.height,
                            scaleFactor,
                            totalRotation,
                            panOffset.x,
                            panOffset.y,
                            brightness,
                            contrast,
                            saturation,
                            currentBaseScale
                        )
                    },
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.crop_confirm)
                    )
                }
            }
        }

        // 1.5 范围约束开关栏 (持久化记忆)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FitScreen,
                        contentDescription = null,
                        tint = if (constrainToImage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.crop_constrain_to_image),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                if (constrainToImage) R.string.crop_constrain_desc_on else R.string.crop_constrain_desc_off
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = constrainToImage,
                    onCheckedChange = { checked ->
                        constrainToImage = checked
                        CropPreferences.setConstrainToImage(context, checked)
                        if (checked && cropRectPx.width() > 0f) {
                            val (clampedPan, clampedScale) = ImageAdjustmentEngine.clampPanAndScale(
                                pan = panOffset,
                                scale = scaleFactor,
                                rotationDegrees = totalRotation,
                                sourceWidth = sourceBitmap.width.toFloat(),
                                sourceHeight = sourceBitmap.height.toFloat(),
                                cropBoxWidth = cropRectPx.width(),
                                cropBoxHeight = cropRectPx.height(),
                                constrainToImage = true
                            )
                            panOffset = clampedPan
                            scaleFactor = clampedScale
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // 2. 核心九宫格裁剪视口
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val viewWidth = constraints.maxWidth.toFloat()
            val viewHeight = constraints.maxHeight.toFloat()
            if (viewWidth <= 0f || viewHeight <= 0f) return@BoxWithConstraints

            viewportSizePx = Size(viewWidth, viewHeight)

            val srcW = sourceBitmap.width.toFloat()
            val srcH = sourceBitmap.height.toFloat()

            // 考虑 90°/270° 旋转对视口适配的影响
            val isRotatedSideways = ((baseRotateSteps / 90f).toInt() % 2 != 0)
            val effectiveSrcW = if (isRotatedSideways) srcH else srcW
            val effectiveSrcH = if (isRotatedSideways) srcW else srcH

            val initialCropRect = remember(viewWidth, viewHeight, effectiveSrcW, effectiveSrcH, isSquare) {
                if (isSquare) {
                    val squareSide = min(viewWidth, viewHeight) * 0.82f
                    val squareLeft = (viewWidth - squareSide) / 2f
                    val squareTop = (viewHeight - squareSide) / 2f
                    RectF(squareLeft, squareTop, squareLeft + squareSide, squareTop + squareSide)
                } else {
                    val availWidth = (viewWidth - marginPx * 2).coerceAtLeast(1f)
                    val availHeight = (viewHeight - marginPx * 2).coerceAtLeast(1f)
                    val fitScale = minOf(availWidth / effectiveSrcW, availHeight / effectiveSrcH)
                    val rectWidth = effectiveSrcW * fitScale
                    val rectHeight = effectiveSrcH * fitScale
                    val left = (viewWidth - rectWidth) / 2f
                    val top = (viewHeight - rectHeight) / 2f
                    RectF(left, top, left + rectWidth, top + rectHeight)
                }
            }

            val calculatedBaseScale = if (isSquare) {
                ImageAdjustmentEngine.calculateBaseScale(
                    sourceWidth = srcW,
                    sourceHeight = srcH,
                    cropBoxWidth = initialCropRect.width(),
                    cropBoxHeight = initialCropRect.height()
                )
            } else {
                val availWidth = (viewWidth - marginPx * 2).coerceAtLeast(1f)
                val availHeight = (viewHeight - marginPx * 2).coerceAtLeast(1f)
                minOf(availWidth / effectiveSrcW, availHeight / effectiveSrcH)
            }
            currentBaseScale = calculatedBaseScale

            val activeCropRect = if (isSquare) {
                initialCropRect
            } else {
                freeCropRect ?: initialCropRect
            }
            cropRectPx = activeCropRect

            val cLeft = activeCropRect.left
            val cTop = activeCropRect.top
            val cRight = activeCropRect.right
            val cBottom = activeCropRect.bottom
            val cWidth = activeCropRect.width()
            val cHeight = activeCropRect.height()

            // 绘制底图 (带 GPU 旋转、缩放、平移与色彩滤镜)
            val imageBitmap = remember(sourceBitmap) { sourceBitmap.asImageBitmap() }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = totalRotation
                        scaleX = scaleFactor
                        scaleY = scaleFactor
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
            ) {
                val drawW = srcW * calculatedBaseScale
                val drawH = srcH * calculatedBaseScale
                val topLeft = Offset((viewWidth - drawW) / 2f, (viewHeight - drawH) / 2f)

                drawImage(
                    image = imageBitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(drawW.roundToInt(), drawH.roundToInt()),
                    colorFilter = colorFilter
                )
            }

            // 绘制视口半透明遮罩与九宫格高亮框、拉伸操作手柄
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scrimColor = Color.Black.copy(alpha = 0.62f)
                // 顶部遮罩
                drawRect(scrimColor, Offset.Zero, Size(viewWidth, cTop))
                // 底部遮罩
                drawRect(scrimColor, Offset(0f, cBottom), Size(viewWidth, viewHeight - cBottom))
                // 左侧遮罩
                drawRect(scrimColor, Offset(0f, cTop), Size(cLeft, cHeight))
                // 右侧遮罩
                drawRect(scrimColor, Offset(cRight, cTop), Size(viewWidth - cRight, cHeight))

                // 裁剪框白色外框 (2.dp)
                drawRect(
                    color = Color.White,
                    topLeft = Offset(cLeft, cTop),
                    size = Size(cWidth, cHeight),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 3x3 经典九宫格网格细线
                val thirdW = cWidth / 3f
                val thirdH = cHeight / 3f
                val gridColor = Color.White.copy(alpha = 0.38f)
                val gridWidth = 1.dp.toPx()

                // 水平内网格
                drawLine(gridColor, Offset(cLeft, cTop + thirdH), Offset(cRight, cTop + thirdH), gridWidth)
                drawLine(gridColor, Offset(cLeft, cTop + thirdH * 2), Offset(cRight, cTop + thirdH * 2), gridWidth)
                // 垂直内网格
                drawLine(gridColor, Offset(cLeft + thirdW, cTop), Offset(cLeft + thirdW, cBottom), gridWidth)
                drawLine(gridColor, Offset(cLeft + thirdW * 2, cTop), Offset(cLeft + thirdW * 2, cBottom), gridWidth)

                // 4 个角落主题色强调角标
                val cornerLen = 22.dp.toPx()
                val cornerStroke = 4.dp.toPx()
                val handleColor = primaryColor

                // 左上角
                drawLine(handleColor, Offset(cLeft, cTop), Offset(cLeft + cornerLen, cTop), cornerStroke)
                drawLine(handleColor, Offset(cLeft, cTop), Offset(cLeft, cTop + cornerLen), cornerStroke)
                // 右上角
                drawLine(handleColor, Offset(cRight, cTop), Offset(cRight - cornerLen, cTop), cornerStroke)
                drawLine(handleColor, Offset(cRight, cTop), Offset(cRight, cTop + cornerLen), cornerStroke)
                // 左下角
                drawLine(handleColor, Offset(cLeft, cBottom), Offset(cLeft + cornerLen, cBottom), cornerStroke)
                drawLine(handleColor, Offset(cLeft, cBottom), Offset(cLeft, cBottom - cornerLen), cornerStroke)
                // 右下角
                drawLine(handleColor, Offset(cRight, cBottom), Offset(cRight - cornerLen, cBottom), cornerStroke)
                drawLine(handleColor, Offset(cRight, cBottom), Offset(cRight, cBottom - cornerLen), cornerStroke)

                // 非方形模式下，在四条边中点绘制拉伸指示条
                if (!isSquare) {
                    val edgeHandleLen = 28.dp.toPx()
                    val edgeHandleStroke = 4.5.dp.toPx()
                    // 顶部中点
                    drawLine(handleColor, Offset(cLeft + cWidth / 2f - edgeHandleLen / 2f, cTop), Offset(cLeft + cWidth / 2f + edgeHandleLen / 2f, cTop), edgeHandleStroke)
                    // 底部中点
                    drawLine(handleColor, Offset(cLeft + cWidth / 2f - edgeHandleLen / 2f, cBottom), Offset(cLeft + cWidth / 2f + edgeHandleLen / 2f, cBottom), edgeHandleStroke)
                    // 左侧中点
                    drawLine(handleColor, Offset(cLeft, cTop + cHeight / 2f - edgeHandleLen / 2f), Offset(cLeft, cTop + cHeight / 2f + edgeHandleLen / 2f), edgeHandleStroke)
                    // 右侧中点
                    drawLine(handleColor, Offset(cRight, cTop + cHeight / 2f - edgeHandleLen / 2f), Offset(cRight, cTop + cHeight / 2f + edgeHandleLen / 2f), edgeHandleStroke)
                }
            }

            var currentHandle by remember { mutableStateOf(CropDragHandle.NONE) }

            // 触摸手势交互层：支持手柄拉伸、框体整体移动与底图变换手势
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isSquare, viewWidth, viewHeight, constrainToImage, initialCropRect) {
                        if (isSquare) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val (clampedPan, clampedScale) = ImageAdjustmentEngine.clampPanAndScale(
                                    pan = panOffset + pan,
                                    scale = scaleFactor * zoom,
                                    rotationDegrees = totalRotation,
                                    sourceWidth = sourceBitmap.width.toFloat(),
                                    sourceHeight = sourceBitmap.height.toFloat(),
                                    cropBoxWidth = initialCropRect.width(),
                                    cropBoxHeight = initialCropRect.height(),
                                    constrainToImage = constrainToImage
                                )
                                scaleFactor = clampedScale
                                panOffset = clampedPan
                            }
                        } else {
                            detectDragGestures(
                                onDragStart = { touchOffset ->
                                    val currentRect = freeCropRect ?: initialCropRect
                                    currentHandle = detectCropHandle(touchOffset, currentRect, touchRadius)
                                },
                                onDrag = { change, dragAmount ->
                                    if (currentHandle != CropDragHandle.NONE) {
                                        change.consume()
                                        val currentRect = freeCropRect ?: initialCropRect
                                        val newRect = RectF(currentRect)
                                        val bLeft = if (constrainToImage) initialCropRect.left else boundPadding
                                        val bTop = if (constrainToImage) initialCropRect.top else boundPadding
                                        val bRight = if (constrainToImage) initialCropRect.right else viewWidth - boundPadding
                                        val bBottom = if (constrainToImage) initialCropRect.bottom else viewHeight - boundPadding

                                        val dx = dragAmount.x
                                        val dy = dragAmount.y

                                        when (currentHandle) {
                                            CropDragHandle.TOP_LEFT -> {
                                                newRect.left = (newRect.left + dx).coerceIn(bLeft, newRect.right - minSize)
                                                newRect.top = (newRect.top + dy).coerceIn(bTop, newRect.bottom - minSize)
                                            }
                                            CropDragHandle.TOP_RIGHT -> {
                                                newRect.right = (newRect.right + dx).coerceIn(newRect.left + minSize, bRight)
                                                newRect.top = (newRect.top + dy).coerceIn(bTop, newRect.bottom - minSize)
                                            }
                                            CropDragHandle.BOTTOM_LEFT -> {
                                                newRect.left = (newRect.left + dx).coerceIn(bLeft, newRect.right - minSize)
                                                newRect.bottom = (newRect.bottom + dy).coerceIn(newRect.top + minSize, bBottom)
                                            }
                                            CropDragHandle.BOTTOM_RIGHT -> {
                                                newRect.right = (newRect.right + dx).coerceIn(newRect.left + minSize, bRight)
                                                newRect.bottom = (newRect.bottom + dy).coerceIn(newRect.top + minSize, bBottom)
                                            }
                                            CropDragHandle.LEFT -> {
                                                newRect.left = (newRect.left + dx).coerceIn(bLeft, newRect.right - minSize)
                                            }
                                            CropDragHandle.RIGHT -> {
                                                newRect.right = (newRect.right + dx).coerceIn(newRect.left + minSize, bRight)
                                            }
                                            CropDragHandle.TOP -> {
                                                newRect.top = (newRect.top + dy).coerceIn(bTop, newRect.bottom - minSize)
                                            }
                                            CropDragHandle.BOTTOM -> {
                                                newRect.bottom = (newRect.bottom + dy).coerceIn(newRect.top + minSize, bBottom)
                                            }
                                            CropDragHandle.INSIDE -> {
                                                val w = newRect.width()
                                                val h = newRect.height()
                                                var l = newRect.left + dx
                                                var t = newRect.top + dy
                                                if (l < bLeft) l = bLeft
                                                if (l + w > bRight) l = bRight - w
                                                if (t < bTop) t = bTop
                                                if (t + h > bBottom) t = bBottom - h
                                                newRect.set(l, t, l + w, t + h)
                                            }
                                            CropDragHandle.NONE -> {}
                                        }
                                        freeCropRect = RectF(newRect)
                                    } else {
                                        panOffset += dragAmount
                                    }
                                },
                                onDragEnd = {
                                    currentHandle = CropDragHandle.NONE
                                },
                                onDragCancel = {
                                    currentHandle = CropDragHandle.NONE
                                }
                            )
                        }
                    }
            )
        }

        // 标尺滑杆控制区 (重置按钮 + 当前数值指示 + 90°旋转 + 刻度标尺)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // 上排控制：重置 (✕)、数值指示 (主题色)、90° 旋转快捷键
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 重置当前工具为默认值
                    IconButton(
                        onClick = {
                            when (activeTab) {
                                CropToolTab.ROTATE -> {
                                    fineAngle = 0f
                                    baseRotateSteps = 0f
                                    freeCropRect = null
                                }
                                CropToolTab.SCALE -> {
                                    scaleFactor = 1f
                                    panOffset = Offset.Zero
                                    freeCropRect = null
                                }
                                CropToolTab.BRIGHTNESS -> brightness = 0f
                                CropToolTab.CONTRAST -> contrast = 0f
                                CropToolTab.SATURATION -> saturation = 0f
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.crop_reset),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 居中数值文本
                    val valueDisplay = when (activeTab) {
                        CropToolTab.ROTATE -> String.format("%.1f°", totalRotation)
                        CropToolTab.SCALE -> "${(scaleFactor * 100).roundToInt()}%"
                        CropToolTab.BRIGHTNESS -> "${brightness.roundToInt()}"
                        CropToolTab.CONTRAST -> "${contrast.roundToInt()}"
                        CropToolTab.SATURATION -> "${saturation.roundToInt()}"
                    }

                    Text(
                        text = valueDisplay,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 90° 旋转快捷键
                    IconButton(
                        onClick = {
                            val newRotate = (baseRotateSteps + 90f) % 360f
                            baseRotateSteps = newRotate
                            freeCropRect = null
                            if (constrainToImage && cropRectPx.width() > 0f) {
                                val (clampedPan, clampedScale) = ImageAdjustmentEngine.clampPanAndScale(
                                    pan = panOffset,
                                    scale = scaleFactor,
                                    rotationDegrees = newRotate + fineAngle,
                                    sourceWidth = sourceBitmap.width.toFloat(),
                                    sourceHeight = sourceBitmap.height.toFloat(),
                                    cropBoxWidth = cropRectPx.width(),
                                    cropBoxHeight = cropRectPx.height(),
                                    constrainToImage = true
                                )
                                panOffset = clampedPan
                                scaleFactor = clampedScale
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Rotate90DegreesCw,
                            contentDescription = stringResource(R.string.crop_rotate_90),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 下排标尺滑动调节器
                when (activeTab) {
                    CropToolTab.ROTATE -> {
                        RulerSlider(
                            value = fineAngle,
                            onValueChange = { angle ->
                                fineAngle = angle
                                if (constrainToImage && cropRectPx.width() > 0f) {
                                    val (clampedPan, clampedScale) = ImageAdjustmentEngine.clampPanAndScale(
                                        pan = panOffset,
                                        scale = scaleFactor,
                                        rotationDegrees = baseRotateSteps + angle,
                                        sourceWidth = sourceBitmap.width.toFloat(),
                                        sourceHeight = sourceBitmap.height.toFloat(),
                                        cropBoxWidth = cropRectPx.width(),
                                        cropBoxHeight = cropRectPx.height(),
                                        constrainToImage = true
                                    )
                                    panOffset = clampedPan
                                    scaleFactor = clampedScale
                                }
                            },
                            valueRange = -45f..45f,
                            step = 0.5f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    CropToolTab.SCALE -> {
                        val minSliderScale = if (constrainToImage) 100f else 50f
                        RulerSlider(
                            value = (scaleFactor * 100f).coerceIn(minSliderScale, 300f),
                            onValueChange = { targetVal ->
                                val targetScale = targetVal / 100f
                                if (cropRectPx.width() > 0f) {
                                    val (clampedPan, clampedScale) = ImageAdjustmentEngine.clampPanAndScale(
                                        pan = panOffset,
                                        scale = targetScale,
                                        rotationDegrees = totalRotation,
                                        sourceWidth = sourceBitmap.width.toFloat(),
                                        sourceHeight = sourceBitmap.height.toFloat(),
                                        cropBoxWidth = cropRectPx.width(),
                                        cropBoxHeight = cropRectPx.height(),
                                        constrainToImage = constrainToImage
                                    )
                                    panOffset = clampedPan
                                    scaleFactor = clampedScale
                                } else {
                                    scaleFactor = targetScale
                                }
                            },
                            valueRange = minSliderScale..300f,
                            step = 2f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    CropToolTab.BRIGHTNESS -> {
                        RulerSlider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            valueRange = -100f..100f,
                            step = 1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    CropToolTab.CONTRAST -> {
                        RulerSlider(
                            value = contrast,
                            onValueChange = { contrast = it },
                            valueRange = -100f..100f,
                            step = 1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    CropToolTab.SATURATION -> {
                        RulerSlider(
                            value = saturation,
                            onValueChange = { saturation = it },
                            valueRange = -100f..100f,
                            step = 1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 4. 底部 5 大工具栏 (旋转、缩放、亮度、对比度、饱和度)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CropToolTab.entries.forEach { tab ->
                    val isSelected = activeTab == tab
                    val tabTitle = stringResource(tab.titleRes)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { activeTab = tab }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tabTitle,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = tabTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
