package com.nanami.koishi.core.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BrandingWatermark
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ViewCarousel
import com.nanami.koishi.R
import com.nanami.koishi.core.model.ToolCategory
import com.nanami.koishi.core.model.ToolItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ToolRepository {
    val availableTools: Flow<List<ToolItem>>
    fun getToolById(toolId: String): ToolItem?
}

class InMemoryToolRepository : ToolRepository {

    private val _registeredTools = MutableStateFlow(
        listOf(
            ToolItem(
                id = "image_stitching",
                nameRes = R.string.tool_image_stitching_name,
                descriptionRes = R.string.tool_image_stitching_desc,
                icon = Icons.Rounded.ViewCarousel,
                category = ToolCategory.IMAGE_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "grid_split",
                nameRes = R.string.tool_grid_split_name,
                descriptionRes = R.string.tool_grid_split_desc,
                icon = Icons.Rounded.GridView,
                category = ToolCategory.IMAGE_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "image_obfuscation",
                nameRes = R.string.tool_image_obfuscation_name,
                descriptionRes = R.string.tool_image_obfuscation_desc,
                icon = Icons.Rounded.Image,
                category = ToolCategory.IMAGE_APPS,
                isFavorite = false,
                hasDot = false
            ),
            ToolItem(
                id = "mirage_tank",
                nameRes = R.string.tool_mirage_tank_name,
                descriptionRes = R.string.tool_mirage_tank_desc,
                icon = Icons.Rounded.Contrast,
                category = ToolCategory.IMAGE_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "qr_code",
                nameRes = R.string.tool_qr_code_name,
                descriptionRes = R.string.tool_qr_code_desc,
                icon = Icons.Rounded.QrCode2,
                category = ToolCategory.IMAGE_APPS,
                isFavorite = false,
                hasDot = false
            ),
            ToolItem(
                id = "image_search",
                nameRes = R.string.tool_image_search_name,
                descriptionRes = R.string.tool_image_search_desc,
                icon = Icons.Rounded.ImageSearch,
                category = ToolCategory.IMAGE_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "watermark",
                nameRes = R.string.tool_watermark_name,
                descriptionRes = R.string.tool_watermark_desc,
                icon = Icons.AutoMirrored.Rounded.BrandingWatermark,
                category = ToolCategory.IMAGE_APPS,
                isFavorite = false,
                hasDot = true
            )
        )
    )

    override val availableTools: Flow<List<ToolItem>> = _registeredTools.asStateFlow()

    override fun getToolById(toolId: String): ToolItem? {
        return _registeredTools.value.find { it.id == toolId }
    }
}
