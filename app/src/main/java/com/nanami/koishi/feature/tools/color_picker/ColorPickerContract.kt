package com.nanami.koishi.feature.tools.color_picker

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.nanami.koishi.feature.tools.color_picker.engine.FavoriteColor
import java.util.Locale

enum class ColorPickerTab {
    PICKER,
    FAVORITES
}

enum class Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

data class ColorPickerUiState(
    val currentTab: ColorPickerTab = ColorPickerTab.PICKER,
    val imageUri: Uri? = null,
    val bitmap: Bitmap? = null,
    val cursorX: Int = 0,
    val cursorY: Int = 0,
    val selectedColor: Color = Color(0xFFFFFFFF),
    val magnification: Float = 2.0f,
    val showMagnificationSheet: Boolean = false,
    val favorites: List<FavoriteColor> = emptyList(),
    val isCurrentColorFavorite: Boolean = false,
    val showClearFavoritesDialog: Boolean = false,
    val selectedFavoriteForDetails: FavoriteColor? = null,
    @StringRes val userMessageRes: Int? = null,
    val userMessageArg: String? = null
) {
    val redInt: Int
        get() = (selectedColor.red * 255f).toInt().coerceIn(0, 255)

    val greenInt: Int
        get() = (selectedColor.green * 255f).toInt().coerceIn(0, 255)

    val blueInt: Int
        get() = (selectedColor.blue * 255f).toInt().coerceIn(0, 255)

    val hexString: String
        get() = String.format(Locale.US, "#%02X%02X%02X", redInt, greenInt, blueInt)

    val rgbString: String
        get() = "$redInt, $greenInt, $blueInt"

    val pixelCoordinateString: String
        get() = "X$cursorX, Y$cursorY"
}

sealed interface ColorPickerUiEvent {
    data class OnSelectTab(val tab: ColorPickerTab) : ColorPickerUiEvent
    data class OnImageSelected(val uri: Uri) : ColorPickerUiEvent
    data object OnClearImage : ColorPickerUiEvent
    data class OnCursorMove(val pixelX: Int, val pixelY: Int) : ColorPickerUiEvent
    data class OnNudgeCursor(val direction: Direction) : ColorPickerUiEvent
    data class OnMagnificationChange(val magnification: Float) : ColorPickerUiEvent
    data class OnShowMagnificationSheet(val show: Boolean) : ColorPickerUiEvent
    data object OnToggleFavoriteCurrentColor : ColorPickerUiEvent
    data class OnDeleteFavorite(val id: String) : ColorPickerUiEvent
    data object OnShowClearFavoritesDialog : ColorPickerUiEvent
    data object OnDismissClearFavoritesDialog : ColorPickerUiEvent
    data object OnConfirmClearFavorites : ColorPickerUiEvent
    data class OnSelectFavoriteForDetails(val favorite: FavoriteColor?) : ColorPickerUiEvent
    data class OnCopyText(val text: String, @StringRes val messageRes: Int, val arg: String = text) : ColorPickerUiEvent
    data object OnDismissMessage : ColorPickerUiEvent
}
