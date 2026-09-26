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
 * 幻影坦克小工具强类型路由
 */
@Serializable
data object MirageTankRoute

/**
 * 水印图独立小工具强类型路由
 */
@Serializable
data object WatermarkRoute

/**
 * 图片素描独立小工具强类型路由
 */
@Serializable
data object ImageSketchRoute

/**
 * 历史上的今天独立小工具强类型路由
 */
@Serializable
data object TodayInHistoryRoute

/**
 * 做个决定独立小工具强类型路由
 */
@Serializable
data object DecisionMakerRoute

@Serializable
data object RulerRoute

@Serializable
data object CurrencyConverterRoute

@Serializable
data object VideoToGifRoute

/**
 * B站封面获取独立小工具强类型路由
 */
@Serializable
data object BiliCoverRoute

/**
 * 表情包制作独立小工具强类型路由
 */
@Serializable
data object MemeMakerRoute
