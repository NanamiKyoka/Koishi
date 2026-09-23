package com.nanami.koishi.core.image.preview

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

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
                userScrollEnabled = currentPageScale <= 1.05f,
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
                try {
                    context.contentResolver.openInputStream(item)?.use { stream ->
                        loadedBitmap = BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // 当页面变为非当前页时，平滑重置缩放和偏移
    LaunchedEffect(isActive) {
        if (!isActive && scale != 1f) {
            scale = 1f
            offset = Offset.Zero
            onScaleChanged(1f)
        }
    }

    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(scale, isActive) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val startTime = System.currentTimeMillis()
                    var isMoved = false

                    if (scale > 1.05f) {
                        // 处于放大状态：由当前页面完全消费拖拽与双指手势
                        var prevCenter = firstDown.position
                        var prevDistance = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }
                            if (activePointers.isEmpty()) break

                            if (activePointers.size >= 2) {
                                val p0 = activePointers[0].position
                                val p1 = activePointers[1].position
                                val currentDistance = hypot(p0.x - p1.x, p0.y - p1.y)

                                if (prevDistance > 0f) {
                                    val zoom = currentDistance / prevDistance
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                    scale = newScale
                                    onScaleChanged(newScale)
                                }
                                val currentCenter = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                                val pan = currentCenter - prevCenter
                                val maxOffsetX = (scale - 1f) * 600f
                                val maxOffsetY = (scale - 1f) * 800f
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                                prevCenter = currentCenter
                                prevDistance = currentDistance
                                event.changes.forEach { it.consume() }
                            } else if (activePointers.size == 1) {
                                val p0 = activePointers[0]
                                val pan = p0.position - prevCenter
                                if (hypot(pan.x, pan.y) > 3f) {
                                    isMoved = true
                                }
                                val maxOffsetX = (scale - 1f) * 600f
                                val maxOffsetY = (scale - 1f) * 800f
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                                prevCenter = p0.position
                                prevDistance = 0f
                                p0.consume()
                            }
                        }

                        // 手势结束：若未拖拽且点击时间短，判定为单击切换操作栏或双击还原
                        if (!isMoved && (System.currentTimeMillis() - startTime) < 300) {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 350) {
                                scale = 1f
                                offset = Offset.Zero
                                onScaleChanged(1f)
                                lastTapTime = 0L
                            } else {
                                lastTapTime = now
                                coroutineScope.launch {
                                    delay(350)
                                    if (lastTapTime == now) {
                                        onToggleControls()
                                    }
                                }
                            }
                        }
                    } else {
                        // scale <= 1.05f (正常全屏视图)：绝不消费单指滑动，让 HorizontalPager 流畅翻页！
                        var prevDistance = 0f
                        var isPinching = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }
                            if (activePointers.isEmpty()) break

                            if (activePointers.size >= 2) {
                                // 两个手指触控：判定为 Pinch 缩放
                                isPinching = true
                                val p0 = activePointers[0].position
                                val p1 = activePointers[1].position
                                val currentDistance = hypot(p0.x - p1.x, p0.y - p1.y)

                                if (prevDistance > 0f) {
                                    val zoom = currentDistance / prevDistance
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                    scale = newScale
                                    onScaleChanged(newScale)
                                }
                                prevDistance = currentDistance
                                event.changes.forEach { it.consume() }
                            } else {
                                val p0 = activePointers[0]
                                if (hypot(p0.position.x - firstDown.position.x, p0.position.y - firstDown.position.y) > 10f) {
                                    isMoved = true
                                }
                                // 单指滑动不调用 consume()，HorizontalPager 接收并执行页面平移翻页
                            }
                        }

                        // 手势结束：若未缩放且未滑动，判定为单击切换操作栏或双击放大
                        if (!isPinching && !isMoved && (System.currentTimeMillis() - startTime) < 300) {
                            val now = System.currentTimeMillis()
                            val tapPos = firstDown.position
                            if (now - lastTapTime < 350 && hypot(tapPos.x - lastTapPos.x, tapPos.y - lastTapPos.y) < 80f) {
                                scale = 2.5f
                                onScaleChanged(2.5f)
                                offset = Offset(
                                    x = (size.width / 2f - tapPos.x).coerceIn(-400f, 400f),
                                    y = (size.height / 2f - tapPos.y).coerceIn(-600f, 600f)
                                )
                                lastTapTime = 0L
                            } else {
                                lastTapTime = now
                                lastTapPos = tapPos
                                coroutineScope.launch {
                                    delay(350)
                                    if (lastTapTime == now) {
                                        onToggleControls()
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
                    translationX = offset.x
                    translationY = offset.y
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
 * 通用图片保存至系统相册 (支持 Bitmap 与 Uri)
 */
private suspend fun saveItemToGallery(context: Context, item: Any): Boolean = withContext(Dispatchers.IO) {
    try {
        val bitmapToSave: Bitmap = when (item) {
            is Bitmap -> item
            is Uri -> {
                context.contentResolver.openInputStream(item)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            else -> null
        } ?: return@withContext false

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "IMG_$timeStamp.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Koishi")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
            resolver.openOutputStream(uri)?.use { os ->
                bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, os)
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Koishi")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, filename)
            FileOutputStream(file).use { os ->
                bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, os)
            }
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
