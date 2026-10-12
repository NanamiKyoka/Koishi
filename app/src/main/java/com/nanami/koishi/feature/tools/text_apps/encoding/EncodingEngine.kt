package com.nanami.koishi.feature.tools.text_apps.encoding

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64

enum class EncodingScheme {
    BASE64,
    URL
}

enum class EncodingDirection {
    ENCODE,
    DECODE
}

data class EncodingOptions(
    val base64UrlSafe: Boolean = false,
    val includePadding: Boolean = true,
    val urlForm: Boolean = false
)

object EncodingEngine {

    fun transform(
        input: String,
        direction: EncodingDirection,
        scheme: EncodingScheme,
        options: EncodingOptions = EncodingOptions()
    ): Result<String> = runCatching {
        when (scheme) {
            EncodingScheme.BASE64 -> transformBase64(input, direction, options)
            EncodingScheme.URL -> transformUrl(input, direction, options)
        }
    }

    private fun transformBase64(
        input: String,
        direction: EncodingDirection,
        options: EncodingOptions
    ): String {
        val encoder = if (options.base64UrlSafe) Base64.getUrlEncoder() else Base64.getEncoder()
        val decoder = if (options.base64UrlSafe) Base64.getUrlDecoder() else Base64.getDecoder()
        return when (direction) {
            EncodingDirection.ENCODE -> {
                val bytes = input.toByteArray(Charsets.UTF_8)
                val encoded = encoder.encodeToString(bytes)
                if (options.includePadding) encoded else encoded.trimEnd('=')
            }

            EncodingDirection.DECODE -> {
                val normalized = input
                    .filterNot { it.isWhitespace() }
                    .replace('-', '+')
                    .replace('_', '/')
                val padding = (4 - normalized.length % 4) % 4
                val padded = normalized + "=".repeat(padding)
                decoder.decode(padded).toString(Charsets.UTF_8)
            }
        }
    }

    private fun transformUrl(
        input: String,
        direction: EncodingDirection,
        options: EncodingOptions
    ): String = when (direction) {
        EncodingDirection.ENCODE -> {
            val encoded = URLEncoder.encode(input, Charsets.UTF_8.name())
            if (options.urlForm) encoded else encoded.replace("+", "%20")
        }

        EncodingDirection.DECODE -> {
            val value = if (options.urlForm) input else input.replace("+", "%2B")
            URLDecoder.decode(value, Charsets.UTF_8.name())
        }
    }
}
