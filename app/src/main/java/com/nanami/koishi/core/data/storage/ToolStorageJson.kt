package com.nanami.koishi.core.data.storage

import kotlinx.serialization.json.Json

/**
 * 通用表内 JSON 载荷的统一编解码配置，保证所有工具写入格式一致
 */
object ToolStorageJson {

    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
}
