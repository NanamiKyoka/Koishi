package com.nanami.koishi.feature.tools.text_apps.hash

import java.security.MessageDigest

enum class HashAlgorithm(val jcaName: String) {
    MD5("MD5"),
    SHA_1("SHA-1"),
    SHA_256("SHA-256"),
    SHA_512("SHA-512")
}

object HashEngine {

    fun digest(input: String, algorithm: HashAlgorithm): String =
        MessageDigest.getInstance(algorithm.jcaName)
            .digest(input.toByteArray(Charsets.UTF_8))
            .toHex()

    fun digestAll(input: String): Map<HashAlgorithm, String> =
        HashAlgorithm.entries.associateWith { digest(input, it) }

    private fun ByteArray.toHex(): String = buildString(size * 2) {
        for (byte in this@toHex) {
            val value = byte.toInt() and 0xFF
            append(HEX[value ushr 4])
            append(HEX[value and 0x0F])
        }
    }

    private const val HEX = "0123456789abcdef"
}
