package com.nanami.koishi.core.image.preview

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 可在多个工具中复用的全屏手势放大/缩小图片预览弹窗
 *
 * 特性：
 * 1. 沉浸式暗黑全屏背景，支持点击遮罩/关闭按钮退出
 * 2. 支持多图左右滑动分页切换 (HorizontalPager)
 * 3. 单页双指手势缩放 (1.0x ~ 5.0x) 与平移 (Pan)
 * 4. 双击快速切换 1.0x 原始视图与 2.5x 局部放大视图
 * 5. 放大状态下禁用 Pager 滑动以优先响应大图平移，1.0x 时支持流畅滑页
 * 6. 支持直接传入 List<Bitmap>, List<Uri>, 单张 Bitmap 或 Uri
 * 7. 顶部显示页码指示器 (如 "3 / 10")，内置当前图片保存到相册功能
 */
@Composable
fun ImagePreviewDialog(
    images: List<Any>,
    initialIndex: Int = 0,
    title: String = "图片预览",
    onIndexChanged: ((Int) -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    if (images.isEmpty()) {
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val safeInitialIndex = initialIndex.coerceIn(0, images.size - 1)
    val pagerState = rememberPagerState(initialPage = safeInitialIndex) { images.size }

    // 监听页码切换，通知外部组件
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onIndexChanged?.invoke(page)
        }
    }

    // 记录当前活跃页的缩放倍率，当放大时禁用 Pager 滑动，防止手势冲突
    var currentPageScale by remember { mutableFloatStateOf(1f) }
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage) {
        currentPageScale = 1f
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f)),
            contentAlignment = Alignment.Center
        ) {
            // 水平滑动分页
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = images[page]
                ZoomableImagePage(
                    item = item,
                    isActive = page == pagerState.currentPage,
                    onScaleChanged = { scale ->
                        if (page == pagerState.currentPage) {
                            currentPageScale = scale
                        }
                    },
                    onToggleControls = {
                        controlsVisible = !controlsVisible
                    }
                )
            }

            // 顶部操作栏
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onDismissRequest,
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "关闭预览",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    val displayTitle = if (images.size > 1) {
                        "$title (${pagerState.currentPage + 1} / ${images.size})"
                    } else {
                        title
                    }

                    Text(
                        text = displayTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 保存当前显示的图片到相册
                    Surface(
                        onClick = {
                            val currentItem = images.getOrNull(pagerState.currentPage)
                            if (currentItem != null) {
                                coroutineScope.launch {
                                    val success = saveItemToGallery(context, currentItem)
                                    Toast.makeText(
                                        context,
                                        if (success) "已保存到相册" else "保存失败",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "保存到相册",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 底部缩放提示条 (放大时展示当前倍率)
            if (currentPageScale > 1.05f) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1fx", currentPageScale),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * 单图快速调用重载
 */
@Composable
fun ImagePreviewDialog(
    bitmap: Bitmap? = null,
    uri: Uri? = null,
    title: String = "图片预览",
    onDismissRequest: () -> Unit
) {
    val items = remember(bitmap, uri) {
        listOfNotNull(bitmap ?: uri)
    }
    ImagePreviewDialog(
        images = items,
        initialIndex = 0,
        title = title,
        onDismissRequest = onDismissRequest
    )
}

/**
 * 单张具备手势缩放、双击缩放和平移能力的页面
 */
@Composable
private fun ZoomableImagePage(
    item: Any,
    isActive: Boolean,
    onScaleChanged: (Float) -> Unit,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var loadedBitmap by remember(item) { mutableStateOf((item as? Bitmap)) }
    var isLoading by remember(item) { mutableStateOf(loadedBitmap == null && item is Uri) }

    LaunchedEffect(item) {
        if (loadedBitmap == null && item is Uri) {
            isLoading = true
            withContext(Dispatchers.IO) {
                loadedBitmap = decodeSampledBitmapFromUri(context, item)
                isLoading = false
            }
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var animationJob by remember { mutableStateOf<Job?>(null) }

    // 当页面变为非当前页或完全离屏时，重置缩放和偏移至初始状态
    LaunchedEffect(isActive) {
        if (!isActive) {
            animationJob?.cancel()
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            onScaleChanged(1f)
        }
    }

    // 动态计算在特定倍率下的真实可移动极值边界（基于容器尺寸与图片实际渲染尺寸，严禁写死数值）
    val calculateMaxOffsets: (Float) -> Pair<Float, Float> = { currentScale ->
        val bmp = loadedBitmap
        if (bmp == null || containerSize.width == 0 || containerSize.height == 0) {
            0f to 0f
        } else {
            val bmpW = bmp.width.toFloat()
            val bmpH = bmp.height.toFloat()
            val boxW = containerSize.width.toFloat()
            val boxH = containerSize.height.toFloat()

            val scaleFit = min(boxW / bmpW, boxH / bmpH)
            val fittedW = bmpW * scaleFit
            val fittedH = bmpH * scaleFit

            val scaledW = fittedW * currentScale
            val scaledH = fittedH * currentScale

            val maxOffsetX = max(0f, (scaledW - boxW) / 2f)
            val maxOffsetY = max(0f, (scaledH - boxH) / 2f)
            maxOffsetX to maxOffsetY
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            // 1. 分层手势：由 detectTapGestures 处理单击（切换操作栏）与双击（平滑动画过渡）
            .pointerInput(containerSize, loadedBitmap) {
                detectTapGestures(
                    onTap = {
                        onToggleControls()
                    },
                    onDoubleTap = { tapPos ->
                        animationJob?.cancel()
                        animationJob = coroutineScope.launch {
                            val currentScale = scale
                            val (maxOffX, maxOffY) = calculateMaxOffsets(2.5f)
                            scaleAnim.snapTo(scale)
                            offsetXAnim.snapTo(offsetX)
                            offsetYAnim.snapTo(offsetY)
                            if (currentScale > 1.05f) {
                                // 处于放大状态：双击平滑恢复到 1.0x 全局居中视图
                                launch {
                                    scaleAnim.animateTo(1f, tween(250)) {
                                        scale = value
                                        onScaleChanged(value)
                                    }
                                }
                                launch {
                                    offsetXAnim.animateTo(0f, tween(250)) {
                                        offsetX = value
                                    }
                                }
                                launch {
                                    offsetYAnim.animateTo(0f, tween(250)) {
                                        offsetY = value
                                    }
                                }
                            } else {
                                // 1.0x 状态：双击平滑过渡至 2.5x 并围绕触控点锚定
                                val targetScale = 2.5f
                                val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                val targetOffsetX = (-(tapPos.x - center.x) * (targetScale - 1f)).coerceIn(-maxOffX, maxOffX)
                                val targetOffsetY = (-(tapPos.y - center.y) * (targetScale - 1f)).coerceIn(-maxOffY, maxOffY)
                                launch {
                                    scaleAnim.animateTo(targetScale, tween(250)) {
                                        scale = value
                                        onScaleChanged(value)
                                    }
                                }
                                launch {
                                    offsetXAnim.animateTo(targetOffsetX, tween(250)) {
                                        offsetX = value
                                    }
                                }
                                launch {
                                    offsetYAnim.animateTo(targetOffsetY, tween(250)) {
                                        offsetY = value
                                    }
                                }
                            }
                        }
                    }
                )
            }
            // 2. 变换手势：处理双指缩放（围绕双指几何中心）与单指大图平移及边缘滑动切页透传
            .pointerInput(containerSize, loadedBitmap) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    animationJob?.cancel()

                    while (true) {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }
                        if (activePointers.isEmpty()) break

                        val currentScale = scale
                        val currentOffsetX = offsetX
                        val currentOffsetY = offsetY

                        if (activePointers.size >= 2) {
                            // 双指手势：围绕双指几何中心锚定缩放与平移
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)

                            val newScale = (currentScale * zoomChange).coerceIn(0.7f, 5f)
                            val (newMaxX, newMaxY) = calculateMaxOffsets(newScale)

                            val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                            val centroidRel = centroid - center

                            val scaleRatio = if (currentScale > 0f) newScale / currentScale else 1f
                            val newX = (currentOffsetX * scaleRatio + centroidRel.x * (1f - scaleRatio) + panChange.x).coerceIn(-newMaxX, newMaxX)
                            val newY = (currentOffsetY * scaleRatio + centroidRel.y * (1f - scaleRatio) + panChange.y).coerceIn(-newMaxY, newMaxY)

                            scale = newScale
                            offsetX = newX
                            offsetY = newY
                            onScaleChanged(newScale)

                            // 消费双指事件，保证多指缩放不被外层干扰
                            event.changes.forEach { it.consume() }
                        } else if (activePointers.size == 1) {
                            // 单指手势
                            val pointer = activePointers[0]
                            val panChange = pointer.position - pointer.previousPosition

                            if (currentScale <= 1.05f) {
                                // 1.0x 视图：不消费任何单指滑动，让外层 HorizontalPager 自由翻页
                            } else {
                                // 放大状态：动态判断是否达到水平/垂直真实可移动边缘
                                val (maxX, maxY) = calculateMaxOffsets(currentScale)

                                var canPanX = false
                                if (panChange.x > 0f) {
                                    // 向右平移：尚未到达最左边缘（offset.x < maxX）
                                    canPanX = currentOffsetX < maxX - 0.5f
                                } else if (panChange.x < 0f) {
                                    // 向左平移：尚未到达最右边缘（offset.x > -maxX）
                                    canPanX = currentOffsetX > -maxX + 0.5f
                                }

                                var canPanY = false
                                if (panChange.y > 0f) {
                                    canPanY = currentOffsetY < maxY - 0.5f
                                } else if (panChange.y < 0f) {
                                    canPanY = currentOffsetY > -maxY + 0.5f
                                }

                                if (canPanX || canPanY) {
                                    val newX = if (canPanX) (currentOffsetX + panChange.x).coerceIn(-maxX, maxX) else currentOffsetX
                                    val newY = if (canPanY) (currentOffsetY + panChange.y).coerceIn(-maxY, maxY) else currentOffsetY

                                    scale = currentScale
                                    offsetX = newX
                                    offsetY = newY

                                    // 如果在水平可移动范围内，或主要是垂直拖拽，消费位移
                                    if (canPanX || abs(panChange.y) > abs(panChange.x)) {
                                        pointer.consume()
                                    }
                                } else {
                                    // 水平平移已达到极值边界且用户继续往边缘方向滑动：
                                    // 绝不消费该水平位移，自然透传给外层 HorizontalPager 进行切页！
                                }
                            }
                        }
                    }

                    // 手指离开屏幕后，若缩放倍率小于 1.0x 或因平移略微越界，平滑回弹
                    if (scale < 1f) {
                        animationJob = coroutineScope.launch {
                            scaleAnim.snapTo(scale)
                            offsetXAnim.snapTo(offsetX)
                            offsetYAnim.snapTo(offsetY)
                            launch {
                                scaleAnim.animateTo(1f, tween(200)) {
                                    scale = value
                                    onScaleChanged(value)
                                }
                            }
                            launch {
                                offsetXAnim.animateTo(0f, tween(200)) {
                                    offsetX = value
                                }
                            }
                            launch {
                                offsetYAnim.animateTo(0f, tween(200)) {
                                    offsetY = value
                                }
                            }
                        }
                    } else {
                        val (maxX, maxY) = calculateMaxOffsets(scale)
                        val clampedX = offsetX.coerceIn(-maxX, maxX)
                        val clampedY = offsetY.coerceIn(-maxY, maxY)
                        if (clampedX != offsetX || clampedY != offsetY) {
                            animationJob = coroutineScope.launch {
                                offsetXAnim.snapTo(offsetX)
                                offsetYAnim.snapTo(offsetY)
                                launch {
                                    offsetXAnim.animateTo(clampedX, tween(150)) {
                                        offsetX = value
                                    }
                                }
                                launch {
                                    offsetYAnim.animateTo(clampedY, tween(150)) {
                                        offsetY = value
                                    }
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
            contentAlignment = Alignment.Center
        ) {
            if (loadedBitmap != null) {
                Image(
                    bitmap = loadedBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Text(
                    text = "无法加载图片",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/**
 * 依据屏幕物理分辨率按需降采样解码大图 Uri，有效避免 OOM 崩溃
 */
private fun decodeSampledBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val displayMetrics = context.resources.displayMetrics
        val reqWidth = displayMetrics.widthPixels
        val reqHeight = displayMetrics.heightPixels

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val rawWidth = options.outWidth
        val rawHeight = options.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return null

        var inSampleSize = 1
        // 允许保留至多 2 倍屏幕分辨率，兼顾双击缩放清晰度与内存开销
        if (rawWidth > reqWidth * 2 || rawHeight > reqHeight * 2) {
            val halfWidth = rawWidth / 2
            val halfHeight = rawHeight / 2
            while ((halfWidth / inSampleSize) >= reqWidth || (halfHeight / inSampleSize) >= reqHeight) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 通用图片保存至系统相册 (适配 Android 10+ / API 29+ Scoped Storage 与 MediaStore IS_PENDING 机制)
 */
private suspend fun saveItemToGallery(context: Context, item: Any): Boolean = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val mimeType: String
        val extension: String
        if (item is Uri) {
            val type = resolver.getType(item)
            mimeType = if (!type.isNullOrBlank() && type.startsWith("image/")) type else "image/png"
            extension = when (mimeType) {
                "image/jpeg", "image/jpg" -> "jpg"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                else -> "png"
            }
        } else {
            mimeType = "image/png"
            extension = "png"
        }

        val filename = "IMG_$timeStamp.$extension"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Koishi")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val targetUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false

        val success = try {
            resolver.openOutputStream(targetUri)?.use { os ->
                when (item) {
                    is Uri -> {
                        // 若输入源已是 Uri，直接通过流转存，避免解码为 Bitmap 再压缩导致的内存浪费与画质损耗
                        resolver.openInputStream(item)?.use { input ->
                            input.copyTo(os)
                            true
                        } ?: false
                    }
                    is Bitmap -> {
                        item.compress(Bitmap.CompressFormat.PNG, 100, os)
                        true
                    }
                    else -> false
                }
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

        if (success) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(targetUri, values, null, null)
            true
        } else {
            resolver.delete(targetUri, null, null)
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
