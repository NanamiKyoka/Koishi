package com.nanami.koishi.feature.tools.lan_transfer.engine.server

import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDevice
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.TransferFileDto
import java.io.OutputStream

data class IncomingUploadRequest(
    val sessionId: String,
    val peer: LanDevice,
    val files: Map<String, TransferFileDto>
) {
    val totalBytes: Long
        get() = files.values.sumOf { it.size }
}

sealed interface IncomingUploadDecision {
    data class Accept(val acceptedFileIds: Set<String>) : IncomingUploadDecision
    data object Reject : IncomingUploadDecision
}

data class IncomingFileUpload(
    val sessionId: String,
    val peer: LanDevice,
    val file: TransferFileDto
)

data class IncomingFileResult(
    val sessionId: String,
    val peer: LanDevice,
    val file: TransferFileDto,
    val bytesWritten: Long,
    val success: Boolean,
    val errorMessage: String? = null
)

interface LocalSendServerCallbacks {
    suspend fun onRegistered(peer: LanDevice) = Unit

    suspend fun onUploadRequested(request: IncomingUploadRequest): IncomingUploadDecision

    suspend fun openFileTarget(request: IncomingFileUpload): OutputStream

    suspend fun onFileFinished(result: IncomingFileResult) = Unit

    suspend fun onSessionCancelled(sessionId: String) = Unit
}
