package com.nanami.koishi.core.data.sync

import android.content.Context
import com.nanami.koishi.BuildConfig
import com.nanami.koishi.core.data.preferences.ThemePreferences
import com.nanami.koishi.core.data.storage.ToolStorageDao
import com.nanami.koishi.core.data.storage.ToolStorageEntity
import com.nanami.koishi.core.data.sync.model.KoishiBackup
import com.nanami.koishi.core.data.sync.model.KoishiPreferencesBackup
import com.nanami.koishi.core.designsystem.theme.AppTheme
import com.nanami.koishi.core.designsystem.theme.ThemeMode
import com.nanami.koishi.core.image.crop.CropPreferences
import com.nanami.koishi.feature.settings.AppLanguage
import com.nanami.koishi.feature.tools.image_search.engine.ImageSearchSettings
import com.nanami.koishi.feature.tools.image_search.engine.ImageSearchSettingsRepository
import com.nanami.koishi.feature.tools.today_in_history.engine.HistorySettings
import com.nanami.koishi.feature.tools.today_in_history.engine.HistorySettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WebDavSyncManager(
    private val context: Context,
    private val dao: ToolStorageDao,
    private val client: WebDavClient = WebDavClient()
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun testConnection(config: WebDavConfig) {
        client.testConnection(config)
    }

    suspend fun backup(config: WebDavConfig): KoishiBackup = withContext(Dispatchers.IO) {
        val backupData = createBackup(config.syncSensitive)
        val backupJson = json.encodeToString(backupData)
        client.uploadBackup(config, backupJson)
        val now = System.currentTimeMillis()
        WebDavPreferences.updateLastBackupTime(context, now)
        backupData
    }

    suspend fun restore(config: WebDavConfig): KoishiBackup = withContext(Dispatchers.IO) {
        val rawJson = client.downloadBackup(config)
        val backupData = json.decodeFromString<KoishiBackup>(rawJson)
        restoreBackup(backupData, config.syncSensitive)
        val now = System.currentTimeMillis()
        WebDavPreferences.updateLastRestoreTime(context, now)
        backupData
    }

    private suspend fun createBackup(includeSensitive: Boolean): KoishiBackup {
        val settingsPrefs = context.getSharedPreferences("koishi_settings", Context.MODE_PRIVATE)
        val langOrdinal = settingsPrefs.getInt("language", AppLanguage.SYSTEM.ordinal)
        val language = AppLanguage.entries.getOrElse(langOrdinal) { AppLanguage.SYSTEM }

        val favoritesPrefs = context.getSharedPreferences("koishi_favorites", Context.MODE_PRIVATE)
        val favorites = favoritesPrefs.getStringSet("favorite_tool_ids", emptySet()) ?: emptySet()

        val searchPrefs = context.getSharedPreferences("koishi_search", Context.MODE_PRIVATE)
        val searchRawJson = searchPrefs.getString("history_list_json", null)
        val searchHistory = if (searchRawJson != null) {
            try {
                json.decodeFromString<List<String>>(searchRawJson)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val allEntities = dao.getAll()
        val validEntities = allEntities.filterNot { it.toolId.startsWith("today_in_history:day:") }

        val toolStorageMap = mutableMapOf<String, String>()
        for (entity in validEntities) {
            when {
                !includeSensitive && entity.toolId == ImageSearchSettingsRepository.TOOL_ID -> {
                    val sanitized = try {
                        val data = json.decodeFromString(ImageSearchSettings.serializer(), entity.payloadJson)
                        json.encodeToString(ImageSearchSettings.serializer(), data.copy(sauceNaoApiKey = ""))
                    } catch (_: Exception) {
                        entity.payloadJson
                    }
                    toolStorageMap[entity.toolId] = sanitized
                }
                !includeSensitive && entity.toolId == HistorySettingsRepository.TOOL_ID -> {
                    val sanitized = try {
                        val data = json.decodeFromString(HistorySettings.serializer(), entity.payloadJson)
                        json.encodeToString(HistorySettings.serializer(), data.copy(showApiAppKey = ""))
                    } catch (_: Exception) {
                        entity.payloadJson
                    }
                    toolStorageMap[entity.toolId] = sanitized
                }
                else -> {
                    toolStorageMap[entity.toolId] = entity.payloadJson
                }
            }
        }

        return KoishiBackup(
            version = 1,
            createdAt = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            includesSensitive = includeSensitive,
            preferences = KoishiPreferencesBackup(
                themeMode = ThemePreferences.themeMode(context).name,
                appTheme = ThemePreferences.appTheme(context).name,
                amoled = ThemePreferences.isAmoled(context),
                language = language.name,
                cropConstrainToImage = CropPreferences.isConstrainToImage(context)
            ),
            favorites = favorites,
            searchHistory = searchHistory,
            toolStorage = toolStorageMap
        )
    }

    private suspend fun restoreBackup(backup: KoishiBackup, syncSensitive: Boolean) {
        val pref = backup.preferences
        ThemeMode.entries.find { it.name == pref.themeMode }?.let {
            ThemePreferences.setThemeMode(context, it)
        }
        AppTheme.entries.find { it.name == pref.appTheme }?.let {
            ThemePreferences.setAppTheme(context, it)
        }
        ThemePreferences.setAmoled(context, pref.amoled)

        AppLanguage.entries.find { it.name == pref.language }?.let { lang ->
            context.getSharedPreferences("koishi_settings", Context.MODE_PRIVATE)
                .edit()
                .putInt("language", lang.ordinal)
                .apply()
        }

        CropPreferences.setConstrainToImage(context, pref.cropConstrainToImage)

        context.getSharedPreferences("koishi_favorites", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("favorite_tool_ids", backup.favorites)
            .apply()

        context.getSharedPreferences("koishi_search", Context.MODE_PRIVATE)
            .edit()
            .putString("history_list_json", json.encodeToString(backup.searchHistory))
            .apply()

        for ((toolId, payloadJson) in backup.toolStorage) {
            if (toolId.startsWith("today_in_history:day:")) continue

            val finalPayload = when {
                !syncSensitive && toolId == ImageSearchSettingsRepository.TOOL_ID -> {
                    val existing = dao.find(toolId)
                    if (existing != null) {
                        try {
                            val local = json.decodeFromString(ImageSearchSettings.serializer(), existing.payloadJson)
                            val incoming = json.decodeFromString(ImageSearchSettings.serializer(), payloadJson)
                            json.encodeToString(
                                ImageSearchSettings.serializer(),
                                incoming.copy(sauceNaoApiKey = local.sauceNaoApiKey)
                            )
                        } catch (_: Exception) {
                            payloadJson
                        }
                    } else {
                        payloadJson
                    }
                }
                !syncSensitive && toolId == HistorySettingsRepository.TOOL_ID -> {
                    val existing = dao.find(toolId)
                    if (existing != null) {
                        try {
                            val local = json.decodeFromString(HistorySettings.serializer(), existing.payloadJson)
                            val incoming = json.decodeFromString(HistorySettings.serializer(), payloadJson)
                            json.encodeToString(
                                HistorySettings.serializer(),
                                incoming.copy(showApiAppKey = local.showApiAppKey)
                            )
                        } catch (_: Exception) {
                            payloadJson
                        }
                    } else {
                        payloadJson
                    }
                }
                else -> payloadJson
            }

            dao.upsert(
                ToolStorageEntity(
                    toolId = toolId,
                    payloadJson = finalPayload,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
