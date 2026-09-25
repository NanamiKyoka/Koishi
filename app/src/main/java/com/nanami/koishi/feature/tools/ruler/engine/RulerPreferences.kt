package com.nanami.koishi.feature.tools.ruler.engine

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration

object RulerPreferences {

    private const val PREFERENCES_NAME = "ruler_preferences"
    private const val KEY_CALIBRATION_SCALE = "calibration_scale"
    private const val KEY_LAST_TARGET_MM = "last_target_mm"

    const val ID1_CARD_SHORT_MM = 54.0f
    const val ID1_CARD_LONG_MM = 85.6f

    fun getCalibrationScale(context: Context): Float {
        val prefs = preferences(context)
        return prefs.getFloat(KEY_CALIBRATION_SCALE, 1.0f)
    }

    fun setCalibrationScale(context: Context, scale: Float) {
        val safeScale = scale.coerceIn(0.2f, 5.0f)
        preferences(context).edit().putFloat(KEY_CALIBRATION_SCALE, safeScale).apply()
    }

    fun resetCalibration(context: Context) {
        preferences(context).edit().putFloat(KEY_CALIBRATION_SCALE, 1.0f).apply()
    }

    fun getLastTargetMm(context: Context): Float {
        val prefs = preferences(context)
        return prefs.getFloat(KEY_LAST_TARGET_MM, ID1_CARD_SHORT_MM)
    }

    fun setLastTargetMm(context: Context, mm: Float) {
        val safeMm = mm.coerceIn(5.0f, 500.0f)
        preferences(context).edit().putFloat(KEY_LAST_TARGET_MM, safeMm).apply()
    }

    fun getBasePxPerMm(context: Context): Float {
        val dm = context.resources.displayMetrics
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val dpi = if (isLandscape) {
            if (dm.widthPixels > dm.heightPixels && dm.ydpi > 0f) dm.ydpi else dm.xdpi
        } else {
            if (dm.xdpi > 0f) dm.xdpi else dm.ydpi
        }
        val safeDpi = if (dpi in 100f..1200f) dpi else dm.densityDpi.toFloat()
        return safeDpi / 25.4f
    }

    fun getCalibratedPxPerMm(context: Context): Float {
        return getBasePxPerMm(context) * getCalibrationScale(context)
    }

    private fun preferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
}
