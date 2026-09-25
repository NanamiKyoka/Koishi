package com.nanami.koishi.core.data.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 一次性图片等临时资源的统一落盘位置，固定在 cacheDir/temp_images 内，
 * 与 filesDir 下的长效数据完全隔离，可被系统缓存回收机制清理。
 */
object TempImageStore {

    const val DIR_NAME = "temp_images"

    private const val LEGACY_COIL_DIR_NAME = "image_cache"

    fun root(context: Context): File =
        File(context.cacheDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    fun newFile(context: Context, prefix: String, extension: String): File =
        File(root(context), "${prefix}_${System.currentTimeMillis()}.$extension")

    suspend fun purgeAll(context: Context) = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, DIR_NAME)
        if (dir.exists()) dir.listFiles()?.forEach { it.deleteRecursively() }
    }

    suspend fun purgeOlderThan(context: Context, maxAgeMillis: Long) = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, DIR_NAME)
        if (!dir.exists()) return@withContext
        val threshold = System.currentTimeMillis() - maxAgeMillis
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < threshold) file.deleteRecursively()
        }
    }

    suspend fun purgeLegacyDirs(context: Context) = withContext(Dispatchers.IO) {
        val legacy = File(context.cacheDir, LEGACY_COIL_DIR_NAME)
        if (legacy.exists()) legacy.deleteRecursively()
    }
}
