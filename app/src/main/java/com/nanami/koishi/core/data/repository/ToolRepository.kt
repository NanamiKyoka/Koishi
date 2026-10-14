package com.nanami.koishi.core.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BrandingWatermark
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Difference
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FindReplace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Gif
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.LocalPostOffice
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Wallpaper
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
            ),            ToolItem(
                id = "prism_tank",
                nameRes = R.string.tool_prism_tank_name,
                descriptionRes = R.string.tool_prism_tank_desc,
                icon = Icons.Rounded.Flare,
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
            ),
            ToolItem(
                id = "image_sketch",
                nameRes = R.string.tool_image_sketch_name,
                descriptionRes = R.string.tool_image_sketch_desc,
                icon = Icons.Rounded.AutoFixHigh,
                category = ToolCategory.IMAGE_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "video_to_gif",
                nameRes = R.string.tool_video_to_gif_name,
                descriptionRes = R.string.tool_video_to_gif_desc,
                icon = Icons.Rounded.Gif,
                category = ToolCategory.IMAGE_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "bili_cover",
                nameRes = R.string.tool_bili_cover_name,
                descriptionRes = R.string.tool_bili_cover_desc,
                icon = Icons.Rounded.Wallpaper,
                category = ToolCategory.IMAGE_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "meme_maker",
                nameRes = R.string.tool_meme_maker_name,
                descriptionRes = R.string.tool_meme_maker_desc,
                icon = Icons.Rounded.EmojiEmotions,
                category = ToolCategory.IMAGE_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "color_picker",
                nameRes = R.string.tool_color_picker_name,
                descriptionRes = R.string.tool_color_picker_desc,
                icon = Icons.Rounded.Colorize,
                category = ToolCategory.IMAGE_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "lan_transfer",
                nameRes = R.string.tool_lan_transfer_name,
                descriptionRes = R.string.tool_lan_transfer_desc,
                icon = Icons.Rounded.FolderShared,
                category = ToolCategory.FILE_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "json_formatter",
                nameRes = R.string.tool_json_formatter_name,
                descriptionRes = R.string.tool_json_formatter_desc,
                icon = Icons.Rounded.DataObject,
                category = ToolCategory.TEXT_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "text_encoding",
                nameRes = R.string.tool_text_encoding_name,
                descriptionRes = R.string.tool_text_encoding_desc,
                icon = Icons.Rounded.SwapHoriz,
                category = ToolCategory.TEXT_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "text_hash",
                nameRes = R.string.tool_text_hash_name,
                descriptionRes = R.string.tool_text_hash_desc,
                icon = Icons.Rounded.Fingerprint,
                category = ToolCategory.TEXT_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "regex_tool",
                nameRes = R.string.tool_regex_tool_name,
                descriptionRes = R.string.tool_regex_tool_desc,
                icon = Icons.Rounded.FindReplace,
                category = ToolCategory.TEXT_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "text_process",
                nameRes = R.string.tool_text_process_name,
                descriptionRes = R.string.tool_text_process_desc,
                icon = Icons.Rounded.TextFields,
                category = ToolCategory.TEXT_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "text_diff",
                nameRes = R.string.tool_text_diff_name,
                descriptionRes = R.string.tool_text_diff_desc,
                icon = Icons.Rounded.Difference,
                category = ToolCategory.TEXT_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "compass",
                nameRes = R.string.tool_compass_name,
                descriptionRes = R.string.tool_compass_desc,
                icon = Icons.Rounded.Explore,
                category = ToolCategory.DEVICE_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "noise_meter",
                nameRes = R.string.tool_noise_meter_name,
                descriptionRes = R.string.tool_noise_meter_desc,
                icon = Icons.Rounded.GraphicEq,
                category = ToolCategory.DEVICE_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "spirit_level",
                nameRes = R.string.tool_spirit_level_name,
                descriptionRes = R.string.tool_spirit_level_desc,
                icon = Icons.Rounded.Straighten,
                category = ToolCategory.DEVICE_APPS,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "today_in_history",
                nameRes = R.string.tool_today_in_history_name,
                descriptionRes = R.string.tool_today_in_history_desc,
                icon = Icons.Rounded.HistoryEdu,
                category = ToolCategory.LIFE,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "decision_maker",
                nameRes = R.string.tool_decision_maker_name,
                descriptionRes = R.string.tool_decision_maker_desc,
                icon = Icons.Rounded.Casino,
                category = ToolCategory.LIFE,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "ruler",
                nameRes = R.string.tool_ruler_name,
                descriptionRes = R.string.tool_ruler_desc,
                icon = Icons.Rounded.Straighten,
                category = ToolCategory.LIFE,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "currency_converter",
                nameRes = R.string.tool_currency_converter_name,
                descriptionRes = R.string.tool_currency_converter_desc,
                icon = Icons.Rounded.CurrencyExchange,
                category = ToolCategory.CALCULATION,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "unit_converter",
                nameRes = R.string.tool_unit_converter_name,
                descriptionRes = R.string.tool_unit_converter_desc,
                icon = Icons.Rounded.SwapHoriz,
                category = ToolCategory.CALCULATION,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "date_time_calculator",
                nameRes = R.string.tool_date_time_calculator_name,
                descriptionRes = R.string.tool_date_time_calculator_desc,
                icon = Icons.Rounded.CalendarMonth,
                category = ToolCategory.CALCULATION,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "bmi_calculator",
                nameRes = R.string.tool_bmi_calculator_name,
                descriptionRes = R.string.tool_bmi_calculator_desc,
                icon = Icons.Rounded.MonitorWeight,
                category = ToolCategory.CALCULATION,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "worth_calculator",
                nameRes = R.string.tool_worth_calculator_name,
                descriptionRes = R.string.tool_worth_calculator_desc,
                icon = Icons.Rounded.Payments,
                category = ToolCategory.CALCULATION,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "postal_code",
                nameRes = R.string.tool_postal_code_name,
                descriptionRes = R.string.tool_postal_code_desc,
                icon = Icons.Rounded.LocalPostOffice,
                category = ToolCategory.QUERY,
                isFavorite = false,
                hasDot = true
            ),
            ToolItem(
                id = "mini_apps",
                nameRes = R.string.tool_mini_apps_name,
                descriptionRes = R.string.tool_mini_apps_desc,
                icon = Icons.Rounded.Apps,
                category = ToolCategory.OTHER,
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
