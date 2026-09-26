package com.nanami.koishi.core.model

import androidx.annotation.StringRes
import com.nanami.koishi.R

enum class ToolCategory(@StringRes val titleRes: Int) {
    ALL(R.string.category_all),
    LIFE(R.string.category_life),
    IMAGE_APPS(R.string.category_image_apps),
    CALCULATION(R.string.category_calculation),
    OTHER(R.string.category_other);
}
