package com.nanami.koishi.core.data.storage

import com.nanami.koishi.core.data.storage.sample.ImageSearchConfig
import com.nanami.koishi.core.data.storage.sample.ImageSearchConfigRepository
import com.nanami.koishi.core.data.storage.sample.OptionWeight
import com.nanami.koishi.core.data.storage.sample.WeightedOptionsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ImageSearchConfigRepositoryTest {

    private val dao = FakeToolStorageDao()
    private val repository = ImageSearchConfigRepository(dao)

    @Test
    fun `config starts from declared defaults`() = runTest {
        assertEquals(ImageSearchConfig(), repository.dataFlow.first())
        assertTrue(repository.currentData().hapticsEnabled)
        assertEquals(1280, repository.currentData().uploadMaxEdge)
    }

    @Test
    fun `config fields are persisted independently`() = runTest {
        repository.setApiKey("  sauce-key  ")
        repository.setAutoSearchOnPick(true)

        val stored = repository.currentData()
        assertEquals("sauce-key", stored.sauceNaoApiKey)
        assertTrue(stored.autoSearchOnPick)
        assertEquals(stored, repository.dataFlow.first())
    }

    @Test
    fun `boolean toggle flips previous value`() = runTest {
        assertTrue(repository.currentData().hapticsEnabled)

        assertFalse(repository.toggleHaptics().hapticsEnabled)
        assertTrue(repository.toggleHaptics().hapticsEnabled)
    }

    @Test
    fun `numeric field is clamped to supported range`() = runTest {
        assertEquals(ImageSearchConfig.MIN_UPLOAD_MAX_EDGE, repository.setUploadMaxEdge(16).uploadMaxEdge)
        assertEquals(ImageSearchConfig.MAX_UPLOAD_MAX_EDGE, repository.setUploadMaxEdge(8192).uploadMaxEdge)
    }

    @Test
    fun `config and list tools share the same table without interference`() = runTest {
        val optionsRepository = WeightedOptionsRepository(dao, Random(1))
        repository.setApiKey("shared-table-key")
        optionsRepository.addOption("出门散步")

        assertEquals("shared-table-key", repository.currentData().sauceNaoApiKey)
        assertEquals(1, optionsRepository.currentData().options.size)
        assertEquals(2, dao.observeAll().first().size)
    }
}

class WeightedOptionsRepositoryTest {

    private val dao = FakeToolStorageDao()
    private val repository = WeightedOptionsRepository(dao, Random(42))

    @Test
    fun `option labels are trimmed and duplicates rejected`() = runTest {
        val created = repository.addOption("  火锅  ", 6)

        assertNotNull(created)
        assertEquals("火锅", created?.label)
        assertEquals(6, created?.weight)
        assertNull(repository.addOption("火锅"))
        assertNull(repository.addOption("   "))
        assertEquals(1, repository.currentData().options.size)
    }

    @Test
    fun `weight is clamped and disabled options are excluded from draw`() = runTest {
        val option = repository.addOption("看电影") ?: error("option should be created")

        assertEquals(OptionWeight.MAX, repository.setWeight(option.id, 99).options.single().weight)

        repository.setEnabled(option.id, false)
        assertTrue(repository.currentData().drawableOptions.isEmpty())
        assertNull(repository.pick())
    }

    @Test
    fun `pick returns null instead of stale result when nothing is drawable`() = runTest {
        val option = repository.addOption("夜宵") ?: error("option should be created")
        assertEquals(option.id, repository.pick()?.id)

        repository.setEnabled(option.id, false)

        assertNull(repository.pick())
        assertEquals(option.id, repository.currentData().lastPickedId)
    }

    @Test
    fun `pick records result and history without duplicates`() = runTest {        val option = repository.addOption("爬山") ?: error("option should be created")

        repeat(3) { assertEquals(option.id, repository.pick()?.id) }

        val stored = repository.currentData()
        assertEquals(option.id, stored.lastPickedId)
        assertEquals(listOf(option.id), stored.recentPickIds)
    }

    @Test
    fun `removing option also clears its history entries`() = runTest {
        val option = repository.addOption("KTV") ?: error("option should be created")
        repository.pick()

        val afterRemove = repository.removeOption(option.id)

        assertTrue(afterRemove.options.isEmpty())
        assertNull(afterRemove.lastPickedId)
        assertTrue(afterRemove.recentPickIds.isEmpty())
    }

    @Test
    fun `reorder keeps unordered entries at the tail`() = runTest {
        val first = repository.addOption("A") ?: error("A should be created")
        val second = repository.addOption("B") ?: error("B should be created")
        val third = repository.addOption("C") ?: error("C should be created")

        val reordered = repository.reorder(listOf(third.id, first.id))

        assertEquals(listOf(third.id, first.id, second.id), reordered.options.map { it.id })
    }

    @Test
    fun `heavier option is drawn more often`() = runTest {
        val light = repository.addOption("轻") ?: error("option should be created")
        val heavy = repository.addOption("重") ?: error("option should be created")
        repository.setWeight(heavy.id, 9)

        var heavyHits = 0
        repeat(400) { if (repository.pick()?.id == heavy.id) heavyHits++ }

        assertTrue(heavyHits > 280)
        assertTrue(heavyHits < 400)
        assertTrue(light.id != heavy.id)
    }

    @Test
    fun `reset restores empty default set`() = runTest {
        repository.addOption("临时项")

        assertTrue(repository.reset())
        assertEquals(0, repository.currentData().options.size)
        assertTrue(repository.dataFlow.first().options.isEmpty())
    }
}
