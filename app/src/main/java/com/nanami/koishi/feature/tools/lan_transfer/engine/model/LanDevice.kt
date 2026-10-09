package com.nanami.koishi.feature.tools.lan_transfer.engine.model

import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.DeviceInfoResponseDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.RegisterRequestDto

enum class LanDeviceProtocol(val wireValue: String) {
    HTTP("http"),
    HTTPS("https");

    companion object {
        fun fromWire(value: String): LanDeviceProtocol? =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
    }
}

enum class LanDeviceType(val wireValue: String) {
    MOBILE("mobile"),
    DESKTOP("desktop"),
    WEB("web"),
    HEADLESS("headless"),
    SERVER("server");

    companion object {
        fun fromWire(value: String?): LanDeviceType = when (value?.lowercase()) {
            MOBILE.wireValue -> MOBILE
            WEB.wireValue -> WEB
            HEADLESS.wireValue -> HEADLESS
            SERVER.wireValue -> SERVER
            else -> DESKTOP
        }
    }
}

data class LanDevice(
    val fingerprint: String,
    val alias: String,
    val version: String,
    val deviceModel: String?,
    val deviceType: LanDeviceType,
    val host: String,
    val port: Int,
    val protocol: LanDeviceProtocol,
    val download: Boolean,
    val lastSeenAt: Long
) {
    val id: String
        get() = fingerprint.ifBlank { "$host:$port" }
}

fun DeviceInfoResponseDto.toLanDevice(
    host: String,
    port: Int,
    protocol: LanDeviceProtocol,
    lastSeenAt: Long
): LanDevice = LanDevice(
    fingerprint = fingerprint,
    alias = alias,
    version = version,
    deviceModel = deviceModel,
    deviceType = LanDeviceType.fromWire(deviceType),
    host = host,
    port = port,
    protocol = protocol,
    download = download,
    lastSeenAt = lastSeenAt
)

fun LanDevice.toRegisterRequest(): RegisterRequestDto = RegisterRequestDto(
    alias = alias,
    version = version,
    deviceModel = deviceModel,
    deviceType = deviceType.wireValue,
    fingerprint = fingerprint,
    port = port,
    protocol = protocol.wireValue,
    download = download
)
