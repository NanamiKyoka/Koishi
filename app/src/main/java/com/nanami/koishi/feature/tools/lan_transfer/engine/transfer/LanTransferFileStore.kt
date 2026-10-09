package com.nanami.koishi.feature.tools.lan_transfer.engine.transfer

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.TransferFileDto
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class LanTransferFileStore(private val context: Context) {

    fun open(file: TransferFileDto): PendingFile {
        val displayName = sanitize(file.fileName)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openWithMediaStore(displayName, file.fileType)
        } else {
            openWithLegacyDirectory(displayName)
        }
    }

    private fun openWithMediaStore(displayName: String, mimeType: String): PendingFile {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DIRECTORY_NAME")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create destination file")
        val stream = resolver.openOutputStream(uri) ?: error("Unable to open destination file")
        return PendingFile(
            output = stream,
            publish = { success ->
                if (success) {
                    val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                    resolver.update(uri, done, null, null)
                } else {
                    resolver.delete(uri, null, null)
                }
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun openWithLegacyDirectory(displayName: String): PendingFile {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            DIRECTORY_NAME
        )
        directory.mkdirs()
        val target = uniqueFile(directory, displayName)
        return PendingFile(
            output = FileOutputStream(target),
            publish = { success -> if (!success) target.delete() }
        )
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
        val publish: (success: Boolean) -> Unit
    )

    companion object {
        const val DIRECTORY_NAME = "Koishi"
    }
}
