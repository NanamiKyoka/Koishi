package com.nanami.koishi.feature.tools.grid_split.engine

import android.graphics.Bitmap
import kotlin.math.min

/**
 * 图像多格切片坐标定义
 *
 * @property col 列索引 (从 0 开始)
 * @property row 行索引 (从 0 开始)
 * @property x 裁剪起点 X 坐标
 * @property y 裁剪起点 Y 坐标
 * @property width 子块宽度
 * @property height 子块高度
 */
data class SliceRect(
    val col: Int,
    val row: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

/**
 * 多格切图计算与提取引擎
 */
object GridSplitEngine {

    /**
     * 计算“切方格”模式下的各切片子块坐标与尺寸
     *
     * 规范：
     * 1. 原图若非 1:1，以中心为基准（Center Crop）截取最大内接正方形 S = min(W, H)。
     * 2. 子块标准宽高为 S / N。
     * 3. 双重循环 X = col * (S / N)，Y = row * (S / N)。
     * 4. 最后一行与最后一列吸收除不尽的像素余数，避免边缘缝隙与黑边。
     */
    fun calculateSquareGridSlices(imageWidth: Int, imageHeight: Int, n: Int): List<SliceRect> {
        require(imageWidth > 0 && imageHeight > 0) { "Image dimensions must be positive" }
        require(n in 2..8) { "Grid order N must be between 2 and 8" }

        val s = min(imageWidth, imageHeight)
        val offsetX = (imageWidth - s) / 2
        val offsetY = (imageHeight - s) / 2
        val step = s / n

        val slices = ArrayList<SliceRect>(n * n)
        for (row in 0 until n) {
            val startY = offsetY + row * step
            val h = if (row == n - 1) (offsetY + s) - startY else step

            for (col in 0 until n) {
                val startX = offsetX + col * step
                val w = if (col == n - 1) (offsetX + s) - startX else step

                slices.add(
                    SliceRect(
                        col = col,
                        row = row,
                        x = startX,
                        y = startY,
                        width = w,
                        height = h
                    )
                )
            }
        }
        return slices
    }

    /**
     * 计算“自定义切”模式下的各切片子块坐标与尺寸
     *
     * 规范：
     * 1. 完整保留原图宽高，不做任何前置裁切。
     * 2. 子块标准宽为 W / Cols，标准高为 H / Rows。
     * 3. 双重循环依次切割出 Rows * Cols 张小图。
     * 4. 边界处理像素余数保证贴合原图边界。
     */
    fun calculateCustomGridSlices(imageWidth: Int, imageHeight: Int, rows: Int, cols: Int): List<SliceRect> {
        require(imageWidth > 0 && imageHeight > 0) { "Image dimensions must be positive" }
        require(rows in 2..8) { "Rows must be between 2 and 8" }
        require(cols in 2..8) { "Cols must be between 2 and 8" }

        val stepW = imageWidth / cols
        val stepH = imageHeight / rows

        val slices = ArrayList<SliceRect>(rows * cols)
        for (row in 0 until rows) {
            val startY = row * stepH
            val h = if (row == rows - 1) imageHeight - startY else stepH

            for (col in 0 until cols) {
                val startX = col * stepW
                val w = if (col == cols - 1) imageWidth - startX else stepW

                slices.add(
                    SliceRect(
                        col = col,
                        row = row,
                        x = startX,
                        y = startY,
                        width = w,
                        height = h
                    )
                )
            }
        }
        return slices
    }

    /**
     * 根据切片坐标截取子 Bitmap 列表
     */
    fun splitBitmap(source: Bitmap, slices: List<SliceRect>): List<Bitmap> {
        return slices.map { slice ->
            Bitmap.createBitmap(source, slice.x, slice.y, slice.width, slice.height)
        }
    }
}
