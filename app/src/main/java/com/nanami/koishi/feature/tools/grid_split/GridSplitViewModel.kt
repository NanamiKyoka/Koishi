package com.nanami.koishi.feature.tools.grid_split

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.feature.tools.grid_split.engine.GridSplitEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class GridSplitViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GridSplitUiState())
    val uiState: StateFlow<GridSplitUiState> = _uiState.asStateFlow()

    fun onEvent(event: GridSplitUiEvent) {
        when (event) {
            is GridSplitUiEvent.OnImageSelected -> loadImage(event.uri)
            is GridSplitUiEvent.OnModeChanged -> _uiState.update { it.copy(mode = event.mode) }
            is GridSplitUiEvent.OnSquareGridNChanged -> _uiState.update { it.copy(squareGridN = event.n.coerceIn(2, 8)) }
            is GridSplitUiEvent.OnCustomRowsChanged -> _uiState.update { it.copy(customRows = event.rows.coerceIn(2, 8)) }
            is GridSplitUiEvent.OnCustomColsChanged -> _uiState.update { it.copy(customCols = event.cols.coerceIn(2, 8)) }
            is GridSplitUiEvent.OnClearImage -> _uiState.update {
                it.copy(
                    selectedUri = null,
                    previewBitmap = null,
                    imageWidth = 0,
                    imageHeight = 0
                )
            }
            is GridSplitUiEvent.OnExecuteCrop -> executeCropAndSave()
            is GridSplitUiEvent.OnDismissMessage -> _uiState.update { it.copy(userMessage = null) }
        }
    }

    private fun loadImage(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                // 1. 获取原图几何尺寸
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                }

                val rawW = boundsOptions.outWidth
                val rawH = boundsOptions.outHeight
                if (rawW <= 0 || rawH <= 0) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(isProcessing = false, userMessage = "无法读取有效图片格式")
                        }
                    }
                    return@launch
                }

                // 2. 计算预览采样率 (限制预览图在 2048px 内以保障流畅度与防 OOM)
                var sampleSize = 1
                val maxDim = max(rawW, rawH)
                while (maxDim / sampleSize > 2048) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val previewBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }

                withContext(Dispatchers.Main) {
                    if (previewBitmap != null) {
                        _uiState.update {
                            it.copy(
                                selectedUri = uri,
                                previewBitmap = previewBitmap,
                                imageWidth = rawW,
                                imageHeight = rawH,
                                isProcessing = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(isProcessing = false, userMessage = "解码图片失败")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(isProcessing = false, userMessage = "读取图片异常: ${e.message}")
                    }
                }
            }
        }
    }

    private fun executeCropAndSave() {
        val state = _uiState.value
        val uri = state.selectedUri ?: return
        if (state.isProcessing) return

        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isProcessing = true) }

            try {
                // 1. 获取原尺寸或高质量解码 Bitmap
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                }
                val rawW = boundsOptions.outWidth
                val rawH = boundsOptions.outHeight

                var sampleSize = 1
                val maxDim = max(rawW, rawH)
                // 超过 4096px 时适当降采样，防止多块切割时产生瞬时 OOM
                while (maxDim / sampleSize > 4096) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val fullBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }

                if (fullBitmap == null) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(isProcessing = false, userMessage = "无法读取图片进行切分")
                        }
                    }
                    return@launch
                }

                val actualW = fullBitmap.width
                val actualH = fullBitmap.height

                // 2. 根据所选模式计算切片几何坐标
                val slices = when (state.mode) {
                    GridSplitMode.SQUARE -> GridSplitEngine.calculateSquareGridSlices(
                        imageWidth = actualW,
                        imageHeight = actualH,
                        n = state.squareGridN
                    )
                    GridSplitMode.CUSTOM -> GridSplitEngine.calculateCustomGridSlices(
                        imageWidth = actualW,
                        imageHeight = actualH,
                        rows = state.customRows,
                        cols = state.customCols
                    )
                }

                val timestamp = System.currentTimeMillis()
                val totalCount = slices.size
                var savedCount = 0

                val resolver = context.contentResolver

                // 3. 逐块切割并流式保存至相册
                for ((index, slice) in slices.withIndex()) {
                    val subBitmap = Bitmap.createBitmap(
                        fullBitmap,
                        slice.x,
                        slice.y,
                        slice.width,
                        slice.height
                    )

                    val sequenceIndex = (index + 1).toString().padStart(2, '0')
                    val filename = "grid_${timestamp}_${sequenceIndex}.png"

                    var isSuccess = false
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val values = ContentValues().apply {
                                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Koishi")
                                put(MediaStore.Images.Media.IS_PENDING, 1)
                            }

                            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                            val itemUri = resolver.insert(collection, values)

                            if (itemUri != null) {
                                resolver.openOutputStream(itemUri)?.use { outStream ->
                                    subBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                                }
                                values.clear()
                                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                                resolver.update(itemUri, values, null, null)
                                isSuccess = true
                            }
                        } else {
                            val dir = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                "Koishi"
                            )
                            if (!dir.exists()) dir.mkdirs()
                            val destFile = File(dir, filename)
                            FileOutputStream(destFile).use { outStream ->
                                subBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                            }
                            val values = ContentValues().apply {
                                put(MediaStore.Images.Media.DATA, destFile.absolutePath)
                                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                            }
                            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                            isSuccess = true
                        }
                    } finally {
                        subBitmap.recycle()
                    }

                    if (isSuccess) {
                        savedCount++
                    }
                }

                fullBitmap.recycle()

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            userMessage = if (savedCount == totalCount) {
                                "已成功切分并保存 $savedCount 张图片至相册 (Pictures/Koishi)"
                            } else {
                                "已保存 $savedCount / $totalCount 张图片至相册"
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(isProcessing = false, userMessage = "切分保存失败: ${e.message}")
                    }
                }
            }
        }
    }
}
