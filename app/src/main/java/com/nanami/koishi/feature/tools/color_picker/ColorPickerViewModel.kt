package com.nanami.koishi.feature.tools.color_picker

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.color_picker.engine.ColorPickerRepository
import com.nanami.koishi.feature.tools.color_picker.engine.FavoriteColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ColorPickerViewModel(
    application: Application,
    private val repository: ColorPickerRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ColorPickerUiState())
    val uiState: StateFlow<ColorPickerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.dataFlow.collect { data ->
                _uiState.update { current ->
                    val isFavorite = data.favorites.any {
                        it.hex.equals(current.hexString, ignoreCase = true)
                    }
                    current.copy(
                        favorites = data.favorites,
                        isCurrentColorFavorite = isFavorite,
                        magnification = if (current.bitmap == null) data.magnification else current.magnification
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            _uiState.value.bitmap?.recycle()
        } catch (_: Throwable) {}
    }

    fun onEvent(event: ColorPickerUiEvent) {
        when (event) {
            is ColorPickerUiEvent.OnSelectTab -> {
                _uiState.update { it.copy(currentTab = event.tab) }
            }

            is ColorPickerUiEvent.OnImageSelected -> {
                loadImage(event.uri)
            }

            is ColorPickerUiEvent.OnClearImage -> {
                val previous = _uiState.value.bitmap
                _uiState.update {
                    val defaultWhite = Color(0xFFFFFFFF)
                    val isFav = it.favorites.any { fav -> fav.hex.equals("#FFFFFF", ignoreCase = true) }
                    it.copy(
                        imageUri = null,
                        bitmap = null,
                        cursorX = 0,
                        cursorY = 0,
                        selectedColor = defaultWhite,
                        isCurrentColorFavorite = isFav
                    )
                }
                previous?.recycle()
            }

            is ColorPickerUiEvent.OnCursorMove -> {
                moveCursor(event.pixelX, event.pixelY)
            }

            is ColorPickerUiEvent.OnNudgeCursor -> {
                val state = _uiState.value
                val bmp = state.bitmap ?: return
                val newX = when (event.direction) {
                    Direction.LEFT -> (state.cursorX - 1).coerceIn(0, bmp.width - 1)
                    Direction.RIGHT -> (state.cursorX + 1).coerceIn(0, bmp.width - 1)
                    else -> state.cursorX
                }
                val newY = when (event.direction) {
                    Direction.UP -> (state.cursorY - 1).coerceIn(0, bmp.height - 1)
                    Direction.DOWN -> (state.cursorY + 1).coerceIn(0, bmp.height - 1)
                    else -> state.cursorY
                }
                moveCursor(newX, newY)
            }

            is ColorPickerUiEvent.OnMagnificationChange -> {
                _uiState.update { it.copy(magnification = event.magnification) }
                viewModelScope.launch {
                    repository.updateMagnification(event.magnification)
                }
            }

            is ColorPickerUiEvent.OnShowMagnificationSheet -> {
                _uiState.update { it.copy(showMagnificationSheet = event.show) }
            }

            is ColorPickerUiEvent.OnToggleFavoriteCurrentColor -> {
                toggleFavoriteCurrentColor()
            }

            is ColorPickerUiEvent.OnDeleteFavorite -> {
                viewModelScope.launch {
                    repository.removeFavorite(event.id)
                }
            }

            is ColorPickerUiEvent.OnShowClearFavoritesDialog -> {
                _uiState.update { it.copy(showClearFavoritesDialog = true) }
            }

            is ColorPickerUiEvent.OnDismissClearFavoritesDialog -> {
                _uiState.update { it.copy(showClearFavoritesDialog = false) }
            }

            is ColorPickerUiEvent.OnConfirmClearFavorites -> {
                viewModelScope.launch {
                    repository.clearFavorites()
                    _uiState.update {
                        it.copy(
                            showClearFavoritesDialog = false,
                            userMessageRes = R.string.color_picker_clear_favorites_cleared,
                            userMessageArg = null
                        )
                    }
                }
            }

            is ColorPickerUiEvent.OnSelectFavoriteForDetails -> {
                _uiState.update { it.copy(selectedFavoriteForDetails = event.favorite) }
            }

            is ColorPickerUiEvent.OnCopyText -> {
                copyToClipboard(event.text)
                _uiState.update {
                    it.copy(
                        userMessageRes = event.messageRes,
                        userMessageArg = event.arg
                    )
                }
            }

            is ColorPickerUiEvent.OnDismissMessage -> {
                _uiState.update { it.copy(userMessageRes = null, userMessageArg = null) }
            }
        }
    }

    private fun loadImage(uri: Uri) {
        viewModelScope.launch {
            val decoded = withContext(Dispatchers.IO) {
                decodeSafeBitmap(uri, MAX_BITMAP_DIMENSION)
            }
            if (decoded != null) {
                val previous = _uiState.value.bitmap
                val initX = decoded.width / 2
                val initY = decoded.height / 2
                val pixelArgb = decoded.getPixel(initX, initY)
                val newColor = Color(pixelArgb)

                val r = (newColor.red * 255f).toInt().coerceIn(0, 255)
                val g = (newColor.green * 255f).toInt().coerceIn(0, 255)
                val b = (newColor.blue * 255f).toInt().coerceIn(0, 255)
                val hex = String.format(java.util.Locale.US, "#%02X%02X%02X", r, g, b)
                val isFav = _uiState.value.favorites.any { it.hex.equals(hex, ignoreCase = true) }

                _uiState.update {
                    it.copy(
                        imageUri = uri,
                        bitmap = decoded,
                        cursorX = initX,
                        cursorY = initY,
                        selectedColor = newColor,
                        isCurrentColorFavorite = isFav
                    )
                }
                previous?.recycle()
            }
        }
    }

    private fun moveCursor(targetX: Int, targetY: Int) {
        val state = _uiState.value
        val bmp = state.bitmap ?: return
        val clampedX = targetX.coerceIn(0, bmp.width - 1)
        val clampedY = targetY.coerceIn(0, bmp.height - 1)

        val pixelArgb = bmp.getPixel(clampedX, clampedY)
        val newColor = Color(pixelArgb)
        val r = (newColor.red * 255f).toInt().coerceIn(0, 255)
        val g = (newColor.green * 255f).toInt().coerceIn(0, 255)
        val b = (newColor.blue * 255f).toInt().coerceIn(0, 255)
        val hex = String.format(java.util.Locale.US, "#%02X%02X%02X", r, g, b)
        val isFav = state.favorites.any { it.hex.equals(hex, ignoreCase = true) }

        _uiState.update {
            it.copy(
                cursorX = clampedX,
                cursorY = clampedY,
                selectedColor = newColor,
                isCurrentColorFavorite = isFav
            )
        }
    }

    private fun toggleFavoriteCurrentColor() {
        val state = _uiState.value
        val hex = state.hexString
        val existing = state.favorites.firstOrNull { it.hex.equals(hex, ignoreCase = true) }

        viewModelScope.launch {
            if (existing != null) {
                repository.removeFavorite(existing.id)
                _uiState.update {
                    it.copy(
                        userMessageRes = R.string.color_picker_favorite_removed,
                        userMessageArg = hex
                    )
                }
            } else {
                val newFavorite = FavoriteColor(
                    id = UUID.randomUUID().toString(),
                    hex = hex,
                    colorArgb = state.selectedColor.toArgb().toLong(),
                    red = state.redInt,
                    green = state.greenInt,
                    blue = state.blueInt,
                    alpha = 255,
                    timestamp = System.currentTimeMillis(),
                    coordinates = state.pixelCoordinateString
                )
                repository.addFavorite(newFavorite)
                _uiState.update {
                    it.copy(
                        userMessageRes = R.string.color_picker_favorite_added,
                        userMessageArg = hex
                    )
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Koishi Color", text))
    }

    private fun decodeSafeBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val context = getApplication<Application>()
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) return null

            var sampleSize = 1
            while (rawWidth / sampleSize > maxDimension || rawHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    companion object {
        private const val MAX_BITMAP_DIMENSION = 2560
    }
}
