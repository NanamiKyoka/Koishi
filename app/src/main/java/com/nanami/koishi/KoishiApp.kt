package com.nanami.koishi

import android.app.Application
import com.nanami.koishi.core.crash.GlobalCrashHandler

/**
 * Koishi 全局 Application 入口
 */
class KoishiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalCrashHandler.initialize(this)
    }
}
