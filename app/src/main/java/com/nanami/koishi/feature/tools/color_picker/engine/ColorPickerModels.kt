package com.nanami.koishi.feature.tools.color_picker.engine

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteColor(
    val id: String,
    val hex: String,
    val colorArgb: Long,
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = 255,
    val timestamp: Long = System.currentTimeMillis(),
    val coordinates: String? = null
)

@Serializable
data class ColorPickerData(
    val favorites: List<FavoriteColor> = emptyList(),
    val lastPickedColor: Long = 0xFFFFFFFF,
    val magnification: Float = 2.0f
)
