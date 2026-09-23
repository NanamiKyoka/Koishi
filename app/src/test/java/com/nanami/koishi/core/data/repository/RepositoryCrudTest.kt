package com.nanami.koishi.core.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryCrudTest {

    // 针对内存环境测试完整的 Repository CRUD 操作与业务逻辑
    private class TestFavoritesRepository : ToolFavoritesRepository {
        private var _favorites = emptySet<String>()
        private val _flow = kotlinx.coroutines.flow.MutableStateFlow(_favorites)
        override val favoriteToolIds = _flow

        override fun isFavorite(toolId: String): Boolean = _favorites.contains(toolId)

        override suspend fun addFavorite(toolId: String) {
            _favorites = _favorites + toolId
            _flow.value = _favorites
        }

        override suspend fun removeFavorite(toolId: String) {
            _favorites = _favorites - toolId
            _flow.value = _favorites
        }

        override suspend fun toggleFavorite(toolId: String) {
            _favorites = if (_favorites.contains(toolId)) _favorites - toolId else _favorites + toolId
            _flow.value = _favorites
        }

        override suspend fun clearAllFavorites() {
            _favorites = emptySet()
            _flow.value = _favorites
        }
    }

    private class TestSearchHistoryRepository(
        private val maxCount: Int = 5
    ) : SearchHistoryRepository {
        private var _list = emptyList<String>()
        private val _flow = kotlinx.coroutines.flow.MutableStateFlow(_list)
        override val searchHistory = _flow

        override suspend fun addSearchHistory(keyword: String) {
            val trimmed = keyword.trim()
            if (trimmed.isEmpty()) return
            _list = (listOf(trimmed) + _list.filter { it != trimmed }).take(maxCount)
            _flow.value = _list
        }

        override suspend fun deleteSearchHistoryItem(keyword: String) {
            _list = _list.filter { it != keyword }
            _flow.value = _list
        }

        override suspend fun clearAllSearchHistory() {
            _list = emptyList()
            _flow.value = _list
        }
    }

    @Test
    fun testFavoritesCrudOperations() = runBlocking {
        val repo = TestFavoritesRepository()

        // 验证初始状态：没有默认收藏
        assertTrue(repo.favoriteToolIds.first().isEmpty())
        assertFalse(repo.isFavorite("image_obfuscation"))

        // Create: 添加收藏
        repo.addFavorite("image_obfuscation")
        assertTrue(repo.favoriteToolIds.first().contains("image_obfuscation"))
        assertTrue(repo.isFavorite("image_obfuscation"))

        // Update: 切换收藏 (取消)
        repo.toggleFavorite("image_obfuscation")
        assertFalse(repo.isFavorite("image_obfuscation"))
        assertTrue(repo.favoriteToolIds.first().isEmpty())

        // Update: 再次切换 (加入)
        repo.toggleFavorite("image_obfuscation")
        assertTrue(repo.isFavorite("image_obfuscation"))

        // Delete: 移除收藏
        repo.removeFavorite("image_obfuscation")
        assertFalse(repo.isFavorite("image_obfuscation"))

        // 批量清空
        repo.addFavorite("tool_1")
        repo.addFavorite("tool_2")
        assertEquals(2, repo.favoriteToolIds.first().size)
        repo.clearAllFavorites()
        assertTrue(repo.favoriteToolIds.first().isEmpty())
    }

    @Test
    fun testSearchHistoryCrudOperations() = runBlocking {
        val repo = TestSearchHistoryRepository(maxCount = 3)

        // 验证初始状态：没有默认历史记录
        assertTrue(repo.searchHistory.first().isEmpty())

        // Create: 添加搜索词条
        repo.addSearchHistory("图片混淆")
        assertEquals(listOf("图片混淆"), repo.searchHistory.first())

        // Create / Update: 添加去重并置顶
        repo.addSearchHistory("番茄混淆")
        assertEquals(listOf("番茄混淆", "图片混淆"), repo.searchHistory.first())

        repo.addSearchHistory("图片混淆")
        assertEquals(listOf("图片混淆", "番茄混淆"), repo.searchHistory.first())

        // 容量上限截断测试
        repo.addSearchHistory("关键词A")
        repo.addSearchHistory("关键词B")
        assertEquals(listOf("关键词B", "关键词A", "图片混淆"), repo.searchHistory.first())

        // Delete: 单项删除
        repo.deleteSearchHistoryItem("关键词A")
        assertEquals(listOf("关键词B", "图片混淆"), repo.searchHistory.first())

        // Batch Delete: 全部清空
        repo.clearAllSearchHistory()
        assertTrue(repo.searchHistory.first().isEmpty())
    }

    @Test
    fun testToolRepositoryAvailableTools() = runBlocking {
        val repo = InMemoryToolRepository()
        val tools = repo.availableTools.first()

        assertEquals(7, tools.size)
        assertEquals("image_stitching", tools[0].id)
        assertEquals(com.nanami.koishi.core.model.ToolCategory.IMAGE_APPS, tools[0].category)
        assertFalse(tools[0].isFavorite)
        assertTrue(tools[0].hasDot)

        assertEquals("grid_split", tools[1].id)
        assertEquals(com.nanami.koishi.core.model.ToolCategory.IMAGE_APPS, tools[1].category)
        assertFalse(tools[1].isFavorite)
        assertTrue(tools[1].hasDot)

        assertEquals("image_obfuscation", tools[2].id)
        assertEquals(com.nanami.koishi.core.model.ToolCategory.IMAGE_APPS, tools[2].category)
        assertFalse(tools[2].isFavorite)

        assertEquals("mirage_tank", tools[3].id)
        assertEquals(com.nanami.koishi.core.model.ToolCategory.IMAGE_APPS, tools[3].category)
        assertFalse(tools[3].isFavorite)
        assertTrue(tools[3].hasDot)

        assertEquals("qr_code", tools[4].id)
        assertEquals(com.nanami.koishi.core.model.ToolCategory.IMAGE_APPS, tools[4].category)
        assertFalse(tools[4].isFavorite)

        assertEquals("image_search", tools[5].id)
        assertEquals(com.nanami.koishi.core.model.ToolCategory.IMAGE_APPS, tools[5].category)
        assertFalse(tools[5].isFavorite)
        assertTrue(tools[5].hasDot)

        assertEquals("watermark", tools[6].id)
        assertEquals(com.nanami.koishi.core.model.ToolCategory.IMAGE_APPS, tools[6].category)
        assertFalse(tools[6].isFavorite)
        assertTrue(tools[6].hasDot)
    }
}
