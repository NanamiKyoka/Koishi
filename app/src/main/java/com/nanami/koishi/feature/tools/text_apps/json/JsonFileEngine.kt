package com.nanami.koishi.feature.tools.text_apps.json

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import android.util.JsonToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Reader
import java.io.Writer
import java.nio.charset.StandardCharsets
import com.nanami.koishi.feature.tools.text_apps.common.TextFileIo

enum class JsonFileMode {
    FORMAT,
    MINIFY,
    VALIDATE
}

object JsonFileEngine {

    suspend fun process(
        context: Context,
        inputUri: Uri,
        outputFile: File?,
        mode: JsonFileMode,
        indentSize: Int = 2
    ): Result<File?> = withContext(Dispatchers.IO) {
        runCatching {
            val reader = TextFileIo.openTextReader(context, inputUri)

            reader.use { input ->
                if (mode == JsonFileMode.VALIDATE) {
                    process(input, null)
                    null
                } else {
                    val target = outputFile ?: error("Output file is required")
                    try {
                        target.outputStream().bufferedWriter(StandardCharsets.UTF_8).use { output ->
                            process(input, output, mode, indentSize)
                        }
                        target
                    } catch (error: Throwable) {
                        target.delete()
                        throw error
                    }
                }
            }
        }
    }

    fun process(
        input: Reader,
        output: Writer?,
        mode: JsonFileMode = JsonFileMode.FORMAT,
        indentSize: Int = 2
    ) {
        require(indentSize in 1..8)
        val reader = JsonReader(input).apply { isLenient = false }
        val writer = output?.let {
            JsonStreamWriter(
                output = it,
                pretty = mode == JsonFileMode.FORMAT,
                indentSize = indentSize
            )
        }
        copyValue(reader, writer)
        check(reader.peek() == JsonToken.END_DOCUMENT) { "Unexpected trailing JSON content" }
        writer?.flush()
    }

    private fun copyValue(reader: JsonReader, writer: JsonStreamWriter?) {
        when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                writer?.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    writer?.name(name)
                    copyValue(reader, writer)
                }
                reader.endObject()
                writer?.endObject()
            }

            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                writer?.beginArray()
                while (reader.hasNext()) {
                    copyValue(reader, writer)
                }
                reader.endArray()
                writer?.endArray()
            }

            JsonToken.STRING -> writer?.string(reader.nextString()) ?: reader.nextString()
            JsonToken.NUMBER -> writer?.number(reader.nextString()) ?: reader.nextString()
            JsonToken.BOOLEAN -> writer?.boolean(reader.nextBoolean()) ?: reader.nextBoolean()
            JsonToken.NULL -> {
                reader.nextNull()
                writer?.nullValue()
            }

            JsonToken.END_DOCUMENT,
            JsonToken.END_ARRAY,
            JsonToken.END_OBJECT,
            JsonToken.NAME -> error("Unexpected JSON token")
        }
    }

    private class JsonStreamWriter(
        private val output: Writer,
        private val pretty: Boolean,
        private val indentSize: Int
    ) {
        private data class Scope(
            val isObject: Boolean,
            var entries: Int = 0,
            var awaitingValue: Boolean = false
        )

        private val scopes = ArrayDeque<Scope>()

        fun beginObject() {
            beforeValue()
            output.write("{")
            scopes.addLast(Scope(isObject = true))
        }

        fun endObject() {
            endScope(close = "}")
        }

        fun beginArray() {
            beforeValue()
            output.write("[")
            scopes.addLast(Scope(isObject = false))
        }

        fun endArray() {
            endScope(close = "]")
        }

        fun name(value: String) {
            val scope = scopes.lastOrNull()
            check(scope?.isObject == true && !scope.awaitingValue)
            if (scope.entries > 0) output.write(",")
            if (pretty) newLine(scopes.size)
            writeQuoted(value)
            output.write(if (pretty) ": " else ":")
            scope.entries++
            scope.awaitingValue = true
        }

        fun string(value: String) {
            beforeValue()
            writeQuoted(value)
        }

        fun number(value: String) {
            beforeValue()
            output.write(value)
        }

        fun boolean(value: Boolean) {
            beforeValue()
            output.write(value.toString())
        }

        fun nullValue() {
            beforeValue()
            output.write("null")
        }

        fun flush() {
            output.flush()
        }

        private fun beforeValue() {
            val scope = scopes.lastOrNull() ?: return
            if (scope.awaitingValue) {
                scope.awaitingValue = false
                return
            }
            if (scope.entries > 0) output.write(",")
            if (pretty) newLine(scopes.size)
            scope.entries++
        }

        private fun endScope(close: String) {
            val scope = scopes.removeLastOrNull() ?: error("Unbalanced JSON writer scope")
            check(!scope.awaitingValue)
            if (pretty && scope.entries > 0) newLine(scopes.size)
            output.write(close)
        }

        private fun newLine(depth: Int) {
            output.write("\n")
            repeat(depth * indentSize) { output.write(" ") }
        }

        private fun writeQuoted(value: String) {
            output.write("\"")
            value.forEach { char ->
                when (char) {
                    '"' -> output.write("\\\"")
                    '\\' -> output.write("\\\\")
                    '\b' -> output.write("\\b")
                    '\u000C' -> output.write("\\f")
                    '\n' -> output.write("\\n")
                    '\r' -> output.write("\\r")
                    '\t' -> output.write("\\t")
                    else -> {
                        if (char.code < 0x20) {
                            output.write("\\u")
                            output.write(HEX_DIGITS[char.code ushr 12 and 0x0F].code)
                            output.write(HEX_DIGITS[char.code ushr 8 and 0x0F].code)
                            output.write(HEX_DIGITS[char.code ushr 4 and 0x0F].code)
                            output.write(HEX_DIGITS[char.code and 0x0F].code)
                        } else {
                            output.write(char.code)
                        }
                    }
                }
            }
            output.write("\"")
        }
    }

    private const val HEX_DIGITS = "0123456789abcdef"
}
