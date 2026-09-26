package com.nanami.koishi.feature.tools.bili_cover.engine

import androidx.annotation.StringRes
import com.nanami.koishi.R

enum class BiliContentType(@StringRes val labelRes: Int) {
    VIDEO(R.string.bili_cover_type_video),
    ARTICLE(R.string.bili_cover_type_article),
    OPUS(R.string.bili_cover_type_opus),
    LIVE(R.string.bili_cover_type_live)
}

sealed interface BiliTarget {
    val type: BiliContentType
    val identifier: String
    val canonicalUrl: String

    data class Video(val bvid: String? = null, val aid: Long? = null) : BiliTarget {
        override val type: BiliContentType get() = BiliContentType.VIDEO
        override val identifier: String get() = bvid ?: "av${aid ?: 0L}"
        override val canonicalUrl: String
            get() = bvid?.let { "https://www.bilibili.com/video/$it" }
                ?: "https://www.bilibili.com/video/av${aid ?: 0L}"
    }

    data class Article(val cvId: Long) : BiliTarget {
        override val type: BiliContentType get() = BiliContentType.ARTICLE
        override val identifier: String get() = "cv$cvId"
        override val canonicalUrl: String get() = "https://www.bilibili.com/read/cv$cvId"
    }

    data class Opus(val opusId: String) : BiliTarget {
        override val type: BiliContentType get() = BiliContentType.OPUS
        override val identifier: String get() = opusId
        override val canonicalUrl: String get() = "https://www.bilibili.com/opus/$opusId"
    }

    data class Live(val roomId: Long) : BiliTarget {
        override val type: BiliContentType get() = BiliContentType.LIVE
        override val identifier: String get() = roomId.toString()
        override val canonicalUrl: String get() = "https://live.bilibili.com/$roomId"
    }
}

sealed interface BiliParseResult {
    data class Target(val target: BiliTarget) : BiliParseResult
    data class ShortLink(val url: String) : BiliParseResult
    data object Empty : BiliParseResult
    data object Unrecognized : BiliParseResult
}

data class BiliCoverAsset(
    val url: String,
    @StringRes val labelRes: Int,
    val labelArg: Int = 0
)

data class BiliCoverMeta(
    @StringRes val labelRes: Int,
    val value: String
)

data class BiliCoverResult(
    val target: BiliTarget,
    val title: String,
    val author: String,
    val covers: List<BiliCoverAsset>,
    val meta: List<BiliCoverMeta> = emptyList(),
    @StringRes val statusRes: Int? = null
)

sealed class BiliCoverException(message: String? = null) : Exception(message) {
    class EmptyInput : BiliCoverException()
    class UnrecognizedInput : BiliCoverException()
    class NotFound : BiliCoverException()
    class RiskControl : BiliCoverException()
    class NoCover : BiliCoverException()
    class ShortLink : BiliCoverException()
    class Network : BiliCoverException()
    class Parse : BiliCoverException()
    class ServerError(val statusCode: Int) : BiliCoverException("code=$statusCode")
}

fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return "00:00"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%02d:%02d".format(minutes, secs)
    }
}
