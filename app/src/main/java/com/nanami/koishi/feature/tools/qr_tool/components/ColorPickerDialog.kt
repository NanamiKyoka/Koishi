package com.nanami.koishi.feature.tools.qr_tool.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nanami.koishi.core.designsystem.component.ClassicColorPickerDialog

/**
 * 符合 Material 3 设计规范的经典 2D 平面色板调色板弹窗
 * 包含：2D HSV 平面色板 + 色相/透明度滑杆 + RGB/HEX 双向实时同步输入
 */
@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    ClassicColorPickerDialog(
        title = title,
        initialColor = initialColor,
        onDismissRequest = onDismissRequest,
        onColorSelected = onColorSelected
    )
}
