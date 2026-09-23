package com.nanami.koishi.feature.tools.image_search.parser

import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.image_search.engine.parser.Ascii2dParser
import com.nanami.koishi.feature.tools.image_search.engine.parser.SauceNaoParser
import com.nanami.koishi.feature.tools.image_search.engine.parser.TraceMoeParser
import com.nanami.koishi.feature.tools.image_search.model.SearchEngineEnum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ImageSearchParsersTest {

    @Test
    fun testSauceNaoParserSuccess() {
        val mockJson = """
        {
          "header": {
            "status": 0,
            "results_returned": 2
          },
          "results": [
            {
              "header": {
                "similarity": "94.21",
                "thumbnail": "https://img1.saucenao.com/sample.jpg",
                "index_name": "Index #5: Pixiv"
              },
              "data": {
                "ext_urls": [
                  "https://www.pixiv.net/artworks/10000000"
                ],
                "title": "博丽灵梦",
                "member_name": "博丽神社",
                "pixiv_id": 10000000
              }
            },
            {
              "header": {
                "similarity": "88.10",
                "thumbnail": "https://img1.saucenao.com/sample2.jpg"
              },
              "data": {
                "twitter_user_handle": "alice_margatroid",
                "tweet_id": "123456789",
                "ext_urls": [
                  "https://twitter.com/alice_margatroid/status/123456789"
                ]
              }
            }
          ]
        }
        """.trimIndent()

        val results = SauceNaoParser.parse(mockJson)
        assertEquals(2, results.size)

        val first = results[0]
        assertEquals(SearchEngineEnum.SAUCENAO, first.engine)
        assertEquals("博丽灵梦", first.title)
        assertEquals(94.21f, first.similarity!!, 0.01f)
        assertEquals("博丽神社", first.author)
        assertEquals("https://www.pixiv.net/artworks/10000000", first.sourceUrl)
        assertEquals("https://img1.saucenao.com/sample.jpg", first.thumbnailUrl)
        assertTrue(first.extraInfo.any { it.first == R.string.image_search_label_pixiv_id && it.second == "10000000" })

        val second = results[1]
        assertEquals(88.10f, second.similarity!!, 0.01f)
        assertEquals("alice_margatroid", second.author)
        assertEquals("https://twitter.com/alice_margatroid/status/123456789", second.sourceUrl)
    }

    @Test
    fun testSauceNaoParserArrayFields() {
        // 测试 creator, material, source 为数组以及非法非 JsonPrimitive 字段的鲁棒性
        val mockJsonWithArrays = """
        {
          "header": {
            "status": 0,
            "results_returned": 1
          },
          "results": [
            {
              "header": {
                "similarity": "91.50",
                "thumbnail": "https://img1.saucenao.com/sample_array.jpg"
              },
              "data": {
                "creator": ["Author A", "Author B"],
                "material": ["Touhou Project", "Alice in Wonderland"],
                "source": ["Magazine Vol.1", "Web Special"],
                "ext_urls": [
                  "https://danbooru.donmai.us/posts/1"
                ]
              }
            }
          ]
        }
        """.trimIndent()

        val results = SauceNaoParser.parse(mockJsonWithArrays)
        assertEquals(1, results.size)
        val item = results[0]
        assertEquals("Author A, Author B", item.author)
        assertEquals("Magazine Vol.1, Web Special", item.title)
        assertEquals(91.50f, item.similarity!!, 0.01f)
    }

    @Test
    fun testSauceNaoParserRateLimitError() {
        val errorJson = """
        {
          "header": {
            "status": -1,
            "message": "Too many requests"
          }
        }
        """.trimIndent()

        try {
            SauceNaoParser.parse(errorJson)
            fail("应抛出频率限制异常")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("频率") == true || e.message?.contains("Too many requests") == true)
        }
    }

    @Test
    fun testTraceMoeParserSuccess() {
        val mockJson = """
        {
          "frameCount": 1000,
          "error": "",
          "result": [
            {
              "anilist": {
                "id": 10165,
                "title": {
                  "native": "日常",
                  "romaji": "Nichijou",
                  "english": "My Ordinary Life"
                }
              },
              "filename": "[Leopard-Raws] Nichijou - 01 (RAW).mp4",
              "episode": 1,
              "from": 75.42,
              "to": 78.10,
              "similarity": 0.952,
              "image": "https://media.trace.moe/image/10165/1/75.jpg"
            }
          ]
        }
        """.trimIndent()

        val results = TraceMoeParser.parse(mockJson)
        assertEquals(1, results.size)

        val item = results[0]
        assertEquals(SearchEngineEnum.TRACE_MOE, item.engine)
        assertEquals("日常", item.title)
        assertEquals(95.2f, item.similarity!!, 0.01f)
        assertEquals("https://media.trace.moe/image/10165/1/75.jpg", item.thumbnailUrl)
        assertEquals("https://anilist.co/anime/10165", item.sourceUrl)

        assertTrue(item.extraInfo.any { it.first == R.string.image_search_label_episode && it.second == "1" })
        assertTrue(item.extraInfo.any { it.first == R.string.image_search_label_timeline && it.second == "01:15 - 01:18" })
    }

    @Test
    fun testTraceMoeTimeFormatting() {
        assertEquals("00:00", TraceMoeParser.formatSeconds(0.0))
        assertEquals("01:30", TraceMoeParser.formatSeconds(90.5))
        assertEquals("01:05:20", TraceMoeParser.formatSeconds(3920.0))
    }

    @Test
    fun testAscii2dParserSuccess() {
        val mockHtml = """
        <!DOCTYPE html>
        <html>
        <body>
            <div class="container">
                <div class="row item-box">
                    <div class="col-xs-12 col-sm-12 col-md-4 image-box">
                        <img src="/images/thumbnail/abc12345.jpg" alt="preview" />
                    </div>
                    <div class="col-xs-12 col-sm-12 col-md-8 detail-box">
                        <h5 class="detail-box-title">
                            <a href="https://www.pixiv.net/artworks/98765432">古明地恋与无意识</a>
                        </h5>
                        <div class="info-box">
                            <a href="https://www.pixiv.net/users/12345">恋之作者</a>
                        </div>
                        <small class="text-muted">1920x1080 | PNG | 2026-05-14</small>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()

        val results = Ascii2dParser.parse(mockHtml, "https://ascii2d.net")
        assertEquals(1, results.size)

        val item = results[0]
        assertEquals(SearchEngineEnum.ASCII2D, item.engine)
        assertEquals("古明地恋与无意识", item.title)
        assertEquals("恋之作者", item.author)
        assertEquals("https://www.pixiv.net/artworks/98765432", item.sourceUrl)
        assertEquals("https://ascii2d.net/images/thumbnail/abc12345.jpg", item.thumbnailUrl)
        assertNull(item.similarity)
        assertTrue(item.extraInfo.any { it.first == R.string.image_search_label_spec })
    }

    @Test
    fun testAscii2dCloudflareDetection() {
        val cloudflareHtml = """
        <!DOCTYPE html>
        <html>
        <head><title>Just a moment...</title></head>
        <body>
            <div id="cf-browser-verification">Verify you are human</div>
        </body>
        </html>
        """.trimIndent()

        assertTrue(Ascii2dParser.isCloudflareBlocked(cloudflareHtml))
        try {
            Ascii2dParser.parse(cloudflareHtml)
            fail("Cloudflare 拦截应抛出异常以触发降级模式")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Cloudflare") == true)
        }
    }
}
