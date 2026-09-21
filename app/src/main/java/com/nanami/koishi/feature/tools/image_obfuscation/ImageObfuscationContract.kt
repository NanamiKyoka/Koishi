package com.nanami.koishi.feature.tools.image_obfuscation

import android.graphics.Bitmap
import android.net.Uri
import com.nanami.koishi.feature.tools.image_obfuscation.engine.ObfuscationMode
import java.util.UUID

data class ObfuscatedImageItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val filename: String,
    val bitmap: Bitmap,
    val thumbnail: Bitmap,
    val width: Int,
    val height: Int
)

data class ImageObfuscationUiState(
    val images: List<ObfuscatedImageItem> = emptyList(),
    val selectedIndex: Int = 0,
    val previewBitmap: Bitmap? = null,
    val mode: ObfuscationMode = ObfuscationMode.TOMATO_GILBERT,
    val key: String = "1",
    val isProcessing: Boolean = false,
    val processingProgress: Float = 0f,
    val userMessage: String? = null,
    val isHelpDialogOpen: Boolean = false
) {
    val currentItem: ObfuscatedImageItem?
        get() = images.getOrNull(selectedIndex)

    val currentBitmap: Bitmap?
        get() = currentItem?.bitmap ?: previewBitmap

    val imageWidth: Int
        get() = currentItem?.width ?: (previewBitmap?.width ?: 0)

    val imageHeight: Int
        get() = currentItem?.height ?: (previewBitmap?.height ?: 0)

    val totalCount: Int
        get() = images.size
}

sealed interface ImageObfuscationUiEvent {
    data class OnImagesSelected(val uris: List<Uri>) : ImageObfuscationUiEvent
    data class OnSelectImageIndex(val index: Int) : ImageObfuscationUiEvent

    data class OnModeSelected(val mode: ObfuscationMode) : ImageObfuscationUiEvent
    data class OnKeyChanged(val key: String) : ImageObfuscationUiEvent

    data object OnObfuscateCurrent : ImageObfuscationUiEvent
    data object OnDeobfuscateCurrent : ImageObfuscationUiEvent
    data object OnRemoveCurrent : ImageObfuscationUiEvent
    data object OnRestoreCurrent : ImageObfuscationUiEvent
    data object OnSaveCurrent : ImageObfuscationUiEvent

    data object OnObfuscateAll : ImageObfuscationUiEvent
    data object OnDeobfuscateAll : ImageObfuscationUiEvent
    data object OnRemoveAll : ImageObfuscationUiEvent
    data object OnRestoreAll : ImageObfuscationUiEvent
    data object OnSaveAll : ImageObfuscationUiEvent

    data object OnRotateCurrent : ImageObfuscationUiEvent
    data class OnToggleHelpDialog(val open: Boolean) : ImageObfuscationUiEvent
    data object OnDismissMessage : ImageObfuscationUiEvent
}
