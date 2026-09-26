package com.nanami.koishi.feature.tools.bili_cover.engine

object BiliCoverParser {

    private const val BV_PATTERN = "BV[0-9A-Za-z]{10}"

    private val BV_TOKEN = Regex("(?<![0-9A-Za-z])$BV_PATTERN(?![0-9A-Za-z])")

    private val BV_EXACT = Regex("^$BV_PATTERN$")

    private val AV_TOKEN = Regex("(?i)(?<![0-9A-Za-z])av(\\d{1,12})(?![0-9])")

    private val BARE_DIGITS = Regex("(?<!\\d)\\d{1,19}(?!\\d)")

    private val QUERY_ID = Regex("(?:^|&)id=(\\d+)")

    private val AID_PARAM = Regex("[?&]aid=(\\d+)")

    private val LINK_TOKEN = Regex(
        "(?i)(?<![\\w.-])(?:https?://)?(?:[\\w-]+\\.)*(?:bilibili\\.com|b23\\.tv|bili2233\\.cn)" +
                "(?:/[^\\s\"'<>，。、；：！？（）【】]*)?")

    private const val OPUS_ID_MIN_LENGTH = 15

    fun parse(input: String): BiliParseResult {
        val text = input.trim()
        if (text.isEmpty()) return BiliParseResult.Empty

        val matches = LINK_TOKEN.findAll(text).toList()
        for (match in matches) {
            val url = normalizeUrl(trimTrailingPunctuation(match.value))
            if (url.isBlank()) continue
            classify(url)?.let { return it }
        }
        if (matches.isNotEmpty()) return BiliParseResult.Unrecognized

        return findBareTarget(text)
            ?.let { BiliParseResult.Target(it) }
            ?: BiliParseResult.Unrecognized
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true) -> trimmed
            trimmed.contains('.') -> "https://$trimmed"
            else -> ""
        }
    }

    private fun trimTrailingPunctuation(raw: String): String =
        raw.trim().trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}', '>', '。', '，', '、')

    private fun classify(url: String): BiliParseResult? {
        val host = hostOf(url)
        val pathWithQuery = url.substringAfter("://", url).substringAfter('/', "")
        val path = pathWithQuery.substringBefore('?')
        val query = pathWithQuery.substringAfter('?', "")
        val segments = path.split('/').filter { it.isNotEmpty() }

        if (host == "b23.tv" || host == "bili2233.cn" || host.endsWith(".b23.tv")) {
            return BiliParseResult.ShortLink(url)
        }
        if (!host.endsWith("bilibili.com")) return null

        if (host.startsWith("live.")) {
            val roomId = segments.lastOrNull { it.all(Char::isDigit) }?.toLongOrNull() ?: return null
            return BiliParseResult.Target(BiliTarget.Live(roomId))
        }

        if (host.startsWith("t.") && segments.size == 1) {
            val id = segments.first()
            if (id.all(Char::isDigit)) return BiliParseResult.Target(BiliTarget.Opus(id))
        }

        return when (segments.firstOrNull()) {
            "video" -> parseVideoSegment(segments.getOrNull(1))
            "read" -> parseReadSegment(segments.getOrNull(1), query)
            "opus" -> segments.getOrNull(1)
                ?.takeIf { it.all(Char::isDigit) }
                ?.let { BiliParseResult.Target(BiliTarget.Opus(it)) }
            else -> null
        } ?: parseEmbeddedVideo(url)
    }

    private fun parseEmbeddedVideo(url: String): BiliParseResult? {
        BV_TOKEN.find(url)?.let { return BiliParseResult.Target(BiliTarget.Video(bvid = it.value)) }
        return AID_PARAM.find(url)
            ?.groupValues?.get(1)
            ?.toLongOrNull()
            ?.let { BiliParseResult.Target(BiliTarget.Video(aid = it)) }
    }

    private fun parseVideoSegment(token: String?): BiliParseResult? {
        val id = token?.takeIf { it.isNotEmpty() } ?: return null
        return when {
            BV_EXACT.matches(id) -> BiliParseResult.Target(BiliTarget.Video(bvid = id))
            id.startsWith("av", true) -> id.drop(2).toLongOrNull()
                ?.let { BiliParseResult.Target(BiliTarget.Video(aid = it)) }
            else -> null
        }
    }

    private fun parseReadSegment(token: String?, query: String): BiliParseResult? {
        val id = token?.takeIf { it.isNotEmpty() } ?: return null
        return when {
            id.startsWith("cv", true) -> id.drop(2).toLongOrNull()
                ?.let { BiliParseResult.Target(BiliTarget.Article(it)) }
            id == "mobile" -> QUERY_ID.find(query)?.groupValues?.get(1)?.toLongOrNull()
                ?.let { BiliParseResult.Target(BiliTarget.Article(it)) }
            else -> null
        }
    }

    private fun findBareTarget(text: String): BiliTarget? {
        BV_TOKEN.find(text)?.let { return BiliTarget.Video(bvid = it.value) }

        AV_TOKEN.find(text)?.let { match ->
            match.groupValues[1].toLongOrNull()?.let { return BiliTarget.Video(aid = it) }
        }

        val digits = BARE_DIGITS.find(text)?.value ?: return null
        return if (digits.length >= OPUS_ID_MIN_LENGTH) {
            BiliTarget.Opus(digits)
        } else {
            digits.toLongOrNull()?.let { BiliTarget.Video(aid = it) }
        }
    }

    private fun hostOf(url: String): String =
        url.substringAfter("://", url)
            .substringBefore('/')
            .substringBefore('?')
            .lowercase()
}
