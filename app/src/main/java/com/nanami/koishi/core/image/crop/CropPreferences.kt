package com.nanami.koishi.core.image.crop

import android.content.Context

/**
 * 图片裁剪个性化偏好持久化管理
 */
object CropPreferences {
    private const val PREFS_NAME = "koishi_crop_settings"
    private const val KEY_CONSTRAIN_TO_IMAGE = "key_constrain_to_image"

    /**
     * 获取是否限定在图片范围内（默认 true）
     */
    fun isConstrainToImage(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_CONSTRAIN_TO_IMAGE, true)
    }

    /**
     * 保存是否限定在图片范围内的设置
     */
    fun setConstrainToImage(context: Context, constrain: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_CONSTRAIN_TO_IMAGE, constrain).apply()
    }
}
