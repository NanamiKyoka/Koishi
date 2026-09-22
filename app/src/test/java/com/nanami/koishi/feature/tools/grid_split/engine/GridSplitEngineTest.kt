package com.nanami.koishi.feature.tools.grid_split.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

class GridSplitEngineTest {

    @Test
    fun squareGrid_landscapeImage_calculatesCenterCropAndExactCoverage() {
        val width = 1920
        val height = 1080
        val n = 3 // 3x3 九宫格

        val slices = GridSplitEngine.calculateSquareGridSlices(width, height, n)
        assertEquals(9, slices.size)

        val s = min(width, height) // 1080
        val expectedOffsetX = (1920 - 1080) / 2 // 420
        val expectedOffsetY = 0
        val step = 1080 / 3 // 360

        // 验证各行各列坐标
        for (r in 0 until n) {
            for (c in 0 until n) {
                val slice = slices[r * n + c]
                assertEquals(c, slice.col)
                assertEquals(r, slice.row)
                assertEquals(expectedOffsetX + c * step, slice.x)
                assertEquals(expectedOffsetY + r * step, slice.y)
                assertEquals(360, slice.width)
                assertEquals(360, slice.height)
            }
        }
    }

    @Test
    fun squareGrid_portraitImage_withRemainder_absorbsRemainderAtEdge() {
        val width = 1000
        val height = 2000
        val n = 3 // 1000 / 3 = 333 余 1

        val slices = GridSplitEngine.calculateSquareGridSlices(width, height, n)
        assertEquals(9, slices.size)

        val s = 1000
        val expectedOffsetX = 0
        val expectedOffsetY = (2000 - 1000) / 2 // 500

        // 前两列宽 333，最后一列宽 334 (333 + 1)
        val topLeft = slices[0] // col 0, row 0
        assertEquals(333, topLeft.width)
        assertEquals(333, topLeft.height)
        assertEquals(0, topLeft.x)
        assertEquals(500, topLeft.y)

        val topRight = slices[2] // col 2, row 0
        assertEquals(334, topRight.width)
        assertEquals(333, topRight.height)
        assertEquals(666, topRight.x)
        assertEquals(500, topRight.y)
        assertEquals(expectedOffsetX + s, topRight.x + topRight.width)

        val bottomRight = slices[8] // col 2, row 2
        assertEquals(334, bottomRight.width)
        assertEquals(334, bottomRight.height)
        assertEquals(expectedOffsetX + s, bottomRight.x + bottomRight.width)
        assertEquals(expectedOffsetY + s, bottomRight.y + bottomRight.height)
    }

    @Test
    fun squareGrid_allOrders2to8_haveZeroGapAndExactArea() {
        val width = 1234
        val height = 987
        val s = min(width, height) // 987
        val expectedOffsetX = (width - s) / 2
        val expectedOffsetY = (height - s) / 2

        for (n in 2..8) {
            val slices = GridSplitEngine.calculateSquareGridSlices(width, height, n)
            assertEquals(n * n, slices.size)

            // 检查每行的宽度和为 s
            for (r in 0 until n) {
                val rowSlices = slices.filter { it.row == r }.sortedBy { it.col }
                assertEquals(n, rowSlices.size)
                val rowWidthSum = rowSlices.sumOf { it.width }
                assertEquals(s, rowWidthSum)

                // 检查切片之间无间隙连续对接
                var currentX = expectedOffsetX
                for (slice in rowSlices) {
                    assertEquals(currentX, slice.x)
                    currentX += slice.width
                }
                assertEquals(expectedOffsetX + s, currentX)
            }

            // 检查每列的高度和为 s
            for (c in 0 until n) {
                val colSlices = slices.filter { it.col == c }.sortedBy { it.row }
                assertEquals(n, colSlices.size)
                val colHeightSum = colSlices.sumOf { it.height }
                assertEquals(s, colHeightSum)

                var currentY = expectedOffsetY
                for (slice in colSlices) {
                    assertEquals(currentY, slice.y)
                    currentY += slice.height
                }
                assertEquals(expectedOffsetY + s, currentY)
            }
        }
    }

    @Test
    fun customGrid_preservesFullImageAndAbsorbsRemainder() {
        val width = 1083 // 奇数分辨率
        val height = 755
        val rows = 4
        val cols = 3

        val slices = GridSplitEngine.calculateCustomGridSlices(width, height, rows, cols)
        assertEquals(rows * cols, slices.size)

        // 检查所有切片是否无缝覆盖 1083 x 755 原图
        for (r in 0 until rows) {
            val rowSlices = slices.filter { it.row == r }.sortedBy { it.col }
            assertEquals(cols, rowSlices.size)
            assertEquals(width, rowSlices.sumOf { it.width })

            var currX = 0
            for (slice in rowSlices) {
                assertEquals(currX, slice.x)
                currX += slice.width
            }
            assertEquals(width, currX)
        }

        for (c in 0 until cols) {
            val colSlices = slices.filter { it.col == c }.sortedBy { it.row }
            assertEquals(rows, colSlices.size)
            assertEquals(height, colSlices.sumOf { it.height })

            var currY = 0
            for (slice in colSlices) {
                assertEquals(currY, slice.y)
                currY += slice.height
            }
            assertEquals(height, currY)
        }
    }

    @Test
    fun customGrid_allCombinations2to8_coverageVerification() {
        val width = 800
        val height = 600

        for (rows in 2..8) {
            for (cols in 2..8) {
                val slices = GridSplitEngine.calculateCustomGridSlices(width, height, rows, cols)
                assertEquals(rows * cols, slices.size)

                val lastSlice = slices.last()
                assertEquals(cols - 1, lastSlice.col)
                assertEquals(rows - 1, lastSlice.row)
                assertEquals(width, lastSlice.x + lastSlice.width)
                assertEquals(height, lastSlice.y + lastSlice.height)
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun squareGrid_orderBelow2_throwsException() {
        GridSplitEngine.calculateSquareGridSlices(100, 100, 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun squareGrid_orderAbove8_throwsException() {
        GridSplitEngine.calculateSquareGridSlices(100, 100, 9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun customGrid_rowsBelow2_throwsException() {
        GridSplitEngine.calculateCustomGridSlices(100, 100, 1, 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun customGrid_colsAbove8_throwsException() {
        GridSplitEngine.calculateCustomGridSlices(100, 100, 3, 9)
    }
}
