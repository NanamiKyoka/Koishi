package com.nanami.koishi.feature.tools.qr_tool.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R

/**
 * 二维码主题配色预设方案
 */
data class QrThemePreset(
    val id: String,
    val name: String,
    val previewColor: Color,
    val darkColor: Color,
    val lightColor: Color,
    val backgroundColor: Color
)

object QrThemePresets {
    val presets = listOf(
        QrThemePreset(
            id = "black",
            name = "经典玄黑",
            previewColor = Color(0xFF1E1E1E),
            darkColor = Color(0xFF1A1A1A),
            lightColor = Color(0xFFFFFFFF),
            backgroundColor = Color(0xFFFFFFFF)
        ),
        QrThemePreset(
            id = "coral",
            name = "豆沙珊瑚",
            previewColor = Color(0xFFE07A7A),
            darkColor = Color(0xFFB84848),
            lightColor = Color(0xFFFDF0F0),
            backgroundColor = Color(0xFFFFFFFF)
        ),
        QrThemePreset(
            id = "matcha",
            name = "抹茶青竹",
            previewColor = Color(0xFF82BA9E),
            darkColor = Color(0xFF3D6B57),
            lightColor = Color(0xFFEAF2EC),
            backgroundColor = Color(0xFFFFFFFF)
        ),
        QrThemePreset(
            id = "orange",
            name = "暖杏日落",
            previewColor = Color(0xFFF5A13C),
            darkColor = Color(0xFFCC7216),
            lightColor = Color(0xFFFDF5EB),
            backgroundColor = Color(0xFFFFFFFF)
        ),
        QrThemePreset(
            id = "blue",
            name = "晴空湛蓝",
            previewColor = Color(0xFF688BF8),
            darkColor = Color(0xFF2C55D4),
            lightColor = Color(0xFFEDF2FE),
            backgroundColor = Color(0xFFFFFFFF)
        ),
        QrThemePreset(
            id = "soft_pink",
            name = "初雪杏粉",
            previewColor = Color(0xFFF8EBEB),
            darkColor = Color(0xFF7A4A57),
            lightColor = Color(0xFFFDF7F8),
            backgroundColor = Color(0xFFFFFFFF)
        ),
        QrThemePreset(
            id = "lime",
            name = "青柠嫩芽",
            previewColor = Color(0xFFB8E04D),
            darkColor = Color(0xFF5A7818),
            lightColor = Color(0xFFF4FBEB),
            backgroundColor = Color(0xFFFFFFFF)
        ),
        QrThemePreset(
            id = "purple",
            name = "幻彩紫晶",
            previewColor = Color(0xFFAE7BF5),
            darkColor = Color(0xFF6E39BA),
            lightColor = Color(0xFFF4EDFF),
            backgroundColor = Color(0xFFFFFFFF)
        )
    )
}

/**
 * 参考图二：选择主题弹出的 BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerBottomSheet(
    onDismissRequest: () -> Unit,
    onThemeSelected: (QrThemePreset) -> Unit,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // 顶部标题（标准 MD3 抽屉风格，通过顶部 Drag Handle 手势下滑或外部点击即可退出）
            Text(
                text = stringResource(R.string.qr_theme_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 预设主题色块 4 列网格（完全复现参考图二）
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(QrThemePresets.presets, key = { it.id }) { preset ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(preset.previewColor)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.15f),
                                shape = CircleShape
                            )
                            .clickable {
                                onThemeSelected(preset)
                                onDismissRequest()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // 纯色圆形圆块
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
