package com.nanami.koishi.feature.tools.today_in_history.engine

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
private data class CachedPayload(
    val sourceName: String,
    val events: List<CachedEvent>
)

/**
 * 长效缓存记录：以日期为键、年份为有效期，跨年后自动判定失效并重新拉取。
 */
@Serializable
data class HistoryTodayCache(
    val dateKey: String,
    val cachedYear: Int,
    val payload: String
)

/**
 * 缓存写入/命中时刻的基准：dateKey 为所查询日期的 MM-dd，year 为当前系统年份。
 */
data class HistoryCacheStamp(val dateKey: String, val year: Int) {
    companion object {
        fun forDate(month: Int, day: Int): HistoryCacheStamp = HistoryCacheStamp(
            dateKey = "%02d-%02d".format(month, day),
            year = Calendar.getInstance().get(Calendar.YEAR)
        )
    }
}

class HistoryCache(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun readDay(month: Int, day: Int, stamp: HistoryCacheStamp): HistoryDay? =
        withContext(Dispatchers.IO) {
            val cached = readValidCache(month, day, stamp) ?: return@withContext null
            val payload = decodePayload(cached.payload) ?: return@withContext null
            if (payload.events.isEmpty()) return@withContext null

            val source = HistorySource.entries.firstOrNull { it.name == payload.sourceName }
                ?: HistorySource.XXAPI

            HistoryDay(
                month = month,
                day = day,
                events = payload.events.map {
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

    suspend fun writeDay(day: HistoryDay, stamp: HistoryCacheStamp) = withContext(Dispatchers.IO) {
        val payload = CachedPayload(
            sourceName = day.source.name,
            events = day.events.map {
                CachedEvent(it.title, it.year, it.month, it.day, it.content, it.imageUrl)
            }
        )
        writeCache(day.month, day.day, stamp, payload)
    }

    suspend fun writeEmpty(month: Int, day: Int, stamp: HistoryCacheStamp) = withContext(Dispatchers.IO) {
        writeCache(month, day, stamp, CachedPayload(HistorySource.XXAPI.name, emptyList()))
    }

    suspend fun isEmptyResultCached(month: Int, day: Int, stamp: HistoryCacheStamp): Boolean =
        withContext(Dispatchers.IO) {
            val cached = readValidCache(month, day, stamp) ?: return@withContext false
            decodePayload(cached.payload)?.events.isNullOrEmpty()
        }

    suspend fun isCached(month: Int, day: Int, stamp: HistoryCacheStamp): Boolean =
        readDay(month, day, stamp) != null

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun writeCache(month: Int, day: Int, stamp: HistoryCacheStamp, payload: CachedPayload) {
        val record = HistoryTodayCache(
            dateKey = stamp.dateKey,
            cachedYear = stamp.year,
            payload = json.encodeToString(payload)
        )
        prefs.edit()
            .putString(cacheKey(month, day), json.encodeToString(record))
            .apply()
    }

    private fun readValidCache(month: Int, day: Int, stamp: HistoryCacheStamp): HistoryTodayCache? {
        val raw = prefs.getString(cacheKey(month, day), null) ?: return null
        val cached = try {
            json.decodeFromString<HistoryTodayCache>(raw)
        } catch (e: Exception) {
            return null
        }
        if (cached.dateKey != stamp.dateKey) return null
        if (cached.cachedYear != stamp.year) return null
        return cached
    }

    private fun decodePayload(payload: String): CachedPayload? = try {
        json.decodeFromString<CachedPayload>(payload)
    } catch (e: Exception) {
        null
    }

    private fun cacheKey(month: Int, day: Int) = "day_%02d_%02d".format(month, day)

    companion object {
        private const val PREFS_NAME = "koishi_history_cache"
    }
}
