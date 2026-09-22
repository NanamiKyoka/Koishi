package com.nanami.koishi.core.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class ToolItem(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val category: ToolCategory,
    val isFavorite: Boolean = false,
    val isNew: Boolean = false,
    val badge: String? = null,
    val hasDot: Boolean = false
)
