package com.nanami.koishi.feature.tools.lan_transfer

import androidx.annotation.StringRes
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDevice
import com.nanami.koishi.feature.tools.lan_transfer.engine.transfer.PickedFile

enum class TransferDirection { SENDING, RECEIVING }

enum class TransferItemStatus { IN_PROGRESS, DONE, FAILED, CANCELLED }

data class TransferItemUi(
    val id: String,
    val fileName: String,
    val direction: TransferDirection,
    val progress: Float,
    val status: TransferItemStatus,
    val sessionId: String? = null,
    val canRetry: Boolean = false
)

data class IncomingRequestUi(
    val sessionId: String,
    val peerAlias: String,
    val fileCount: Int,
    val totalBytes: Long
)

data class LanTransferSettingsUi(
    val alias: String = "",
    val pinEnabled: Boolean = false,
    val pin: String = "",
    val saveDirectory: String = "Koishi",
    val autoAccept: Boolean = false,
    val computeChecksums: Boolean = false,
    val verifyChecksums: Boolean = true
)

data class LanTransferUiState(
    val alias: String = "",
    val receiveEnabled: Boolean = false,
    val settings: LanTransferSettingsUi = LanTransferSettingsUi(),
    val devices: List<LanDevice> = emptyList(),
    val transfers: List<TransferItemUi> = emptyList(),
    val incomingRequest: IncomingRequestUi? = null,
    val selectedFiles: List<PickedFile> = emptyList(),
    @StringRes val userMessageRes: Int? = null,
    val userMessageArgs: List<String> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface LanTransferUiEvent {
    data class OnToggleReceive(val enabled: Boolean) : LanTransferUiEvent
    data object OnRefresh : LanTransferUiEvent
    data class OnFilesPicked(val files: List<PickedFile>) : LanTransferUiEvent
    data class OnSendTo(val deviceId: String) : LanTransferUiEvent
    data class OnAcceptIncoming(val accepted: Boolean) : LanTransferUiEvent
    data class OnCancelTransfer(val id: String) : LanTransferUiEvent
    data class OnRetryTransfer(val id: String) : LanTransferUiEvent
    data class OnSaveSettings(
        val alias: String,
        val pinEnabled: Boolean,
        val pin: String,
        val saveDirectory: String,
        val autoAccept: Boolean,
        val computeChecksums: Boolean,
        val verifyChecksums: Boolean
    ) : LanTransferUiEvent
    data object OnDismissMessage : LanTransferUiEvent
    data class OnDismissTransfer(val id: String) : LanTransferUiEvent
}
