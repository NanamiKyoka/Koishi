package com.nanami.koishi.feature.tools.qr_tool

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.qr_tool.components.QrThemePreset
import com.nanami.koishi.feature.tools.qr_tool.engine.QrConfig
import com.nanami.koishi.feature.tools.qr_tool.engine.QrDotStyle
import com.nanami.koishi.feature.tools.qr_tool.engine.QrGeneratorUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

class QrToolViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(QrToolUiState())
    val uiState: StateFlow<QrToolUiState> = _uiState.asStateFlow()

    private var generateJob: Job? = null

    init {
        triggerGeneration(debounceMs = 0)
    }

    private fun getString(resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }

    fun onEvent(event: QrToolUiEvent) {
        when (event) {
            is QrToolUiEvent.OnContentChange -> {
                _uiState.update { it.copy(content = event.content) }
                triggerGeneration()
            }
            QrToolUiEvent.OnClearContent -> {
                _uiState.update { it.copy(content = "") }
                triggerGeneration()
            }
            is QrToolUiEvent.OnDarkColorChange -> {
                _uiState.update { it.copy(darkColor = event.color) }
                triggerGeneration()
            }
            is QrToolUiEvent.OnLightColorChange -> {
                _uiState.update { it.copy(lightColor = event.color) }
                triggerGeneration()
            }
            is QrToolUiEvent.OnBackgroundColorChange -> {
                _uiState.update { it.copy(backgroundColor = event.color) }
                triggerGeneration()
            }
            is QrToolUiEvent.OnTogglePickFromBg -> {
                val newState = event.enabled
                _uiState.update { current ->
                    var updatedDark = current.darkColor
                    if (newState && current.bgBitmap != null) {
                        val dominant = QrGeneratorUtil.extractDominantColor(current.bgBitmap)
                        updatedDark = androidx.compose.ui.graphics.Color(dominant)
                    }
                    current.copy(isPickFromBg = newState, darkColor = updatedDark)
                }
                triggerGeneration()
            }
            is QrToolUiEvent.OnThemeSelected -> {
                applyThemePreset(event.preset)
            }
            is QrToolUiEvent.OnDotStyleChange -> {
                _uiState.update { it.copy(dotStyle = event.style) }
                triggerGeneration()
            }
            is QrToolUiEvent.OnDotScaleChange -> {
                _uiState.update { it.copy(dotScale = event.scale) }
                triggerGeneration()
            }
            is QrToolUiEvent.OnLogoSelected -> {
                loadLogo(event.uri)
            }
            QrToolUiEvent.OnClearLogo -> {
                _uiState.value.logoBitmap?.recycle()
                _uiState.update { it.copy(logoUri = null, logoBitmap = null) }
                triggerGeneration(debounceMs = 0)
            }
            is QrToolUiEvent.OnBgSelected -> {
                loadBackground(event.uri)
            }
            QrToolUiEvent.OnClearBg -> {
                _uiState.value.bgBitmap?.recycle()
                _uiState.update { it.copy(bgUri = null, bgBitmap = null) }
                triggerGeneration(debounceMs = 0)
            }
            is QrToolUiEvent.OnBgAlphaChange -> {
                _uiState.update { it.copy(bgAlpha = event.alpha) }
                triggerGeneration()
            }
            is QrToolUiEvent.OnMarginChange -> {
                _uiState.update { it.copy(marginDp = event.margin) }
                triggerGeneration()
            }
            QrToolUiEvent.OnOpenThemePicker -> {
                _uiState.update { it.copy(showThemePicker = true) }
            }
            QrToolUiEvent.OnDismissThemePicker -> {
                _uiState.update { it.copy(showThemePicker = false) }
            }
            is QrToolUiEvent.OnOpenColorPicker -> {
                _uiState.update { it.copy(activeColorPicker = event.target) }
            }
            QrToolUiEvent.OnDismissColorPicker -> {
                _uiState.update { it.copy(activeColorPicker = null) }
            }
            QrToolUiEvent.OnSaveToGallery -> {
                saveToGallery()
            }
            QrToolUiEvent.OnResetDefaults -> {
                _uiState.value.logoBitmap?.recycle()
                _uiState.value.bgBitmap?.recycle()
                _uiState.value = QrToolUiState()
                triggerGeneration(debounceMs = 0)
            }
            QrToolUiEvent.OnClearUserMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
        }
    }

    private fun applyThemePreset(preset: QrThemePreset) {
        _uiState.update {
            it.copy(
                darkColor = preset.darkColor,
                lightColor = preset.lightColor,
                backgroundColor = preset.backgroundColor,
                showThemePicker = false
            )
        }
        triggerGeneration(debounceMs = 0)
    }

    private fun loadLogo(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                val stream = resolver.openInputStream(uri)
                val original = BitmapFactory.decodeStream(stream)
                stream?.close()

                if (original != null) {
                    val scaled = if (original.width > 512 || original.height > 512) {
                        Bitmap.createScaledBitmap(original, 512, 512, true).also {
                            if (it != original) original.recycle()
                        }
                    } else original

                    _uiState.value.logoBitmap?.recycle()
                    _uiState.update { it.copy(logoUri = uri, logoBitmap = scaled) }
                    triggerGeneration(debounceMs = 0)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = getString(R.string.msg_process_failed, e.localizedMessage ?: "")) }
            }
        }
    }

    private fun loadBackground(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                val stream = resolver.openInputStream(uri)
                val original = BitmapFactory.decodeStream(stream)
                stream?.close()

                if (original != null) {
                    val maxDim = 1024
                    val scaled = if (original.width > maxDim || original.height > maxDim) {
                        val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height)
                        val w = (original.width * ratio).toInt()
                        val h = (original.height * ratio).toInt()
                        Bitmap.createScaledBitmap(original, w, h, true).also {
                            if (it != original) original.recycle()
                        }
                    } else original

                    _uiState.value.bgBitmap?.recycle()

                    // 如果开启了“从背景图取色”，提取主色
                    var darkColor = _uiState.value.darkColor
                    if (_uiState.value.isPickFromBg) {
                        val dominant = QrGeneratorUtil.extractDominantColor(scaled)
                        darkColor = androidx.compose.ui.graphics.Color(dominant)
                    }

                    _uiState.update { it.copy(bgUri = uri, bgBitmap = scaled, darkColor = darkColor) }
                    triggerGeneration(debounceMs = 0)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = getString(R.string.msg_process_failed, e.localizedMessage ?: "")) }
            }
        }
    }

    private fun triggerGeneration(debounceMs: Long = 120L) {
        generateJob?.cancel()
        generateJob = viewModelScope.launch(Dispatchers.Default) {
            if (debounceMs > 0) {
                delay(debounceMs)
            }
            _uiState.update { it.copy(isGenerating = true) }
            val state = _uiState.value

            val marginPx = (state.marginDp * 3f).toInt()
            val config = QrConfig(
                content = state.content.ifBlank { " " },
                outputSize = 1024,
                darkColor = state.darkColor.toArgb(),
                lightColor = state.lightColor.toArgb(),
                backgroundColor = state.backgroundColor.toArgb(),
                dotStyle = state.dotStyle,
                dotScale = state.dotScale,
                marginPx = marginPx,
                logoBitmap = state.logoBitmap,
                bgBitmap = state.bgBitmap,
                bgAlpha = state.bgAlpha,
                isPickFromBg = state.isPickFromBg
            )

            val bitmap = QrGeneratorUtil.generate(config)
            _uiState.update { current ->
                current.copy(qrBitmap = bitmap, isGenerating = false)
            }
        }
    }

    private fun saveToGallery() {
        val bitmap = _uiState.value.qrBitmap ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "QR_$timeStamp.png"

                var isSuccess = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Koishi")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { os ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                        }
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                        isSuccess = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Koishi")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, filename)
                    FileOutputStream(file).use { os ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                    }
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DATA, file.absolutePath)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    }
                    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    isSuccess = true
                }

                _uiState.update {
                    it.copy(userMessage = if (isSuccess) getString(R.string.qr_saved_success) else getString(R.string.msg_save_failed))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = getString(R.string.msg_save_failed) + ": " + e.localizedMessage) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            _uiState.value.logoBitmap?.recycle()
            _uiState.value.bgBitmap?.recycle()
            _uiState.value.qrBitmap?.recycle()
        } catch (_: Exception) {}
    }
}
