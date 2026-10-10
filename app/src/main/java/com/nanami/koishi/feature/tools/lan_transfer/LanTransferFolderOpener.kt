package com.nanami.koishi.feature.tools.lan_transfer

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.nanami.koishi.feature.tools.lan_transfer.engine.transfer.ReceivedFileLocation

internal object LanTransferFolderOpener {

    fun open(context: Context, location: ReceivedFileLocation): Boolean {
        val folderUri = Uri.parse(location.folderUri)
        val intents = listOf(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(folderUri, DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            Intent(Intent.ACTION_VIEW).apply {
                data = folderUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
        )
        return intents.any { intent ->
            runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
    }
}
