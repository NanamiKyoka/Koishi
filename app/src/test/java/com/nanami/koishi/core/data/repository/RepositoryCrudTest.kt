package com.nanami.koishi.core.data.repository

import com.nanami.koishi.core.model.ToolCategory
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

        // 期望的工具目录：按展示顺序排列 (id, category, hasDot)
        val expected = listOf(
            Triple("image_stitching", ToolCategory.IMAGE_APPS, true),
            Triple("grid_split", ToolCategory.IMAGE_APPS, true),
            Triple("image_obfuscation", ToolCategory.IMAGE_APPS, false),
            Triple("mirage_tank", ToolCategory.IMAGE_APPS, true),
            Triple("qr_code", ToolCategory.IMAGE_APPS, false),
            Triple("image_search", ToolCategory.IMAGE_APPS, true),
            Triple("watermark", ToolCategory.IMAGE_APPS, true),
            Triple("image_sketch", ToolCategory.IMAGE_APPS, true),
            Triple("video_to_gif", ToolCategory.IMAGE_APPS, true),
            Triple("bili_cover", ToolCategory.IMAGE_APPS, true),
            Triple("meme_maker", ToolCategory.IMAGE_APPS, true),
            Triple("today_in_history", ToolCategory.LIFE, true),
            Triple("decision_maker", ToolCategory.LIFE, true),
            Triple("ruler", ToolCategory.LIFE, true),
            Triple("currency_converter", ToolCategory.CALCULATION, true)
        )

        assertEquals(expected.size, tools.size)
        assertEquals(expected.map { it.first }, tools.map { it.id })

        // 逐个校验分类、默认收藏状态与红点标记，并按 id 校验检索能力
        expected.forEachIndexed { index, (id, category, hasDot) ->
            val tool = tools[index]
            assertEquals("工具 $id 的分类不符", category, tool.category)
            assertFalse("工具 $id 不应默认收藏", tool.isFavorite)
            assertEquals("工具 $id 的红点标记不符", hasDot, tool.hasDot)
            assertEquals("getToolById 未能取回 $id", tool, repo.getToolById(id))
        }

        // 不存在的工具应返回 null
        assertEquals(null, repo.getToolById("not_a_real_tool"))
    }

    @Test
    fun testToolIdsAreUnique() = runBlocking {
        val repo = InMemoryToolRepository()
        val ids = repo.availableTools.first().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
