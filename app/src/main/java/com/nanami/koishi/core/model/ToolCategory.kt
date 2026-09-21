package com.nanami.koishi.core.model

import androidx.annotation.StringRes
import com.nanami.koishi.R

enum class ToolCategory(@StringRes val titleRes: Int) {
    ALL(R.string.category_all),
    COMMON(R.string.category_common),
    CALC_CONVERT(R.string.category_calc_convert),
    DEV_CODE(R.string.category_dev_code),
    HARDWARE(R.string.category_hardware),
    TEXT_IMAGE(R.string.category_text_image)
}
