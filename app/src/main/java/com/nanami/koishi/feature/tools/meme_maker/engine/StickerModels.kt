package com.nanami.koishi.feature.tools.meme_maker.engine

import android.graphics.Color
import android.graphics.Typeface
import androidx.annotation.StringRes
import com.nanami.koishi.R
import java.util.UUID

enum class StickerKind {
    IMAGE,
    TEXT
}

enum class MemeTextFont(@StringRes val labelRes: Int) {
    DEFAULT(R.string.meme_font_default),
    BOLD(R.string.meme_font_bold),
    SERIF(R.string.meme_font_serif),
    MONOSPACE(R.string.meme_font_monospace);

    val typeface: Typeface
        get() = when (this) {
            DEFAULT -> Typeface.DEFAULT
            BOLD -> Typeface.DEFAULT_BOLD
            SERIF -> Typeface.SERIF
            MONOSPACE -> Typeface.MONOSPACE
        }
}

/**
 * 贴纸素材来源，远程素材包内的文件与用户相册导入的图片都统一成贴纸
 */
sealed interface StickerSource {

    val key: String

    data class Asset(val packId: String, val fileName: String) : StickerSource {
        override val key: String get() = "asset:$packId/$fileName"
    }

    data class Local(val uri: String) : StickerSource {
        override val key: String get() = "local:$uri"
    }
}

/**
 * 贴纸在画布中的摆放参数，全部以画布短边为基准做归一化，
 * 保证预览尺寸与导出分辨率下视觉表现完全一致
 */
data class StickerTransform(
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val scale: Float = 0.4f,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
    val flipHorizontal: Boolean = false
)

data class TextStickerSpec(
    val text: String,
    val color: Int = Color.WHITE,
    val font: MemeTextFont = MemeTextFont.DEFAULT,
    val strokeColor: Int = Color.BLACK,
    val strokeEnabled: Boolean = true
)

/**
 * [groupKey] 标记同一份文字的来源，复制或批量调整时用得上
 */
data class PlacedSticker(
    val id: String = UUID.randomUUID().toString(),
    val kind: StickerKind,
    val transform: StickerTransform = StickerTransform(),
    val source: StickerSource? = null,
    val text: TextStickerSpec? = null,
    val groupKey: String? = null
)
