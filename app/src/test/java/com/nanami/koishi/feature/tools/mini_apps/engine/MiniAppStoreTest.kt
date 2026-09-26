package com.nanami.koishi.feature.tools.mini_apps.engine

import com.nanami.koishi.core.data.storage.FakeToolStorageDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MiniAppStoreTest {

    private val dao = FakeToolStorageDao()
    private val store = MiniAppStore(dao)

    @Test
    fun `new entries are stored with normalized urls and host fallback titles`() = runTest {
        val result = store.add("  Example.com  ", "", now = 1L)

        val entry = (result as MiniAppAddResult.Added).entry
        assertEquals("https://example.com", entry.url)
        assertEquals("example.com", entry.title)
        assertFalse(entry.hasCustomTitle)
    }

    @Test
    fun `custom titles are kept as given`() = runTest {
        val result = store.add("https://example.com", "  示例站点  ", now = 1L)

        val entry = (result as MiniAppAddResult.Added).entry
        assertEquals("示例站点", entry.title)
        assertTrue(entry.hasCustomTitle)
    }

    @Test
    fun `entries are listed newest first`() = runTest {
        store.add("https://a.example.com", "A", now = 1L)
        store.add("https://b.example.com", "B", now = 2L)

        assertEquals(listOf("B", "A"), store.currentData().entries.map { it.title })
    }

    @Test
    fun `invalid and duplicated urls are refused`() = runTest {
        assertTrue(store.add("not a url", "", now = 1L) is MiniAppAddResult.InvalidUrl)

        store.add("https://example.com", "A", now = 1L)
        assertTrue(store.add("https://EXAMPLE.com", "", now = 2L) is MiniAppAddResult.Duplicate)
        assertEquals(1, store.currentData().entries.size)
    }

    @Test
    fun `entries can be removed by id`() = runTest {
        val entry = (store.add("https://example.com", "A", now = 1L) as MiniAppAddResult.Added).entry
        store.add("https://another.example.com", "B", now = 2L)

        store.remove(entry.id)

        assertEquals(listOf("B"), store.currentData().entries.map { it.title })
    }

    @Test
    fun `entries can be reordered while invalid moves are ignored`() = runTest {
        store.add("https://a.example.com", "A", now = 1L)
        store.add("https://b.example.com", "B", now = 2L)
        store.add("https://c.example.com", "C", now = 3L)

        store.move(fromIndex = 0, toIndex = 2)
        assertEquals(listOf("B", "A", "C"), store.currentData().entries.map { it.title })

        store.move(fromIndex = 1, toIndex = 1)
        store.move(fromIndex = 6, toIndex = 0)
        store.move(fromIndex = 0, toIndex = -1)
        assertEquals(listOf("B", "A", "C"), store.currentData().entries.map { it.title })
    }

    @Test
    fun `desktop mode is remembered per entry`() = runTest {
        val first = (store.add("https://a.example.com", "A", now = 1L) as MiniAppAddResult.Added).entry
        val second = (store.add("https://b.example.com", "B", now = 2L) as MiniAppAddResult.Added).entry

        store.setDesktopMode(first.id, true)

        val modes = store.currentData().entries.associate { it.id to it.desktopMode }
        assertTrue(modes[first.id] == true)
        assertFalse(modes[second.id] == true)
        assertFalse(first.desktopMode)
    }

    @Test
    fun `page titles fill placeholders but never overwrite the input`() = runTest {
        val placeholder = (store.add("https://example.com", "", now = 1L) as MiniAppAddResult.Added).entry
        val custom = (store.add("https://another.example.com", "自定义", now = 2L) as MiniAppAddResult.Added).entry

        store.applyPageTitle(placeholder.id, "真实标题", "https://example.com")
        store.applyPageTitle(placeholder.id, "example.com", "https://example.com")
        store.applyPageTitle(custom.id, "真实标题", "https://another.example.com")

        val titles = store.currentData().entries.associate { it.id to it.title }
        assertEquals("真实标题", titles[placeholder.id])
        assertEquals("自定义", titles[custom.id])
    }
}
