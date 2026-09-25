package com.nanami.koishi

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.nanami.koishi.core.crash.GlobalCrashHandler
import com.nanami.koishi.core.data.cache.TempImageCleaner
import com.nanami.koishi.core.data.cache.TempImageStore
import com.nanami.koishi.core.data.storage.ToolStorageDao
import com.nanami.koishi.core.data.storage.ToolStorageDatabase
import java.io.File

/**
 * Koishi 全局 Application 入口
 */
class KoishiApp : Application(), ImageLoaderFactory {

    val toolStorageDao: ToolStorageDao by lazy {
        ToolStorageDatabase.get(this).toolStorageDao()
    }

    override fun onCreate() {
        super.onCreate()
        GlobalCrashHandler.initialize(this)
        TempImageCleaner(this).install()
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.20)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(TempImageStore.root(this), "coil"))
                .maxSizeBytes(DISK_CACHE_MAX_BYTES)
                .build()
        }
        .build()

    private companion object {
        const val DISK_CACHE_MAX_BYTES = 64L * 1024 * 1024
    }
}
