package com.nanami.koishi.feature.tools.watermark

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.PillShape
import com.nanami.koishi.core.designsystem.ToolCardShape
import com.nanami.koishi.core.designsystem.component.ClassicColorPickerDialog
import com.nanami.koishi.core.image.crop.rememberCropImageLauncher
import com.nanami.koishi.core.image.preview.ImagePreviewDialog
import com.nanami.koishi.core.util.AlbumFolders
import com.nanami.koishi.feature.tools.watermark.engine.WatermarkFont
import com.nanami.koishi.feature.tools.watermark.engine.WatermarkType
import kotlin.math.roundToInt

@Composable
fun WatermarkRoute(
    viewModel: WatermarkViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(WatermarkUiEvent.OnDismissMessage)
        }
    }

    WatermarkScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkScreen(
    state: WatermarkUiState,
    onEvent: (WatermarkUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 裁剪启动器
    val bgCropLauncher = rememberCropImageLauncher { uri ->
        onEvent(WatermarkUiEvent.OnBackgroundSelected(uri))
    }
    val watermarkCropLauncher = rememberCropImageLauncher { uri ->
        onEvent(WatermarkUiEvent.OnWatermarkImageSelected(uri))
    }

    // 系统媒体选择器（免裁剪直接选取）
    val bgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onEvent(WatermarkUiEvent.OnBackgroundSelected(it)) }
    }
    val watermarkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onEvent(WatermarkUiEvent.OnWatermarkImageSelected(it)) }
    }

    // 全屏大图预览状态
    var showPreviewDialog by remember { mutableStateOf(false) }

    if (showPreviewDialog && state.previewBitmap != null) {
        ImagePreviewDialog(
            images = listOf(state.previewBitmap),
            title = stringResource(R.string.watermark_title),
            albumFolder = AlbumFolders.WATERMARK,
            onDismissRequest = { showPreviewDialog = false }
        )
    }

    if (state.showColorPicker) {
        ClassicColorPickerDialog(
            title = stringResource(R.string.watermark_color_label),
            initialColor = Color(state.config.textColor),
            onDismissRequest = { onEvent(WatermarkUiEvent.OnToggleColorPicker(false)) },
            onColorSelected = { color ->
                onEvent(WatermarkUiEvent.OnTextColorChanged(color.toArgb()))
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.watermark_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onEvent(WatermarkUiEvent.OnResetConfig) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.mirage_tank_reset_params),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { onEvent(WatermarkUiEvent.OnSaveResult) },
                        enabled = state.backgroundBitmap != null && !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = stringResource(R.string.watermark_save),
                                tint = if (state.backgroundBitmap != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 实时预览区域 (支持点击放大预览)
            WatermarkPreviewCard(
                state = state,
                onPickBg = { bgPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onCropBg = { bgCropLauncher.launch(isSquare = false, target = "watermark_bg") },
                onClearBg = { onEvent(WatermarkUiEvent.OnClearBackground) },
                onPreviewClick = { showPreviewDialog = true }
            )

            // 模式选择 Tab (文字水印 / 图片水印)
            PrimaryTabRow(
                selectedTabIndex = if (state.config.type == WatermarkType.TEXT) 0 else 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PillShape),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = state.config.type == WatermarkType.TEXT,
                    onClick = { onEvent(WatermarkUiEvent.OnModeChanged(WatermarkType.TEXT)) },
                    text = {
                        Text(
                            stringResource(R.string.watermark_tab_text),
                            fontWeight = if (state.config.type == WatermarkType.TEXT) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = state.config.type == WatermarkType.IMAGE,
                    onClick = { onEvent(WatermarkUiEvent.OnModeChanged(WatermarkType.IMAGE)) },
                    text = {
                        Text(
                            stringResource(R.string.watermark_tab_image),
                            fontWeight = if (state.config.type == WatermarkType.IMAGE) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Rounded.Image, contentDescription = null) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 根据模式渲染专属调节卡片
            when (state.config.type) {
                WatermarkType.TEXT -> {
                    TextWatermarkPanel(
                        state = state,
                        onEvent = onEvent
                    )
                }
                WatermarkType.IMAGE -> {
                    ImageWatermarkPanel(
                        state = state,
                        onPickWatermark = { watermarkPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onCropWatermark = { watermarkCropLauncher.launch(isSquare = false, target = "watermark_fg") },
                        onClearWatermark = { onEvent(WatermarkUiEvent.OnClearWatermarkImage) }
                    )
                }
            }

            // 通用平铺参数滑块调节卡片
            CommonSettingsPanel(
                state = state,
                onEvent = onEvent
            )
        }
    }
}

/**
 * 实时预览展示卡片
 */
@Composable
private fun WatermarkPreviewCard(
    state: WatermarkUiState,
    onPickBg: () -> Unit,
    onCropBg: () -> Unit,
    onClearBg: () -> Unit,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = ToolCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val preview = state.previewBitmap
            if (preview != null && !preview.isRecycled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onPreviewClick),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = stringResource(R.string.watermark_title),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // 放大预览提示图标（左下角胶囊）
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomIn,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "点击放大",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 顶部右侧删除背景按钮
                    IconButton(
                        onClick = onClearBg,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.grid_split_clear_image),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // 空状态引导：提供选择背景图与裁剪背景图两个入口
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.watermark_empty_bg_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onPickBg,
                            shape = PillShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.watermark_pick_bg), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = onCropBg,
                            shape = PillShape,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.watermark_crop_bg_action), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (state.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 文字水印专属设置卡片
 */
@Composable
private fun TextWatermarkPanel(
    state: WatermarkUiState,
    onEvent: (WatermarkUiEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ToolCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 文字输入框
            OutlinedTextField(
                value = state.config.text,
                onValueChange = { onEvent(WatermarkUiEvent.OnTextChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.watermark_tab_text)) },
                placeholder = { Text(stringResource(R.string.watermark_input_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                trailingIcon = {
                    if (state.config.text.isNotEmpty()) {
                        IconButton(onClick = { onEvent(WatermarkUiEvent.OnTextChanged("")) }) {
                            Icon(Icons.Rounded.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            )

            // 预设文字快速填充 (恋之水绿胶囊)
            val presets = listOf(
                stringResource(R.string.watermark_preset_sample),
                stringResource(R.string.watermark_preset_confidential),
                stringResource(R.string.watermark_preset_audit),
                stringResource(R.string.watermark_preset_repost)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets) { preset ->
                    val isSelected = state.config.text == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onEvent(WatermarkUiEvent.OnTextChanged(preset)) },
                        label = { Text(preset) },
                        shape = PillShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            // 字体选择 (使用恋之水绿作为选中色)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.watermark_font_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(WatermarkFont.entries) { font ->
                        val isSelected = state.config.font == font
                        FilterChip(
                            selected = isSelected,
                            onClick = { onEvent(WatermarkUiEvent.OnFontChanged(font)) },
                            label = { Text(stringResource(font.labelRes)) },
                            shape = PillShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }

            // 颜色选择按钮条 (以 Surface 药丸卡片包裹)
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PillShape)
                    .clickable { onEvent(WatermarkUiEvent.OnToggleColorPicker(true)) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FormatColorFill,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.watermark_color_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp, 24.dp)
                                .clip(PillShape)
                                .background(Color(state.config.textColor))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PillShape)
                        )
                        TextButton(
                            onClick = { onEvent(WatermarkUiEvent.OnToggleColorPicker(true)) },
                            shape = PillShape,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.Colorize, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = String.format("#%06X", (0xFFFFFF and state.config.textColor)),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 图片水印专属设置卡片
 */
@Composable
private fun ImageWatermarkPanel(
    state: WatermarkUiState,
    onPickWatermark: () -> Unit,
    onCropWatermark: () -> Unit,
    onClearWatermark: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ToolCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val wmBmp = state.config.watermarkBitmap
            if (wmBmp != null && !wmBmp.isRecycled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        bitmap = wmBmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(4.dp),
                        contentScale = ContentScale.Fit
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${wmBmp.width} × ${wmBmp.height} px",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilledTonalButton(
                                    onClick = onPickWatermark,
                                    shape = PillShape,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    contentPadding = ButtonDefaults.ContentPadding
                                ) {
                                    Text(
                                        stringResource(R.string.watermark_pick_watermark_img),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = onCropWatermark,
                                    shape = PillShape,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                    contentPadding = ButtonDefaults.ContentPadding
                                ) {
                                    Text(
                                        stringResource(R.string.watermark_crop_watermark_img),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onClearWatermark,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.grid_split_clear_image),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onPickWatermark)
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.watermark_no_watermark_img),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onPickWatermark,
                            shape = PillShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.watermark_pick_watermark_img), fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = onCropWatermark,
                            shape = PillShape,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.watermark_crop_watermark_img), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 通用平铺微调面板 (不透明度、角度、大小/缩放、水平垂直间距)
 */
@Composable
private fun CommonSettingsPanel(
    state: WatermarkUiState,
    onEvent: (WatermarkUiEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ToolCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 不透明度
            SliderSettingItem(
                title = stringResource(R.string.watermark_alpha_label),
                valueText = "${(state.config.alpha * 100).roundToInt()}%",
                value = state.config.alpha,
                valueRange = 0.05f..1f,
                onValueChange = { onEvent(WatermarkUiEvent.OnAlphaChanged(it)) }
            )

            // 旋转角度
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.watermark_rotation_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "${state.config.rotation.roundToInt()}°",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
                Slider(
                    value = state.config.rotation,
                    onValueChange = { onEvent(WatermarkUiEvent.OnRotationChanged(it)) },
                    valueRange = -180f..180f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    )
                )

                // 常见角度快捷选择 - 使用 LazyRow 杜绝窄屏挤压换行
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val angles = listOf(-90f, -45f, -30f, 0f, 30f, 45f, 90f)
                    items(angles) { angle ->
                        val isSelected = state.config.rotation.roundToInt() == angle.roundToInt()
                        FilterChip(
                            selected = isSelected,
                            onClick = { onEvent(WatermarkUiEvent.OnRotationChanged(angle)) },
                            label = { Text("${angle.toInt()}°") },
                            shape = PillShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }

            // 大小 / 缩放比例
            if (state.config.type == WatermarkType.TEXT) {
                SliderSettingItem(
                    title = stringResource(R.string.watermark_size_label),
                    valueText = "${state.config.textSize.roundToInt()} sp",
                    value = state.config.textSize,
                    valueRange = 12f..96f,
                    onValueChange = { onEvent(WatermarkUiEvent.OnTextSizeChanged(it)) }
                )
            } else {
                SliderSettingItem(
                    title = stringResource(R.string.watermark_scale_label),
                    valueText = "${(state.config.imageScale * 100).roundToInt()}%",
                    value = state.config.imageScale,
                    valueRange = 0.1f..3f,
                    onValueChange = { onEvent(WatermarkUiEvent.OnImageScaleChanged(it)) }
                )
            }

            // 水平间距
            SliderSettingItem(
                title = stringResource(R.string.watermark_spacing_h_label),
                valueText = "${state.config.horizontalSpacing.roundToInt()} px",
                value = state.config.horizontalSpacing,
                valueRange = 20f..400f,
                onValueChange = { onEvent(WatermarkUiEvent.OnHorizontalSpacingChanged(it)) }
            )

            // 垂直间距
            SliderSettingItem(
                title = stringResource(R.string.watermark_spacing_v_label),
                valueText = "${state.config.verticalSpacing.roundToInt()} px",
                value = state.config.verticalSpacing,
                valueRange = 20f..400f,
                onValueChange = { onEvent(WatermarkUiEvent.OnVerticalSpacingChanged(it)) }
            )
        }
    }
}

@Composable
private fun SliderSettingItem(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            )
        )
    }
}
