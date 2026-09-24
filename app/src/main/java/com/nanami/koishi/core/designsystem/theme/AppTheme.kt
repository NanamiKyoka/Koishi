package com.nanami.koishi.core.designsystem.theme

import androidx.annotation.StringRes
import com.nanami.koishi.R

enum class AppTheme(@StringRes val titleRes: Int) {
    KOISHI(R.string.theme_koishi),
    MONET(R.string.theme_monet),
    CATPPUCCIN(R.string.theme_catppuccin),
    NORD(R.string.theme_nord),
    TOKYO_NIGHT(R.string.theme_tokyonight),
    GRUVBOX(R.string.theme_gruvbox),
    ROSE_PINE(R.string.theme_rosepine),
    EVERFOREST(R.string.theme_everforest),
    LAVENDER(R.string.theme_lavender),
    MONOCHROME(R.string.theme_monochrome)
}
