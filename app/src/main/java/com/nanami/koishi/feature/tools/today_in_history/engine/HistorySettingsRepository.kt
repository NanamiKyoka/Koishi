package com.nanami.koishi.feature.tools.today_in_history.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import kotlinx.serialization.Serializable

@Serializable
data class HistorySettings(
    val showApiAppKey: String = "",
    val lastUsedSource: HistorySource = HistorySource.XXAPI
)

class HistorySettingsRepository(
    dao: ToolStorageDao
) : BaseToolRepository<HistorySettings>(
    toolId = TOOL_ID,
    serializer = HistorySettings.serializer(),
    dao = dao,
    defaultData = HistorySettings()
) {

    suspend fun saveShowApiAppKey(appKey: String): HistorySettings =
        updateData { it.copy(showApiAppKey = appKey.trim()) }

    suspend fun recordSource(source: HistorySource): HistorySettings =
        updateData { it.copy(lastUsedSource = source) }

    companion object {
        const val TOOL_ID = "today_in_history"
    }
}
