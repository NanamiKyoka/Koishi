package com.nanami.koishi.core.data.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object StorageUsage {

    private const val UPDATES_DIR_NAME = "updates"

    suspend fun cacheSizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        directorySize(context.cacheDir)
    }

    suspend fun dataSizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        val dataRoot = context.filesDir?.parentFile ?: return@withContext 0L
        directorySize(context.filesDir) +
            directorySize(File(dataRoot, "databases")) +
            directorySize(File(dataRoot, "shared_prefs"))
    }

    suspend fun clearCache(context: Context): Long = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir ?: return@withContext 0L
        val freed = directorySize(cacheDir)
        cacheDir.listFiles()
            ?.filterNot { it.name == UPDATES_DIR_NAME }
            ?.forEach { runCatching { it.deleteRecursively() } }
        freed
    }

    private fun directorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        return dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    }
}