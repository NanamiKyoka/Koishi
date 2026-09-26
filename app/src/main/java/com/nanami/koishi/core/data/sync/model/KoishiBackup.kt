package com.nanami.koishi.core.data.sync.model

import kotlinx.serialization.Serializable

@Serializable
data class KoishiBackup(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val appVersion: String = "",
    val includesSensitive: Boolean = false,
    val preferences: KoishiPreferencesBackup = KoishiPreferencesBackup(),
    val favorites: Set<String> = emptySet(),
    val searchHistory: List<String> = emptyList(),
    val toolStorage: Map<String, String> = emptyMap()
)

@Serializable
data class KoishiPreferencesBackup(
    val themeMode: String = "",
    val appTheme: String = "",
    val amoled: Boolean = false,
    val language: String = "",
    val cropConstrainToImage: Boolean = true
)
