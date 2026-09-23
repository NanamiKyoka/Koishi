package com.nanami.koishi.feature.tools.image_search.engine.parser

import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.image_search.model.SearchResultItem
import com.nanami.koishi.feature.tools.image_search.model.SearchEngineEnum
import org.jsoup.Jsoup

class Ascii2dCloudflareException : Exception("Cloudflare blocked")

object Ascii2dParser {

    /**
     * 判断 HTML 是否为 Cloudflare 防护或反爬阻断页面
     */
    fun isCloudflareBlocked(html: String): Boolean {
        if (html.isBlank()) return false
        val lower = html.lowercase()
        return lower.contains("just a moment") ||
                lower.contains("cloudflare") && lower.contains("turnstile") ||
                lower.contains("attention required! | cloudflare") ||
                lower.contains("challenge-running") ||
                lower.contains("cf-browser-verification")
    }

    /**
     * 解析 ascii2d 搜索结果 HTML 页面
     */
    fun parse(html: String, baseUrl: String = "https://ascii2d.net"): List<SearchResultItem> {
        if (html.isBlank()) return emptyList()
        if (isCloudflareBlocked(html)) {
            throw Ascii2dCloudflareException()
        }

        val doc = Jsoup.parse(html, baseUrl)
        val items = mutableListOf<SearchResultItem>()

        val itemBoxes = doc.select(".item-box")

        for (box in itemBoxes) {
            val detailBox = box.selectFirst(".detail-box") ?: continue
            val imageBox = box.selectFirst(".image-box")

            // 缩略图
            val img = imageBox?.selectFirst("img")
            val thumbnail = img?.attr("abs:data-src")?.ifBlank { null }
                ?: img?.attr("abs:src")?.ifBlank { null }

            // 标题
            val titleElem = detailBox.selectFirst("h5")
            val titleLink = titleElem?.selectFirst("a")
            val title = titleLink?.text()?.ifBlank { null }
                ?: titleElem?.text()?.ifBlank { null }
                ?: ""

            // 来源链接 (优先取标题的链接，或者 pixiv / twitter / fantia 等链接)
            var sourceUrl = titleLink?.attr("abs:href")?.ifBlank { null }
            if (sourceUrl.isNullOrBlank() || sourceUrl.contains("ascii2d.net")) {
                val candidateLink = detailBox.selectFirst("a[href*='pixiv.net'], a[href*='twitter.com'], a[href*='x.com'], a[href*='fantia.jp'], a[href*='dlsite.com'], a[href*='booth.pm']")
                if (candidateLink != null) {
                    sourceUrl = candidateLink.attr("abs:href")
                }
            }

            // 作者信息
            val authorElem = detailBox.selectFirst("a[href*='users'], a[href*='twitter.com'], a[href*='x.com'], a[href*='fantia'], small a")
            val author = authorElem?.text()?.ifBlank { null }

            // 补充规格信息 (如分辨率、格式、日期等)
            val infoBox = detailBox.selectFirst(".info-box, small")
            val extraList = mutableListOf<Pair<Int, String>>()
            infoBox?.text()?.let { infoText ->
                if (infoText.isNotBlank()) {
                    extraList.add(R.string.image_search_label_spec to infoText.trim())
                }
            }

            items.add(
                SearchResultItem(
                    engine = SearchEngineEnum.ASCII2D,
                    title = title,
                    similarity = null,
                    thumbnailUrl = thumbnail,
                    author = author,
                    sourceUrl = sourceUrl,
                    extraInfo = extraList
                )
            )
        }

        return items
    }
}
