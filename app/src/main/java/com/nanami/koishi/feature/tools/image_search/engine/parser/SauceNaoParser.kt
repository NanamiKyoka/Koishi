package com.nanami.koishi.feature.tools.image_search.engine.parser

import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.image_search.model.SearchResultItem
import com.nanami.koishi.feature.tools.image_search.model.SearchEngineEnum
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class SauceNaoException(val statusCode: Int, message: String) : Exception(message)

object SauceNaoParser {

    /**
     * 图库 / 聚合站域名片段。
     *
     * SauceNAO 的 ext_urls 往往把 Danbooru、Gelbooru 这类图库页排在第一位，
     * 但它们只是转载镜像，点进去看到的不是原作者的作品页，因此降为兜底。
     */
    private val GALLERY_HOSTS = listOf(
        "danbooru.donmai.us",
        "gelbooru.com",
        "safebooru.org",
        "safebooru.donmai.us",
        "chan.sankakucomplex.com",
        "sankakucomplex.com",
        "yande.re",
        "konachan.com",
        "konachan.net",
        "e-shuushuu.net",
        "anime-pictures.net",
        "rule34.xxx",
        "behoimi.org",
        "mangadex.org",
        "e-hentai.org",
        "exhentai.org",
        "nhentai.net",
        "tsumino.com",
        "booru.allthefallen.moe"
    )

    /**
     * 作品原始页域名优先级（越靠前越优先）。
     *
     * 命中后可直接跳转到创作者发布作品的原始页面，而不是图库转载页。
     */
    private val ARTWORK_HOST_PRIORITY = listOf(
        "pixiv.net",
        "twitter.com",
        "x.com",
        "fanbox.cc",
        "nijie.info",
        "seiga.nicovideo.jp",
        "skeb.jp",
        "pawoo.net",
        "baraag.net",
        "fantia.jp",
        "boosty.to",
        "patreon.com",
        "subscribestar.adult",
        "fanbox.pixiv.net",
        "deviantart.com",
        "artstation.com",
        "tinami.com",
        "pixiv.me",
        "mangadex.org",
        "anidb.net",
        "myanimelist.net",
        "anilist.co",
        "anime-planet.com",
        "imdb.com",
        "thetvdb.com",
        "dlsite.com",
        "dmm.co.jp",
        "getchu.com",
        "melonbooks.co.jp",
        "toranoana.jp",
        "booth.pm",
        "alicesoft.com",
        "visual-arts.jp",
        "key.visualarts.gr.jp",
        "type-moon.com"
    )

    private fun hostOf(url: String): String =
        url.substringAfter("://", url)
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .lowercase()
            .removePrefix("www.")

    /**
     * 判断候选字符串是否为可打开的 http/https 链接。
     *
     * SauceNAO 的 data.source 并非总是 URL：在 Anime / Manga / H-Anime 等索引中
     * 它是自然语言描述（例如 "Nichijou ep 1"、"Kyoto Animation"、"Comic Yuri Hime"）。
     * 若不过滤，这类文本会一路传到 Custom Tabs 导致「无法打开浏览器」。
     */
    private fun isHttpUrl(candidate: String?): Boolean {
        val value = candidate?.trim().orEmpty()
        if (value.isEmpty()) return false
        return value.startsWith("http://", ignoreCase = true) ||
                value.startsWith("https://", ignoreCase = true)
    }

    private fun isGalleryUrl(url: String): Boolean {
        val host = hostOf(url)
        return GALLERY_HOSTS.any { host == it || host.endsWith(".$it") }
    }

    private fun artworkPriority(url: String): Int {
        val host = hostOf(url)
        val idx = ARTWORK_HOST_PRIORITY.indexOfFirst { host == it || host.endsWith(".$it") }
        return if (idx >= 0) idx else Int.MAX_VALUE
    }

    /**
     * 从候选链接中挑出「作品原始页」。
     *
     */
    internal fun resolveSourceUrl(sourceField: String?, extUrls: List<String>): String? {
        val candidates = extUrls.filter { isHttpUrl(it) }
        val sourceUrl = sourceField?.takeIf { isHttpUrl(it) }

        sourceUrl?.takeIf { !isGalleryUrl(it) }?.let { return it }

        candidates
            .filter { artworkPriority(it) != Int.MAX_VALUE }
            .minByOrNull { artworkPriority(it) }
            ?.let { return it }

        candidates.firstOrNull { !isGalleryUrl(it) }?.let { return it }

        sourceUrl?.let { return it }

        return candidates.firstOrNull()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * 安全提取 JsonElement 为 String，如果为 Array 则拼接，遇到 Object/Null 不崩溃
     */
    fun JsonElement?.asSafeString(): String? {
        if (this == null) return null
        return when (this) {
            is JsonPrimitive -> this.contentOrNull
            is JsonArray -> this.mapNotNull { it.asSafeString() }.filter { it.isNotBlank() }.joinToString(", ").ifBlank { null }
            else -> null
        }
    }

    private fun JsonElement?.asSafeInt(): Int? {
        if (this == null) return null
        return when (this) {
            is JsonPrimitive -> this.intOrNull
            else -> null
        }
    }

    private fun JsonElement?.asSafeFloat(): Float? {
        if (this == null) return null
        return when (this) {
            is JsonPrimitive -> this.contentOrNull?.toFloatOrNull()
            else -> null
        }
    }

    /**
     * 解析 SauceNAO API 返回的 JSON 字符串
     */
    fun parse(jsonString: String): List<SearchResultItem> {
        if (jsonString.isBlank()) return emptyList()

        val rootElement = json.parseToJsonElement(jsonString)
        if (rootElement !is JsonObject) {
            return emptyList()
        }

        val rootHeader = rootElement["header"]?.let { if (it is JsonObject) it else null }
        val status = rootHeader?.get("status")?.asSafeInt() ?: 0
        val message = rootHeader?.get("message")?.asSafeString()

        if (status != 0) {
            throw SauceNaoException(status, message ?: "SauceNAO error status: $status")
        }

        val resultsArray = rootElement["results"]?.let { if (it is JsonArray) it else null } ?: return emptyList()
        val items = mutableListOf<SearchResultItem>()

        for (elem in resultsArray) {
            if (elem !is JsonObject) continue
            val header = elem["header"]?.let { if (it is JsonObject) it else null }
            val data = elem["data"]?.let { if (it is JsonObject) it else null }

            val similarity = header?.get("similarity")?.asSafeFloat()
            val thumbnail = header?.get("thumbnail")?.asSafeString()

            // 作品标题（安全解析字符串或数组）
            val title = data?.get("title")?.asSafeString()
                ?: data?.get("source")?.asSafeString()
                ?: data?.get("jp_name")?.asSafeString()
                ?: data?.get("eng_name")?.asSafeString()
                ?: data?.get("material")?.asSafeString()
                ?: header?.get("index_name")?.asSafeString()
                ?: ""

            // 作者/创作者
            val author = data?.get("member_name")?.asSafeString()
                ?: data?.get("author_name")?.asSafeString()
                ?: data?.get("twitter_user_handle")?.asSafeString()
                ?: data?.get("creator")?.asSafeString()

            // 外链列表
            val extUrls = mutableListOf<String>()
            data?.get("ext_urls")?.let { urlsElem ->
                if (urlsElem is JsonArray) {
                    for (u in urlsElem) {
                        u.asSafeString()?.let { extUrls.add(it) }
                    }
                } else {
                    urlsElem.asSafeString()?.let { extUrls.add(it) }
                }
            }

            val primarySourceUrl = resolveSourceUrl(
                sourceField = data?.get("source")?.asSafeString(),
                extUrls = extUrls
            )

            val extraList = mutableListOf<Pair<Int, String>>()
            data?.get("pixiv_id")?.asSafeString()?.let {
                extraList.add(R.string.image_search_label_pixiv_id to it)
            }
            data?.get("tweet_id")?.asSafeString()?.let {
                extraList.add(R.string.image_search_label_tweet_id to it)
            }
            data?.get("part")?.asSafeString()?.let {
                extraList.add(R.string.image_search_label_episode to it)
            }
            data?.get("est_time")?.asSafeString()?.let {
                extraList.add(R.string.image_search_label_timeline to it)
            }

            items.add(
                SearchResultItem(
                    engine = SearchEngineEnum.SAUCENAO,
                    title = title,
                    similarity = similarity,
                    thumbnailUrl = thumbnail,
                    author = author,
                    sourceUrl = primarySourceUrl,
                    extraInfo = extraList
                )
            )
        }

        return items
    }
}
