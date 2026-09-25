package com.nanami.koishi.feature.tools.decision_maker.engine

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 主题档案的导入导出通道，负责与 SAF 选择的文档 URI 交互
 */
class DecisionArchiveStore(private val context: Context) {

    suspend fun write(uri: Uri, json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(json.toByteArray())
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun read(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            null
        }
    }
}
