package com.nanami.koishi.core.designsystem.theme.colorscheme

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.DynamicScheme
import com.materialkolor.toColorScheme

val isDynamicColorAvailable: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

internal class MonetColorScheme(context: Context) : BaseColorScheme() {

    private val monet: BaseColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        SystemMonetColorScheme(context)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        val seed = WallpaperManager.getInstance(context)
            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            ?.primaryColor
            ?.toArgb()
        if (seed != null) SeedMonetColorScheme(Color(seed)) else KoishiColorScheme
    } else {
        KoishiColorScheme
    }

    override val darkScheme: ColorScheme
        get() = monet.darkScheme

    override val lightScheme: ColorScheme
        get() = monet.lightScheme
}

@RequiresApi(Build.VERSION_CODES.S)
private class SystemMonetColorScheme(context: Context) : BaseColorScheme() {

    override val lightScheme = dynamicLightColorScheme(context)
    override val darkScheme = dynamicDarkColorScheme(context)
}

internal class SeedMonetColorScheme(seed: Color) : BaseColorScheme() {

    override val lightScheme = generateScheme(seed = seed, dark = false)
    override val darkScheme = generateScheme(seed = seed, dark = true)

    private fun generateScheme(seed: Color, dark: Boolean): ColorScheme {
        return DynamicScheme(
            seedColor = seed,
            isDark = dark,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.Expressive
        ).toColorScheme(isAmoled = false)
    }
}
