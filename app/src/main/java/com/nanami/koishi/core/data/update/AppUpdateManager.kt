package com.nanami.koishi.core.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class AppUpdateManager(
    private val owner: String = "NanamiKyoka",
    private val repo: String = "Koishi",
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun checkForUpdate(
        currentVersion: String,
        supportedAbis: Array<String> = Build.SUPPORTED_ABIS ?: emptyArray()
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Koishi-App")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error("HTTP ${response.code}: ${response.message}")
                }
                val bodyString = response.body?.string().orEmpty()
                if (bodyString.isBlank()) {
                    return@withContext UpdateCheckResult.Error("Empty response body")
                }
                val dto = json.decodeFromString<GithubReleaseDto>(bodyString)
                val release = AppRelease(
                    tagName = dto.tagName,
                    versionName = dto.tagName.trim().removePrefix("v").removePrefix("V"),
                    name = dto.name.orEmpty().ifEmpty { dto.tagName },
                    changelog = dto.body.orEmpty(),
                    htmlUrl = dto.htmlUrl,
                    assets = dto.assets.map { assetDto ->
                        ReleaseAsset(
                            name = assetDto.name,
                            downloadUrl = assetDto.browserDownloadUrl,
                            size = assetDto.size,
                            contentType = assetDto.contentType.orEmpty()
                        )
                    }
                )

                val latestVersion = SemanticVersion.parse(release.versionName)
                val current = SemanticVersion.parse(currentVersion)

                if (latestVersion > current) {
                    val matchedAsset = selectBestAsset(release.assets, supportedAbis)
                    UpdateCheckResult.NewVersion(release, matchedAsset)
                } else {
                    UpdateCheckResult.AlreadyLatest(currentVersion)
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun downloadApk(
        downloadUrl: String,
        destinationFile: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "Koishi-App")
            .build()

        destinationFile.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Download failed with HTTP ${response.code}")
                }
                val body = response.body ?: throw IOException("Empty response body")
                val contentLength = body.contentLength()

                body.byteStream().use { input ->
                    destinationFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesCopied = 0L
                        var read = input.read(buffer)
                        while (read != -1) {
                            coroutineContext.ensureActive()
                            output.write(buffer, 0, read)
                            bytesCopied += read
                            onProgress(bytesCopied, contentLength)
                            read = input.read(buffer)
                        }
                        output.flush()
                    }
                }
            }
        } catch (e: Exception) {
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            throw e
        }

        destinationFile
    }

    fun selectBestAsset(
        assets: List<ReleaseAsset>,
        supportedAbis: Array<String> = Build.SUPPORTED_ABIS ?: emptyArray()
    ): ReleaseAsset? {
        val apkAssets = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apkAssets.isEmpty()) return null

        for (abi in supportedAbis) {
            val match = apkAssets.firstOrNull { it.name.contains(abi, ignoreCase = true) }
            if (match != null) return match
        }

        val universal = apkAssets.firstOrNull { it.name.contains("universal", ignoreCase = true) }
        if (universal != null) return universal

        return apkAssets.firstOrNull()
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
