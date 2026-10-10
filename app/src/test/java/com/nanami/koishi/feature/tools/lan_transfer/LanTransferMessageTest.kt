package com.nanami.koishi.feature.tools.lan_transfer

import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.TransferFileDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanTransferMessageTest {

    @Test
    fun returnsPreviewForSingleTextFile() {
        val files = mapOf(
            "message" to TransferFileDto(
                id = "message",
                fileName = "message.txt",
                size = 5,
                fileType = "text/plain",
                preview = "hello"
            )
        )

        assertEquals("hello", files.singleTextMessageOrNull())
    }

    @Test
    fun acceptsTextMimeParameters() {
        val files = mapOf(
            "message" to TransferFileDto(
                id = "message",
                fileName = "message.txt",
                size = 5,
                fileType = "text/plain; charset=utf-8",
                preview = "hello"
            )
        )

        assertEquals("hello", files.singleTextMessageOrNull())
    }

    @Test
    fun ignoresTextFileWithoutPreview() {
        val files = mapOf(
            "file" to TransferFileDto(
                id = "file",
                fileName = "document.txt",
                size = 5,
                fileType = "text/plain"
            )
        )

        assertNull(files.singleTextMessageOrNull())
    }

    @Test
    fun ignoresMultipleFiles() {
        val files = mapOf(
            "first" to TransferFileDto(
                id = "first",
                fileName = "first.txt",
                size = 5,
                fileType = "text/plain",
                preview = "first"
            ),
            "second" to TransferFileDto(
                id = "second",
                fileName = "second.txt",
                size = 6,
                fileType = "text/plain",
                preview = "second"
            )
        )

        assertNull(files.singleTextMessageOrNull())
    }

    @Test
    fun ignoresNonTextPreview() {
        val files = mapOf(
            "image" to TransferFileDto(
                id = "image",
                fileName = "image.png",
                size = 5,
                fileType = "image/png",
                preview = "preview"
            )
        )

        assertNull(files.singleTextMessageOrNull())
    }
}
