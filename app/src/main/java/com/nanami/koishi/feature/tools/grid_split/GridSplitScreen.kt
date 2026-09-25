package com.nanami.koishi.feature.tools.grid_split

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Grid3x3
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.image.crop.rememberCropImageLauncher
import kotlin.math.min

@Composable
fun GridSplitRoute(
    viewModel: GridSplitViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(GridSplitUiEvent.OnDismissMessage)
        }
    }

    GridSplitScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridSplitScreen(
    state: GridSplitUiState,
    onEvent: (GridSplitUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 通用裁剪启动器 (切方格时强制 1:1 正方形裁剪，自定义切时自由裁剪)
    val cropLauncher = rememberCropImageLauncher { uri ->
        onEvent(GridSplitUiEvent.OnImageSelected(uri))
    }

    val launchImagePicker = {
        val isSquare = state.mode == GridSplitMode.SQUARE
        cropLauncher.launch(
            isSquare = isSquare,
            target = if (isSquare) "grid_square" else "grid_custom"
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.grid_split_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
                    if (state.previewBitmap != null) {
                        IconButton(
                            onClick = launchImagePicker
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = stringResource(R.string.grid_split_change_image)
                            )
                        }
                        IconButton(
                            onClick = { onEvent(GridSplitUiEvent.OnClearImage) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.grid_split_clear_image),
                                tint = MaterialTheme.colorScheme.error
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
        ) {
            // 1. 顶部模式切换 (MD3 Tab 切换)
            PrimaryTabRow(
                selectedTabIndex = if (state.mode == GridSplitMode.SQUARE) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Tab(
                    selected = state.mode == GridSplitMode.SQUARE,
                    onClick = { onEvent(GridSplitUiEvent.OnModeChanged(GridSplitMode.SQUARE)) },
                    text = { Text(stringResource(R.string.grid_split_tab_square), fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Rounded.Grid3x3, contentDescription = null) }
                )
                Tab(
                    selected = state.mode == GridSplitMode.CUSTOM,
                    onClick = { onEvent(GridSplitUiEvent.OnModeChanged(GridSplitMode.CUSTOM)) },
                    text = { Text(stringResource(R.string.grid_split_tab_custom), fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Rounded.ViewModule, contentDescription = null) }
                )
            }

            // 2. 中间图片与网格预览区域 (OverlayView)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.previewBitmap == null) {
                    // 未选择图片时的占位区域
                    GridPlaceholderCard(
                        onSelectImage = launchImagePicker
                    )
                } else {
                    // 已选图片：图片内容 + 网格虚线蒙层 (OverlayView)
                    GridImageWithOverlay(
                        state = state,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 3. 底部参数控制面板
            GridControlPanel(
                state = state,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    }
}

/**
 * 未选择图片时的占位卡片
 */
@Composable
private fun GridPlaceholderCard(
    onSelectImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.grid_split_empty_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.grid_split_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSelectImage,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.grid_split_select_image),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * 图片展示与网格虚线蒙层 (OverlayView)
 */
@Composable
private fun GridImageWithOverlay(
    state: GridSplitUiState,
    modifier: Modifier = Modifier
) {
    val bitmap = state.previewBitmap ?: return
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val maxW = maxWidth
        val maxH = maxHeight

        val (frameW, frameH, rows, cols) = when (state.mode) {
            GridSplitMode.SQUARE -> {
                // 切方格模式：严格保持 1:1 正方形
                val side = min(maxW.value, maxH.value).dp * 0.96f
                Quadruple(side, side, state.squareGridN, state.squareGridN)
            }
            GridSplitMode.CUSTOM -> {
                // 自定义切模式：自适应原图宽高比
                val imgAspect = if (state.imageHeight > 0) {
                    state.imageWidth.toFloat() / state.imageHeight.toFloat()
                } else 1.0f

                val availableAspect = maxW.value / maxH.value
                val (w, h) = if (availableAspect > imgAspect) {
                    val computedH = maxH.value * 0.96f
                    (computedH * imgAspect).dp to computedH.dp
                } else {
                    val computedW = maxW.value * 0.96f
                    computedW.dp to (computedW / imgAspect).dp
                }
                Quadruple(w, h, state.customRows, state.customCols)
            }
        }

        Box(
            modifier = Modifier
                .size(frameW, frameH)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            // 底图展示：切方格使用 ContentScale.Crop (居中截取最大正方形)，自定义切使用 ContentScale.FillBounds
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = if (state.mode == GridSplitMode.SQUARE) ContentScale.Crop else ContentScale.FillBounds
            )

            // 覆盖的网格虚线蒙层 (OverlayView)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                val primaryLineColor = Color.White
                val shadowLineColor = Color.Black.copy(alpha = 0.5f)

                // 绘制外边界高亮框
                drawRect(
                    color = shadowLineColor,
                    topLeft = Offset.Zero,
                    size = Size(canvasW, canvasH),
                    style = Stroke(width = 4.dp.toPx())
                )
                drawRect(
                    color = primaryLineColor,
                    topLeft = Offset.Zero,
                    size = Size(canvasW, canvasH),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 绘制水平分割虚线 (行数由参数严格决定)
                for (r in 1 until rows) {
                    val y = r * (canvasH / rows)
                    // 阴影线底层（确保在亮色背景上清晰可见）
                    drawLine(
                        color = shadowLineColor,
                        start = Offset(0f, y),
                        end = Offset(canvasW, y),
                        strokeWidth = 3.dp.toPx(),
                        pathEffect = pathEffect
                    )
                    // 主虚线顶层
                    drawLine(
                        color = primaryLineColor,
                        start = Offset(0f, y),
                        end = Offset(canvasW, y),
                        strokeWidth = 1.8.dp.toPx(),
                        pathEffect = pathEffect
                    )
                }

                // 绘制垂直分割虚线 (列数由参数严格决定)
                for (c in 1 until cols) {
                    val x = c * (canvasW / cols)
                    // 阴影线底层
                    drawLine(
                        color = shadowLineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, canvasH),
                        strokeWidth = 3.dp.toPx(),
                        pathEffect = pathEffect
                    )
                    // 主虚线顶层
                    drawLine(
                        color = primaryLineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, canvasH),
                        strokeWidth = 1.8.dp.toPx(),
                        pathEffect = pathEffect
                    )
                }
            }
        }
    }
}

/**
 * 底部控制面板
 */
@Composable
private fun GridControlPanel(
    state: GridSplitUiState,
    onEvent: (GridSplitUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            when (state.mode) {
                GridSplitMode.SQUARE -> {
                    // 切方格模式控制参数
                    SquareModeControls(
                        squareGridN = state.squareGridN,
                        totalSlices = state.totalSquareSlices,
                        onNChanged = { onEvent(GridSplitUiEvent.OnSquareGridNChanged(it)) }
                    )
                }
                GridSplitMode.CUSTOM -> {
                    // 自定义切模式控制参数
                    CustomModeControls(
                        rows = state.customRows,
                        cols = state.customCols,
                        totalSlices = state.totalCustomSlices,
                        onRowsChanged = { onEvent(GridSplitUiEvent.OnCustomRowsChanged(it)) },
                        onColsChanged = { onEvent(GridSplitUiEvent.OnCustomColsChanged(it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 底部常驻“裁剪”操作入口
            Button(
                onClick = { onEvent(GridSplitUiEvent.OnExecuteCrop) },
                enabled = state.previewBitmap != null && !state.isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.grid_split_processing),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Crop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.grid_split_btn_crop),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * 切方格控制区域
 */
@Composable
private fun SquareModeControls(
    squareGridN: Int,
    totalSlices: Int,
    onNChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.grid_split_square_n_label),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.grid_split_square_summary, squareGridN, totalSlices),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 微调步进组件 (选择 2 到 8)
            StepperRow(
                value = squareGridN,
                min = 2,
                max = 8,
                label = "${squareGridN}x${squareGridN}",
                onValueChange = onNChanged
            )
        }
    }
}

/**
 * 自定义切控制区域
 */
@Composable
private fun CustomModeControls(
    rows: Int,
    cols: Int,
    totalSlices: Int,
    onRowsChanged: (Int) -> Unit,
    onColsChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 行数量与列数量步进选择器
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 行数量
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.grid_split_custom_rows_label),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StepperRow(
                        value = rows,
                        min = 2,
                        max = 8,
                        label = "$rows 行",
                        onValueChange = onRowsChanged
                    )
                }
            }

            // 列数量
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.grid_split_custom_cols_label),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StepperRow(
                        value = cols,
                        min = 2,
                        max = 8,
                        label = "$cols 列",
                        onValueChange = onColsChanged
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 下方文本显示：如“2x2 切为 4 个图片”
        Text(
            text = stringResource(R.string.grid_split_custom_summary, rows, cols, totalSlices),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

/**
 * 通用微调步进器（已适配恋恋主题绿色主调）
 */
@Composable
private fun StepperRow(
    value: Int,
    min: Int,
    max: Int,
    label: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilledIconButton(
            onClick = { if (value > min) onValueChange(value - 1) },
            enabled = value > min,
            colors = buttonColors,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = "减少",
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        FilledIconButton(
            onClick = { if (value < max) onValueChange(value + 1) },
            enabled = value < max,
            colors = buttonColors,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "增加",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

