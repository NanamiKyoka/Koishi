package com.nanami.koishi.feature.tools.today_in_history.engine

import android.content.Context
import android.content.SharedPreferences

class HistoryPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var showApiAppKey: String
        get() = prefs.getString(KEY_SHOW_API_APP_KEY, "").orEmpty()
        private set(value) {
            prefs.edit().putString(KEY_SHOW_API_APP_KEY, value.trim()).apply()
        }

    var lastUsedSource: HistorySource
        get() {
            val name = prefs.getString(KEY_LAST_SOURCE, null) ?: return HistorySource.XXAPI
            return HistorySource.entries.firstOrNull { it.name == name } ?: HistorySource.XXAPI
        }
        private set(value) {
            prefs.edit().putString(KEY_LAST_SOURCE, value.name).apply()
        }

    val hasShowApiKey: Boolean
        get() = showApiAppKey.isNotBlank()

    fun saveShowApiAppKey(key: String) {
        showApiAppKey = key
    }

    fun recordSource(source: HistorySource) {
        lastUsedSource = source
    }

    companion object {
        private const val PREFS_NAME = "koishi_history_prefs"
        private const val KEY_SHOW_API_APP_KEY = "showapi_app_key"
        private const val KEY_LAST_SOURCE = "last_used_source"
    }
}
