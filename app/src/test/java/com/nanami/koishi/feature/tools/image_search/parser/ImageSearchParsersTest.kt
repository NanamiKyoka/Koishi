package com.nanami.koishi.feature.tools.image_search.parser

import com.nanami.koishi.R
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
    fun testSauceNaoSourcePrefersArtworkOverDanbooru() {
        val mockJson = """
        {
          "header": { "status": 0, "results_returned": 1 },
          "results": [
            {
              "header": { "similarity": "92.00", "thumbnail": "https://img1.saucenao.com/t.jpg" },
              "data": {
                "title": "作品名",
                "member_name": "作者",
                "ext_urls": [
                  "https://danbooru.donmai.us/posts/1234567",
                  "https://www.pixiv.net/artworks/10000000"
                ],
                "source": "https://www.pixiv.net/artworks/10000000"
              }
            }
          ]
        }
        """.trimIndent()

        val item = SauceNaoParser.parse(mockJson).single()
        assertEquals("https://www.pixiv.net/artworks/10000000", item.sourceUrl)
    }

    @Test
    fun testSauceNaoSourcePicksArtworkWhenSourceFieldIsMissing() {
        // 没有 source 字段时，应从 ext_urls 中挑作品站而不是排第一的图库站
        val mockJson = """
        {
          "header": { "status": 0, "results_returned": 1 },
          "results": [
            {
              "header": { "similarity": "90.00", "thumbnail": "https://img1.saucenao.com/t.jpg" },
              "data": {
                "title": "作品名",
                "ext_urls": [
                  "https://gelbooru.com/index.php?page=post&s=view&id=999",
                  "https://twitter.com/someone/status/1111111111"
                ]
              }
            }
          ]
        }
        """.trimIndent()

        val item = SauceNaoParser.parse(mockJson).single()
        assertEquals("https://twitter.com/someone/status/1111111111", item.sourceUrl)
    }

    @Test
    fun testSauceNaoSourcePrefersHigherPriorityArtworkHost() {
        // Pixiv 优先级高于 Fanbox/Twitter，应选 Pixiv
        val mockJson = """
        {
          "header": { "status": 0, "results_returned": 1 },
          "results": [
            {
              "header": { "similarity": "88.00", "thumbnail": "https://img1.saucenao.com/t.jpg" },
              "data": {
                "title": "作品名",
                "ext_urls": [
                  "https://chan.sankakucomplex.com/post/show/987654",
                  "https://twitter.com/someone/status/1111111111",
                  "https://www.pixiv.net/artworks/55555555"
                ]
              }
            }
          ]
        }
        """.trimIndent()

        val item = SauceNaoParser.parse(mockJson).single()
        assertEquals("https://www.pixiv.net/artworks/55555555", item.sourceUrl)
    }

    @Test
    fun testSauceNaoSourceFallsBackToGalleryWhenNothingElseExists() {
        // 全部都是图库站时，仍要给出可点击的链接，不能返回 null
        val mockJson = """
        {
          "header": { "status": 0, "results_returned": 1 },
          "results": [
            {
              "header": { "similarity": "70.00", "thumbnail": "https://img1.saucenao.com/t.jpg" },
              "data": {
                "title": "作品名",
                "ext_urls": ["https://danbooru.donmai.us/posts/1234567"]
              }
            }
          ]
        }
        """.trimIndent()

        val item = SauceNaoParser.parse(mockJson).single()
        assertNotNull("即便只有图库站也应返回链接", item.sourceUrl)
        assertEquals("https://danbooru.donmai.us/posts/1234567", item.sourceUrl)
    }

    @Test
    fun testSauceNaoSourceIgnoresGalleryUrlInSourceField() {
        // data.source 本身是图库站时不应直接采用，继续从 ext_urls 找作品页
        val mockJson = """
        {
          "header": { "status": 0, "results_returned": 1 },
          "results": [
            {
              "header": { "similarity": "86.00", "thumbnail": "https://img1.saucenao.com/t.jpg" },
              "data": {
                "title": "作品名",
                "source": "https://danbooru.donmai.us/posts/1234567",
                "ext_urls": [
                  "https://danbooru.donmai.us/posts/1234567",
                  "https://www.pixiv.net/artworks/77777777"
                ]
              }
            }
          ]
        }
        """.trimIndent()

        val item = SauceNaoParser.parse(mockJson).single()
        assertEquals("https://www.pixiv.net/artworks/77777777", item.sourceUrl)
    }

    @Test
    fun testSauceNaoSourceRejectsNonUrlSourceField() {
        // Anime / Manga 索引里 data.source 是自然语言描述，不是链接
        // 以前会原样返回 → 传到 Custom Tabs 抛异常 → "无法打开浏览器或 Google 应用"
        val mockJson = """
        {
          "header": { "status": 0, "results_returned": 1 },
          "results": [
            {
              "header": { "similarity": "95.00", "thumbnail": "https://img1.saucenao.com/t.jpg" },
              "data": {
                "title": "Nichijou",
                "source": "Nichijou ep 1",
                "ext_urls": ["https://myanimelist.net/anime/10165"]
              }
            }
          ]
        }
        """.trimIndent()

        val item = SauceNaoParser.parse(mockJson).single()
        assertEquals("https://myanimelist.net/anime/10165", item.sourceUrl)
    }

    @Test
    fun testSauceNaoSourceRejectsNonUrlSourceWithNoExtUrls() {
        // source 是纯文本、ext_urls 也没有可用链接时，必须返回 null，绝不能把文本当网址
        val mockJson = """
        {
          "header": { "status": 0, "results_returned": 1 },
          "results": [
            {
              "header": { "similarity": "95.00", "thumbnail": "https://img1.saucenao.com/t.jpg" },
              "data": {
                "title": "Nichijou",
                "source": "Nichijou ep 1"
              }
            }
          ]
        }
        """.trimIndent()

        val item = SauceNaoParser.parse(mockJson).single()
        assertNull("非 URL 的 source 不应被当作链接返回", item.sourceUrl)
    }

    @Test
    fun testResolveSourceUrlRejectsNonHttpCandidates() {
        assertNull(
            SauceNaoParser.resolveSourceUrl(
                sourceField = "Kyoto Animation",
                extUrls = emptyList()
            )
        )
        assertEquals(
            "https://www.pixiv.net/artworks/3",
            SauceNaoParser.resolveSourceUrl(
                sourceField = "Comic Yuri Hime",
                extUrls = listOf("https://www.pixiv.net/artworks/3")
            )
        )
        // ext_urls 里混入非链接文本也应被过滤
        assertEquals(
            "https://www.pixiv.net/artworks/4",
            SauceNaoParser.resolveSourceUrl(
                sourceField = null,
                extUrls = listOf("not a url", "https://www.pixiv.net/artworks/4")
            )
        )
    }

    @Test
    fun testResolveSourceUrlDirectly() {
        assertEquals(
            "https://www.pixiv.net/artworks/1",
            SauceNaoParser.resolveSourceUrl(
                sourceField = null,
                extUrls = listOf("https://danbooru.donmai.us/posts/1", "https://www.pixiv.net/artworks/1")
            )
        )

        assertEquals(
            "https://www.pixiv.net/artworks/9",
            SauceNaoParser.resolveSourceUrl(
                sourceField = "https://www.pixiv.net/artworks/9",
                extUrls = listOf("https://twitter.com/u/status/1")
            )
        )

        assertNull(SauceNaoParser.resolveSourceUrl(null, emptyList()))

        assertEquals(
            "https://example.com/a",
            SauceNaoParser.resolveSourceUrl(
                sourceField = null,
                extUrls = listOf("https://danbooru.donmai.us/posts/1", "https://example.com/a")
            )
        )
    }

    @Test
    fun testSauceNaoParserRateLimitError() {        val errorJson = """
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

}
