package com.nanami.koishi.feature.tools.image_stitching.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.graphics.toArgb
import com.nanami.koishi.feature.tools.image_stitching.StitchingCommonSettings
import com.nanami.koishi.feature.tools.image_stitching.SubtitleSettings
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageStitchingEngine {

    fun stitchVertical(bitmaps: List<Bitmap>, settings: StitchingCommonSettings): Bitmap {
        require(bitmaps.isNotEmpty()) { "Bitmaps list cannot be empty" }
        if (bitmaps.size == 1) return scaleBitmap(bitmaps[0], settings.scaleRatio)

        val targetWidth = if (settings.autoScaleToMin) {
            bitmaps.minOf { it.width }
        } else {
            bitmaps.maxOf { it.width }
        }

        val scaledHeights = bitmaps.map { bmp ->
            val scale = targetWidth.toFloat() / bmp.width
            max(1, (bmp.height * scale).roundToInt())
        }

        val totalHeightRaw = scaledHeights.sum() + (bitmaps.size - 1) * settings.gapPx
        val finalWidth = max(1, (targetWidth * settings.scaleRatio).roundToInt())
        val finalHeight = max(1, (totalHeightRaw * settings.scaleRatio).roundToInt())

        val resultBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        val bgPaint = Paint().apply {
            color = settings.backgroundColor.color.toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, finalWidth.toFloat(), finalHeight.toFloat(), bgPaint)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        var currentY = 0f
        val gapScaled = settings.gapPx * settings.scaleRatio

        bitmaps.forEachIndexed { index, bmp ->
            val hScaled = scaledHeights[index] * settings.scaleRatio
            val dstRect = Rect(0, currentY.roundToInt(), finalWidth, (currentY + hScaled).roundToInt())
            canvas.drawBitmap(bmp, null, dstRect, paint)
            currentY += hScaled + gapScaled
        }

        return resultBitmap
    }

    fun stitchHorizontal(bitmaps: List<Bitmap>, settings: StitchingCommonSettings): Bitmap {
        require(bitmaps.isNotEmpty()) { "Bitmaps list cannot be empty" }
        if (bitmaps.size == 1) return scaleBitmap(bitmaps[0], settings.scaleRatio)

        val targetHeight = if (settings.autoScaleToMin) {
            bitmaps.minOf { it.height }
        } else {
            bitmaps.maxOf { it.height }
        }

        val scaledWidths = bitmaps.map { bmp ->
            val scale = targetHeight.toFloat() / bmp.height
            max(1, (bmp.width * scale).roundToInt())
        }

        val totalWidthRaw = scaledWidths.sum() + (bitmaps.size - 1) * settings.gapPx
        val finalWidth = max(1, (totalWidthRaw * settings.scaleRatio).roundToInt())
        val finalHeight = max(1, (targetHeight * settings.scaleRatio).roundToInt())

        val resultBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        val bgPaint = Paint().apply {
            color = settings.backgroundColor.color.toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, finalWidth.toFloat(), finalHeight.toFloat(), bgPaint)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        var currentX = 0f
        val gapScaled = settings.gapPx * settings.scaleRatio

        bitmaps.forEachIndexed { index, bmp ->
            val wScaled = scaledWidths[index] * settings.scaleRatio
            val dstRect = Rect(currentX.roundToInt(), 0, (currentX + wScaled).roundToInt(), finalHeight)
            canvas.drawBitmap(bmp, null, dstRect, paint)
            currentX += wScaled + gapScaled
        }

        return resultBitmap
    }

    fun stitchSubtitles(
        bitmaps: List<Bitmap>,
        settings: SubtitleSettings,
        maxPreviewDimension: Int = 0
    ): Bitmap {
        require(bitmaps.isNotEmpty()) { "Bitmaps list cannot be empty" }
        if (bitmaps.size == 1) return bitmaps[0]

        val processedBitmaps = if (settings.removeBlackBorders) {
            bitmaps.map { detectAndCropBlackBorders(it) }
        } else {
            bitmaps
        }

        val firstImage = processedBitmaps[0]
        val targetWidth = firstImage.width
        val firstHeight = firstImage.height

        val cropStartPct = min(settings.cropRange.start, settings.cropRange.endInclusive).coerceIn(0f, 1f)
        val cropEndPct = max(settings.cropRange.start, settings.cropRange.endInclusive).coerceIn(0f, 1f)

        data class SubtitleSliceInfo(val bitmap: Bitmap, val srcRect: Rect, val destHeight: Int)

        val slices = processedBitmaps.drop(1).map { bmp ->
            val srcTop = (bmp.height * cropStartPct).roundToInt().coerceIn(0, bmp.height - 1)
            val srcBottom = (bmp.height * cropEndPct).roundToInt().coerceIn(srcTop + 1, bmp.height)
            val srcH = srcBottom - srcTop
            val srcRect = Rect(0, srcTop, bmp.width, srcBottom)
            val scale = targetWidth.toFloat() / bmp.width
            val destH = max(1, (srcH * scale).roundToInt())
            SubtitleSliceInfo(bmp, srcRect, destH)
        }

        val totalHeight = firstHeight + slices.sumOf { it.destHeight }

        val scaleFactor = if (maxPreviewDimension > 0) {
            val maxDim = max(targetWidth, totalHeight)
            if (maxDim > maxPreviewDimension) maxPreviewDimension.toFloat() / maxDim else 1.0f
        } else {
            1.0f
        }

        val finalWidth = max(1, (targetWidth * scaleFactor).roundToInt())
        val finalHeight = max(1, (totalHeight * scaleFactor).roundToInt())

        val resultBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val scaledFirstHeight = (firstHeight * scaleFactor).roundToInt()
        canvas.drawBitmap(firstImage, null, Rect(0, 0, finalWidth, scaledFirstHeight), paint)

        var currentY = scaledFirstHeight
        slices.forEach { slice ->
            val sliceDestH = (slice.destHeight * scaleFactor).roundToInt()
            val dstRect = Rect(0, currentY, finalWidth, currentY + sliceDestH)
            canvas.drawBitmap(slice.bitmap, slice.srcRect, dstRect, paint)
            currentY += sliceDestH
        }

        return resultBitmap
    }

    fun detectAndCropBlackBorders(source: Bitmap, threshold: Int = 24): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= 10 || height <= 10) return source

        fun isBlackPixel(pixel: Int): Boolean {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            return r <= threshold && g <= threshold && b <= threshold
        }

        val sampleStepX = max(1, width / 40)
        val sampleStepY = max(1, height / 40)

        // Top border
        var top = 0
        for (y in 0 until height / 3) {
            var isAllBlack = true
            for (x in 0 until width step sampleStepX) {
                if (!isBlackPixel(source.getPixel(x, y))) {
                    isAllBlack = false
                    break
                }
            }
            if (isAllBlack) top = y + 1 else break
        }

        // Bottom border
        var bottom = height
        for (y in height - 1 downTo (height * 2 / 3)) {
            var isAllBlack = true
            for (x in 0 until width step sampleStepX) {
                if (!isBlackPixel(source.getPixel(x, y))) {
                    isAllBlack = false
                    break
                }
            }
            if (isAllBlack) bottom = y else break
        }

        // Left border
        var left = 0
        for (x in 0 until width / 3) {
            var isAllBlack = true
            for (y in 0 until height step sampleStepY) {
                if (!isBlackPixel(source.getPixel(x, y))) {
                    isAllBlack = false
                    break
                }
            }
            if (isAllBlack) left = x + 1 else break
        }

        // Right border
        var right = width
        for (x in width - 1 downTo (width * 2 / 3)) {
            var isAllBlack = true
            for (y in 0 until height step sampleStepY) {
                if (!isBlackPixel(source.getPixel(x, y))) {
                    isAllBlack = false
                    break
                }
            }
            if (isAllBlack) right = x else break
        }

        val croppedW = right - left
        val croppedH = bottom - top

        if (croppedW >= width * 0.4f && croppedH >= height * 0.4f && (left > 0 || top > 0 || right < width || bottom < height)) {
            return Bitmap.createBitmap(source, left, top, croppedW, croppedH)
        }

        return source
    }

    private fun scaleBitmap(source: Bitmap, ratio: Float): Bitmap {
        if (ratio >= 0.999f) return source
        val w = max(1, (source.width * ratio).roundToInt())
        val h = max(1, (source.height * ratio).roundToInt())
        return Bitmap.createScaledBitmap(source, w, h, true)
    }
}
