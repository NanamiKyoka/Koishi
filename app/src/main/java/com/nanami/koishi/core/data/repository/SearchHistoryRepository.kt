package com.nanami.koishi.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 搜索历史数据仓库
 */
interface SearchHistoryRepository {
    /** 观察当前搜索历史记录流（保序且去重，最新在前） */
    val searchHistory: Flow<List<String>>

    /** 添加或更新搜索历史词条 (Create / Update) */
    suspend fun addSearchHistory(keyword: String)

    /** 删除单个搜索历史词条 (Delete) */
    suspend fun deleteSearchHistoryItem(keyword: String)

    /** 清空全部搜索历史 (Batch Delete) */
    suspend fun clearAllSearchHistory()
}

class SharedPreferencesSearchHistoryRepository(
    context: Context,
    private val maxHistoryCount: Int = 20,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SearchHistoryRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    private val _history = MutableStateFlow(loadHistory())
    override val searchHistory: Flow<List<String>> = _history.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_SEARCH_HISTORY) {
            _history.value = loadHistory()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun loadHistory(): List<String> {
        val rawJson = prefs.getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<String>>(rawJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addSearchHistory(keyword: String) = withContext(ioDispatcher) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return@withContext

        val current = _history.value
        // 去除已有同名条目并插入到最前，保留最新次序并截取最大容量
        val updated = (listOf(trimmed) + current.filter { it != trimmed }).take(maxHistoryCount)
        saveHistory(updated)
    }

    override suspend fun deleteSearchHistoryItem(keyword: String) = withContext(ioDispatcher) {
        val current = _history.value
        val updated = current.filter { it != keyword }
        saveHistory(updated)
    }

    override suspend fun clearAllSearchHistory() = withContext(ioDispatcher) {
        saveHistory(emptyList())
    }

    private fun saveHistory(list: List<String>) {
        _history.value = list
        try {
            val encoded = json.encodeToString(list)
            prefs.edit().putString(KEY_SEARCH_HISTORY, encoded).apply()
        } catch (e: Exception) {
            // Log or ignore
        }
    }

    companion object {
        private const val PREFS_NAME = "koishi_search"
        private const val KEY_SEARCH_HISTORY = "history_list_json"
    }
}
