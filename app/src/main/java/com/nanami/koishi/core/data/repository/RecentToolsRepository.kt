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

interface RecentToolsRepository {
    val recentToolIds: Flow<List<String>>

    suspend fun recordUsage(toolId: String)

    suspend fun clearRecent()

    suspend fun reload()
}

class SharedPreferencesRecentToolsRepository(
    context: Context,
    private val maxCount: Int = 12,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RecentToolsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    private val _recentIds = MutableStateFlow(loadRecent())
    override val recentToolIds: Flow<List<String>> = _recentIds.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_RECENT) {
            _recentIds.value = loadRecent()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override suspend fun reload() = withContext(ioDispatcher) {
        _recentIds.value = loadRecent()
    }

    private fun loadRecent(): List<String> {
        val raw = prefs.getString(KEY_RECENT, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<String>>(raw).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun recordUsage(toolId: String) = withContext(ioDispatcher) {
        if (toolId.isBlank()) return@withContext
        val updated = (listOf(toolId) + _recentIds.value.filter { it != toolId }).take(maxCount)
        save(updated)
    }

    override suspend fun clearRecent() = withContext(ioDispatcher) {
        save(emptyList())
    }

    private fun save(list: List<String>) {
        _recentIds.value = list
        prefs.edit().putString(KEY_RECENT, json.encodeToString(list)).apply()
    }

    companion object {
        private const val PREFS_NAME = "koishi_recent_tools"
        private const val KEY_RECENT = "recent_tool_ids_json"
    }
}