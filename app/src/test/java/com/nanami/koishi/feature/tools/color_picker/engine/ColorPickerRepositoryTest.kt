package com.nanami.koishi.feature.tools.color_picker.engine

import com.nanami.koishi.core.data.storage.FakeToolStorageDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorPickerRepositoryTest {

    private val dao = FakeToolStorageDao()
    private val repository = ColorPickerRepository(dao)

    @Test
    fun `default state has empty favorites and default values`() = runTest {
        val data = repository.currentData()
        assertTrue(data.favorites.isEmpty())
        assertEquals(2.0f, data.magnification)
    }

    @Test
    fun `add favorite prepends item and deduplicates by hex`() = runTest {
        val color1 = FavoriteColor(
            id = "1",
            hex = "#F5B5C0",
            colorArgb = 0xFFF5B5C0,
            red = 245,
            green = 181,
            blue = 192
        )
        val color2 = FavoriteColor(
            id = "2",
            hex = "#FFFFFF",
            colorArgb = 0xFFFFFFFF,
            red = 255,
            green = 255,
            blue = 255
        )

        repository.addFavorite(color1)
        var data = repository.addFavorite(color2)
        assertEquals(2, data.favorites.size)
        assertEquals("#FFFFFF", data.favorites[0].hex)
        assertEquals("#F5B5C0", data.favorites[1].hex)

        val color1Updated = FavoriteColor(
            id = "3",
            hex = "#f5b5c0",
            colorArgb = 0xFFF5B5C0,
            red = 245,
            green = 181,
            blue = 192
        )
        data = repository.addFavorite(color1Updated)
        assertEquals(2, data.favorites.size)
        assertEquals("#f5b5c0", data.favorites[0].hex)
    }

    @Test
    fun `remove favorite removes matching item by id`() = runTest {
        val color1 = FavoriteColor(id = "1", hex = "#111111", colorArgb = 0xFF111111, red = 17, green = 17, blue = 17)
        val color2 = FavoriteColor(id = "2", hex = "#222222", colorArgb = 0xFF222222, red = 34, green = 34, blue = 34)

        repository.addFavorite(color1)
        repository.addFavorite(color2)

        val updated = repository.removeFavorite("1")
        assertEquals(1, updated.favorites.size)
        assertEquals("2", updated.favorites.first().id)
    }

    @Test
    fun `clear favorites empties the list`() = runTest {
        val color = FavoriteColor(id = "1", hex = "#111111", colorArgb = 0xFF111111, red = 17, green = 17, blue = 17)
        repository.addFavorite(color)
        assertFalse(repository.currentData().favorites.isEmpty())

        val cleared = repository.clearFavorites()
        assertTrue(cleared.favorites.isEmpty())
        assertTrue(repository.dataFlow.first().favorites.isEmpty())
    }

    @Test
    fun `update magnification persists setting`() = runTest {
        val updated = repository.updateMagnification(8.0f)
        assertEquals(8.0f, updated.magnification)
        assertEquals(8.0f, repository.currentData().magnification)
    }
}
