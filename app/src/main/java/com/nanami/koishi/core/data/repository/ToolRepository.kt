package com.nanami.koishi.core.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.QrCode2
import com.nanami.koishi.R
import com.nanami.koishi.core.model.ToolCategory
import com.nanami.koishi.core.model.ToolItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 工具注册管理仓库。
 * 核心原则：做一个功能放一个，未完整实现的功能绝不放在界面上。
 */
interface ToolRepository {
    /** 观察所有已注册上架的可用工具列表 */
    val availableTools: Flow<List<ToolItem>>

    /** 根据唯一 ID 获取工具 */
    fun getToolById(toolId: String): ToolItem?
}

class InMemoryToolRepository : ToolRepository {

    /**
     * 遵循“做一个放一个”准则：
     * 注册目前已完整实现的图片混淆与二维码工具。
     * 默认未收藏（收藏状态由 ToolFavoritesRepository 动态注入）。
     */
    private val _registeredTools = MutableStateFlow(
        listOf(
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
                id = "qr_code",
                nameRes = R.string.tool_qr_code_name,
                descriptionRes = R.string.tool_qr_code_desc,
                icon = Icons.Rounded.QrCode2,
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
