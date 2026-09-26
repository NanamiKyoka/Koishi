package com.nanami.koishi.feature.home.poetry

data class Hitokoto(
    val uuid: String,
    val text: String,
    val source: String,
    val author: String
) {
    val websiteUrl: String get() = "$WEBSITE?uuid=$uuid"

    companion object {
        const val WEBSITE = "https://hitokoto.cn"
    }
}
