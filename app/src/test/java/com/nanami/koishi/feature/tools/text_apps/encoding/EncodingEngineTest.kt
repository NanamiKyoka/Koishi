package com.nanami.koishi.feature.tools.text_apps.encoding

import org.junit.Assert.assertEquals
import org.junit.Test

class EncodingEngineTest {

    @Test
    fun base64RoundTripPreservesUnicode() {
        val encoded = EncodingEngine.transform(
            input = "Koishi 恋恋",
            direction = EncodingDirection.ENCODE,
            scheme = EncodingScheme.BASE64
        ).getOrThrow()
        val decoded = EncodingEngine.transform(
            input = encoded,
            direction = EncodingDirection.DECODE,
            scheme = EncodingScheme.BASE64
        ).getOrThrow()

        assertEquals("Koishi 恋恋", decoded)
    }

    @Test
    fun base64SupportsUrlSafeWithoutPadding() {
        val options = EncodingOptions(base64UrlSafe = true, includePadding = false)
        val encoded = EncodingEngine.transform(
            input = "subjects?_d",
            direction = EncodingDirection.ENCODE,
            scheme = EncodingScheme.BASE64,
            options = options
        ).getOrThrow()

        assertEquals("c3ViamVjdHM_X2Q", encoded)
    }

    @Test
    fun urlEncodingUsesPercentEncodedSpace() {
        val encoded = EncodingEngine.transform(
            input = "a b+c",
            direction = EncodingDirection.ENCODE,
            scheme = EncodingScheme.URL
        ).getOrThrow()

        assertEquals("a%20b%2Bc", encoded)
    }

    @Test
    fun urlFormEncodingUsesPlusForSpace() {
        val encoded = EncodingEngine.transform(
            input = "a b",
            direction = EncodingDirection.ENCODE,
            scheme = EncodingScheme.URL,
            options = EncodingOptions(urlForm = true)
        ).getOrThrow()

        assertEquals("a+b", encoded)
    }
}
