package com.nanami.koishi.core.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nanami.koishi.MainActivity
import com.nanami.koishi.core.designsystem.KoishiTheme

class CrashActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CRASH_REPORT = "extra_crash_report"
        const val EXTRA_CRASH_MESSAGE = "extra_crash_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashReport = intent.getStringExtra(EXTRA_CRASH_REPORT) ?: "未获取到崩溃日志详情"
        val crashMessage = intent.getStringExtra(EXTRA_CRASH_MESSAGE) ?: "未知异常"

        setContent {
            KoishiTheme {
                CrashScreen(
                    crashReport = crashReport,
                    errorMessage = crashMessage,
                    onRestartClick = {
                        val restartIntent = Intent(this@CrashActivity, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(restartIntent)
                        finish()
                    }
                )
            }
        }
    }
}
