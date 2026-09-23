package com.nanami.koishi.core.crash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.nanami.koishi.MainActivity
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.KoishiTheme
import com.nanami.koishi.core.util.LocaleHelper
import com.nanami.koishi.feature.settings.AppLanguage
import com.nanami.koishi.feature.settings.ThemeMode
import java.util.Locale

class CrashActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CRASH_REPORT = "extra_crash_report"
        const val EXTRA_CRASH_MESSAGE = "extra_crash_message"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashReport = intent.getStringExtra(EXTRA_CRASH_REPORT) ?: getString(R.string.crash_no_report)
        val crashMessage = intent.getStringExtra(EXTRA_CRASH_MESSAGE) ?: getString(R.string.crash_unknown_exception)

        val prefs = getSharedPreferences("koishi_settings", Context.MODE_PRIVATE)
        val themeOrdinal = prefs.getInt("theme_mode", ThemeMode.SYSTEM.ordinal)
        val themeMode = ThemeMode.entries.getOrElse(themeOrdinal) { ThemeMode.SYSTEM }
        val dynamicColor = prefs.getBoolean("dynamic_color", false)
        val langOrdinal = prefs.getInt("language", AppLanguage.SYSTEM.ordinal)
        val language = AppLanguage.entries.getOrElse(langOrdinal) { AppLanguage.SYSTEM }

        val targetLocale = when (language) {
            AppLanguage.ZH -> Locale.SIMPLIFIED_CHINESE
            AppLanguage.EN -> Locale.ENGLISH
            AppLanguage.SYSTEM -> null
        }

        setContent {
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val currentConfig = LocalConfiguration.current
            val (effectiveConfig, localizedContext) = remember(language, currentConfig) {
                LocaleHelper.wrapWithActivity(this@CrashActivity, currentConfig, targetLocale)
            }

            CompositionLocalProvider(
                LocalConfiguration provides effectiveConfig,
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this@CrashActivity
            ) {
                KoishiTheme(
                    darkTheme = isDark,
                    dynamicColor = dynamicColor
                ) {
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
}
