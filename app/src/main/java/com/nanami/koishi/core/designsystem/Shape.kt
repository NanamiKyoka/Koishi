package com.nanami.koishi.core.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Koishi MD3 形状设计规范
 *
 * 规范要点：
 * 1. 卡片、面板、浮层容器采用 24dp - 28dp 超大圆角 (Extra Large)，展现现代化温柔无感体验。
 * 2. 按钮、搜索框、分类标签、指示器统一采用全圆角药丸形 (Full Pill Shape)。
 */
val KoishiShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * 全圆角药丸形状 (Full Pill Shape)
 */
val PillShape = CircleShape

/**
 * MD3 工具卡片专属圆角 (28dp)
 */
val ToolCardShape = RoundedCornerShape(28.dp)

/**
 * 底部弹窗与大容器专属圆角 (顶部 32dp，平滑优雅)
 */
val BottomSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
