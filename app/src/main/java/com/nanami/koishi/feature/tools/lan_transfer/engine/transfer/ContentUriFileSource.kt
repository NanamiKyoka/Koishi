package com.nanami.koishi.feature.tools.lan_transfer.engine.transfer

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendUploadSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest
import java.time.Instant

data class PickedFile(
    val uri: Uri,
    val fileName: String,
    val size: Long,
    val mimeType: String,
    val modifiedAt: Long? = null,
    val accessedAt: Long? = null,
    val previewText: String? = null,
    val temporary: Boolean = false,
    val displayName: String? = null
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
        val modifiedAt = queryDocumentTimestamp(resolver, uri)
            ?: queryTimestamp(resolver, uri, MediaStore.MediaColumns.DATE_MODIFIED)?.times(1_000)
        val accessedAt = queryTimestamp(resolver, uri, MediaStore.MediaColumns.DATE_ADDED)?.times(1_000)
        return PickedFile(
            uri = uri,
            fileName = fileName,
            size = size,
            mimeType = resolver.getType(uri) ?: "application/octet-stream",
            modifiedAt = modifiedAt,
            accessedAt = accessedAt
        )
    }

    fun source(resolver: ContentResolver, file: PickedFile): LocalSendUploadSource =
        object : LocalSendUploadSource {
            override val size: Long = file.size
            override val contentType: String = file.mimeType

            override fun openStream(): InputStream =
                resolver.openInputStream(file.uri) ?: error("Unable to read ${file.fileName}")
        }

    suspend fun sha256(resolver: ContentResolver, file: PickedFile): String =
        withContext(Dispatchers.IO) {
            val digest = MessageDigest.getInstance("SHA-256")
            val input = resolver.openInputStream(file.uri)
                ?: error("Unable to read ${file.fileName}")
            input.use { stream ->
                val buffer = ByteArray(UPLOAD_HASH_BUFFER_SIZE)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = stream.read(buffer)
                    if (read < 0) break
                    if (read > 0) digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }

    private fun queryDocumentTimestamp(resolver: ContentResolver, uri: Uri): Long? =
        queryTimestamp(resolver, uri, DocumentsContract.Document.COLUMN_LAST_MODIFIED)

    private fun queryTimestamp(resolver: ContentResolver, uri: Uri, column: String): Long? =
        runCatching {
            resolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(column)
                if (index < 0 || cursor.isNull(index)) null else cursor.getLong(index)
            }
        }.getOrNull()

    fun Instant.toMetadataTimestamp(): String = toString()

    private const val UPLOAD_HASH_BUFFER_SIZE = 256 * 1024
}
