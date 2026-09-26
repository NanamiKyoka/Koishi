package com.nanami.koishi.core.data.sync

import android.content.Context
import android.content.SharedPreferences

data class WebDavConfig(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val syncSensitive: Boolean = false,
    val lastBackupTime: Long = 0L,
    val lastRestoreTime: Long = 0L
)

object WebDavPreferences {

    private const val PREFS_NAME = "koishi_webdav"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_SYNC_SENSITIVE = "sync_sensitive"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    private const val KEY_LAST_RESTORE_TIME = "last_restore_time"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadConfig(context: Context): WebDavConfig {
        val p = prefs(context)
        return WebDavConfig(
            serverUrl = p.getString(KEY_SERVER_URL, "").orEmpty(),
            username = p.getString(KEY_USERNAME, "").orEmpty(),
            password = p.getString(KEY_PASSWORD, "").orEmpty(),
            syncSensitive = p.getBoolean(KEY_SYNC_SENSITIVE, false),
            lastBackupTime = p.getLong(KEY_LAST_BACKUP_TIME, 0L),
            lastRestoreTime = p.getLong(KEY_LAST_RESTORE_TIME, 0L)
        )
    }

    fun saveConfig(context: Context, config: WebDavConfig) {
        prefs(context).edit()
            .putString(KEY_SERVER_URL, config.serverUrl)
            .putString(KEY_USERNAME, config.username)
            .putString(KEY_PASSWORD, config.password)
            .putBoolean(KEY_SYNC_SENSITIVE, config.syncSensitive)
            .putLong(KEY_LAST_BACKUP_TIME, config.lastBackupTime)
            .putLong(KEY_LAST_RESTORE_TIME, config.lastRestoreTime)
            .apply()
    }

    fun updateLastBackupTime(context: Context, timestamp: Long) {
        prefs(context).edit().putLong(KEY_LAST_BACKUP_TIME, timestamp).apply()
    }

    fun updateLastRestoreTime(context: Context, timestamp: Long) {
        prefs(context).edit().putLong(KEY_LAST_RESTORE_TIME, timestamp).apply()
    }
}
