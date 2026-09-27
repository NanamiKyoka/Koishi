package com.nanami.koishi.feature.tools.color_picker.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao

class ColorPickerRepository(
    dao: ToolStorageDao
) : BaseToolRepository<ColorPickerData>(
    toolId = TOOL_ID,
    serializer = ColorPickerData.serializer(),
    dao = dao,
    defaultData = ColorPickerData()
) {

    suspend fun addFavorite(color: FavoriteColor): ColorPickerData = updateData { current ->
        val filtered = current.favorites.filterNot { it.hex.equals(color.hex, ignoreCase = true) }
        current.copy(favorites = listOf(color) + filtered)
    }

    suspend fun removeFavorite(id: String): ColorPickerData = updateData { current ->
        current.copy(favorites = current.favorites.filterNot { it.id == id })
    }

    suspend fun clearFavorites(): ColorPickerData = updateData { current ->
        current.copy(favorites = emptyList())
    }

    suspend fun updateMagnification(magnification: Float): ColorPickerData = updateData { current ->
        current.copy(magnification = magnification)
    }

    suspend fun updateLastPickedColor(colorArgb: Long): ColorPickerData = updateData { current ->
        current.copy(lastPickedColor = colorArgb)
    }

    companion object {
        const val TOOL_ID = "color_picker"
    }
}
