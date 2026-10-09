package com.nanami.koishi.feature.tools.lan_transfer.engine.protocol

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val alias: String,
    val version: String,
    val deviceModel: String? = null,
    val deviceType: String? = null,
    val fingerprint: String,
    val port: Int,
    val protocol: String,
    val download: Boolean = false
)

@Serializable
data class DeviceInfoResponseDto(
    val alias: String,
    val version: String,
    val deviceModel: String? = null,
    val deviceType: String? = null,
    val fingerprint: String,
    val download: Boolean = false
)

@Serializable
data class FileMetadataDto(
    val modified: String? = null,
    val accessed: String? = null
)

@Serializable
data class TransferFileDto(
    val id: String,
    val fileName: String,
    val size: Long,
    val fileType: String,
    val sha256: String? = null,
    val preview: String? = null,
    val metadata: FileMetadataDto? = null
)

@Serializable
data class PrepareUploadRequestDto(
    val info: RegisterRequestDto,
    val files: Map<String, TransferFileDto>
)

@Serializable
data class PrepareUploadResponseDto(
    val sessionId: String,
    val files: Map<String, String>
)

@Serializable
data class MulticastMessageDto(
    val alias: String,
    val version: String,
    val deviceModel: String? = null,
    val deviceType: String? = null,
    val fingerprint: String,
    val port: Int,
    val protocol: String,
    val download: Boolean = false
)

@Serializable
data class ErrorResponseDto(
    val message: String
)
