package com.nanami.koishi.navigation

import kotlinx.serialization.Serializable

/**
 * 首页路由 (Type-Safe Route)
 */
@Serializable
data object HomeRoute

/**
 * 收藏夹路由
 */
@Serializable
data object FavoritesRoute

/**
 * 设置页面路由
 */
@Serializable
data object SettingsRoute

/**
 * 图片混淆/反混淆独立小工具强类型路由
 */
@Serializable
data object ImageObfuscationRoute

/**
 * 二维码工具独立小工具强类型路由
 */
@Serializable
data object QrToolRoute

@Serializable
data object ImageStitchingRoute

/**
 * 多格切图小工具强类型路由
 */
@Serializable
data object GridSplitRoute

/**
 * 以图搜图小工具强类型路由
 */
@Serializable
data object ImageSearchRoute

/**
 * 工具详情或独立小工具通用强类型路由
 *
 * @property toolId 工具唯一ID
 * @property toolTitle 工具名称
 */
@Serializable
data class ToolDetailRoute(
    val toolId: String,
    val toolTitle: String
)
