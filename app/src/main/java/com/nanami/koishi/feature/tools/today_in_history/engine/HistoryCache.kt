package com.nanami.koishi.feature.tools.today_in_history.engine

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Calendar

@Serializable
private data class CachedEvent(
    val title: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val content: String = "",
    val imageUrl: String = ""
)

@Serializable
private data class CachedDay(
    val month: Int,
    val day: Int,
    val sourceName: String,
    val cachedOnDayKey: String,
    val events: List<CachedEvent>
)

class HistoryCache(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun readDay(month: Int, day: Int, todayKey: String): HistoryDay? {
        val raw = prefs.getString(cacheKey(month, day), null) ?: return null
        val cached = try {
            json.decodeFromString<CachedDay>(raw)
        } catch (e: Exception) {
            return null
        }
        if (cached.cachedOnDayKey != todayKey) return null
        if (cached.events.isEmpty()) return null

        val source = HistorySource.entries.firstOrNull { it.name == cached.sourceName }
            ?: HistorySource.XXAPI

        return HistoryDay(
            month = cached.month,
            day = cached.day,
            events = cached.events.map {
                HistoryEvent(
                    title = it.title,
                    year = it.year,
                    month = it.month,
                    day = it.day,
                    content = it.content,
                    imageUrl = it.imageUrl
                )
            },
            source = source
        )
    }

    suspend fun writeDay(day: HistoryDay, todayKey: String) = withContext(Dispatchers.IO) {
        val payload = CachedDay(
            month = day.month,
            day = day.day,
            sourceName = day.source.name,
            cachedOnDayKey = todayKey,
            events = day.events.map {
                CachedEvent(
                    title = it.title,
                    year = it.year,
                    month = it.month,
                    day = it.day,
                    content = it.content,
                    imageUrl = it.imageUrl
                )
            }
        )
        prefs.edit()
            .putString(cacheKey(day.month, day.day), json.encodeToString(payload))
            .apply()
    }

    suspend fun writeEmpty(month: Int, day: Int, todayKey: String) = withContext(Dispatchers.IO) {
        val payload = CachedDay(
            month = month,
            day = day,
            sourceName = HistorySource.XXAPI.name,
            cachedOnDayKey = todayKey,
            events = emptyList()
        )
        prefs.edit()
            .putString(cacheKey(month, day), json.encodeToString(payload))
            .apply()
    }

    fun isEmptyResultCached(month: Int, day: Int, todayKey: String): Boolean {
        val raw = prefs.getString(cacheKey(month, day), null) ?: return false
        val cached = try {
            json.decodeFromString<CachedDay>(raw)
        } catch (e: Exception) {
            return false
        }
        return cached.cachedOnDayKey == todayKey && cached.events.isEmpty()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isCached(month: Int, day: Int, todayKey: String): Boolean =
        readDay(month, day, todayKey) != null

    private fun cacheKey(month: Int, day: Int) = "day_%02d_%02d".format(month, day)

    companion object {
        private const val PREFS_NAME = "koishi_history_cache"

        fun todayKey(): String {
            val calendar = Calendar.getInstance()
            return "%04d-%02d-%02d".format(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
}
