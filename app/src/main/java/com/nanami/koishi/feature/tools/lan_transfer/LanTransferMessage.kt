package com.nanami.koishi.feature.tools.lan_transfer

import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.TransferFileDto

internal fun Map<String, TransferFileDto>.singleTextMessageOrNull(): String? {
    val file = values.singleOrNull() ?: return null
    if (!file.fileType.substringBefore(';').trim().startsWith("text/", ignoreCase = true)) {
        return null
    }
    return file.preview
}
