package com.nanami.koishi.core.image.crop

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Rotate90DegreesCw
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import kotlin.math.min
import kotlin.math.roundToInt

enum class CropToolTab(@StringRes val titleRes: Int, val icon: ImageVector) {
    ROTATE(R.string.crop_tab_rotate, Icons.AutoMirrored.Rounded.RotateRight),
    SCALE(R.string.crop_tab_scale, Icons.Rounded.Crop),
    BRIGHTNESS(R.string.crop_tab_brightness, Icons.Rounded.WbSunny),
    CONTRAST(R.string.crop_tab_contrast, Icons.Rounded.Contrast),
    SATURATION(R.string.crop_tab_saturation, Icons.Rounded.ColorLens)
}

/**
 * 专业 MD3 图片裁剪与调整界面 (1:1 还原用户参考设计)
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
        saturation: Float
    ) -> Unit
) {
    if (sourceBitmap == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    var activeTab by remember { mutableStateOf(CropToolTab.ROTATE) }

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
                            saturation
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

        // 2. 核心九宫格裁剪视口
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scaleFactor = (scaleFactor * zoom).coerceIn(0.5f, 4.0f)
                        panOffset += pan
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val viewWidth = constraints.maxWidth.toFloat()
            val viewHeight = constraints.maxHeight.toFloat()
            viewportSizePx = Size(viewWidth, viewHeight)

            // 计算裁剪框尺寸 (1:1 正方形或自适应)
            val cropBoxSide = min(viewWidth, viewHeight) * 0.82f
            val cropLeft = (viewWidth - cropBoxSide) / 2f
            val cropTop = (viewHeight - cropBoxSide) / 2f
            val cropRight = cropLeft + cropBoxSide
            val cropBottom = cropTop + cropBoxSide
            cropRectPx = RectF(cropLeft, cropTop, cropRight, cropBottom)

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
                val srcW = sourceBitmap.width.toFloat()
                val srcH = sourceBitmap.height.toFloat()
                val baseScale = ImageAdjustmentEngine.calculateBaseScale(
                    sourceWidth = srcW,
                    sourceHeight = srcH,
                    cropBoxWidth = cropBoxSide,
                    cropBoxHeight = cropBoxSide
                )
                val drawW = srcW * baseScale
                val drawH = srcH * baseScale
                val topLeft = Offset((viewWidth - drawW) / 2f, (viewHeight - drawH) / 2f)

                drawImage(
                    image = imageBitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(drawW.roundToInt(), drawH.roundToInt()),
                    colorFilter = colorFilter
                )
            }

            // 绘制视口半透明遮罩与九宫格高亮框
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 四周暗色遮罩
                val scrimColor = Color.Black.copy(alpha = 0.62f)
                // 顶部
                drawRect(scrimColor, Offset.Zero, Size(viewWidth, cropTop))
                // 底部
                drawRect(scrimColor, Offset(0f, cropBottom), Size(viewWidth, viewHeight - cropBottom))
                // 左侧
                drawRect(scrimColor, Offset(0f, cropTop), Size(cropLeft, cropBoxSide))
                // 右侧
                drawRect(scrimColor, Offset(cropRight, cropTop), Size(viewWidth - cropRight, cropBoxSide))

                // 裁剪框白色外框 (2.dp)
                drawRect(
                    color = Color.White,
                    topLeft = Offset(cropLeft, cropTop),
                    size = Size(cropBoxSide, cropBoxSide),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 3x3 经典九宫格网格细线
                val third = cropBoxSide / 3f
                val gridColor = Color.White.copy(alpha = 0.38f)
                val gridWidth = 1.dp.toPx()

                // 水平内网格
                drawLine(gridColor, Offset(cropLeft, cropTop + third), Offset(cropRight, cropTop + third), gridWidth)
                drawLine(gridColor, Offset(cropLeft, cropTop + third * 2), Offset(cropRight, cropTop + third * 2), gridWidth)
                // 垂直内网格
                drawLine(gridColor, Offset(cropLeft + third, cropTop), Offset(cropLeft + third, cropBottom), gridWidth)
                drawLine(gridColor, Offset(cropLeft + third * 2, cropTop), Offset(cropLeft + third * 2, cropBottom), gridWidth)

                // 4 个角落主题色强调角标
                val cornerLen = 18.dp.toPx()
                val cornerStroke = 3.5.dp.toPx()
                val cornerColor = primaryColor
                // 左上
                drawLine(cornerColor, Offset(cropLeft, cropTop), Offset(cropLeft + cornerLen, cropTop), cornerStroke)
                drawLine(cornerColor, Offset(cropLeft, cropTop), Offset(cropLeft, cropTop + cornerLen), cornerStroke)
                // 右上
                drawLine(cornerColor, Offset(cropRight, cropTop), Offset(cropRight - cornerLen, cropTop), cornerStroke)
                drawLine(cornerColor, Offset(cropRight, cropTop), Offset(cropRight, cropTop + cornerLen), cornerStroke)
                // 左下
                drawLine(cornerColor, Offset(cropLeft, cropBottom), Offset(cropLeft + cornerLen, cropBottom), cornerStroke)
                drawLine(cornerColor, Offset(cropLeft, cropBottom), Offset(cropLeft, cropBottom - cornerLen), cornerStroke)
                // 右下
                drawLine(cornerColor, Offset(cropRight, cropBottom), Offset(cropRight - cornerLen, cropBottom), cornerStroke)
                drawLine(cornerColor, Offset(cropRight, cropBottom), Offset(cropRight, cropBottom - cornerLen), cornerStroke)
            }
        }

        // 3. 标尺滑杆控制区 (重置按钮 + 当前数值指示 + 90°旋转 + 刻度标尺)
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
                                }
                                CropToolTab.SCALE -> scaleFactor = 1f
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
                            baseRotateSteps = (baseRotateSteps + 90f) % 360f
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
                            onValueChange = { fineAngle = it },
                            valueRange = -45f..45f,
                            step = 0.5f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    CropToolTab.SCALE -> {
                        RulerSlider(
                            value = scaleFactor * 100f,
                            onValueChange = { scaleFactor = it / 100f },
                            valueRange = 100f..300f,
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
