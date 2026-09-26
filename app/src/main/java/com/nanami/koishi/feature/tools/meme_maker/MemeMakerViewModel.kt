package com.nanami.koishi.feature.tools.meme_maker

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.core.data.storage.ToolStorageDatabase
import com.nanami.koishi.core.util.AlbumFolders
import com.nanami.koishi.feature.tools.meme_maker.data.MemeLocalSticker
import com.nanami.koishi.feature.tools.meme_maker.data.MemeLocalStickerRepository
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeAssetPack
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeAssetRepository
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeAssetSource
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeRenderer
import com.nanami.koishi.feature.tools.meme_maker.engine.PlacedSticker
import com.nanami.koishi.feature.tools.meme_maker.engine.StickerKind
import com.nanami.koishi.feature.tools.meme_maker.engine.StickerSource
import com.nanami.koishi.feature.tools.meme_maker.engine.StickerTransform
import com.nanami.koishi.feature.tools.meme_maker.engine.TextStickerSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MemeMakerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MemeMakerUiState())
    val uiState: StateFlow<MemeMakerUiState> = _uiState.asStateFlow()

    private val assetRepository = MemeAssetRepository(application)

    private val localStickerRepository = MemeLocalStickerRepository(
        ToolStorageDatabase.get(application).toolStorageDao()
    )

    private val backgroundCache = mutableMapOf<String, Bitmap>()

    private val stickerBitmapCache = mutableMapOf<String, Bitmap>()

    private var taskJob: Job? = null

    init {
        refreshLibrary()
    }

    override fun onCleared() {
        super.onCleared()
        taskJob?.cancel()
        backgroundCache.values.forEach { if (!it.isRecycled) it.recycle() }
        stickerBitmapCache.values.forEach { if (!it.isRecycled) it.recycle() }
        _uiState.value.previewBitmap?.takeIf { !it.isRecycled }?.recycle()
        backgroundCache.clear()
        stickerBitmapCache.clear()
    }

    fun onEvent(event: MemeMakerUiEvent) {
        when (event) {
            is MemeMakerUiEvent.OnBackgroundPicked -> applyBackground(event.uri)
            MemeMakerUiEvent.OnClearBackground -> clearBackground()

            is MemeMakerUiEvent.OnStickerAssetPicked -> addImageSticker(event.asset.source)
            is MemeMakerUiEvent.OnLocalStickerPicked -> pickLocalSticker(event.uri)
            is MemeMakerUiEvent.OnRemoveLocalSticker -> removeLocalSticker(event.uri)
            MemeMakerUiEvent.OnAddTextSticker -> addTextSticker()
            is MemeMakerUiEvent.OnSelectSticker -> _uiState.update { it.copy(selectedStickerId = event.stickerId) }
            is MemeMakerUiEvent.OnTransformSticker -> updateSticker(event.stickerId) {
                it.copy(transform = event.transform)
            }
            is MemeMakerUiEvent.OnDeleteSticker -> deleteSticker(event.stickerId)
            is MemeMakerUiEvent.OnDuplicateSticker -> duplicateSticker(event.stickerId)
            is MemeMakerUiEvent.OnFlipSticker -> updateSticker(event.stickerId) {
                it.copy(transform = it.transform.copy(flipHorizontal = !it.transform.flipHorizontal))
            }
            is MemeMakerUiEvent.OnBringStickerToFront -> reorderSticker(event.stickerId, toFront = true)
            is MemeMakerUiEvent.OnSendStickerToBack -> reorderSticker(event.stickerId, toFront = false)

            is MemeMakerUiEvent.OnOpenTextEditor -> openTextEditor(event.stickerId)
            MemeMakerUiEvent.OnCloseTextEditor -> _uiState.update { it.copy(textEditorStickerId = null) }
            is MemeMakerUiEvent.OnEditorTextChanged -> {
                _uiState.update { it.copy(editorText = event.text) }
                editSelectedText { it.copy(text = event.text) }
            }
            is MemeMakerUiEvent.OnEditorFontChanged -> {
                _uiState.update { it.copy(editorFont = event.font) }
                editSelectedText { it.copy(font = event.font) }
            }
            is MemeMakerUiEvent.OnEditorStrokeToggled -> {
                _uiState.update { it.copy(editorStrokeEnabled = event.enabled) }
                editSelectedText { it.copy(strokeEnabled = event.enabled) }
            }
            is MemeMakerUiEvent.OnOpenColorPicker -> _uiState.update { it.copy(colorTarget = event.target) }
            MemeMakerUiEvent.OnCloseColorPicker -> _uiState.update { it.copy(colorTarget = null) }
            is MemeMakerUiEvent.OnColorPicked -> applyPickedColor(event.color)

            is MemeMakerUiEvent.OnDownloadPack -> downloadPack(event.packId)
            is MemeMakerUiEvent.OnRemovePack -> removePack(event.packId)
            is MemeMakerUiEvent.OnToggleAssetPanel -> _uiState.update { it.copy(showAssetPanel = event.show) }

            MemeMakerUiEvent.OnSavePng -> saveResult()
            MemeMakerUiEvent.OnRequestPreview -> openPreview()
            MemeMakerUiEvent.OnClosePreview -> closePreview()
            MemeMakerUiEvent.OnDismissMessage -> _uiState.update { it.copy(userMessage = null) }
            is MemeMakerUiEvent.OnToggleHelpDialog -> _uiState.update { it.copy(showHelpDialog = event.show) }
        }
    }

    /**
     * 标签先落到内置清单上，本地已有素材立刻可见；远程清单只作为后台增量，
     * 回来后替换掉内置项，避免网络请求把首屏拖成一段空白等待
     */
    private fun refreshLibrary() {
        viewModelScope.launch {
            val builtIn = MemeAssetSource.BUILT_IN_PACKS
            publishLibrary(builtIn, prune = false)
            val remote = assetRepository.loadManifest()
            if (remote == builtIn) return@launch
            publishLibrary(remote, prune = true)
        }
    }

    /**
     * prune 只在清单确定可信时开启：清单为空意味着没拿到可用描述，
     * 此时清理会把已下载的素材目录连同文件一起删掉
     */
    private suspend fun publishLibrary(packs: List<MemeAssetPack>, prune: Boolean) {
        val assets = withContext(Dispatchers.IO) { assetRepository.listAssets(packs) }
        val localStickers = pruneLocalStickers()
        if (prune) assetRepository.pruneUnknownPacks(packs)
        _uiState.update {
            it.copy(packs = packs, assets = assets, localStickers = localStickers)
        }
    }

    /**
     * 用户导入的贴纸同样以本地为唯一依据，原图被删除后条目一并移除
     */
    private suspend fun pruneLocalStickers(): List<MemeLocalSticker> {
        val stored = localStickerRepository.currentData().stickers
        if (stored.isEmpty()) return stored
        val readable = withContext(Dispatchers.IO) {
            stored.filter { assetRepository.isUriReadable(Uri.parse(it.uri)) }.map { it.uri }.toSet()
        }
        return if (readable.size == stored.size) stored else localStickerRepository.retainOnly(readable)
    }

    private fun applyBackground(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            persistUriPermission(uri)
            val bitmap = withContext(Dispatchers.IO) {
                assetRepository.decodeLocalImage(uri, MAX_BACKGROUND_DIMENSION)
            }
            if (bitmap == null) {
                _uiState.update {
                    it.copy(isProcessing = false, userMessage = getString(R.string.meme_image_read_failed))
                }
                return@launch
            }

            val key = uri.toString()
            backgroundCache.values.forEach { cached -> if (!cached.isRecycled) cached.recycle() }
            backgroundCache.clear()
            backgroundCache[key] = bitmap

            _uiState.update {
                it.copy(
                    background = key,
                    backgroundBitmap = bitmap,
                    canvasWidth = bitmap.width,
                    canvasHeight = bitmap.height,
                    isProcessing = false
                )
            }
        }
    }

    private fun clearBackground() {
        backgroundCache.values.forEach { if (!it.isRecycled) it.recycle() }
        backgroundCache.clear()
        _uiState.update {
            it.copy(
                background = null,
                backgroundBitmap = null,
                canvasWidth = MemeRenderer.DEFAULT_CANVAS_SIZE,
                canvasHeight = MemeRenderer.DEFAULT_CANVAS_SIZE
            )
        }
    }

    private fun addImageSticker(source: StickerSource) {
        viewModelScope.launch {
            if (ensureStickerBitmap(source) == null) {
                _uiState.update { it.copy(userMessage = getString(R.string.meme_sticker_unavailable)) }
                return@launch
            }
            appendSticker(
                PlacedSticker(
                    kind = StickerKind.IMAGE,
                    transform = StickerTransform(scale = INITIAL_IMAGE_STICKER_SCALE),
                    source = source
                )
            )
        }
    }

    private fun pickLocalSticker(uri: Uri) {
        viewModelScope.launch {
            persistUriPermission(uri)
            val source = StickerSource.Local(uri.toString())
            if (ensureStickerBitmap(source) == null) {
                _uiState.update { it.copy(userMessage = getString(R.string.meme_sticker_unavailable)) }
                return@launch
            }

            val name = withContext(Dispatchers.IO) { queryDisplayName(uri) }
                ?: uri.lastPathSegment.orEmpty()
            val stored = withContext(Dispatchers.IO) { localStickerRepository.add(uri.toString(), name) }

            appendSticker(
                PlacedSticker(
                    kind = StickerKind.IMAGE,
                    transform = StickerTransform(scale = INITIAL_IMAGE_STICKER_SCALE),
                    source = source
                )
            )
            _uiState.update { it.copy(localStickers = stored) }
        }
    }

    private fun removeLocalSticker(uri: String) {
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) { localStickerRepository.remove(uri) }
            val key = StickerSource.Local(uri).key
            _uiState.update { state ->
                state.copy(
                    localStickers = stored,
                    stickers = state.stickers.filterNot { it.source?.key == key }
                )
            }
            syncStickerBitmaps()
        }
    }

    private fun addTextSticker() {
        val sticker = PlacedSticker(
            kind = StickerKind.TEXT,
            transform = StickerTransform(scale = INITIAL_TEXT_STICKER_SCALE),
            text = TextStickerSpec(text = getString(R.string.meme_default_text))
        )
        appendSticker(sticker)
        openTextEditor(sticker.id)
    }

    private fun appendSticker(sticker: PlacedSticker) {
        _uiState.update { it.copy(stickers = it.stickers + sticker, selectedStickerId = sticker.id) }
        viewModelScope.launch { syncStickerBitmaps() }
    }

    private fun deleteSticker(stickerId: String) {
        _uiState.update {
            it.copy(
                stickers = it.stickers.filterNot { item -> item.id == stickerId },
                selectedStickerId = it.selectedStickerId?.takeIf { id -> id != stickerId },
                textEditorStickerId = it.textEditorStickerId?.takeIf { id -> id != stickerId }
            )
        }
        viewModelScope.launch { syncStickerBitmaps() }
    }

    private fun duplicateSticker(stickerId: String) {
        val sticker = _uiState.value.stickers.find { it.id == stickerId } ?: return
        val offset = DUPLICATE_OFFSET_RATIO
        appendSticker(
            sticker.copy(
                id = java.util.UUID.randomUUID().toString(),
                groupKey = null,
                transform = sticker.transform.copy(
                    centerX = (sticker.transform.centerX + offset).coerceIn(0f, 1f),
                    centerY = (sticker.transform.centerY + offset).coerceIn(0f, 1f)
                )
            )
        )
    }

    private fun reorderSticker(stickerId: String, toFront: Boolean) {
        _uiState.update { state ->
            val target = state.stickers.find { it.id == stickerId } ?: return@update state
            val rest = state.stickers.filterNot { it.id == stickerId }
            state.copy(stickers = if (toFront) rest + target else listOf(target) + rest)
        }
    }

    private fun updateSticker(stickerId: String, transform: (PlacedSticker) -> PlacedSticker) {
        _uiState.update { state ->
            state.copy(stickers = state.stickers.map { if (it.id == stickerId) transform(it) else it })
        }
    }

    private fun openTextEditor(stickerId: String) {
        val sticker = _uiState.value.stickers.find { it.id == stickerId } ?: return
        val spec = sticker.text ?: return
        _uiState.update {
            it.copy(
                selectedStickerId = stickerId,
                textEditorStickerId = stickerId,
                editorText = spec.text,
                editorFont = spec.font,
                editorStrokeEnabled = spec.strokeEnabled,
                editorColor = spec.color,
                editorStrokeColor = spec.strokeColor
            )
        }
    }

    private fun editSelectedText(transform: (TextStickerSpec) -> TextStickerSpec) {
        val stickerId = _uiState.value.textEditorStickerId ?: return
        updateSticker(stickerId) { sticker ->
            val spec = sticker.text ?: return@updateSticker sticker
            sticker.copy(text = transform(spec))
        }
    }

    private fun applyPickedColor(color: Int) {
        val target = _uiState.value.colorTarget ?: return
        when (target) {
            MemeColorTarget.TEXT -> {
                _uiState.update { it.copy(editorColor = color) }
                editSelectedText { it.copy(color = color) }
            }
            MemeColorTarget.STROKE -> {
                _uiState.update { it.copy(editorStrokeColor = color) }
                editSelectedText { it.copy(strokeColor = color) }
            }
        }
        _uiState.update { it.copy(colorTarget = null) }
    }

    private fun downloadPack(packId: String) {
        val pack = _uiState.value.packs.find { it.id == packId } ?: return
        taskJob?.cancel()
        taskJob = viewModelScope.launch {
            _uiState.update { it.copy(downloadingPackId = packId, downloadProgress = 0f) }
            val result = assetRepository.ensurePack(pack) { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            }
            result.fold(
                onSuccess = {
                    val assets = withContext(Dispatchers.IO) { assetRepository.listAssets(_uiState.value.packs) }
                    _uiState.update {
                        it.copy(
                            assets = assets,
                            downloadingPackId = null,
                            downloadProgress = 0f,
                            userMessage = getString(R.string.meme_asset_download_success, pack.name)
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            downloadingPackId = null,
                            downloadProgress = 0f,
                            userMessage = getString(R.string.meme_asset_download_failed)
                        )
                    }
                }
            )
        }
    }

    private fun removePack(packId: String) {
        val state = _uiState.value
        val removedKeys = state.assets.filter { it.packId == packId }.map { it.source.key }.toSet()
        assetRepository.removePack(packId)
        viewModelScope.launch {
            val assets = withContext(Dispatchers.IO) { assetRepository.listAssets(state.packs) }
            _uiState.update {
                it.copy(
                    assets = assets,
                    stickers = it.stickers.filterNot { sticker -> sticker.source?.key in removedKeys },
                    selectedStickerId = null,
                    textEditorStickerId = null,
                    userMessage = getString(R.string.meme_asset_removed)
                )
            }
            syncStickerBitmaps()
        }
    }

    private fun saveResult() {
        val snapshot = _uiState.value
        if (!snapshot.hasContent) {
            _uiState.update { it.copy(userMessage = getString(R.string.meme_need_content)) }
            return
        }

        taskJob?.cancel()
        taskJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isProcessing = true, progressStage = getString(R.string.meme_rendering_image))
            }

            val composed = composeResult(snapshot)
            val success = if (composed == null) {
                false
            } else {
                withContext(Dispatchers.IO) {
                    val tempFile = File(
                        getApplication<Application>().cacheDir,
                        "meme_${System.currentTimeMillis()}.png"
                    )
                    try {
                        FileOutputStream(tempFile).use { stream ->
                            composed.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        }
                        copyToGallery(tempFile, tempFile.name, MIME_PNG)
                    } catch (t: Throwable) {
                        false
                    } finally {
                        tempFile.delete()
                    }
                }
            }
            composed?.recycle()

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    progressStage = "",
                    userMessage = if (success) {
                        getString(R.string.meme_saved_image_success)
                    } else {
                        getString(R.string.meme_save_failed)
                    }
                )
            }
        }
    }

    private fun openPreview() {
        val snapshot = _uiState.value
        if (!snapshot.hasContent) {
            _uiState.update { it.copy(userMessage = getString(R.string.meme_need_content)) }
            return
        }

        taskJob?.cancel()
        taskJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isProcessing = true, progressStage = getString(R.string.meme_rendering_preview))
            }
            val bitmap = composeResult(snapshot)
            _uiState.update {
                it.copy(
                    previewBitmap = bitmap,
                    isProcessing = false,
                    progressStage = "",
                    userMessage = if (bitmap == null) getString(R.string.meme_save_failed) else null
                )
            }
        }
    }

    private fun closePreview() {
        _uiState.update { it.copy(previewBitmap = null, selectedStickerId = null) }
    }

    private suspend fun composeResult(snapshot: MemeMakerUiState): Bitmap? = withContext(Dispatchers.Default) {
        val background = snapshot.backgroundBitmap?.takeIf { !it.isRecycled }
        val bitmaps = snapshot.stickers.mapNotNull { it.source }.distinctBy { it.key }
            .mapNotNull { source -> ensureStickerBitmap(source)?.let { source.key to it } }
            .toMap()

        try {
            MemeRenderer.render(
                background = background,
                stickers = snapshot.stickers,
                width = snapshot.canvasWidth,
                height = snapshot.canvasHeight,
                stickerBitmaps = bitmaps
            )
        } catch (t: Throwable) {
            null
        }
    }

    private suspend fun ensureStickerBitmap(source: StickerSource): Bitmap? {
        stickerBitmapCache[source.key]?.let { if (!it.isRecycled) return it }
        val bitmap = withContext(Dispatchers.IO) {
            when (source) {
                is StickerSource.Asset -> assetRepository.loadStickerBitmap(
                    packId = source.packId,
                    fileName = source.fileName,
                    maxSize = MAX_STICKER_DIMENSION
                )

                is StickerSource.Local -> {
                    val uri = Uri.parse(source.uri)
                    if (assetRepository.isUriReadable(uri)) {
                        assetRepository.decodeLocalImage(uri, MAX_STICKER_DIMENSION)
                    } else {
                        null
                    }
                }
            }
        } ?: return null
        stickerBitmapCache[source.key] = bitmap
        return bitmap
    }

    private suspend fun syncStickerBitmaps() {
        val sources = _uiState.value.stickers.mapNotNull { it.source }.distinctBy { it.key }
        val keep = sources.map { it.key }.toSet()

        stickerBitmapCache.keys.filterNot { it in keep }.forEach { key ->
            stickerBitmapCache.remove(key)?.takeIf { !it.isRecycled }?.recycle()
        }
        sources.forEach { ensureStickerBitmap(it) }

        _uiState.update { it.copy(stickerBitmaps = stickerBitmapCache.toMap()) }
    }

    private fun copyToGallery(source: File, fileName: String, mimeType: String): Boolean {
        if (!source.isFile || source.length() <= 0) return false
        val context = getApplication<Application>()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/" + AlbumFolders.MEME_MAKER
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, values) ?: return false
                resolver.openOutputStream(itemUri)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
                true
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    AlbumFolders.MEME_MAKER
                )
                if (!dir.exists()) dir.mkdirs()
                val destination = File(dir, fileName)
                source.inputStream().use { input ->
                    FileOutputStream(destination).use { output -> input.copyTo(output) }
                }
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, destination.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                true
            }
        } catch (t: Throwable) {
            false
        }
    }

    private fun persistUriPermission(uri: Uri) {
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Throwable) {
        }
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        getApplication<Application>().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    } catch (t: Throwable) {
        null
    }

    private fun getString(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)

    companion object {
        private const val MAX_BACKGROUND_DIMENSION = 2048
        private const val MAX_STICKER_DIMENSION = 512
        private const val INITIAL_IMAGE_STICKER_SCALE = 0.4f
        private const val INITIAL_TEXT_STICKER_SCALE = 0.09f
        private const val DUPLICATE_OFFSET_RATIO = 0.04f
        private const val MIME_PNG = "image/png"
    }
}
