package com.nanami.koishi.feature.tools.postal_code.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 中国大陆省市区县邮编数据的下载与本地缓存。
 * 首次使用时从 CDN 拉取原始 JSON，转存为紧凑文本，之后完全离线可用。
 * 数据来源：https://github.com/tombcato/china-zipcode-data （MIT License）
 */
class PostalDatasetRepository(context: Context) {

    private val appContext = context.applicationContext
    private val cacheFile = File(appContext.filesDir, CACHE_FILE_NAME)

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    @Volatile
    private var memory: PostalDataset? = null

    suspend fun load(): PostalDataset? = withContext(Dispatchers.IO) {
        memory?.let { return@withContext it }
        if (!cacheFile.exists()) return@withContext null
        val loaded = runCatching { PostalDataset.fromCompact(cacheFile.readText()) }.getOrNull()
        if (loaded != null && loaded.size > 0) memory = loaded
        loaded
    }

    suspend fun download(onProgress: (Int) -> Unit): PostalDataset = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(REMOTE_URL)
            .header("Accept", "application/json")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw PostalException.ServerError()

            val body = response.body ?: throw PostalException.Parse()
            val totalBytes = body.contentLength().takeIf { it > 0 } ?: 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            val sink = java.io.ByteArrayOutputStream()
            var readBytes = 0L
            var lastPercent = -1

            body.byteStream().use { stream ->
                while (true) {
                    val count = stream.read(buffer)
                    if (count == -1) break
                    sink.write(buffer, 0, count)
                    readBytes += count
                    if (totalBytes > 0) {
                        val percent = ((readBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent)
                        }
                    }
                }
            }

            onProgress(100)
            val dataset = PostalDataset.fromRaw(sink.toString(Charsets.UTF_8.name()))
            if (dataset.size == 0) throw PostalException.Parse()

            cacheFile.writeText(PostalDataset.toCompact(dataset))
            memory = dataset
            dataset
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        memory = null
        runCatching { cacheFile.delete() }
        Unit
    }

    private companion object {
        const val REMOTE_URL =
            "https://cdn.jsdelivr.net/npm/@tombcato/china-zipcode-data@1.0.2/china_zipcode_adcode.json"
        const val CACHE_FILE_NAME = "postal_cn_dataset_v1.txt"
    }
}
