package com.nanami.koishi.feature.tools.text_apps.json

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonFormatterEngineTest {

    @Test
    fun formatProducesReadableJson() {
        val result = JsonFormatterEngine.format("""{"name":"Koishi","items":[1,true,null]}""", 2)

        assertEquals(
            """
            {
              "name": "Koishi",
              "items": [
                1,
                true,
                null
              ]
            }
            """.trimIndent(),
            result.getOrThrow()
        )
    }

    @Test
    fun minifyRemovesWhitespace() {
        val result = JsonFormatterEngine.minify(
            """
            {
              "name": "Koishi"
            }
            """.trimIndent()
        )

        assertEquals("""{"name":"Koishi"}""", result.getOrThrow())
    }

    @Test
    fun invalidJsonReturnsFailure() {
        assertTrue(JsonFormatterEngine.format("{invalid}").isFailure)
    }
}
