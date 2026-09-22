package com.nanami.koishi.core.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
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
     * 仅注册目前已完整实现并经过算法与无损验证的图片混淆工具。
     * 默认未收藏（收藏状态由 ToolFavoritesRepository 动态注入）。
     */
    private val _registeredTools = MutableStateFlow(
        listOf(
            ToolItem(
                id = "image_obfuscation",
                nameRes = R.string.tool_image_obfuscation_name,
                descriptionRes = R.string.tool_image_obfuscation_desc,
                icon = Icons.Rounded.Image,
                category = ToolCategory.TEXT_IMAGE,
                isFavorite = false,
                hasDot = false
            )
        )
    )

    override val availableTools: Flow<List<ToolItem>> = _registeredTools.asStateFlow()

    override fun getToolById(toolId: String): ToolItem? {
        return _registeredTools.value.find { it.id == toolId }
    }
}
