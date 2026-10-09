package com.nanami.koishi.feature.tools.lan_transfer.engine.transfer

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendUploadSource
import java.io.InputStream

data class PickedFile(
    val uri: Uri,
    val fileName: String,
    val size: Long,
    val mimeType: String
)

object ContentUriFileSource {

    fun describe(resolver: ContentResolver, uri: Uri): PickedFile? {
        var name: String? = null
        var size = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        val fileName = name ?: uri.lastPathSegment?.substringAfterLast('/') ?: return null
        if (size < 0) return null
        return PickedFile(
            uri = uri,
            fileName = fileName,
            size = size,
            mimeType = resolver.getType(uri) ?: "application/octet-stream"
        )
    }

    fun source(resolver: ContentResolver, file: PickedFile): LocalSendUploadSource =
        object : LocalSendUploadSource {
            override val size: Long = file.size
            override val contentType: String = file.mimeType

            override fun openStream(): InputStream =
                resolver.openInputStream(file.uri) ?: error("Unable to read ${file.fileName}")
        }
}
