package com.nanami.koishi.feature.tools.bili_cover.engine

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.nanami.koishi.core.util.AlbumFolders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object BiliCoverDownloader {

    suspend fun save(context: Context, url: String, displayName: String): Boolean = withContext(Dispatchers.IO) {
        val bytes = try {
            BiliCoverEngines.fetchImageBytes(url)
        } catch (e: Exception) {
            return@withContext false
        }

        val extension = extensionOf(url)
        val fileName = "$displayName.$extension"
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeOf(extension))
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/" + AlbumFolders.BILI_COVER
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val targetUri = resolver.insert(collection, values) ?: return@withContext false

            val written = try {
                resolver.openOutputStream(targetUri)?.use { it.write(bytes) } != null
            } catch (e: Exception) {
                false
            }

            if (!written) {
                resolver.delete(targetUri, null, null)
                return@withContext false
            }

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(targetUri, values, null, null)
            true
        } else {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                AlbumFolders.BILI_COVER
            )
            if (!directory.exists() && !directory.mkdirs()) return@withContext false

            val target = File(directory, fileName)
            try {
                FileOutputStream(target).use { it.write(bytes) }
            } catch (e: Exception) {
                return@withContext false
            }

            resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DATA, target.absolutePath)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeOf(extension))
                }
            )
            true
        }
    }

    private fun extensionOf(url: String): String {
        val path = url.substringBefore('?').lowercase()
        return when {
            path.endsWith(".png") -> "png"
            path.endsWith(".webp") -> "webp"
            path.endsWith(".gif") -> "gif"
            path.endsWith(".bmp") -> "bmp"
            else -> "jpg"
        }
    }

    private fun mimeOf(extension: String): String = when (extension) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        else -> "image/jpeg"
    }
}
