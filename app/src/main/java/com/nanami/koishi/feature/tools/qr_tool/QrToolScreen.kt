package com.nanami.koishi.feature.tools.qr_tool

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Square
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanami.koishi.R
import com.nanami.koishi.core.image.crop.ImageCropActivity
import com.nanami.koishi.core.image.crop.rememberCropImageLauncher
import com.nanami.koishi.core.image.preview.ImagePreviewDialog
import com.nanami.koishi.core.util.AlbumFolders
import com.nanami.koishi.feature.tools.qr_tool.components.ColorPickerDialog
import com.nanami.koishi.feature.tools.qr_tool.components.ThemePickerBottomSheet
import com.nanami.koishi.feature.tools.qr_tool.engine.QrDotStyle
import com.nanami.koishi.feature.tools.qr_tool.scan.QrScanActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrToolRoute(
    viewModel: QrToolViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPreview by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
            viewModel.onEvent(QrToolUiEvent.OnClearUserMessage)
        }
    }

    // 扫描 Activity 启动器
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scannedText = result.data?.getStringExtra(QrScanActivity.EXTRA_RESULT_TEXT)
            if (!scannedText.isNullOrBlank()) {
                viewModel.onEvent(QrToolUiEvent.OnContentChange(scannedText))
            }
        }
    }

    // 通用裁剪启动器 (Logo 与 背景图)
    val logoCropLauncher = rememberCropImageLauncher { uri ->
        viewModel.onEvent(QrToolUiEvent.OnLogoSelected(uri))
    }
    val bgCropLauncher = rememberCropImageLauncher { uri ->
        viewModel.onEvent(QrToolUiEvent.OnBgSelected(uri))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            var menuExpanded by remember { mutableStateOf(false) }

            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tool_qr_code_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
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
                    // 保存图片
                    IconButton(onClick = { viewModel.onEvent(QrToolUiEvent.OnSaveToGallery) }) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = stringResource(R.string.qr_btn_save),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // 扫一扫
                    IconButton(onClick = {
                        try {
                            val intent = Intent(context, QrScanActivity::class.java)
                            scanLauncher.launch(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.widget.Toast.makeText(context, "无法启动扫码: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = stringResource(R.string.qr_btn_scan),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // 更多菜单
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.qr_btn_more),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.qr_menu_reset)) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.onEvent(QrToolUiEvent.OnResetDefaults)
                                }
                            )
                            if (uiState.logoBitmap != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.qr_menu_clear_logo)) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.onEvent(QrToolUiEvent.OnClearLogo)
                                    }
                                )
                            }
                            if (uiState.bgBitmap != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.qr_menu_clear_bg)) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.onEvent(QrToolUiEvent.OnClearBg)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 顶部预览与输入卡片组 (参考图一顶部)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 左侧卡片：二维码实时渲染预览
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(168.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = uiState.qrBitmap != null) {
                            showPreview = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        uiState.qrBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = stringResource(R.string.tool_qr_code_name),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } ?: run {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }

                // 右侧卡片：二维码内容输入
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(168.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 标题横条
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Notes,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.qr_content_label),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 多行输入区
                            Box(modifier = Modifier.weight(1f)) {
                                if (uiState.content.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.qr_content_placeholder),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                BasicTextField(
                                    value = uiState.content,
                                    onValueChange = { viewModel.onEvent(QrToolUiEvent.OnContentChange(it)) },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // 右下角清除按钮
                        if (uiState.content.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onEvent(QrToolUiEvent.OnClearContent) },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.clear_search_desc),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. 颜色配置区域 (参考图一深色、浅色、背景色、从背景图取色、选择主题)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 第一行：[深色] 与 [浅色] 药丸胶囊按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ColorCapsuleButton(
                        label = stringResource(R.string.qr_color_dark),
                        color = uiState.darkColor,
                        enabled = !uiState.isPickFromBg,
                        onClick = { viewModel.onEvent(QrToolUiEvent.OnOpenColorPicker(ColorPickerTarget.DARK)) }
                    )
                    ColorCapsuleButton(
                        label = stringResource(R.string.qr_color_light),
                        color = uiState.lightColor,
                        enabled = !uiState.isPickFromBg,
                        onClick = { viewModel.onEvent(QrToolUiEvent.OnOpenColorPicker(ColorPickerTarget.LIGHT)) }
                    )
                }

                // 第二行：[背景色] 与 [从背景图取色] + [选择主题 >]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ColorCapsuleButton(
                            label = stringResource(R.string.qr_color_bg),
                            color = uiState.backgroundColor,
                            enabled = true,
                            onClick = { viewModel.onEvent(QrToolUiEvent.OnOpenColorPicker(ColorPickerTarget.BACKGROUND)) }
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                viewModel.onEvent(QrToolUiEvent.OnTogglePickFromBg(!uiState.isPickFromBg))
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.qr_extract_from_bg),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Checkbox(
                                checked = uiState.isPickFromBg,
                                onCheckedChange = { viewModel.onEvent(QrToolUiEvent.OnTogglePickFromBg(it)) }
                            )
                        }
                    }

                    // 选择主题入口 (带右箭头)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.onEvent(QrToolUiEvent.OnOpenThemePicker) }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.qr_select_theme),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // 3. 数据点样式与比例 (参考图一)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 数据点样式选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.qr_dot_style),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 方形
                        StyleOptionPill(
                            label = stringResource(R.string.qr_style_square),
                            icon = Icons.Rounded.Square,
                            isSelected = uiState.dotStyle == QrDotStyle.SQUARE,
                            activeColor = MaterialTheme.colorScheme.primary,
                            onClick = { viewModel.onEvent(QrToolUiEvent.OnDotStyleChange(QrDotStyle.SQUARE)) }
                        )
                        // 圆形
                        StyleOptionPill(
                            label = stringResource(R.string.qr_style_circle),
                            icon = Icons.Rounded.Circle,
                            isSelected = uiState.dotStyle == QrDotStyle.CIRCLE,
                            activeColor = MaterialTheme.colorScheme.primary,
                            onClick = { viewModel.onEvent(QrToolUiEvent.OnDotStyleChange(QrDotStyle.CIRCLE)) }
                        )
                    }
                }

                // 数据点比例滑块
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.qr_dot_scale),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(100.dp)
                    )
                    Slider(
                        value = uiState.dotScale,
                        onValueChange = { viewModel.onEvent(QrToolUiEvent.OnDotScaleChange(it)) },
                        valueRange = 0.3f..1.0f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                            inactiveTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // 4. Logo与背景图设置按钮组 (参考图一)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = { logoCropLauncher.launch(isSquare = true, target = "logo") },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.logoBitmap != null) "更换Logo" else stringResource(R.string.qr_btn_set_logo),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }

                FilledTonalButton(
                    onClick = { bgCropLauncher.launch(isSquare = true, target = "bg") },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.bgBitmap != null) "更换背景" else stringResource(R.string.qr_btn_set_bg),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }

            // 5. 背景图透明度滑块
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.qr_bg_alpha),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(100.dp)
                )
                Slider(
                    value = uiState.bgAlpha,
                    onValueChange = { viewModel.onEvent(QrToolUiEvent.OnBgAlphaChange(it)) },
                    valueRange = 0.0f..1.0f,
                    steps = 20,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                        inactiveTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // 6. 边框宽度滑块
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.qr_margin),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(100.dp)
                )
                Slider(
                    value = uiState.marginDp,
                    onValueChange = { viewModel.onEvent(QrToolUiEvent.OnMarginChange(it)) },
                    valueRange = 0f..32f,
                    steps = 16,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                        inactiveTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 底部主题选择抽屉 (参考图二)
        if (uiState.showThemePicker) {
            ThemePickerBottomSheet(
                onDismissRequest = { viewModel.onEvent(QrToolUiEvent.OnDismissThemePicker) },
                onThemeSelected = { preset -> viewModel.onEvent(QrToolUiEvent.OnThemeSelected(preset)) }
            )
        }

        // 颜色选择弹窗
        uiState.activeColorPicker?.let { target ->
            val (title, currentColor) = when (target) {
                ColorPickerTarget.DARK -> stringResource(R.string.qr_color_dark) to uiState.darkColor
                ColorPickerTarget.LIGHT -> stringResource(R.string.qr_color_light) to uiState.lightColor
                ColorPickerTarget.BACKGROUND -> stringResource(R.string.qr_color_bg) to uiState.backgroundColor
            }
            ColorPickerDialog(
                title = title,
                initialColor = currentColor,
                onDismissRequest = { viewModel.onEvent(QrToolUiEvent.OnDismissColorPicker) },
                onColorSelected = { selected ->
                    when (target) {
                        ColorPickerTarget.DARK -> viewModel.onEvent(QrToolUiEvent.OnDarkColorChange(selected))
                        ColorPickerTarget.LIGHT -> viewModel.onEvent(QrToolUiEvent.OnLightColorChange(selected))
                        ColorPickerTarget.BACKGROUND -> viewModel.onEvent(QrToolUiEvent.OnBackgroundColorChange(selected))
                    }
                }
            )
        }

        // 大图预览弹窗
        if (showPreview && uiState.qrBitmap != null) {
            ImagePreviewDialog(
                bitmap = uiState.qrBitmap,
                title = stringResource(R.string.tool_qr_code_name),
                albumFolder = AlbumFolders.QR_CODE,
                onDismissRequest = { showPreview = false }
            )
        }
    }
}

/**
 * 颜色配置药丸按钮 (圆形色块 + 文本)
 */
@Composable
private fun ColorCapsuleButton(
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (enabled) color else color.copy(alpha = 0.35f))
                    .border(0.5.dp, Color.Black.copy(alpha = if (enabled) 0.2f else 0.1f), CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

/**
 * 样式切换药丸 (方形/圆形)
 */
@Composable
private fun StyleOptionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
