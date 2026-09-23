package com.nanami.koishi.feature.tools.mirage_tank

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.PillShape
import com.nanami.koishi.core.designsystem.ToolCardShape
import com.nanami.koishi.feature.tools.mirage_tank.engine.MirageTankMode
import java.util.Locale

@Composable
fun MirageTankRoute(
    viewModel: MirageTankViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(MirageTankUiEvent.OnDismissMessage)
        }
    }

    MirageTankScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirageTankScreen(
    state: MirageTankUiState,
    onEvent: (MirageTankUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onEvent(MirageTankUiEvent.OnFrontImageSelected(it)) }
    }

    val backPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onEvent(MirageTankUiEvent.OnBackImageSelected(it)) }
    }

    if (state.isHelpDialogOpen) {
        MirageTankHelpDialog(onDismiss = { onEvent(MirageTankUiEvent.OnToggleHelpDialog(false)) })
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.mirage_tank_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                actions = {
                    if (state.frontImage != null || state.backImage != null) {
                        IconButton(onClick = { onEvent(MirageTankUiEvent.OnSwapImages) }) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = stringResource(R.string.mirage_tank_swap_images)
                            )
                        }
                    }
                    TextButton(onClick = { onEvent(MirageTankUiEvent.OnToggleHelpDialog(true)) }) {
                        Text(
                            text = stringResource(R.string.help),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 表图与里图选取卡片区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImageSourceCard(
                    title = stringResource(R.string.mirage_tank_front_image),
                    subTitle = stringResource(R.string.mirage_tank_front_image_desc),
                    badgeColor = MaterialTheme.colorScheme.primary,
                    imageItem = state.frontImage,
                    onPick = {
                        frontPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onClear = { onEvent(MirageTankUiEvent.OnClearFrontImage) },
                    modifier = Modifier.weight(1f)
                )

                ImageSourceCard(
                    title = stringResource(R.string.mirage_tank_back_image),
                    subTitle = stringResource(R.string.mirage_tank_back_image_desc),
                    badgeColor = MaterialTheme.colorScheme.tertiary,
                    imageItem = state.backImage,
                    onPick = {
                        backPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onClear = { onEvent(MirageTankUiEvent.OnClearBackImage) },
                    modifier = Modifier.weight(1f)
                )
            }

            // 2. 模式与调参控制区
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ToolCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.mirage_tank_mode_and_params),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { onEvent(MirageTankUiEvent.OnResetParams) }) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.mirage_tank_reset_params))
                        }
                    }

                    // 色彩模式切换 Tab
                    PrimaryTabRow(
                        selectedTabIndex = if (state.params.mode == MirageTankMode.GRAYSCALE) 0 else 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PillShape)
                    ) {
                        Tab(
                            selected = state.params.mode == MirageTankMode.GRAYSCALE,
                            onClick = { onEvent(MirageTankUiEvent.OnModeChanged(MirageTankMode.GRAYSCALE)) },
                            text = { Text(stringResource(R.string.mirage_tank_mode_grayscale)) }
                        )
                        Tab(
                            selected = state.params.mode == MirageTankMode.COLOR,
                            onClick = { onEvent(MirageTankUiEvent.OnModeChanged(MirageTankMode.COLOR)) },
                            text = { Text(stringResource(R.string.mirage_tank_mode_color)) }
                        )
                    }

                    // 参数滑块：表图亮度与对比度
                    ParameterSlider(
                        label = stringResource(R.string.mirage_tank_front_lightness),
                        value = state.params.frontLightness,
                        range = 0.5f..1.5f,
                        onValueChange = { onEvent(MirageTankUiEvent.OnFrontLightnessChanged(it)) }
                    )

                    ParameterSlider(
                        label = stringResource(R.string.mirage_tank_front_contrast),
                        value = state.params.frontContrast,
                        range = 0.5f..1.5f,
                        onValueChange = { onEvent(MirageTankUiEvent.OnFrontContrastChanged(it)) }
                    )

                    // 参数滑块：里图亮度与对比度
                    ParameterSlider(
                        label = stringResource(R.string.mirage_tank_back_lightness),
                        value = state.params.backLightness,
                        range = 0.5f..1.5f,
                        onValueChange = { onEvent(MirageTankUiEvent.OnBackLightnessChanged(it)) }
                    )

                    ParameterSlider(
                        label = stringResource(R.string.mirage_tank_back_contrast),
                        value = state.params.backContrast,
                        range = 0.5f..1.5f,
                        onValueChange = { onEvent(MirageTankUiEvent.OnBackContrastChanged(it)) }
                    )

                    // 棋盘格调和开关
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.mirage_tank_checkerboard),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.mirage_tank_checkerboard_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.params.enableCheckerboard,
                            onCheckedChange = { onEvent(MirageTankUiEvent.OnCheckerboardToggled(it)) }
                        )
                    }

                    AnimatedVisibility(visible = state.params.enableCheckerboard) {
                        ParameterSlider(
                            label = stringResource(R.string.mirage_tank_checkerboard_strength),
                            value = state.params.checkerboardStrength,
                            range = 0.05f..0.4f,
                            onValueChange = { onEvent(MirageTankUiEvent.OnCheckerboardStrengthChanged(it)) }
                        )
                    }
                }
            }

            // 3. 效果对比预览区
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ToolCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.mirage_tank_preview_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 预览背景模式切换器 (白底 / 黑底 / 无极)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PreviewModeChip(
                            title = stringResource(R.string.mirage_tank_preview_white),
                            icon = Icons.Rounded.LightMode,
                            selected = state.previewBackgroundMode == PreviewBackgroundMode.WHITE,
                            onClick = { onEvent(MirageTankUiEvent.OnPreviewBackgroundModeChanged(PreviewBackgroundMode.WHITE)) },
                            modifier = Modifier.weight(1f)
                        )
                        PreviewModeChip(
                            title = stringResource(R.string.mirage_tank_preview_black),
                            icon = Icons.Rounded.DarkMode,
                            selected = state.previewBackgroundMode == PreviewBackgroundMode.BLACK,
                            onClick = { onEvent(MirageTankUiEvent.OnPreviewBackgroundModeChanged(PreviewBackgroundMode.BLACK)) },
                            modifier = Modifier.weight(1f)
                        )
                        PreviewModeChip(
                            title = stringResource(R.string.mirage_tank_preview_custom),
                            icon = Icons.Rounded.Contrast,
                            selected = state.previewBackgroundMode == PreviewBackgroundMode.CUSTOM,
                            onClick = { onEvent(MirageTankUiEvent.OnPreviewBackgroundModeChanged(PreviewBackgroundMode.CUSTOM)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 无极调节滑块
                    AnimatedVisibility(visible = state.previewBackgroundMode == PreviewBackgroundMode.CUSTOM) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.mirage_tank_bg_ratio_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.US, "%d%%", (state.customBackgroundRatio * 100).toInt()),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = state.customBackgroundRatio,
                                onValueChange = { onEvent(MirageTankUiEvent.OnCustomBackgroundRatioChanged(it)) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    // 动态背景渲染容器
                    val targetBgColor = if (state.previewBackgroundMode == PreviewBackgroundMode.WHITE) {
                        Color.White
                    } else if (state.previewBackgroundMode == PreviewBackgroundMode.BLACK) {
                        Color.Black
                    } else {
                        val ratio = state.customBackgroundRatio
                        Color(red = ratio, green = ratio, blue = ratio, alpha = 1f)
                    }

                    val animatedBgColor by animateColorAsState(
                        targetValue = targetBgColor,
                        animationSpec = tween(durationMillis = 200),
                        label = "preview_bg_anim"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(animatedBgColor)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val resultBitmap = state.resultBitmap
                        if (resultBitmap != null) {
                            Image(
                                bitmap = resultBitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.mirage_tank_preview_title),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = if (targetBgColor.red < 0.5f) Color.LightGray else Color.DarkGray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (!state.hasBothImages) {
                                        stringResource(R.string.mirage_tank_hint_pick_both)
                                    } else {
                                        stringResource(R.string.mirage_tank_hint_ready)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (targetBgColor.red < 0.5f) Color.LightGray else Color.DarkGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (state.isProcessing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // 4. 操作按钮区
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onEvent(MirageTankUiEvent.OnGenerateTank) },
                    enabled = state.hasBothImages && !state.isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = PillShape
                ) {
                    Icon(imageVector = Icons.Rounded.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.mirage_tank_generate))
                }

                Button(
                    onClick = { onEvent(MirageTankUiEvent.OnSaveTank) },
                    enabled = state.canSave,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Rounded.SaveAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.mirage_tank_save_lossless))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageSourceCard(
    title: String,
    subTitle: String,
    badgeColor: Color,
    imageItem: MirageTankImageItem?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = ToolCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
                if (imageItem != null) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.grid_split_clear_image),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = subTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onPick() },
                contentAlignment = Alignment.Center
            ) {
                if (imageItem != null) {
                    Image(
                        bitmap = imageItem.bitmap.asImageBitmap(),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.mirage_tank_tap_to_pick),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (imageItem != null) {
                Text(
                    text = "${imageItem.width}×${imageItem.height}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = String.format(Locale.US, "%.2fx", value),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun PreviewModeChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = modifier.height(36.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            shape = PillShape,
            modifier = modifier.height(36.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MirageTankHelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.mirage_tank_help_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.mirage_tank_help_p1),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.mirage_tank_help_p2),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.mirage_tank_help_p3),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}
