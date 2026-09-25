package com.nanami.koishi.feature.tools.image_obfuscation

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.core.util.AlbumFolders
import com.nanami.koishi.feature.tools.image_obfuscation.engine.ImageObfuscator
import com.nanami.koishi.feature.tools.image_obfuscation.engine.ObfuscationAction
import com.nanami.koishi.feature.tools.image_obfuscation.engine.ObfuscationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ImageObfuscationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ImageObfuscationUiState())
    val uiState: StateFlow<ImageObfuscationUiState> = _uiState.asStateFlow()

    override fun onCleared() {
        super.onCleared()
        try {
            _uiState.value.images.forEach {
                it.bitmap.recycle()
                it.thumbnail.recycle()
            }
        } catch (_: Throwable) {}
    }

    private fun getString(resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }

    fun onEvent(event: ImageObfuscationUiEvent) {
        when (event) {
            is ImageObfuscationUiEvent.OnImagesSelected -> loadImages(event.uris)
            is ImageObfuscationUiEvent.OnSelectImageIndex -> selectIndex(event.index)
            is ImageObfuscationUiEvent.OnModeSelected -> {
                _uiState.update { current ->
                    val newKey = if (event.mode == ObfuscationMode.TOMATO_GILBERT) {
                        ObfuscationMode.TOMATO_GILBERT.defaultKey
                    } else {
                        if (current.mode == ObfuscationMode.TOMATO_GILBERT || current.key == "1" || current.key.isBlank()) {
                            event.mode.defaultKey
                        } else {
                            current.key
                        }
                    }
                    current.copy(
                        mode = event.mode,
                        key = newKey
                    )
                }
            }
            is ImageObfuscationUiEvent.OnKeyChanged -> {
                _uiState.update { it.copy(key = event.key) }
            }
            is ImageObfuscationUiEvent.OnObfuscateCurrent -> processSingle(ObfuscationAction.OBFUSCATE)
            is ImageObfuscationUiEvent.OnDeobfuscateCurrent -> processSingle(ObfuscationAction.DEOBFUSCATE)
            is ImageObfuscationUiEvent.OnRemoveCurrent -> removeCurrent()
            is ImageObfuscationUiEvent.OnRestoreCurrent -> restoreCurrent()
            is ImageObfuscationUiEvent.OnSaveCurrent -> saveSingle()

            is ImageObfuscationUiEvent.OnObfuscateAll -> processBatch(ObfuscationAction.OBFUSCATE)
            is ImageObfuscationUiEvent.OnDeobfuscateAll -> processBatch(ObfuscationAction.DEOBFUSCATE)
            is ImageObfuscationUiEvent.OnRemoveAll -> removeAll()
            is ImageObfuscationUiEvent.OnRestoreAll -> restoreAll()
            is ImageObfuscationUiEvent.OnSaveAll -> saveBatch()

            is ImageObfuscationUiEvent.OnRotateCurrent -> rotateCurrent()
            is ImageObfuscationUiEvent.OnToggleHelpDialog -> {
                _uiState.update { it.copy(isHelpDialogOpen = event.open) }
            }
            is ImageObfuscationUiEvent.OnDismissMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
        }
    }

    private fun selectIndex(index: Int) {
        val cur = _uiState.value
        val newIndex = index.coerceIn(0, (cur.images.size - 1).coerceAtLeast(0))
        _uiState.update { it.copy(selectedIndex = newIndex) }
    }

    private fun loadImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingProgress = 0f,
                    userMessage = getString(R.string.msg_loading_images, uris.size)
                )
            }

            val context = getApplication<Application>()
            val loadedItems = mutableListOf<ObfuscatedImageItem>()
            val total = uris.size

            withContext(Dispatchers.IO) {
                for ((idx, uri) in uris.withIndex()) {
                    try {
                        val filename = queryFilename(uri)
                        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        } ?: continue

                        val w = bitmap.width
                        val h = bitmap.height
                        val thumbnail = extractThumbnail(bitmap)

                        loadedItems.add(
                            ObfuscatedImageItem(
                                id = UUID.randomUUID().toString(),
                                uri = uri,
                                filename = filename,
                                bitmap = bitmap,
                                thumbnail = thumbnail,
                                width = w,
                                height = h
                            )
                        )
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                    _uiState.update { it.copy(processingProgress = (idx + 1).toFloat() / total) }
                }
            }

            val newImages = _uiState.value.images + loadedItems
            val selectedIdx = if (_uiState.value.images.isEmpty() && newImages.isNotEmpty()) 0 else _uiState.value.selectedIndex

            _uiState.update { current ->
                current.copy(
                    images = newImages,
                    selectedIndex = selectedIdx,
                    isProcessing = false,
                    userMessage = getString(R.string.msg_loaded_images, loadedItems.size)
                )
            }
        }
    }

    private fun validateKey(mode: ObfuscationMode, key: String): Boolean {
        if (!mode.requiresKey) return true
        return when (mode) {
            ObfuscationMode.TOMATO_GILBERT -> {
                val num = key.toDoubleOrNull() ?: return false
                num > 0.0 && num <= 1.618
            }
            ObfuscationMode.PIC_ENCRYPT_ROW,
            ObfuscationMode.PIC_ENCRYPT_ROW_AND_COLUMN -> {
                val num = key.toDoubleOrNull() ?: return false
                num > 0.0 && num < 1.0
            }
            else -> key.isNotBlank()
        }
    }

    private fun processSingle(action: ObfuscationAction) {
        val state = _uiState.value
        val item = state.currentItem ?: run {
            _uiState.update { it.copy(userMessage = getString(R.string.msg_select_images_first)) }
            return
        }
        if (!validateKey(state.mode, state.key)) {
            val msg = if (state.mode.isNumericKey) {
                getString(R.string.msg_invalid_decimal_key)
            } else {
                getString(R.string.msg_invalid_text_key)
            }
            _uiState.update { it.copy(userMessage = msg) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val (resultBitmap, newThumb) = withContext(Dispatchers.Default) {
                    val outputBitmap = ImageObfuscator.process(
                        source = item.bitmap,
                        mode = state.mode,
                        action = action,
                        key = state.key
                    )
                    val thumb = extractThumbnail(outputBitmap)
                    outputBitmap to thumb
                }

                val updatedItem = item.copy(
                    bitmap = resultBitmap,
                    thumbnail = newThumb,
                    width = resultBitmap.width,
                    height = resultBitmap.height
                )

                _uiState.update { cur ->
                    val newImages = cur.images.toMutableList().apply {
                        this[cur.selectedIndex] = updatedItem
                    }
                    cur.copy(
                        images = newImages,
                        isProcessing = false,
                        userMessage = getString(R.string.msg_action_completed, getString(action.titleRes))
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = if (t is OutOfMemoryError) getString(R.string.msg_oom_error) else getString(R.string.msg_process_failed, t.localizedMessage ?: "")
                    )
                }
            }
        }
    }

    private fun processBatch(action: ObfuscationAction) {
        val state = _uiState.value
        if (state.images.isEmpty()) {
            _uiState.update { it.copy(userMessage = getString(R.string.msg_select_images_first)) }
            return
        }
        if (!validateKey(state.mode, state.key)) {
            val msg = if (state.mode.isNumericKey) {
                getString(R.string.msg_invalid_decimal_key)
            } else {
                getString(R.string.msg_invalid_text_key)
            }
            _uiState.update { it.copy(userMessage = msg) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingProgress = 0f) }
            try {
                val updatedList = mutableListOf<ObfuscatedImageItem>()
                val total = state.images.size

                withContext(Dispatchers.Default) {
                    for ((idx, item) in state.images.withIndex()) {
                        try {
                            val outputBitmap = ImageObfuscator.process(
                                source = item.bitmap,
                                mode = state.mode,
                                action = action,
                                key = state.key
                            )
                            val thumb = extractThumbnail(outputBitmap)
                            updatedList.add(
                                item.copy(
                                    bitmap = outputBitmap,
                                    thumbnail = thumb,
                                    width = outputBitmap.width,
                                    height = outputBitmap.height
                                )
                            )
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            updatedList.add(item)
                        }

                        _uiState.update { it.copy(processingProgress = (idx + 1).toFloat() / total) }
                    }
                }

                _uiState.update {
                    it.copy(
                        images = updatedList,
                        isProcessing = false,
                        userMessage = getString(R.string.msg_all_action_completed, getString(action.titleRes))
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = getString(R.string.msg_batch_failed, t.localizedMessage ?: "")
                    )
                }
            }
        }
    }

    private fun removeCurrent() {
        val cur = _uiState.value
        if (cur.images.isEmpty()) return
        val removed = cur.images[cur.selectedIndex]
        try {
            removed.bitmap.recycle()
            removed.thumbnail.recycle()
        } catch (_: Throwable) {}

        val newImages = cur.images.toMutableList().apply {
            removeAt(cur.selectedIndex)
        }
        val newIndex = if (newImages.isEmpty()) 0 else cur.selectedIndex.coerceIn(0, newImages.size - 1)

        _uiState.update {
            it.copy(
                images = newImages,
                selectedIndex = newIndex,
                userMessage = getString(R.string.msg_removed_current)
            )
        }
    }

    private fun removeAll() {
        val cur = _uiState.value
        for (item in cur.images) {
            try {
                item.bitmap.recycle()
                item.thumbnail.recycle()
            } catch (_: Throwable) {}
        }
        _uiState.update {
            it.copy(images = emptyList(), selectedIndex = 0, previewBitmap = null, userMessage = getString(R.string.msg_removed_all))
        }
    }

    private fun restoreCurrent() {
        val cur = _uiState.value
        val item = cur.currentItem ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val restoredItem = withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                val bitmap = context.contentResolver.openInputStream(item.uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: return@withContext null
                val thumb = extractThumbnail(bitmap)
                item.copy(bitmap = bitmap, thumbnail = thumb, width = bitmap.width, height = bitmap.height)
            }

            if (restoredItem != null) {
                item.bitmap.recycle()
                item.thumbnail.recycle()
                _uiState.update {
                    val list = it.images.toMutableList().apply {
                        this[it.selectedIndex] = restoredItem
                    }
                    it.copy(images = list, isProcessing = false, userMessage = getString(R.string.msg_restored_current))
                }
            } else {
                _uiState.update { it.copy(isProcessing = false, userMessage = getString(R.string.file_not_found)) }
            }
        }
    }

    private fun restoreAll() {
        val cur = _uiState.value
        if (cur.images.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val context = getApplication<Application>()
            val restoredList = withContext(Dispatchers.IO) {
                cur.images.map { item ->
                    try {
                        val bitmap = context.contentResolver.openInputStream(item.uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                        if (bitmap != null) {
                            val thumb = extractThumbnail(bitmap)
                            item.bitmap.recycle()
                            item.thumbnail.recycle()
                            item.copy(bitmap = bitmap, thumbnail = thumb, width = bitmap.width, height = bitmap.height)
                        } else {
                            item
                        }
                    } catch (_: Throwable) {
                        item
                    }
                }
            }

            _uiState.update {
                it.copy(images = restoredList, isProcessing = false, userMessage = getString(R.string.msg_restored_all))
            }
        }
    }

    private fun rotateCurrent() {
        val cur = _uiState.value
        val item = cur.currentItem ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val (rotatedBitmap, newThumb) = withContext(Dispatchers.Default) {
                val matrix = Matrix().apply { postRotate(90f) }
                val rotated = Bitmap.createBitmap(item.bitmap, 0, 0, item.bitmap.width, item.bitmap.height, matrix, true)
                val thumb = extractThumbnail(rotated)
                rotated to thumb
            }

            item.bitmap.recycle()
            item.thumbnail.recycle()
            val newItem = item.copy(bitmap = rotatedBitmap, thumbnail = newThumb, width = rotatedBitmap.width, height = rotatedBitmap.height)
            _uiState.update {
                val list = it.images.toMutableList().apply {
                    this[it.selectedIndex] = newItem
                }
                it.copy(images = list, isProcessing = false)
            }
        }
    }

    private fun saveSingle() {
        val cur = _uiState.value
        val item = cur.currentItem ?: run {
            _uiState.update { it.copy(userMessage = getString(R.string.msg_no_image_to_save)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val success = withContext(Dispatchers.IO) {
                saveBitmapToGallery(item.bitmap, 0)
            }
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    userMessage = if (success) getString(R.string.msg_saved_to_gallery) else getString(R.string.msg_save_failed)
                )
            }
        }
    }

    private fun saveBatch() {
        val cur = _uiState.value
        if (cur.images.isEmpty()) {
            _uiState.update { it.copy(userMessage = getString(R.string.msg_no_image_to_save)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val count = withContext(Dispatchers.IO) {
                var saved = 0
                for ((idx, item) in cur.images.withIndex()) {
                    if (saveBitmapToGallery(item.bitmap, idx)) {
                        saved++
                    }
                }
                saved
            }
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    userMessage = getString(R.string.msg_saved_all_to_gallery, count)
                )
            }
        }
    }

    private fun extractThumbnail(source: Bitmap): Bitmap {
        val maxThumb = 240
        val ratio = if (source.width > source.height) {
            if (source.width > maxThumb) source.width.toFloat() / maxThumb else 1f
        } else {
            if (source.height > maxThumb) source.height.toFloat() / maxThumb else 1f
        }
        val targetW = (source.width / ratio).toInt().coerceAtLeast(1)
        val targetH = (source.height / ratio).toInt().coerceAtLeast(1)
        return ThumbnailUtils.extractThumbnail(source, targetW, targetH)
    }

    private fun queryFilename(uri: Uri): String {
        val context = getApplication<Application>()
        var name = "image_${System.currentTimeMillis()}.png"
        try {
            val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    name = cursor.getString(idx) ?: name
                }
            }
        } catch (_: Throwable) {}
        return name
    }

    private fun saveBitmapToGallery(bitmap: Bitmap, indexOffset: Int): Boolean {
        val context = getApplication<Application>()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val ms = (System.currentTimeMillis() % 1000 + indexOffset)
        val filename = "Koishi_${dateStr}_${String.format(Locale.US, "%03d", ms)}.png"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + AlbumFolders.IMAGE_OBFUSCATION)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = context.contentResolver.insert(collection, values) ?: return false

                context.contentResolver.openOutputStream(itemUri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(itemUri, values, null, null)
                true
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), AlbumFolders.IMAGE_OBFUSCATION)
                if (!dir.exists()) dir.mkdirs()
                val targetFile = File(dir, filename)
                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                true
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            false
        }
    }
}
