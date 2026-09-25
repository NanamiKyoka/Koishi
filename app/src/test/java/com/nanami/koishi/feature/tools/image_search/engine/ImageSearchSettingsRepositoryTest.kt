package com.nanami.koishi.feature.tools.image_search.engine

import com.nanami.koishi.core.data.storage.FakeToolStorageDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ImageSearchSettingsRepositoryTest {

    private val dao = FakeToolStorageDao()
    private val repository = ImageSearchSettingsRepository(dao)

    @Test
    fun `api key starts empty`() = runTest {
        assertEquals("", repository.currentData().sauceNaoApiKey)
        assertEquals("", repository.dataFlow.first().sauceNaoApiKey)
    }

    @Test
    fun `api key is trimmed before it is stored`() = runTest {
        repository.saveApiKey("  saucenao-key  ")

        assertEquals("saucenao-key", repository.currentData().sauceNaoApiKey)
        assertEquals("saucenao-key", repository.dataFlow.first().sauceNaoApiKey)
        assertNotNull(dao.find(ImageSearchSettingsRepository.TOOL_ID))
    }

    @Test
    fun `api key can be cleared again`() = runTest {
        repository.saveApiKey("saucenao-key")

        repository.saveApiKey("   ")

        assertEquals("", repository.currentData().sauceNaoApiKey)
    }
}
