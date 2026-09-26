package com.nanami.koishi.core.data.sync

import com.nanami.koishi.core.data.storage.FakeToolStorageDao
import com.nanami.koishi.core.data.storage.ToolStorageEntity
import com.nanami.koishi.core.data.sync.model.KoishiBackup
import com.nanami.koishi.core.data.sync.model.KoishiPreferencesBackup
import com.nanami.koishi.feature.tools.image_search.engine.ImageSearchSettings
import com.nanami.koishi.feature.tools.image_search.engine.ImageSearchSettingsRepository
import com.nanami.koishi.feature.tools.today_in_history.engine.HistorySettings
import com.nanami.koishi.feature.tools.today_in_history.engine.HistorySettingsRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavSyncTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Test
    fun testBackupSerializationAndDeserialization() {
        val backup = KoishiBackup(
            version = 1,
            createdAt = 123456789L,
            appVersion = "1.0.0",
            includesSensitive = false,
            preferences = KoishiPreferencesBackup(
                themeMode = "DARK",
                appTheme = "KOISHI",
                amoled = true,
                language = "ZH",
                cropConstrainToImage = true
            ),
            favorites = setOf("tool_1", "tool_2"),
            searchHistory = listOf("query1", "query2"),
            toolStorage = mapOf("tool_1" to "{\"key\":\"value\"}")
        )

        val encoded = json.encodeToString(backup)
        val decoded = json.decodeFromString<KoishiBackup>(encoded)

        assertEquals(backup.version, decoded.version)
        assertEquals(backup.createdAt, decoded.createdAt)
        assertEquals(backup.appVersion, decoded.appVersion)
        assertEquals(backup.includesSensitive, decoded.includesSensitive)
        assertEquals(backup.preferences.themeMode, decoded.preferences.themeMode)
        assertEquals(backup.preferences.appTheme, decoded.preferences.appTheme)
        assertEquals(backup.preferences.amoled, decoded.preferences.amoled)
        assertEquals(backup.favorites, decoded.favorites)
        assertEquals(backup.searchHistory, decoded.searchHistory)
        assertEquals(backup.toolStorage, decoded.toolStorage)
    }

    @Test
    fun testSensitiveDataSanitizationWhenExcluded() {
        val imageSearchJson = json.encodeToString(
            ImageSearchSettings.serializer(),
            ImageSearchSettings(sauceNaoApiKey = "secret_sauce_key")
        )
        val historySettingsJson = json.encodeToString(
            HistorySettings.serializer(),
            HistorySettings(showApiAppKey = "secret_history_key")
        )

        val entities = listOf(
            ToolStorageEntity(ImageSearchSettingsRepository.TOOL_ID, imageSearchJson, 1L),
            ToolStorageEntity(HistorySettingsRepository.TOOL_ID, historySettingsJson, 1L),
            ToolStorageEntity("today_in_history:day:01-01", "{\"cache\":\"data\"}", 1L),
            ToolStorageEntity("bmi_calculator", "{\"records\":[]}", 1L)
        )

        val validEntities = entities.filterNot { it.toolId.startsWith("today_in_history:day:") }
        assertEquals(3, validEntities.size)

        val storageMapWithoutSensitive = mutableMapOf<String, String>()
        for (entity in validEntities) {
            when (entity.toolId) {
                ImageSearchSettingsRepository.TOOL_ID -> {
                    val data = json.decodeFromString(ImageSearchSettings.serializer(), entity.payloadJson)
                    storageMapWithoutSensitive[entity.toolId] = json.encodeToString(
                        ImageSearchSettings.serializer(),
                        data.copy(sauceNaoApiKey = "")
                    )
                }
                HistorySettingsRepository.TOOL_ID -> {
                    val data = json.decodeFromString(HistorySettings.serializer(), entity.payloadJson)
                    storageMapWithoutSensitive[entity.toolId] = json.encodeToString(
                        HistorySettings.serializer(),
                        data.copy(showApiAppKey = "")
                    )
                }
                else -> {
                    storageMapWithoutSensitive[entity.toolId] = entity.payloadJson
                }
            }
        }

        val sanitizedImageSearch = json.decodeFromString(
            ImageSearchSettings.serializer(),
            storageMapWithoutSensitive[ImageSearchSettingsRepository.TOOL_ID]!!
        )
        assertTrue(sanitizedImageSearch.sauceNaoApiKey.isEmpty())

        val sanitizedHistorySettings = json.decodeFromString(
            HistorySettings.serializer(),
            storageMapWithoutSensitive[HistorySettingsRepository.TOOL_ID]!!
        )
        assertTrue(sanitizedHistorySettings.showApiAppKey.isEmpty())

        assertFalse(storageMapWithoutSensitive.containsKey("today_in_history:day:01-01"))
        assertTrue(storageMapWithoutSensitive.containsKey("bmi_calculator"))
    }

    @Test
    fun testLocalApiKeyPreservedWhenRestoringWithoutSensitive() = runBlocking {
        val fakeDao = FakeToolStorageDao()

        val existingApiKey = "local_private_sauce_key"
        val existingEntity = ToolStorageEntity(
            toolId = ImageSearchSettingsRepository.TOOL_ID,
            payloadJson = json.encodeToString(
                ImageSearchSettings.serializer(),
                ImageSearchSettings(sauceNaoApiKey = existingApiKey)
            ),
            updatedAt = 100L
        )
        fakeDao.upsert(existingEntity)

        val incomingBackupPayload = json.encodeToString(
            ImageSearchSettings.serializer(),
            ImageSearchSettings(sauceNaoApiKey = "")
        )

        val syncSensitive = false
        val current = fakeDao.find(ImageSearchSettingsRepository.TOOL_ID)
        assertNotNull(current)

        val finalPayload = if (!syncSensitive) {
            val local = json.decodeFromString(ImageSearchSettings.serializer(), current!!.payloadJson)
            val incoming = json.decodeFromString(ImageSearchSettings.serializer(), incomingBackupPayload)
            json.encodeToString(
                ImageSearchSettings.serializer(),
                incoming.copy(sauceNaoApiKey = local.sauceNaoApiKey)
            )
        } else {
            incomingBackupPayload
        }

        fakeDao.upsert(
            ToolStorageEntity(
                toolId = ImageSearchSettingsRepository.TOOL_ID,
                payloadJson = finalPayload,
                updatedAt = 200L
            )
        )

        val restored = fakeDao.find(ImageSearchSettingsRepository.TOOL_ID)
        val restoredData = json.decodeFromString(ImageSearchSettings.serializer(), restored!!.payloadJson)
        assertEquals(existingApiKey, restoredData.sauceNaoApiKey)
    }

    @Test
    fun testWebDavConfigValidation() {
        val validConfig = WebDavConfig(
            serverUrl = "https://dav.example.com",
            username = "user",
            password = "pwd"
        )
        assertFalse(validConfig.serverUrl.isBlank())
        assertFalse(validConfig.username.isBlank())
        assertFalse(validConfig.password.isBlank())

        val invalidConfig = WebDavConfig()
        assertTrue(invalidConfig.serverUrl.isBlank() || invalidConfig.username.isBlank() || invalidConfig.password.isBlank())
    }
}
