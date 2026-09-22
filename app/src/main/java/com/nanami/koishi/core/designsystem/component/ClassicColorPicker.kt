package com.nanami.koishi.core.designsystem.component

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import kotlin.math.roundToInt

/**
 * 经典专业 2D 平面色板 (Classic 2D Area) + 单轴滑杆 (Hue & Alpha) + RGB/HEX 双向输入
 * 严格遵循 Material 3 设计规范与主题自适应
 */
@Composable
fun ClassicColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    // 提取初始 HSV 与 Alpha
    val initialHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(initialColor.toArgb(), hsv)
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember { mutableFloatStateOf(initialColor.alpha) }

    // 计算当前颜色
    val currentColor = remember(hue, saturation, value, alpha) {
        Color.hsv(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f), alpha.coerceIn(0f, 1f))
    }

    // 触发外部回调
    LaunchedEffect(currentColor) {
        onColorChanged(currentColor)
    }

    // 数值输入状态
    var inputMode by remember { mutableIntStateOf(0) } // 0: HEX, 1: RGB
    var hexText by remember { mutableStateOf(formatHex(currentColor)) }
    var rText by remember { mutableStateOf((currentColor.red * 255).roundToInt().toString()) }
    var gText by remember { mutableStateOf((currentColor.green * 255).roundToInt().toString()) }
    var bText by remember { mutableStateOf((currentColor.blue * 255).roundToInt().toString()) }
    var aText by remember { mutableStateOf((currentColor.alpha * 100).roundToInt().toString()) }

    // 当滑块/色板改变时更新输入框显示
    LaunchedEffect(currentColor) {
        val newHex = formatHex(currentColor)
        if (hexText.uppercase() != newHex.uppercase()) {
            hexText = newHex
        }
        rText = (currentColor.red * 255).roundToInt().toString()
        gText = (currentColor.green * 255).roundToInt().toString()
        bText = (currentColor.blue * 255).roundToInt().toString()
        aText = (currentColor.alpha * 100).roundToInt().toString()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 2D 平面色板 (X: 饱和度 0~1, Y: 明暗度 1~0)
        ClassicColorArea2D(
            hue = hue,
            saturation = saturation,
            value = value,
            onSaturationValueChanged = { s, v ->
                saturation = s
                value = v
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        // 2. 色相 (Hue) 滑块 (白环滑块位于彩虹条正中)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "色相 (Hue)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${hue.roundToInt()}°",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            ColorBarSlider(
                value = hue,
                valueRange = 0f..360f,
                onValueChange = { hue = it },
                trackBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red
                    )
                )
            )
        }

        // 3. 透明度 (Alpha) 滑块 (白环滑块位于透明渐变条正中)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "透明度 (Alpha)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(alpha * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            ColorBarSlider(
                value = alpha,
                valueRange = 0f..1f,
                onValueChange = { alpha = it },
                isCheckerboard = true,
                trackBrush = Brush.horizontalGradient(
                    colors = listOf(
                        currentColor.copy(alpha = 0f),
                        currentColor.copy(alpha = 1f)
                    )
                )
            )
        }

        // 4. RGB 与 HEX 输入切换及文本框 (适配当前主题色)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = inputMode == 0,
                    onClick = { inputMode = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("HEX", fontWeight = FontWeight.Medium)
                }
                SegmentedButton(
                    selected = inputMode == 1,
                    onClick = { inputMode = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("RGB", fontWeight = FontWeight.Medium)
                }
            }

            if (inputMode == 0) {
                // HEX 输入框
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        val clean = input.filter { it.isLetterOrDigit() || it == '#' }.take(9)
                        hexText = clean
                        val parsed = parseHexColor(clean)
                        if (parsed != null) {
                            val hsv = FloatArray(3)
                            AndroidColor.colorToHSV(parsed.toArgb(), hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2]
                            alpha = parsed.alpha
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("十六进制色彩代码") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            } else {
                // RGB 输入栏 (R, G, B, A 并排)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RgbChannelField(
                        label = "R",
                        value = rText,
                        modifier = Modifier.weight(1f)
                    ) { newR ->
                        rText = newR
                        newR.toIntOrNull()?.let { r ->
                            val validR = r.coerceIn(0, 255)
                            val g = gText.toIntOrNull()?.coerceIn(0, 255) ?: 0
                            val b = bText.toIntOrNull()?.coerceIn(0, 255) ?: 0
                            val color = Color(validR, g, b)
                            val hsv = FloatArray(3)
                            AndroidColor.colorToHSV(color.toArgb(), hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2]
                        }
                    }

                    RgbChannelField(
                        label = "G",
                        value = gText,
                        modifier = Modifier.weight(1f)
                    ) { newG ->
                        gText = newG
                        newG.toIntOrNull()?.let { g ->
                            val validG = g.coerceIn(0, 255)
                            val r = rText.toIntOrNull()?.coerceIn(0, 255) ?: 0
                            val b = bText.toIntOrNull()?.coerceIn(0, 255) ?: 0
                            val color = Color(r, validG, b)
                            val hsv = FloatArray(3)
                            AndroidColor.colorToHSV(color.toArgb(), hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2]
                        }
                    }

                    RgbChannelField(
                        label = "B",
                        value = bText,
                        modifier = Modifier.weight(1f)
                    ) { newB ->
                        bText = newB
                        newB.toIntOrNull()?.let { b ->
                            val validB = b.coerceIn(0, 255)
                            val r = rText.toIntOrNull()?.coerceIn(0, 255) ?: 0
                            val g = gText.toIntOrNull()?.coerceIn(0, 255) ?: 0
                            val color = Color(r, g, validB)
                            val hsv = FloatArray(3)
                            AndroidColor.colorToHSV(color.toArgb(), hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2]
                        }
                    }

                    RgbChannelField(
                        label = "A%",
                        value = aText,
                        modifier = Modifier.weight(1f)
                    ) { newA ->
                        aText = newA
                        newA.toIntOrNull()?.let { a ->
                            alpha = (a.coerceIn(0, 100) / 100f)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 专为色彩条设计的单轴滑块：滑块正正位于条形轨道正上方，完美贴合参考设计
 */
@Composable
private fun ColorBarSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    trackBrush: Brush,
    isCheckerboard: Boolean = false,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val thumbRadius = 12.dp
        val thumbRadiusPx = with(density) { thumbRadius.toPx() }
        val usableWidthPx = (widthPx - 2 * thumbRadiusPx).coerceAtLeast(1f)

        val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        val thumbCenterXPx = thumbRadiusPx + fraction * usableWidthPx

        val updateFromPosition: (Float) -> Unit = { x ->
            val clampedX = (x - thumbRadiusPx).coerceIn(0f, usableWidthPx)
            val newFraction = clampedX / usableWidthPx
            val newValue = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
            onValueChange(newValue)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        updateFromPosition(offset.x)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        updateFromPosition(change.position.x)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // 1. 轨道 (Track) - 高度 18.dp，居中对齐
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
            ) {
                if (isCheckerboard) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCheckerboard(size, checkSize = 7f)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(trackBrush)
                )
            }

            // 2. MD3 垂直主题标线滑块 (Vertical Thumb Bar) - 随当前主题色变化
            val barWidth = 6.dp
            val barHeight = 26.dp
            val barShape = RoundedCornerShape(3.dp)
            val halfBarWidthPx = with(density) { (barWidth / 2).toPx() }

            Box(
                modifier = Modifier
                    .offset { IntOffset((thumbCenterXPx - halfBarWidthPx).roundToInt(), 0) }
                    .size(barWidth, barHeight)
                    .shadow(3.dp, barShape)
                    .clip(barShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.dp, Color.White.copy(alpha = 0.9f), barShape)
            )
        }
    }
}

/**
 * 2D 矩形色彩空间 (X: 饱和度 0~1, Y: 明暗度 1~0)
 */
@Composable
private fun ClassicColorArea2D(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChanged: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var areaSize by remember { mutableStateOf(Size.Zero) }

    val baseHueColor = remember(hue) {
        Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f)
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (areaSize.width > 0 && areaSize.height > 0) {
                        val s = (offset.x / areaSize.width).coerceIn(0f, 1f)
                        val v = (1f - (offset.y / areaSize.height)).coerceIn(0f, 1f)
                        onSaturationValueChanged(s, v)
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    if (areaSize.width > 0 && areaSize.height > 0) {
                        val s = (change.position.x / areaSize.width).coerceIn(0f, 1f)
                        val v = (1f - (change.position.y / areaSize.height)).coerceIn(0f, 1f)
                        onSaturationValueChanged(s, v)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            areaSize = size
            val w = size.width
            val h = size.height

            // 1. 底层：当前色相纯色
            drawRect(color = baseHueColor, size = size)

            // 2. 水平渐变：从左侧纯白向右侧透明过渡
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    startX = 0f,
                    endX = w
                ),
                size = size
            )

            // 3. 垂直渐变：从顶部透明向底部纯黑过渡
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = h
                ),
                size = size
            )

            // 4. 当前选中点指示器光标 (双层高对比圆环)
            val thumbX = (saturation.coerceIn(0f, 1f) * w)
            val thumbY = ((1f - value.coerceIn(0f, 1f)) * h)
            val thumbRadius = 10.dp.toPx()

            // 外层黑色轮廓
            drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = thumbRadius + 1.5.dp.toPx(),
                center = Offset(thumbX, thumbY),
                style = Stroke(width = 2.dp.toPx())
            )
            // 内层白色圆环
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(thumbX, thumbY),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

/**
 * RGB 单通道输入小组件
 */
@Composable
private fun RgbChannelField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(3)
            onValueChange(digits)
        },
        modifier = modifier,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

/**
 * 绘制棋盘格透明指示纹理
 */
private fun DrawScope.drawCheckerboard(size: Size, checkSize: Float) {
    val cols = (size.width / checkSize).toInt() + 1
    val rows = (size.height / checkSize).toInt() + 1
    val light = Color(0xFFE0E0E0)
    val dark = Color(0xFFB0B0B0)

    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val isEven = (row + col) % 2 == 0
            drawRect(
                color = if (isEven) light else dark,
                topLeft = Offset(col * checkSize, row * checkSize),
                size = Size(checkSize, checkSize)
            )
        }
    }
}

/**
 * 格式化输出十六进制色彩
 */
internal fun formatHex(color: Color): String {
    val a = (color.alpha * 255).roundToInt().coerceIn(0, 255)
    val r = (color.red * 255).roundToInt().coerceIn(0, 255)
    val g = (color.green * 255).roundToInt().coerceIn(0, 255)
    val b = (color.blue * 255).roundToInt().coerceIn(0, 255)
    return if (a == 255) {
        String.format("#%02X%02X%02X", r, g, b)
    } else {
        String.format("#%02X%02X%02X%02X", a, r, g, b)
    }
}

/**
 * 解析十六进制色彩 (支持 #RRGGBB, RRGGBB, #AARRGGBB, AARRGGBB)
 */
internal fun parseHexColor(hex: String): Color? {
    val clean = hex.removePrefix("#").trim()
    return try {
        when (clean.length) {
            6 -> {
                val colorInt = clean.toLong(16).toInt() or (0xFF shl 24)
                Color(colorInt)
            }
            8 -> {
                val colorLong = clean.toLong(16)
                Color(colorLong.toInt())
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Material 3 经典专业色彩选择器弹窗 (ClassicColorPickerDialog)
 */
@Composable
fun ClassicColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                // 实时预览色彩胶囊 (带透明棋盘底)
                Box(
                    modifier = Modifier
                        .size(40.dp, 28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCheckerboard(size, checkSize = 8f)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(selectedColor)
                    )
                }
            }
        },
        text = {
            ClassicColorPicker(
                initialColor = initialColor,
                onColorChanged = { selectedColor = it }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(selectedColor)
                    onDismissRequest()
                }
            ) {
                Text(
                    text = "确定",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = stringResource(R.string.dialog_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
