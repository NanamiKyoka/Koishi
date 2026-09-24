package com.nanami.koishi.core.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val KoishiShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val PillShape = CircleShape

val ToolCardShape = RoundedCornerShape(28.dp)

val BottomSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
