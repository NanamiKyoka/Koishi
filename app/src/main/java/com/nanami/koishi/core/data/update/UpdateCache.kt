package com.nanami.koishi.core.data.update

import android.content.Context
import android.content.SharedPreferences

interface UpdateCache {
    fun cachedReleaseBody(): String?
    fun lastCheckedAt(): Long
    fun save(releaseBody: String, checkedAt: Long)
}

object NoopUpdateCache : UpdateCache {
    override fun cachedReleaseBody(): String? = null
    override fun lastCheckedAt(): Long = 0L
    override fun save(releaseBody: String, checkedAt: Long) = Unit
}

class SharedPreferencesUpdateCache(context: Context) : UpdateCache {

    private val preferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun cachedReleaseBody(): String? = preferences.getString(KEY_RELEASE_BODY, null)

    override fun lastCheckedAt(): Long = preferences.getLong(KEY_LAST_CHECKED_AT, 0L)

    override fun save(releaseBody: String, checkedAt: Long) {
        preferences.edit()
            .putString(KEY_RELEASE_BODY, releaseBody)
            .putLong(KEY_LAST_CHECKED_AT, checkedAt)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "koishi_update"
        const val KEY_RELEASE_BODY = "cached_release_body"
        const val KEY_LAST_CHECKED_AT = "last_checked_at"
    }
}
