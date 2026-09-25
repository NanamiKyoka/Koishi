package com.nanami.koishi.core.data.cache

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 负责临时图片的生命周期回收：冷启动时清空整个临时目录，
 * 应用退到后台时回收上一轮会话遗留的陈旧文件（保留本轮的近期产物以免打断正在进行的预览/分享）。
 */
class TempImageCleaner(private val application: Application) :
    Application.ActivityLifecycleCallbacks {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var startedActivities = 0

    fun install() {
        application.registerActivityLifecycleCallbacks(this)
        scope.launch {
            TempImageStore.purgeAll(application)
            TempImageStore.purgeLegacyDirs(application)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities++
    }

    override fun onActivityStopped(activity: Activity) {
        if (startedActivities > 0) startedActivities--
        if (startedActivities == 0) {
            scope.launch { TempImageStore.purgeOlderThan(application, STALE_THRESHOLD_MILLIS) }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    private companion object {
        const val STALE_THRESHOLD_MILLIS = 30 * 60 * 1000L
    }
}
