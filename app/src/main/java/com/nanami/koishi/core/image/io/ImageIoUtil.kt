package com.nanami.koishi.core.image.io

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageIoUtil {

    const val DEFAULT_MAX_DIMENSION = 1920

    fun decodeBitmap(context: Context, uri: Uri, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }

            var width = bounds.outWidth
            var height = bounds.outHeight
            if (width <= 0 || height <= 0) return null

            val safeMax = maxDimension.coerceAtLeast(1)
            var sampleSize = 1
            while (width > safeMax || height > safeMax) {
                sampleSize *= 2
                width /= 2
                height /= 2
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (error: Throwable) {
            error.printStackTrace()
            null
        }
    }

    fun savePng(context: Context, bitmap: Bitmap, albumFolder: String, prefix: String): Boolean {
        val filename = buildFileName(prefix)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(context, bitmap, albumFolder, filename)
            } else {
                saveViaFile(bitmap, albumFolder, filename)
            }
        } catch (error: Throwable) {
            error.printStackTrace()
            false
        }
    }

    private fun saveViaMediaStore(
        context: Context,
        bitmap: Bitmap,
        albumFolder: String,
        filename: String
    ): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + albumFolder)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val itemUri = context.contentResolver.insert(collection, values) ?: return false

        val written = context.contentResolver.openOutputStream(itemUri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: false

        if (!written) {
            context.contentResolver.delete(itemUri, null, null)
            return false
        }

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(itemUri, values, null, null)
        return true
    }

    @Suppress("DEPRECATION")
    private fun saveViaFile(bitmap: Bitmap, albumFolder: String, filename: String): Boolean {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumFolder)
        if (!dir.exists() && !dir.mkdirs()) return false
        FileOutputStream(File(dir, filename)).use { out ->
            return bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun buildFileName(prefix: String): String {
        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val millis = System.currentTimeMillis() % 1000
        return "${prefix}_${date}_${String.format(Locale.US, "%03d", millis)}.png"
    }
}