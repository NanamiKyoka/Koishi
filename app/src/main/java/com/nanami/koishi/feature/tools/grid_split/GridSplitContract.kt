package com.nanami.koishi.feature.tools.grid_split

import android.graphics.Bitmap
import android.net.Uri

/**
 * 多格切图模式
 */
enum class GridSplitMode {
    /**
     * 切方格模式（1:1 中心裁切，N x N 联动）
     */
    SQUARE,

    /**
     * 自定义切模式（原图完整保留宽高比，行与列独立选择）
     */
    CUSTOM
}

/**
 * 多格切图 UI 状态
 */
data class GridSplitUiState(
    val selectedUri: Uri? = null,
    val previewBitmap: Bitmap? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val mode: GridSplitMode = GridSplitMode.SQUARE,
    val squareGridN: Int = 3,
    val customRows: Int = 2,
    val customCols: Int = 2,
    val isProcessing: Boolean = false,
    val userMessage: String? = null
) {
    val totalSquareSlices: Int get() = squareGridN * squareGridN
    val totalCustomSlices: Int get() = customRows * customCols
}

/**
 * 多格切图 UI 事件
 */
sealed interface GridSplitUiEvent {
    data class OnImageSelected(val uri: Uri) : GridSplitUiEvent
    data class OnModeChanged(val mode: GridSplitMode) : GridSplitUiEvent
    data class OnSquareGridNChanged(val n: Int) : GridSplitUiEvent
    data class OnCustomRowsChanged(val rows: Int) : GridSplitUiEvent
    data class OnCustomColsChanged(val cols: Int) : GridSplitUiEvent
    data object OnClearImage : GridSplitUiEvent
    data object OnExecuteCrop : GridSplitUiEvent
    data object OnDismissMessage : GridSplitUiEvent
}
