package com.nanami.koishi

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.nanami.koishi.core.crash.GlobalCrashHandler
import com.nanami.koishi.core.data.cache.TempImageCleaner
import com.nanami.koishi.core.data.cache.TempImageStore
import com.nanami.koishi.core.di.coreDataModule
import com.nanami.koishi.core.di.featureModule
import com.nanami.koishi.core.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.io.File

/**
 * Koishi 全局 Application 入口
 */
class KoishiApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@KoishiApp)
            modules(coreDataModule, featureModule, viewModelModule)
        }
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
