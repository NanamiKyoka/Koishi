package com.nanami.koishi.core.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import com.nanami.koishi.feature.settings.AppLanguage
import java.util.Locale

object LocaleHelper {

    fun getTargetLocale(context: Context): Locale? {
        val prefs = context.getSharedPreferences("koishi_settings", Context.MODE_PRIVATE)
        val langOrdinal = prefs.getInt("language", AppLanguage.SYSTEM.ordinal)
        val language = AppLanguage.entries.getOrElse(langOrdinal) { AppLanguage.SYSTEM }
        return when (language) {
            AppLanguage.ZH -> Locale.SIMPLIFIED_CHINESE
            AppLanguage.EN -> Locale.ENGLISH
            AppLanguage.SYSTEM -> null
        }
    }

    fun applyLocale(baseContext: Context): Context {
        val targetLocale = getTargetLocale(baseContext) ?: return baseContext
        val config = Configuration(baseContext.resources.configuration).apply {
            setLocale(targetLocale)
            setLayoutDirection(targetLocale)
        }
        return baseContext.createConfigurationContext(config)
    }

    fun wrapWithActivity(
        activityContext: Context,
        effectiveConfig: Configuration,
        targetLocale: Locale?
    ): Pair<Configuration, Context> {
        return if (targetLocale != null) {
            val config = Configuration(effectiveConfig).apply {
                setLocale(targetLocale)
                setLayoutDirection(targetLocale)
            }
            val confContext = activityContext.createConfigurationContext(config)
            val wrapped = object : ContextWrapper(activityContext) {
                override fun getResources(): Resources = confContext.resources
                override fun createConfigurationContext(overrideConfiguration: Configuration): Context {
                    return confContext.createConfigurationContext(overrideConfiguration)
                }
            }
            config to wrapped
        } else {
            effectiveConfig to activityContext
        }
    }
}
