package com.nanami.koishi.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

interface ToolVisibilityRepository {
    val hiddenToolIds: Flow<Set<String>>

    fun isHidden(toolId: String): Boolean

    suspend fun setToolHidden(toolId: String, hidden: Boolean)

    suspend fun setHiddenToolIds(toolIds: Set<String>)

    suspend fun clearHiddenTools()

    suspend fun reload()
}

class SharedPreferencesToolVisibilityRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ToolVisibilityRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hiddenToolIds = MutableStateFlow(loadHiddenTools())
    override val hiddenToolIds: Flow<Set<String>> = _hiddenToolIds.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_HIDDEN_TOOLS) {
            _hiddenToolIds.value = loadHiddenTools()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun isHidden(toolId: String): Boolean = _hiddenToolIds.value.contains(toolId)

    override suspend fun setToolHidden(toolId: String, hidden: Boolean) = withContext(ioDispatcher) {
        if (toolId.isBlank()) return@withContext
        val updated = if (hidden) _hiddenToolIds.value + toolId else _hiddenToolIds.value - toolId
        saveHiddenTools(updated)
    }

    override suspend fun setHiddenToolIds(toolIds: Set<String>) = withContext(ioDispatcher) {
        saveHiddenTools(toolIds.filter { it.isNotBlank() }.toSet())
    }

    override suspend fun clearHiddenTools() = withContext(ioDispatcher) {
        saveHiddenTools(emptySet())
    }

    override suspend fun reload() = withContext(ioDispatcher) {
        _hiddenToolIds.value = loadHiddenTools()
    }

    private fun loadHiddenTools(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN_TOOLS, emptySet())?.toSet() ?: emptySet()
    }

    private fun saveHiddenTools(toolIds: Set<String>) {
        _hiddenToolIds.value = toolIds
        prefs.edit().putStringSet(KEY_HIDDEN_TOOLS, toolIds).apply()
    }

    companion object {
        private const val PREFS_NAME = "koishi_tool_visibility"
        private const val KEY_HIDDEN_TOOLS = "hidden_tool_ids"
    }
}
