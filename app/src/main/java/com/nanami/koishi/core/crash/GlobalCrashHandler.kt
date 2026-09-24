package com.nanami.koishi.core.crash

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlin.system.exitProcess

/**
 * 全局未捕获异常拦截器
 */
class GlobalCrashHandler private constructor(
    private val application: Application,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            Log.e("KoishiCrash", "FATAL EXCEPTION: ${thread.name}", throwable)

            val packageInfo = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    application.packageManager.getPackageInfo(
                        application.packageName,
                        android.content.pm.PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    application.packageManager.getPackageInfo(application.packageName, 0)
                }
            }.getOrNull()

            val versionName = packageInfo?.versionName ?: "Unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo?.longVersionCode ?: 0L
            } else {
                @Suppress("DEPRECATION")
                packageInfo?.versionCode?.toLong() ?: 0L
            }

            val crashReport = buildString {
                appendLine("=== Koishi Crash Report ===")
                appendLine("App Version: $versionName ($versionCode)")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                @Suppress("DEPRECATION")
                appendLine("Thread: ${thread.name} (id: ${thread.id})")
                appendLine("===========================")
                appendLine()
                appendLine(throwable.stackTraceToString())
            }

            val intent = Intent(application, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_CRASH_REPORT, crashReport)
                putExtra(CrashActivity.EXTRA_CRASH_MESSAGE, throwable.localizedMessage ?: throwable.javaClass.simpleName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            application.startActivity(intent)

            // 终止崩溃的主进程，交给独立进程的 CrashActivity 展示
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        } catch (e: Exception) {
            Log.e("KoishiCrash", "Failed to handle uncaught exception", e)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        fun initialize(application: Application) {
            val handler = GlobalCrashHandler(
                application = application,
                defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            )
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }
    }
}
