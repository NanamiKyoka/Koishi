package com.nanami.koishi.feature.tools.text_apps.hash

import org.junit.Assert.assertEquals
import org.junit.Test

class HashEngineTest {

    @Test
    fun emptyStringMatchesKnownVectors() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", HashEngine.digest("", HashAlgorithm.MD5))
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", HashEngine.digest("", HashAlgorithm.SHA_1))
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            HashEngine.digest("", HashAlgorithm.SHA_256)
        )
        assertEquals(
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" +
                "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            HashEngine.digest("", HashAlgorithm.SHA_512)
        )
    }

    @Test
    fun abcMatchesKnownVectors() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", HashEngine.digest("abc", HashAlgorithm.MD5))
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", HashEngine.digest("abc", HashAlgorithm.SHA_1))
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            HashEngine.digest("abc", HashAlgorithm.SHA_256)
        )
    }

    @Test
    fun digestAllReturnsEveryAlgorithm() {
        val digests = HashEngine.digestAll("Koishi")

        assertEquals(HashAlgorithm.entries.toSet(), digests.keys)
    }
}
