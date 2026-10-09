package com.nanami.koishi.feature.tools.lan_transfer.engine.protocol

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object LocalSendProtocol {
    const val VERSION = "2.2"
    const val DEFAULT_PORT = 53317
    const val MULTICAST_GROUP = "224.0.0.167"
    const val API_PREFIX = "/api/localsend/v2"
    const val UPLOAD_CHUNK_SIZE = 256 * 1024
}

enum class LocalSendEndpoint(val path: String) {
    INFO("/info"),
    REGISTER("/register"),
    PREPARE_UPLOAD("/prepare-upload"),
    UPLOAD("/upload"),
    CANCEL("/cancel");

    fun url(host: String, port: Int, protocol: String): HttpUrl? =
        "$protocol://${host.hostForUrl()}:$port${LocalSendProtocol.API_PREFIX}$path".toHttpUrlOrNull()
}

private fun String.hostForUrl(): String = if (contains(':')) "[$this]" else this
