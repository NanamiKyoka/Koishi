package com.nanami.koishi.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 工具收藏数据仓库（支持完整的 CRUD 接口与异步持久化）
 */
interface ToolFavoritesRepository {
    /** 观察所有已收藏的工具 ID 集合 */
    val favoriteToolIds: Flow<Set<String>>

    /** 查询特定工具是否已收藏 */
    fun isFavorite(toolId: String): Boolean

    /** 增加收藏 (Create) */
    suspend fun addFavorite(toolId: String)

    /** 移除收藏 (Delete) */
    suspend fun removeFavorite(toolId: String)

    /** 切换收藏状态 (Update) */
    suspend fun toggleFavorite(toolId: String)

    /** 清空全部收藏 (Batch Delete) */
    suspend fun clearAllFavorites()
}

class SharedPreferencesToolFavoritesRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ToolFavoritesRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _favoriteIds = MutableStateFlow(loadFavorites())
    override val favoriteToolIds: Flow<Set<String>> = _favoriteIds.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_FAVORITES) {
            _favoriteIds.value = loadFavorites()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun loadFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    override fun isFavorite(toolId: String): Boolean {
        return _favoriteIds.value.contains(toolId)
    }

    override suspend fun addFavorite(toolId: String) = withContext(ioDispatcher) {
        val updated = _favoriteIds.value + toolId
        saveFavorites(updated)
    }

    override suspend fun removeFavorite(toolId: String) = withContext(ioDispatcher) {
        val updated = _favoriteIds.value - toolId
        saveFavorites(updated)
    }

    override suspend fun toggleFavorite(toolId: String) = withContext(ioDispatcher) {
        val current = _favoriteIds.value
        val updated = if (current.contains(toolId)) current - toolId else current + toolId
        saveFavorites(updated)
    }

    override suspend fun clearAllFavorites() = withContext(ioDispatcher) {
        saveFavorites(emptySet())
    }

    private fun saveFavorites(ids: Set<String>) {
        _favoriteIds.value = ids
        prefs.edit().putStringSet(KEY_FAVORITES, ids).apply()
    }

    companion object {
        private const val PREFS_NAME = "koishi_favorites"
        private const val KEY_FAVORITES = "favorite_tool_ids"
    }
}
