package com.nanami.koishi.core.model

import androidx.annotation.StringRes
import com.nanami.koishi.R

enum class ToolCategory(@StringRes val titleRes: Int) {
    ALL(R.string.category_all),
    COMMON(R.string.category_common),
    CALC_CONVERT(R.string.category_calc_convert),
    DEV_CODE(R.string.category_dev_code),
    HARDWARE(R.string.category_hardware),
    IMAGE_APPS(R.string.category_text_image);

    companion object {
        @Deprecated("Renamed to IMAGE_APPS", ReplaceWith("IMAGE_APPS"))
        val TEXT_IMAGE = IMAGE_APPS
    }
}
