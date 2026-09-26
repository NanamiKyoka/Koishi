package com.nanami.koishi.feature.tools.bili_cover.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BiliCoverParserTest {

    private fun target(input: String): BiliTarget =
        (BiliCoverParser.parse(input) as BiliParseResult.Target).target

    private fun assertUnrecognized(input: String) {
        assertEquals(BiliParseResult.Unrecognized, BiliCoverParser.parse(input))
    }

    @Test
    fun `blank input reports empty`() {
        assertEquals(BiliParseResult.Empty, BiliCoverParser.parse(""))
        assertEquals(BiliParseResult.Empty, BiliCoverParser.parse("   "))
    }

    @Test
    fun `bare bv id is recognized`() {
        val parsed = target("BV1GJ411x7h7")
        assertTrue(parsed is BiliTarget.Video)
        assertEquals("BV1GJ411x7h7", (parsed as BiliTarget.Video).bvid)
    }

    @Test
    fun `bare av id is recognized`() {
        val parsed = target("av170001")
        assertEquals(170001L, (parsed as BiliTarget.Video).aid)
    }

    @Test
    fun `bare numeric id is treated as avid`() {
        val parsed = target("170001")
        assertEquals(170001L, (parsed as BiliTarget.Video).aid)
    }

    @Test
    fun `long numeric id is treated as opus`() {
        val parsed = target("947531371067211815")
        assertTrue(parsed is BiliTarget.Opus)
    }

    @Test
    fun `video urls keep their identifier`() {
        assertEquals(
            "BV1GJ411x7h7",
            (target("https://www.bilibili.com/video/BV1GJ411x7h7") as BiliTarget.Video).bvid
        )
        assertEquals(
            "BV1GJ411x7h7",
            (target("https://www.bilibili.com/video/BV1GJ411x7h7/?spm_id_from=333.999") as BiliTarget.Video).bvid
        )
        assertEquals(
            170001L,
            (target("https://www.bilibili.com/video/av170001") as BiliTarget.Video).aid
        )
        assertEquals(
            "BV1GJ411x7h7",
            (target("m.bilibili.com/video/BV1GJ411x7h7") as BiliTarget.Video).bvid
        )
    }

    @Test
    fun `video bvid in query is recognized`() {
        assertEquals(
            "BV1GJ411x7h7",
            (target("https://www.bilibili.com/list/watchlater?bvid=BV1GJ411x7h7") as BiliTarget.Video).bvid
        )
    }

    @Test
    fun `article urls are recognized`() {
        assertEquals(23435927L, (target("https://www.bilibili.com/read/cv23435927/") as BiliTarget.Article).cvId)
        assertEquals(
            23435927L,
            (target("https://www.bilibili.com/read/mobile?id=23435927") as BiliTarget.Article).cvId
        )
    }

    @Test
    fun `opus and legacy dynamic urls are recognized`() {
        assertEquals(
            "947531371067211815",
            (target("https://www.bilibili.com/opus/947531371067211815") as BiliTarget.Opus).opusId
        )
        assertEquals(
            "953619104940425225",
            (target("https://t.bilibili.com/953619104940425225") as BiliTarget.Opus).opusId
        )
    }

    @Test
    fun `live urls are recognized`() {
        assertEquals(5441L, (target("https://live.bilibili.com/5441") as BiliTarget.Live).roomId)
        assertEquals(5441L, (target("https://live.bilibili.com/blanc/5441") as BiliTarget.Live).roomId)
    }

    @Test
    fun `short links are handed over for expansion`() {
        val withScheme = BiliCoverParser.parse("【示例视频】 https://b23.tv/AbCdEf")
        assertTrue(withScheme is BiliParseResult.ShortLink)
        assertEquals("https://b23.tv/AbCdEf", (withScheme as BiliParseResult.ShortLink).url)

        val withoutScheme = BiliCoverParser.parse("b23.tv/AbCdEf")
        assertTrue(withoutScheme is BiliParseResult.ShortLink)
    }

    @Test
    fun `unsupported bilibili urls are not mistaken for ids`() {
        assertUnrecognized("https://space.bilibili.com/12345")
        assertUnrecognized("https://www.bilibili.com/read/readlist/rl716666")
    }

    @Test
    fun `plain text is unrecognized`() {
        assertUnrecognized("这是一段普通文本")
        assertUnrecognized("https://www.bilibili.com")
    }

    @Test
    fun `targets expose canonical urls`() {
        assertEquals(
            "https://www.bilibili.com/video/BV1GJ411x7h7",
            target("BV1GJ411x7h7").canonicalUrl
        )
        assertEquals(
            "https://www.bilibili.com/video/av170001",
            target("av170001").canonicalUrl
        )
        assertEquals(
            "https://www.bilibili.com/read/cv23435927",
            target("https://www.bilibili.com/read/cv23435927").canonicalUrl
        )
        assertEquals(
            "https://live.bilibili.com/5441",
            target("https://live.bilibili.com/5441").canonicalUrl
        )
    }

    @Test
    fun `duration is formatted with hour rollover`() {
        assertEquals("00:00", formatDuration(0))
        assertEquals("03:33", formatDuration(213))
        assertEquals("1:05:20", formatDuration(3920))
    }
}
