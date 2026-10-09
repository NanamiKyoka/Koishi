package com.nanami.koishi.feature.tools.lan_transfer.engine.protocol

import android.os.Build
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceProtocol
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceType
import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendIdentityStore

class LocalDeviceInfoProvider(
    private val identityStore: LocalSendIdentityStore
) {
    fun create(
        alias: String,
        port: Int = LocalSendProtocol.DEFAULT_PORT,
        protocol: LanDeviceProtocol = LanDeviceProtocol.HTTPS
    ): RegisterRequestDto = RegisterRequestDto(
        alias = alias.ifBlank { defaultAlias() },
        version = LocalSendProtocol.VERSION,
        deviceModel = Build.MODEL?.takeIf { it.isNotBlank() },
        deviceType = LanDeviceType.MOBILE.wireValue,
        fingerprint = identityStore.get().fingerprint,
        port = port,
        protocol = protocol.wireValue,
        download = false
    )

    private fun defaultAlias(): String =
        Build.MODEL?.takeIf { it.isNotBlank() } ?: "Koishi Android"
}
