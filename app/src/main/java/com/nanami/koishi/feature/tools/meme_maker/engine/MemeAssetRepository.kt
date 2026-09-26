package com.nanami.koishi.feature.tools.meme_maker.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

data class MemeStickerAsset(
    val packId: String,
    val packName: String,
    val fileName: String,
    val displayName: String,
    val file: File,
    val zipUrl: String
) {
    val source: StickerSource.Asset get() = StickerSource.Asset(packId, fileName)
}

/**
 * 远程素材包的下载、解压与本地素材索引。
 *
 * 素材列表始终以本地磁盘为唯一依据：文件被清掉后对应条目自然从列表中消失，
 * 不会留下打不开的幽灵记录，也不会在用户未主动操作时重新下载。
 */
class MemeAssetRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun packDir(packId: String): File = File(File(context.filesDir, ASSET_DIR_NAME), packId)

    fun isPackReady(packId: String): Boolean {
        val dir = packDir(packId)
        return dir.isDirectory && dir.listFiles()?.any { it.isFile && it.isStickerFile() } == true
    }

    suspend fun loadManifest(): List<MemeAssetPack> = withContext(Dispatchers.IO) {
        val remote = runCatching {
            val bytes = downloadBytes(MemeAssetSource.MANIFEST_FILE)
            json.decodeFromString<MemeAssetManifest>(bytes.toString(Charsets.UTF_8))
        }.getOrNull()

        remote?.packs
            ?.filter { it.id.isNotBlank() && it.zip.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: MemeAssetSource.BUILT_IN_PACKS
    }

    suspend fun ensurePack(
        pack: MemeAssetPack,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        if (isPackReady(pack.id)) return@withContext Result.success(packDir(pack.id))

        val zipCache = File(context.cacheDir, "meme_${pack.id}_${System.currentTimeMillis()}.zip")
        try {
            onProgress(0.05f)
            zipCache.writeBytes(downloadBytes(pack.zip))

            onProgress(0.7f)
            val target = packDir(pack.id)
            if (target.exists()) target.deleteRecursively()
            if (!target.mkdirs()) throw IOException("cannot create ${target.absolutePath}")
            unzipStickers(zipCache, target)

            if (!isPackReady(pack.id)) throw IOException("no sticker extracted from ${pack.zip}")
            onProgress(1f)
            Result.success(target)
        } catch (t: Throwable) {
            runCatching { packDir(pack.id).deleteRecursively() }
            Result.failure(t)
        } finally {
            zipCache.delete()
        }
    }

    fun listAssets(packs: List<MemeAssetPack>): List<MemeStickerAsset> = packs.flatMap { pack ->
        val zipUrl = MemeAssetSource.candidateUrls(pack.zip).first()
        packDir(pack.id).listFiles()
            ?.filter { it.isFile && it.isStickerFile() }
            ?.sortedBy { it.name }
            ?.map { file ->
                MemeStickerAsset(
                    packId = pack.id,
                    packName = pack.name,
                    fileName = file.name,
                    displayName = file.nameWithoutExtension,
                    file = file,
                    zipUrl = zipUrl
                )
            }
            .orEmpty()
    }

    fun removePack(packId: String) {
        runCatching { packDir(packId).deleteRecursively() }
    }

    fun pruneUnknownPacks(packs: List<MemeAssetPack>) {
        val known = packs.map { it.id }.toSet()
        val root = File(context.filesDir, ASSET_DIR_NAME)
        root.listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.name !in known) {
                runCatching { dir.deleteRecursively() }
            }
        }
    }

    fun loadStickerBitmap(asset: MemeStickerAsset, maxSize: Int = 512): Bitmap? =
        decodeFile(asset.file, maxSize)

    fun loadStickerBitmap(packId: String, fileName: String, maxSize: Int = 512): Bitmap? =
        decodeFile(File(packDir(packId), fileName), maxSize)

    fun decodeLocalFile(file: File, maxDimension: Int): Bitmap? = decodeFile(file, maxDimension)

    fun decodeLocalImage(uri: Uri, maxDimension: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longer = maxOf(bounds.outWidth, bounds.outHeight)
        if (longer <= 0) {
            null
        } else {
            var sample = 1
            while (longer / sample > maxDimension) sample *= 2
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    } catch (t: Throwable) {
        null
    }

    fun isUriReadable(uri: Uri): Boolean = try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
    } catch (t: Throwable) {
        false
    }

    private fun decodeFile(file: File, maxSize: Int): Bitmap? = try {
        if (!file.isFile) {
            null
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val longer = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (longer / sample > maxSize) sample *= 2
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            )
        }
    } catch (t: Throwable) {
        null
    }

    /**
     * 按字节顺序逐个尝试主源与镜像，全部失败才抛出最后一次的异常
     */
    private fun downloadBytes(file: String): ByteArray {
        var lastError: Throwable? = null
        for (url in MemeAssetSource.candidateUrls(file)) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    return response.body?.bytes() ?: throw IOException("empty response body")
                }
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw lastError ?: IOException("download failed: $file")
    }

    /**
     * 素材包内的文件名未携带 UTF-8 标记，按 GBK 解码才能还原中文名，
     * 文件名本身不参与业务逻辑，仅用于展示与去重
     */
    private fun unzipStickers(zipFile: File, targetDir: File) {
        ZipFile(zipFile, LEGACY_CHARSET).use { archive ->
            val entries = archive.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory) continue
                val fileName = entry.name.replace('\\', '/').substringAfterLast('/')
                if (!fileName.isStickerFile()) continue
                val outFile = File(targetDir, fileName)
                archive.getInputStream(entry).use { input ->
                    FileOutputStream(outFile).use { output -> input.copyTo(output) }
                }
            }
        }
    }

    private fun File.isStickerFile(): Boolean = name.isStickerFile()

    private fun String.isStickerFile(): Boolean =
        substringAfterLast('.', "").lowercase() in STICKER_EXTENSIONS

    companion object {
        private const val ASSET_DIR_NAME = "meme_assets"
        private const val LEGACY_CHARSET_NAME = "GBK"
        private val LEGACY_CHARSET: Charset = Charset.forName(LEGACY_CHARSET_NAME)
        private val STICKER_EXTENSIONS = setOf("png", "webp")
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    }
}
