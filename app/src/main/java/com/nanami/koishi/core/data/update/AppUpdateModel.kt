package com.nanami.koishi.core.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubReleaseDto(
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("body")
    val body: String? = null,
    @SerialName("html_url")
    val htmlUrl: String,
    @SerialName("published_at")
    val publishedAt: String? = null,
    @SerialName("assets")
    val assets: List<GithubAssetDto> = emptyList()
)

@Serializable
data class GithubAssetDto(
    @SerialName("name")
    val name: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    @SerialName("size")
    val size: Long = 0L,
    @SerialName("content_type")
    val contentType: String? = null
)

data class AppRelease(
    val tagName: String,
    val versionName: String,
    val name: String,
    val changelog: String,
    val htmlUrl: String,
    val assets: List<ReleaseAsset>
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val contentType: String
)

sealed interface UpdateCheckResult {
    data class NewVersion(
        val release: AppRelease,
        val matchedAsset: ReleaseAsset?
    ) : UpdateCheckResult

    data class AlreadyLatest(
        val currentVersion: String
    ) : UpdateCheckResult

    data class Error(
        val message: String
    ) : UpdateCheckResult
}

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val raw: String
) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    companion object {
        fun parse(version: String): SemanticVersion {
            val clean = version.trim().removePrefix("v").removePrefix("V")
            val base = clean.substringBefore('-').substringBefore('+')
            val parts = base.split('.').mapNotNull { it.toIntOrNull() }
            val major = parts.getOrElse(0) { 0 }
            val minor = parts.getOrElse(1) { 0 }
            val patch = parts.getOrElse(2) { 0 }
            return SemanticVersion(major, minor, patch, clean)
        }
    }
}
