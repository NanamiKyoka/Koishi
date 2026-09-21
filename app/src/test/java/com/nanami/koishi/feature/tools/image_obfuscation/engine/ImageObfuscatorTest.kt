package com.nanami.koishi.feature.tools.image_obfuscation.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Random

class ImageObfuscatorTest {

    @Test
    fun testDefaultKeysMatchPicEncrypt() {
        assertEquals("1", ObfuscationMode.TOMATO_GILBERT.defaultKey)
        assertEquals("0.666", ObfuscationMode.BLOCK.defaultKey)
        assertEquals("0.666", ObfuscationMode.ROW_PIXEL.defaultKey)
        assertEquals("0.666", ObfuscationMode.PER_PIXEL.defaultKey)
        assertEquals("0.666", ObfuscationMode.PIC_ENCRYPT_ROW.defaultKey)
        assertEquals("0.666", ObfuscationMode.PIC_ENCRYPT_ROW_AND_COLUMN.defaultKey)
    }

    private fun generateRandomPixels(width: Int, height: Int): IntArray {
        val random = Random(42)
        val pixels = IntArray(width * height)
        for (i in pixels.indices) {
            pixels[i] = random.nextInt()
        }
        return pixels
    }

    @Test
    fun testTomatoReversibility() {
        val w = 64
        val h = 48
        val original = generateRandomPixels(w, h)
        val key = 1.0

        val encrypted = ImageObfuscator.processTomato(original, w, h, key, isEncrypt = true)
        assertFalse(original.contentEquals(encrypted))

        val decrypted = ImageObfuscator.processTomato(encrypted, w, h, key, isEncrypt = false)
        assertArrayEquals(original, decrypted)
    }

    @Test
    fun testRowPixelReversibility() {
        val w = 60
        val h = 50
        val original = generateRandomPixels(w, h)
        val key = "0.666"

        val encrypted = ImageObfuscator.processRowPixel(original, w, h, key, isEncrypt = true)
        assertFalse(original.contentEquals(encrypted))

        val decrypted = ImageObfuscator.processRowPixel(encrypted, w, h, key, isEncrypt = false)
        assertArrayEquals(original, decrypted)
    }

    @Test
    fun testPerPixelReversibility() {
        val w = 50
        val h = 40
        val original = generateRandomPixels(w, h)
        val key = "0.666"

        val encrypted = ImageObfuscator.processPerPixel(original, w, h, key, isEncrypt = true)
        assertFalse(original.contentEquals(encrypted))

        val decrypted = ImageObfuscator.processPerPixel(encrypted, w, h, key, isEncrypt = false)
        assertArrayEquals(original, decrypted)
    }

    @Test
    fun testBlockReversibility() {
        val w = 70
        val h = 50
        val original = generateRandomPixels(w, h)
        val key = "0.666"

        val encryptedResult = ImageObfuscator.processBlock(original, w, h, key, isEncrypt = true)
        val decryptedResult = ImageObfuscator.processBlock(
            encryptedResult.pixels,
            encryptedResult.width,
            encryptedResult.height,
            key,
            isEncrypt = false
        )

        // Trim decryptedResult back to original w x h
        val restored = IntArray(w * h)
        for (j in 0 until h) {
            for (i in 0 until w) {
                restored[i + j * w] = decryptedResult.pixels[i + j * decryptedResult.width]
            }
        }
        assertArrayEquals(original, restored)
    }

    @Test
    fun testPicEncryptRowReversibility() {
        val w = 64
        val h = 48
        val original = generateRandomPixels(w, h)
        val key = 0.666

        val encrypted = ImageObfuscator.processPicEncryptRow(original, w, h, key, isEncrypt = true)
        assertFalse(original.contentEquals(encrypted))

        val decrypted = ImageObfuscator.processPicEncryptRow(encrypted, w, h, key, isEncrypt = false)
        assertArrayEquals(original, decrypted)
    }

    @Test
    fun testPicEncryptRowColumnReversibility() {
        val w = 60
        val h = 45
        val original = generateRandomPixels(w, h)
        val key = 0.666

        val encrypted = ImageObfuscator.processPicEncryptRowColumn(original, w, h, key, isEncrypt = true)
        assertFalse(original.contentEquals(encrypted))

        val decrypted = ImageObfuscator.processPicEncryptRowColumn(encrypted, w, h, key, isEncrypt = false)
        assertArrayEquals(original, decrypted)
    }
}
