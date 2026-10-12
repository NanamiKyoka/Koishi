package com.nanami.koishi.feature.tools.prism_tank.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import kotlinx.serialization.Serializable

@Serializable
data class PrismTankSettings(
    val params: PrismTankParams = PrismTankParams(),
    val revealConfig: PrismTankRevealConfig = PrismTankRevealConfig()
)

class PrismTankSettingsRepository(
    dao: ToolStorageDao
) : BaseToolRepository<PrismTankSettings>(
    toolId = TOOL_ID,
    serializer = PrismTankSettings.serializer(),
    dao = dao,
    defaultData = PrismTankSettings()
) {

    suspend fun save(params: PrismTankParams, revealConfig: PrismTankRevealConfig): PrismTankSettings =
        updateData { it.copy(params = params, revealConfig = revealConfig) }

    companion object {
        const val TOOL_ID = "prism_tank"
    }
}