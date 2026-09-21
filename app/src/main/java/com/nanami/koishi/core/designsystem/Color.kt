package com.nanami.koishi.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 古明地恋 (Koishi Komeiji) 配色体系
 *
 * 视觉考据与调色灵感：
 * - Primary (发色 & 衬衣)：灵动清新的水绿 / 灰苔藓绿 (Sage & Mint Green)
 * - Secondary (第三只眼 & 封闭线条)：深邃神秘的紫罗兰 / 蓝紫灰 (Iris & Violet)
 * - Tertiary (礼帽缎带 & 蔷薇细节)：明媚优雅的琥珀缎带金黄 (Amber Ribbon Gold)
 * - Surface / Containers (黑色礼帽与神秘背景)：微绿调的雅白与深邃墨炭黑，构建完美的 MD3 容器阶梯
 */

// 浅色模式 - 恋恋调色板 (Light Palette)
val KoishiLightPrimary = Color(0xFF1E6A44)
val KoishiLightOnPrimary = Color(0xFFFFFFFF)
val KoishiLightPrimaryContainer = Color(0xFFA6F2C1)
val KoishiLightOnPrimaryContainer = Color(0xFF002111)

val KoishiLightSecondary = Color(0xFF5F5979)
val KoishiLightOnSecondary = Color(0xFFFFFFFF)
val KoishiLightSecondaryContainer = Color(0xFFE5DEFF)
val KoishiLightOnSecondaryContainer = Color(0xFF1C1733)

val KoishiLightTertiary = Color(0xFF755C00)
val KoishiLightOnTertiary = Color(0xFFFFFFFF)
val KoishiLightTertiaryContainer = Color(0xFFFFE08B)
val KoishiLightOnTertiaryContainer = Color(0xFF241A00)

val KoishiLightError = Color(0xFFBA1A1A)
val KoishiLightOnError = Color(0xFFFFFFFF)
val KoishiLightErrorContainer = Color(0xFFFFDAD6)
val KoishiLightOnErrorContainer = Color(0xFF410002)

val KoishiLightBackground = Color(0xFFF6FAF4)
val KoishiLightOnBackground = Color(0xFF181D19)
val KoishiLightSurface = Color(0xFFF6FAF4)
val KoishiLightOnSurface = Color(0xFF181D19)
val KoishiLightSurfaceVariant = Color(0xFFDCE5DC)
val KoishiLightOnSurfaceVariant = Color(0xFF404942)
val KoishiLightOutline = Color(0xFF717A72)
val KoishiLightOutlineVariant = Color(0xFFC0C9C0)

// MD3 Surface Containers (Light)
val KoishiLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val KoishiLightSurfaceContainerLow = Color(0xFFF0F5EE)
val KoishiLightSurfaceContainer = Color(0xFFEBEFE9)
val KoishiLightSurfaceContainerHigh = Color(0xFFE5EAE3)
val KoishiLightSurfaceContainerHighest = Color(0xFFDFE4DE)

// 深色模式 - 恋之调色板 (Dark Palette)
val KoishiDarkPrimary = Color(0xFF8CD5A6)
val KoishiDarkOnPrimary = Color(0xFF003820)
val KoishiDarkPrimaryContainer = Color(0xFF005231)
val KoishiDarkOnPrimaryContainer = Color(0xFFA6F2C1)

val KoishiDarkSecondary = Color(0xFFC8BFEE)
val KoishiDarkOnSecondary = Color(0xFF302B49)
val KoishiDarkSecondaryContainer = Color(0xFF474160)
val KoishiDarkOnSecondaryContainer = Color(0xFFE5DEFF)

val KoishiDarkTertiary = Color(0xFFECC34A)
val KoishiDarkOnTertiary = Color(0xFF3E2E00)
val KoishiDarkTertiaryContainer = Color(0xFF594400)
val KoishiDarkOnTertiaryContainer = Color(0xFFFFE08B)

val KoishiDarkError = Color(0xFFFFB4AB)
val KoishiDarkOnError = Color(0xFF690005)
val KoishiDarkErrorContainer = Color(0xFF93000A)
val KoishiDarkOnErrorContainer = Color(0xFFFFDAD6)

val KoishiDarkBackground = Color(0xFF0F1511)
val KoishiDarkOnBackground = Color(0xFFDEE4DD)
val KoishiDarkSurface = Color(0xFF0F1511)
val KoishiDarkOnSurface = Color(0xFFDEE4DD)
val KoishiDarkSurfaceVariant = Color(0xFF404942)
val KoishiDarkOnSurfaceVariant = Color(0xFFC0C9C0)
val KoishiDarkOutline = Color(0xFF8A938B)
val KoishiDarkOutlineVariant = Color(0xFF404942)

// MD3 Surface Containers (Dark)
val KoishiDarkSurfaceContainerLowest = Color(0xFF0A0F0C)
val KoishiDarkSurfaceContainerLow = Color(0xFF171D19)
val KoishiDarkSurfaceContainer = Color(0xFF1B211D)
val KoishiDarkSurfaceContainerHigh = Color(0xFF252C27)
val KoishiDarkSurfaceContainerHighest = Color(0xFF303732)

/**
 * 恋恋浅色配色方案
 */
val KoishiLightColorScheme = lightColorScheme(
    primary = KoishiLightPrimary,
    onPrimary = KoishiLightOnPrimary,
    primaryContainer = KoishiLightPrimaryContainer,
    onPrimaryContainer = KoishiLightOnPrimaryContainer,
    secondary = KoishiLightSecondary,
    onSecondary = KoishiLightOnSecondary,
    secondaryContainer = KoishiLightSecondaryContainer,
    onSecondaryContainer = KoishiLightOnSecondaryContainer,
    tertiary = KoishiLightTertiary,
    onTertiary = KoishiLightOnTertiary,
    tertiaryContainer = KoishiLightTertiaryContainer,
    onTertiaryContainer = KoishiLightOnTertiaryContainer,
    error = KoishiLightError,
    onError = KoishiLightOnError,
    errorContainer = KoishiLightErrorContainer,
    onErrorContainer = KoishiLightOnErrorContainer,
    background = KoishiLightBackground,
    onBackground = KoishiLightOnBackground,
    surface = KoishiLightSurface,
    onSurface = KoishiLightOnSurface,
    surfaceVariant = KoishiLightSurfaceVariant,
    onSurfaceVariant = KoishiLightOnSurfaceVariant,
    outline = KoishiLightOutline,
    outlineVariant = KoishiLightOutlineVariant,
    surfaceContainerLowest = KoishiLightSurfaceContainerLowest,
    surfaceContainerLow = KoishiLightSurfaceContainerLow,
    surfaceContainer = KoishiLightSurfaceContainer,
    surfaceContainerHigh = KoishiLightSurfaceContainerHigh,
    surfaceContainerHighest = KoishiLightSurfaceContainerHighest
)

/**
 * 恋恋深色配色方案
 */
val KoishiDarkColorScheme = darkColorScheme(
    primary = KoishiDarkPrimary,
    onPrimary = KoishiDarkOnPrimary,
    primaryContainer = KoishiDarkPrimaryContainer,
    onPrimaryContainer = KoishiDarkOnPrimaryContainer,
    secondary = KoishiDarkSecondary,
    onSecondary = KoishiDarkOnSecondary,
    secondaryContainer = KoishiDarkSecondaryContainer,
    onSecondaryContainer = KoishiDarkOnSecondaryContainer,
    tertiary = KoishiDarkTertiary,
    onTertiary = KoishiDarkOnTertiary,
    tertiaryContainer = KoishiDarkTertiaryContainer,
    onTertiaryContainer = KoishiDarkOnTertiaryContainer,
    error = KoishiDarkError,
    onError = KoishiDarkOnError,
    errorContainer = KoishiDarkErrorContainer,
    onErrorContainer = KoishiDarkOnErrorContainer,
    background = KoishiDarkBackground,
    onBackground = KoishiDarkOnBackground,
    surface = KoishiDarkSurface,
    onSurface = KoishiDarkOnSurface,
    surfaceVariant = KoishiDarkSurfaceVariant,
    onSurfaceVariant = KoishiDarkOnSurfaceVariant,
    outline = KoishiDarkOutline,
    outlineVariant = KoishiDarkOutlineVariant,
    surfaceContainerLowest = KoishiDarkSurfaceContainerLowest,
    surfaceContainerLow = KoishiDarkSurfaceContainerLow,
    surfaceContainer = KoishiDarkSurfaceContainer,
    surfaceContainerHigh = KoishiDarkSurfaceContainerHigh,
    surfaceContainerHighest = KoishiDarkSurfaceContainerHighest
)
