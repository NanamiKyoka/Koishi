package com.nanami.koishi.feature.tools.lan_transfer.engine.settings

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import kotlinx.serialization.Serializable

@Serializable
data class LanTransferSettings(
    val alias: String = "",
    val receiveEnabled: Boolean = false,
    val pinEnabled: Boolean = false,
    val pin: String = "",
    val saveDirectory: String = "Koishi",
    val autoAcceptKnownDevices: Boolean = false,
    val computeChecksums: Boolean = false,
    val verifyChecksums: Boolean = true
)

class LanTransferSettingsRepository(dao: ToolStorageDao) : BaseToolRepository<LanTransferSettings>(
    toolId = TOOL_ID,
    serializer = LanTransferSettings.serializer(),
    dao = dao,
    defaultData = LanTransferSettings()
) {
    companion object {
        const val TOOL_ID = "lan_transfer"
    }
}
