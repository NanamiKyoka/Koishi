package com.nanami.koishi.feature.tools.bili_cover.engine

import com.nanami.koishi.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object BiliCoverEngines {

    private const val VIDEO_API = "https://api.bilibili.com/x/web-interface/view"
    private const val ARTICLE_API = "https://api.bilibili.com/x/article/viewinfo"
    private const val OPUS_API = "https://api.bilibili.com/x/polymer/web-dynamic/v1/opus/detail"
    private const val LIVE_API = "https://api.live.bilibili.com/room/v1/Room/get_info"
    private const val LIVE_MASTER_API = "https://api.live.bilibili.com/live_user/v1/Master/info"

    private const val HOME_URL = "https://www.bilibili.com/"
    private const val WEB_REFERER = "https://www.bilibili.com/"
    private const val LIVE_REFERER = "https://live.bilibili.com/"

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    private const val CODE_RISK_CONTROL = -352
    private const val CODE_RATE_LIMIT = -412
    private const val CODE_NOT_FOUND = -404
    private const val CODE_BAD_REQUEST = -400

    private const val OPUS_TITLE_SUFFIX = " - 哔哩哔哩"

    private val BUVID_COOKIE = Regex("buvid3=([^;]+)")

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * 访客标识，接口风控（-352 / -412）时用得上，首次使用或命中风控后从首页响应头里补采
     */
    private var buvid3: String? = null

    suspend fun resolveShortLink(url: String): String = withContext(Dispatchers.IO) {
        val request = baseRequest(url, WEB_REFERER).get().build()
        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: Exception) {
            throw toNetworkException(e)
        }
        response.use { res ->
            if (!res.isSuccessful) throw BiliCoverException.ShortLink()
            val finalUrl = res.request.url.toString()
            val host = hostOf(finalUrl)
            if (host == "b23.tv" || host == "bili2233.cn" || host.endsWith(".b23.tv")) {
                throw BiliCoverException.ShortLink()
            }
            finalUrl
        }
    }

    suspend fun fetchCover(target: BiliTarget): BiliCoverResult = when (target) {
        is BiliTarget.Video -> fetchVideo(target)
        is BiliTarget.Article -> fetchArticle(target)
        is BiliTarget.Opus -> fetchOpus(target)
        is BiliTarget.Live -> fetchLive(target)
    }

    suspend fun fetchImageBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val response = try {
            httpClient.newCall(baseRequest(url, WEB_REFERER).get().build()).execute()
        } catch (e: Exception) {
            throw toNetworkException(e)
        }
        response.use { res ->
            if (!res.isSuccessful) throw BiliCoverException.ServerError(res.code)
            res.body?.byteStream()?.readBytes() ?: throw BiliCoverException.Parse()
        }
    }

    private suspend fun fetchVideo(target: BiliTarget.Video): BiliCoverResult {
        val query = target.bvid?.let { "bvid=$it" } ?: "aid=${target.aid ?: 0L}"
        val root = fetchApi("$VIDEO_API?$query")
        val data = root["data"] as? JsonObject ?: throw BiliCoverException.Parse()

        val covers = mutableListOf<BiliCoverAsset>()
        addCover(covers, data["pic"].text(), R.string.bili_cover_label_cover)
        (data["pages"] as? JsonArray)?.forEach { element ->
            val page = element as? JsonObject ?: return@forEach
            addCover(
                covers,
                page["first_frame"].text(),
                R.string.bili_cover_label_first_frame,
                page["page"].number().toInt()
            )
        }
        requireCovers(covers)

        return BiliCoverResult(
            target = target,
            title = data["title"].text(),
            author = (data["owner"] as? JsonObject)?.get("name").text(),
            covers = covers,
            meta = listOf(
                BiliCoverMeta(R.string.bili_cover_meta_bvid, data["bvid"].text()),
                BiliCoverMeta(R.string.bili_cover_meta_aid, data["aid"].number().toString()),
                BiliCoverMeta(R.string.bili_cover_meta_duration, formatDuration(data["duration"].number())),
                BiliCoverMeta(
                    R.string.bili_cover_meta_views,
                    ((data["stat"] as? JsonObject)?.get("view").number()).toString()
                )
            )
        )
    }

    private suspend fun fetchArticle(target: BiliTarget.Article): BiliCoverResult {
        val root = fetchApi("$ARTICLE_API?id=${target.cvId}")
        val data = root["data"] as? JsonObject ?: throw BiliCoverException.Parse()

        val covers = mutableListOf<BiliCoverAsset>()
        addCover(
            covers,
            data["origin_image_urls"].strings().firstOrNull() ?: data["banner_url"].text(),
            R.string.bili_cover_label_banner
        )
        data["image_urls"].strings().forEachIndexed { index, url ->
            addCover(covers, url, R.string.bili_cover_label_article_image, index + 1)
        }
        requireCovers(covers)

        val stats = data["stats"] as? JsonObject
        return BiliCoverResult(
            target = target,
            title = data["title"].text(),
            author = data["author_name"].text(),
            covers = covers,
            meta = listOf(
                BiliCoverMeta(R.string.bili_cover_meta_article_id, target.identifier),
                BiliCoverMeta(R.string.bili_cover_meta_reads, (stats?.get("view").number()).toString()),
                BiliCoverMeta(R.string.bili_cover_meta_likes, (stats?.get("like").number()).toString())
            )
        )
    }

    private suspend fun fetchOpus(target: BiliTarget.Opus): BiliCoverResult {
        val root = fetchApi(
            "$OPUS_API?id=${target.opusId}&timezone_offset=-480&features=itemOpusStyle,opusBigCover",
            ensureCookie = true
        )
        val item = (root["data"] as? JsonObject)?.get("item") as? JsonObject
            ?: throw BiliCoverException.NotFound()
        val modules = (item["modules"] as? JsonArray).orEmpty()

        val covers = mutableListOf<BiliCoverAsset>()
        modules.filter { it.moduleType() == MODULE_TYPE_CONTENT }.forEach moduleLoop@{ module ->
            val content = (module as? JsonObject)?.get("module_content") as? JsonObject ?: return@moduleLoop
            (content["paragraphs"] as? JsonArray).orEmpty().forEach paragraphLoop@{ paragraph ->
                val pic = (paragraph as? JsonObject)?.get("pic") as? JsonObject ?: return@paragraphLoop
                (pic["pics"] as? JsonArray).orEmpty().forEach { picItem ->
                    addCover(
                        covers,
                        (picItem as? JsonObject)?.get("url").text(),
                        if (covers.isEmpty()) R.string.bili_cover_label_cover
                        else R.string.bili_cover_label_opus_image,
                        covers.size + 1
                    )
                }
            }
        }
        requireCovers(covers)

        val title = modules.firstOrNull { it.moduleType() == MODULE_TYPE_TITLE }
            ?.let { (it as? JsonObject)?.get("module_title") as? JsonObject }
            ?.get("text").text()
            .orEmpty()
            .ifBlank { (item["basic"] as? JsonObject)?.get("title").text() }

        val author = modules.firstOrNull { it.moduleType() == MODULE_TYPE_AUTHOR }
            ?.let { (it as? JsonObject)?.get("module_author") as? JsonObject }
            ?.get("name").text()
            .orEmpty()

        return BiliCoverResult(
            target = target,
            title = title.removeSuffix(OPUS_TITLE_SUFFIX).trim(),
            author = author,
            covers = covers,
            meta = listOf(BiliCoverMeta(R.string.bili_cover_meta_opus_id, target.opusId))
        )
    }

    private suspend fun fetchLive(target: BiliTarget.Live): BiliCoverResult {
        val root = fetchApi("$LIVE_API?room_id=${target.roomId}", referer = LIVE_REFERER)
        val data = root["data"] as? JsonObject ?: throw BiliCoverException.Parse()

        val covers = mutableListOf<BiliCoverAsset>()
        addCover(covers, data["user_cover"].text(), R.string.bili_cover_label_live_cover)
        addCover(covers, data["keyframe"].text(), R.string.bili_cover_label_live_keyframe)
        addCover(covers, data["background"].text(), R.string.bili_cover_label_live_background)
        requireCovers(covers)

        val uid = data["uid"].number()
        return BiliCoverResult(
            target = target,
            title = data["title"].text(),
            author = fetchLiveAnchorName(uid),
            covers = covers,
            meta = listOf(
                BiliCoverMeta(R.string.bili_cover_meta_room_id, data["room_id"].number().toString()),
                BiliCoverMeta(R.string.bili_cover_meta_uid, uid.toString()),
                BiliCoverMeta(R.string.bili_cover_meta_online, data["online"].number().toString())
            ),
            statusRes = when (data["live_status"].number().toInt()) {
                1 -> R.string.bili_cover_live_status_live
                2 -> R.string.bili_cover_live_status_round
                else -> R.string.bili_cover_live_status_offline
            }
        )
    }

    private suspend fun fetchLiveAnchorName(uid: Long): String = try {
        if (uid <= 0L) {
            ""
        } else {
            val root = fetchApi("$LIVE_MASTER_API?uid=$uid", referer = LIVE_REFERER)
            ((root["data"] as? JsonObject)?.get("info") as? JsonObject)?.get("uname").text()
        }
    } catch (e: Exception) {
        ""
    }

    private suspend fun fetchApi(
        url: String,
        referer: String = WEB_REFERER,
        ensureCookie: Boolean = false
    ): JsonObject {
        if (ensureCookie) ensureBuvid()

        var root = parseRoot(httpGet(url, referer))
        if (codeOf(root) == CODE_RISK_CONTROL || codeOf(root) == CODE_RATE_LIMIT) {
            ensureBuvid(force = true)
            root = parseRoot(httpGet(url, referer))
        }

        val code = codeOf(root)
        if (code != 0) throw classifyCode(code)
        return root
    }

    private suspend fun ensureBuvid(force: Boolean = false) {
        if (!force && buvid3 != null) return
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(HOME_URL)
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    response.headers("Set-Cookie").forEach { cookie ->
                        BUVID_COOKIE.find(cookie)?.let { buvid3 = it.groupValues[1] }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun httpGet(url: String, referer: String): String = withContext(Dispatchers.IO) {
        val response = try {
            httpClient.newCall(baseRequest(url, referer).get().build()).execute()
        } catch (e: Exception) {
            throw toNetworkException(e)
        }
        response.use { res ->
            if (!res.isSuccessful) throw BiliCoverException.ServerError(res.code)
            res.body?.string().orEmpty()
        }
    }

    private fun baseRequest(url: String, referer: String): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", referer)
            .header("Accept", "application/json, text/plain, */*")
        buvid3?.let { builder.header("Cookie", "buvid3=$it") }
        return builder
    }

    private fun parseRoot(body: String): JsonObject = try {
        json.parseToJsonElement(body) as? JsonObject ?: throw BiliCoverException.Parse()
    } catch (e: BiliCoverException) {
        throw e
    } catch (e: Exception) {
        throw BiliCoverException.Parse()
    }

    private fun codeOf(root: JsonObject): Int = (root["code"] as? JsonPrimitive)?.intOrNull ?: 0

    private fun classifyCode(code: Int): BiliCoverException = when (code) {
        CODE_NOT_FOUND, CODE_BAD_REQUEST -> BiliCoverException.NotFound()
        CODE_RISK_CONTROL, CODE_RATE_LIMIT -> BiliCoverException.RiskControl()
        else -> BiliCoverException.ServerError(code)
    }

    private fun toNetworkException(e: Exception): BiliCoverException = when (e) {
        is SocketException, is SocketTimeoutException, is UnknownHostException, is IOException ->
            BiliCoverException.Network()
        else -> BiliCoverException.Network()
    }

    private fun addCover(
        covers: MutableList<BiliCoverAsset>,
        rawUrl: String,
        labelRes: Int,
        labelArg: Int = 0
    ) {
        val url = normalizeImageUrl(rawUrl)
        if (url.isBlank()) return
        if (covers.any { it.url == url }) return
        covers += BiliCoverAsset(url = url, labelRes = labelRes, labelArg = labelArg)
    }

    private fun requireCovers(covers: List<BiliCoverAsset>) {
        if (covers.isEmpty()) throw BiliCoverException.NoCover()
    }

    /**
     * B站图床把裁剪与压缩规则拼在 @ 之后，截断后即可取回未压缩原图；
     * 视频封面字段历史上返回 http 明文，而 Android 9+ 默认拦截明文流量，故统一升级协议
     */
    private fun normalizeImageUrl(raw: String): String {
        val trimmed = raw.trim().substringBefore('@')
        return when {
            trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("http://") -> "https://" + trimmed.removePrefix("http://")
            trimmed.startsWith("//") -> "https:$trimmed"
            else -> ""
        }
    }

    private fun hostOf(url: String): String =
        url.substringAfter("://", url)
            .substringBefore('/')
            .substringBefore('?')
            .lowercase()

    private fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()

    private fun JsonElement?.number(): Long = (this as? JsonPrimitive)?.longOrNull ?: 0L

    private fun JsonElement?.strings(): List<String> =
        (this as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
            .filter { it.isNotEmpty() }

    private fun JsonElement.moduleType(): String = (this as? JsonObject)?.get("module_type").text()

    private const val MODULE_TYPE_CONTENT = "MODULE_TYPE_CONTENT"
    private const val MODULE_TYPE_TITLE = "MODULE_TYPE_TITLE"
    private const val MODULE_TYPE_AUTHOR = "MODULE_TYPE_AUTHOR"
}
