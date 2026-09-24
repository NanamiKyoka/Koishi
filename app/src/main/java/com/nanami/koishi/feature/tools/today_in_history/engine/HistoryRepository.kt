package com.nanami.koishi.feature.tools.today_in_history.engine

sealed interface HistoryLoadResult {
    data class Success(val day: HistoryDay, val fromCache: Boolean) : HistoryLoadResult
    data class Failure(val reason: HistoryException) : HistoryLoadResult
    data object Empty : HistoryLoadResult
}

class HistoryRepository(
    private val preferences: HistoryPreferences,
    private val cache: HistoryCache
) {

    suspend fun load(month: Int, day: Int, forceRefresh: Boolean = false): HistoryLoadResult {
        val todayKey = HistoryCache.todayKey()

        if (!forceRefresh) {
            cache.readDay(month, day, todayKey)?.let { cached ->
                return HistoryLoadResult.Success(cached, fromCache = true)
            }
            if (cache.isEmptyResultCached(month, day, todayKey)) {
                return HistoryLoadResult.Empty
            }
        }

        val appKey = preferences.showApiAppKey
        var primaryError: HistoryException? = null

        if (appKey.isNotBlank()) {
            try {
                val events = HistoryEngines.fetchFromShowApi(appKey, month, day)
                if (events.isNotEmpty()) {
                    val result = HistoryDay(month, day, events, HistorySource.SHOW_API)
                    cache.writeDay(result, todayKey)
                    preferences.recordSource(HistorySource.SHOW_API)
                    lastPrimaryError = null
                    return HistoryLoadResult.Success(result, fromCache = false)
                }
                cache.writeEmpty(month, day, todayKey)
                lastPrimaryError = null
                return HistoryLoadResult.Empty
            } catch (e: HistoryException) {
                primaryError = e
            }
        } else {
            primaryError = HistoryException.MissingApiKey()
        }

        val fallbackResult = loadFromFallback(month, day, todayKey)
        lastPrimaryError = primaryError
        return fallbackResult
    }

    var lastPrimaryError: HistoryException? = null
        private set

    private suspend fun loadFromFallback(month: Int, day: Int, todayKey: String): HistoryLoadResult {
        val events = try {
            HistoryEngines.fetchFromXXapi(month, day)
        } catch (e: HistoryException) {
            return HistoryLoadResult.Failure(e)
        } catch (e: Exception) {
            return HistoryLoadResult.Failure(HistoryException.Network())
        }

        if (events.isEmpty()) {
            cache.writeEmpty(month, day, todayKey)
            return HistoryLoadResult.Empty
        }

        val result = HistoryDay(month, day, events, HistorySource.XXAPI)
        cache.writeDay(result, todayKey)
        preferences.recordSource(HistorySource.XXAPI)
        return HistoryLoadResult.Success(result, fromCache = false)
    }
}
