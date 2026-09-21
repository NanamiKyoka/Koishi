package com.nanami.koishi.feature.tools.image_obfuscation.engine

import androidx.annotation.StringRes
import com.nanami.koishi.R

enum class ObfuscationMode(
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val requiresKey: Boolean,
    val isNumericKey: Boolean,
    val defaultKey: String,
    @StringRes val keyHintRes: Int
) {
    TOMATO_GILBERT(
        titleRes = R.string.mode_tomato_title,
        descRes = R.string.mode_tomato_desc,
        requiresKey = false,
        isNumericKey = false,
        defaultKey = "1",
        keyHintRes = R.string.mode_tomato_hint
    ),
    BLOCK(
        titleRes = R.string.mode_block_title,
        descRes = R.string.mode_block_desc,
        requiresKey = true,
        isNumericKey = false,
        defaultKey = "0.666",
        keyHintRes = R.string.hint_custom_text_key
    ),
    ROW_PIXEL(
        titleRes = R.string.mode_row_title,
        descRes = R.string.mode_row_desc,
        requiresKey = true,
        isNumericKey = false,
        defaultKey = "0.666",
        keyHintRes = R.string.hint_custom_text_key
    ),
    PER_PIXEL(
        titleRes = R.string.mode_pixel_title,
        descRes = R.string.mode_pixel_desc,
        requiresKey = true,
        isNumericKey = false,
        defaultKey = "0.666",
        keyHintRes = R.string.hint_custom_text_key
    ),
    PIC_ENCRYPT_ROW(
        titleRes = R.string.mode_pic_row_title,
        descRes = R.string.mode_pic_row_desc,
        requiresKey = true,
        isNumericKey = true,
        defaultKey = "0.666",
        keyHintRes = R.string.hint_decimal_key
    ),
    PIC_ENCRYPT_ROW_AND_COLUMN(
        titleRes = R.string.mode_pic_row_col_title,
        descRes = R.string.mode_pic_row_col_desc,
        requiresKey = true,
        isNumericKey = true,
        defaultKey = "0.666",
        keyHintRes = R.string.hint_decimal_key
    );
}

enum class ObfuscationAction(@StringRes val titleRes: Int) {
    OBFUSCATE(R.string.btn_obfuscate),
    DEOBFUSCATE(R.string.btn_deobfuscate)
}
