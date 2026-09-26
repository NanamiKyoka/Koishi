package com.nanami.koishi.feature.tools.meme_maker.engine

import kotlinx.serialization.Serializable

/**
 * 远程素材包描述，素材本体不随应用打包，由清单下发的 zip 地址按需下载
 */
@Serializable
data class MemeAssetPack(
    val id: String,
    val name: String,
    val zip: String,
    val cover: String? = null,
    val files: List<String> = emptyList()
)

@Serializable
data class MemeAssetManifest(
    val version: Int = 1,
    val packs: List<MemeAssetPack> = emptyList()
)

object MemeAssetSource {

    private const val REPO_BASE = "https://raw.githubusercontent.com/NanamiKyoka/Koishi/master/res/meme"

    private const val MIRROR_BASE = "https://cdn.jsdelivr.net/gh/NanamiKyoka/Koishi@master/res/meme"

    const val MANIFEST_FILE = "index.json"

    val BUILT_IN_PACKS = listOf(
        MemeAssetPack(
            id = "cheshire",
            name = "柴郡",
            zip = "Cheshire.zip",
            cover = "柴郡_01.png"
        ),
        MemeAssetPack(
            id = "cheshire_chan",
            name = "小柴郡",
            zip = "CheshireChan.zip",
            cover = "小柴郡_01.png"
        )
    )

    /**
     * 返回同一个资源的主源与镜像地址，按顺序尝试可绕开单一 CDN 不可用的情况
     */
    fun candidateUrls(file: String): List<String> = listOf("$REPO_BASE/$file", "$MIRROR_BASE/$file")
}
