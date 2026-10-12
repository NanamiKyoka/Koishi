package com.nanami.koishi.feature.tools.mirage_tank.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import kotlinx.serialization.Serializable

@Serializable
data class MirageTankSettings(
    val params: MirageTankParams = MirageTankParams()
)

class MirageTankSettingsRepository(
    dao: ToolStorageDao
) : BaseToolRepository<MirageTankSettings>(
    toolId = TOOL_ID,
    serializer = MirageTankSettings.serializer(),
    dao = dao,
    defaultData = MirageTankSettings()
) {

    suspend fun saveParams(params: MirageTankParams): MirageTankSettings =
        updateData { it.copy(params = params) }

    companion object {
        const val TOOL_ID = "mirage_tank"
    }
}