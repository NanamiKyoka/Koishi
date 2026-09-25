package com.nanami.koishi.feature.tools.today_in_history.engine

import com.nanami.koishi.core.data.storage.ToolStorageDao
import com.nanami.koishi.core.data.storage.ToolStorageEntity
import com.nanami.koishi.core.data.storage.ToolStorageJson
import kotlinx.serialization.Serializable
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
    val dateKey: String,
    val cachedYear: Int,
    val sourceName: String,
    val events: List<CachedEvent>
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

/**
 * 按日期分片的缓存：每个日期在通用表中占一条记录，跨年后由记录内的年份判定失效
 */
class HistoryCache(private val dao: ToolStorageDao) {

    private val json = ToolStorageJson.instance

    suspend fun readDay(month: Int, day: Int, stamp: HistoryCacheStamp): HistoryDay? {
        val cached = readValid(month, day, stamp) ?: return null
        if (cached.events.isEmpty()) return null

        val source = HistorySource.entries.firstOrNull { it.name == cached.sourceName }
            ?: HistorySource.XXAPI

        return HistoryDay(
            month = month,
            day = day,
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

    suspend fun writeDay(day: HistoryDay, stamp: HistoryCacheStamp) {
        write(
            month = day.month,
            day = day.day,
            payload = CachedDay(
                dateKey = stamp.dateKey,
                cachedYear = stamp.year,
                sourceName = day.source.name,
                events = day.events.map {
                    CachedEvent(it.title, it.year, it.month, it.day, it.content, it.imageUrl)
                }
            )
        )
    }

    suspend fun writeEmpty(month: Int, day: Int, stamp: HistoryCacheStamp) {
        write(
            month = month,
            day = day,
            payload = CachedDay(
                dateKey = stamp.dateKey,
                cachedYear = stamp.year,
                sourceName = HistorySource.XXAPI.name,
                events = emptyList()
            )
        )
    }

    suspend fun isEmptyResultCached(month: Int, day: Int, stamp: HistoryCacheStamp): Boolean {
        val cached = readValid(month, day, stamp) ?: return false
        return cached.events.isEmpty()
    }

    private suspend fun write(month: Int, day: Int, payload: CachedDay) {
        dao.upsert(
            ToolStorageEntity(
                toolId = cacheKey(month, day),
                payloadJson = json.encodeToString(CachedDay.serializer(), payload),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun readValid(month: Int, day: Int, stamp: HistoryCacheStamp): CachedDay? {
        val entity = dao.find(cacheKey(month, day)) ?: return null
        val cached = try {
            json.decodeFromString(CachedDay.serializer(), entity.payloadJson)
        } catch (e: Exception) {
            return null
        }
        if (cached.dateKey != stamp.dateKey) return null
        if (cached.cachedYear != stamp.year) return null
        return cached
    }

    private fun cacheKey(month: Int, day: Int): String =
        "$CACHE_PREFIX%02d-%02d".format(month, day)

    private companion object {
        const val CACHE_PREFIX = "${HistorySettingsRepository.TOOL_ID}:day:"
    }
}
