package com.nanami.koishi.feature.tools.text_apps.common

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

data class ImportedTextFile(
    val uri: Uri,
    val displayName: String,
    val byteCount: Long?,
    val inlineText: String?
) {
    val isInline: Boolean get() = inlineText != null
}

enum class TextImportErrorKind {
    TOO_LARGE,
    UNSUPPORTED_ENCODING,
    READ_FAILED
}

class TextImportException(
    val kind: TextImportErrorKind,
    val byteLimit: Long? = null,
    cause: Throwable? = null
) : Exception(cause?.message, cause)

object TextFileIo {

    const val DEFAULT_INLINE_LIMIT_BYTES = 1024L * 1024L
    const val RESULT_INLINE_LIMIT_BYTES = 512L * 1024L

    suspend fun importDocument(
        context: Context,
        uri: Uri,
        inlineLimitBytes: Long = DEFAULT_INLINE_LIMIT_BYTES
    ): ImportedTextFile = withContext(Dispatchers.IO) {
        val metadata = queryMetadata(context, uri)
        if (metadata.byteCount != null && metadata.byteCount > inlineLimitBytes) {
            return@withContext ImportedTextFile(
                uri = uri,
                displayName = metadata.displayName,
                byteCount = metadata.byteCount,
                inlineText = null
            )
        }

        val bytes = try {
            readAtMost(context.contentResolver.openInputStream(uri), inlineLimitBytes)
        } catch (error: TextImportException) {
            if (error.kind == TextImportErrorKind.TOO_LARGE) {
                return@withContext ImportedTextFile(
                    uri = uri,
                    displayName = metadata.displayName,
                    byteCount = metadata.byteCount,
                    inlineText = null
                )
            }
            throw error
        } catch (error: Throwable) {
            throw TextImportException(TextImportErrorKind.READ_FAILED, cause = error)
        }

        val text = try {
            decodeText(bytes)
        } catch (error: CharacterCodingException) {
            throw TextImportException(TextImportErrorKind.UNSUPPORTED_ENCODING, cause = error)
        }

        ImportedTextFile(
            uri = uri,
            displayName = metadata.displayName,
            byteCount = metadata.byteCount ?: bytes.size.toLong(),
            inlineText = text
        )
    }

    suspend fun writeText(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
        val writer = context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(StandardCharsets.UTF_8)
            ?: error("Unable to open output stream")
        writer.use { it.write(text) }
    }

    suspend fun copyToFile(source: File, context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: error("Unable to open output stream")
        output.use { destination ->
            source.inputStream().buffered().use { input -> input.copyTo(destination) }
        }
    }

    fun createCacheFile(context: Context, prefix: String, suffix: String): File =
        File.createTempFile(prefix, suffix, context.cacheDir)

    fun openTextReader(context: Context, uri: Uri): Reader {
        val input = BufferedInputStream(
            context.contentResolver.openInputStream(uri) ?: error("Unable to open input stream")
        )
        val (charset, bomLength) = detectCharset(input)
        if (bomLength > 0) input.read(ByteArray(bomLength))
        return InputStreamReader(input, charset)
    }

    private fun readAtMost(input: InputStream?, limitBytes: Long): ByteArray {
        requireNotNull(input) { "Unable to open input stream" }
        input.use { stream ->
            val output = ByteArrayOutputStream(minOf(limitBytes, 64L * 1024L).toInt())
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > limitBytes) {
                    throw TextImportException(TextImportErrorKind.TOO_LARGE, limitBytes)
                }
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        }
    }

    private fun decodeText(bytes: ByteArray): String {
        val (charset, offset) = when {
            bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() &&
                bytes[2] == 0xBF.toByte() -> StandardCharsets.UTF_8 to 3

            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                StandardCharsets.UTF_16LE to 2

            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                StandardCharsets.UTF_16BE to 2

            else -> StandardCharsets.UTF_8 to 0
        }

        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return decoder.decode(ByteBuffer.wrap(bytes, offset, bytes.size - offset)).toString()
    }

    private fun detectCharset(input: BufferedInputStream): Pair<Charset, Int> {
        input.mark(3)
        val prefix = ByteArray(3)
        val count = input.read(prefix)
        input.reset()
        return when {
            count >= 3 &&
                prefix[0] == 0xEF.toByte() &&
                prefix[1] == 0xBB.toByte() &&
                prefix[2] == 0xBF.toByte() -> StandardCharsets.UTF_8 to 3

            count >= 2 && prefix[0] == 0xFF.toByte() && prefix[1] == 0xFE.toByte() ->
                StandardCharsets.UTF_16LE to 2

            count >= 2 && prefix[0] == 0xFE.toByte() && prefix[1] == 0xFF.toByte() ->
                StandardCharsets.UTF_16BE to 2

            else -> StandardCharsets.UTF_8 to 0
        }
    }

    private fun queryMetadata(context: Context, uri: Uri): DocumentMetadata {
        var displayName: String? = null
        var byteCount: Long? = null
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0 && !cursor.isNull(nameIndex)) displayName = cursor.getString(nameIndex)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) byteCount = cursor.getLong(sizeIndex)
        }
        return DocumentMetadata(
            displayName = displayName ?: uri.lastPathSegment ?: "document.txt",
            byteCount = byteCount
        )
    }

    private data class DocumentMetadata(
        val displayName: String,
        val byteCount: Long?
    )
}
