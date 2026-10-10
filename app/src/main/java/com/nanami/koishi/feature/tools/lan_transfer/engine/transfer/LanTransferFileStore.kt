package com.nanami.koishi.feature.tools.lan_transfer.engine.transfer

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.TransferFileDto
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.time.Instant

class LanTransferFileStore(private val context: Context) {

    fun open(file: TransferFileDto, directoryName: String = DIRECTORY_NAME): PendingFile {
        val displayName = sanitize(file.fileName)
        val directory = sanitizeDirectory(directoryName)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openWithMediaStore(displayName, file, directory)
        } else {
            openWithLegacyDirectory(displayName, file, directory)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun openWithMediaStore(displayName: String, file: TransferFileDto, directory: String): PendingFile {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, file.fileType.ifBlank { "application/octet-stream" })
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$directory")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create destination file")
        val stream = resolver.openOutputStream(uri) ?: error("Unable to open destination file")
        return PendingFile(
            output = stream,
            location = downloadFolderLocation(directory),
            publish = { success ->
                if (success) {
                    applyMediaStoreTimestamp(resolver, uri, file)
                    val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                    resolver.update(uri, done, null, null)
                } else {
                    resolver.delete(uri, null, null)
                }
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun openWithLegacyDirectory(displayName: String, file: TransferFileDto, directoryName: String): PendingFile {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            directoryName
        )
        directory.mkdirs()
        val target = uniqueFile(directory, displayName)
        return PendingFile(
            output = FileOutputStream(target),
            location = downloadFolderLocation(directoryName),
            publish = { success ->
                if (success) {
                    applyFileTimestamp(target, file)
                } else {
                    target.delete()
                }
            }
        )
    }

    private fun applyMediaStoreTimestamp(
        resolver: ContentResolver,
        uri: android.net.Uri,
        file: TransferFileDto
    ) {
        val modifiedSeconds = file.metadata?.modified
            ?.let { runCatching { Instant.parse(it).epochSecond }.getOrNull() }
            ?: return
        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.MediaColumns.DATE_MODIFIED, modifiedSeconds) },
            null,
            null
        )
    }

    private fun applyFileTimestamp(target: File, file: TransferFileDto) {
        val modified = file.metadata?.modified
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: return
        val accessed = file.metadata.accessed
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: modified
        runCatching {
            Files.getFileAttributeView(target.toPath(), BasicFileAttributeView::class.java)
                .setTimes(
                    FileTime.from(modified),
                    FileTime.from(accessed),
                    null
                )
        }.onFailure {
            target.setLastModified(modified.toEpochMilli())
        }
    }

    private fun uniqueFile(directory: File, displayName: String): File {
        val baseName = displayName.substringBeforeLast('.', displayName)
        val extension = displayName.substringAfterLast('.', "")
        var candidate = File(directory, displayName)
        var index = 1
        while (candidate.exists()) {
            val nextName = if (extension.isEmpty()) {
                "$baseName ($index)"
            } else {
                "$baseName ($index).$extension"
            }
            candidate = File(directory, nextName)
            index += 1
        }
        return candidate
    }

    private fun downloadFolderLocation(directoryName: String): ReceivedFileLocation {
        val documentId = "primary:${Environment.DIRECTORY_DOWNLOADS}/$directoryName"
        return ReceivedFileLocation(
            folderUri = DocumentsContract.buildDocumentUri(
                EXTERNAL_STORAGE_AUTHORITY,
                documentId
            ).toString()
        )
    }

    private fun sanitizeDirectory(name: String): String = name
        .replace('/', '_')
        .replace('\\', '_')
        .trim()
        .ifBlank { DIRECTORY_NAME }

    private fun sanitize(name: String): String {
        val cleaned = name
            .replace('/', '_')
            .replace('\\', '_')
            .filterNot { it.isISOControl() }
            .trim()
        return cleaned.ifBlank { "koishi_file" }
    }

    data class PendingFile(
        val output: OutputStream,
        val location: ReceivedFileLocation,
        val publish: (success: Boolean) -> Unit
    )

    companion object {
        const val DIRECTORY_NAME = "Koishi"
        private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    }
}
