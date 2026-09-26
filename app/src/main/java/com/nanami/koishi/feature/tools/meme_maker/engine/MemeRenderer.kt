package com.nanami.koishi.feature.tools.meme_maker.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * 表情包合成引擎。
 *
 * 所有贴纸的位置、尺寸、字号都以画布短边为基准做归一化换算，
 * 因此低分辨率预览与全分辨率导出得到的结果完全一致。
 */
object MemeRenderer {

    const val DEFAULT_CANVAS_SIZE = 1080

    private const val TEXT_STROKE_RATIO = 0.12f
    private const val TEXT_LINE_SPACING = 1.15f

    fun render(
        background: Bitmap?,
        stickers: List<PlacedSticker>,
        width: Int,
        height: Int,
        stickerBitmaps: Map<String, Bitmap>
    ): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val shortSide = min(width, height).toFloat()

        if (background != null && !background.isRecycled) {
            drawFit(canvas, background, width, height)
        }

        stickers.forEach { sticker ->
            when (sticker.kind) {
                StickerKind.IMAGE -> drawImageSticker(canvas, sticker, stickerBitmaps, width, height, shortSide)
                StickerKind.TEXT -> drawTextSticker(canvas, sticker, width, height, shortSide)
            }
        }

        return output
    }

    /**
     * 文字贴纸在给定画布尺寸下的实际显示尺寸，供命中测试与手势换算使用
     */
    fun measureText(spec: TextStickerSpec, scale: Float, shortSide: Float): Pair<Float, Float> {
        if (spec.text.isBlank()) return 0f to 0f
        val paint = buildTextPaint(spec, max(8f, scale * shortSide))
        val lines = spec.text.split('\n')
        val width = lines.maxOf { paint.measureText(it) }
        val metrics = paint.fontMetrics
        val lineHeight = (metrics.descent - metrics.ascent) * TEXT_LINE_SPACING
        return width to lineHeight * lines.size
    }

    fun imageStickerSize(bitmap: Bitmap, scale: Float, shortSide: Float): Pair<Float, Float> {
        val width = max(1f, scale * shortSide)
        val height = if (bitmap.width <= 0) width else width * bitmap.height / bitmap.width
        return width to height
    }

    /**
     * 等比缩放并居中绘制，保证底图内容完整可见、四周留白而不是被裁掉
     */
    private fun drawFit(canvas: Canvas, source: Bitmap, width: Int, height: Int) {
        if (source.width <= 0 || source.height <= 0) return
        val scale = min(width.toFloat() / source.width, height.toFloat() / source.height)
        val scaledWidth = source.width * scale
        val scaledHeight = source.height * scale
        val left = (width - scaledWidth) / 2f
        val top = (height - scaledHeight) / 2f
        val destination = RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(source, null, destination, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun drawImageSticker(
        canvas: Canvas,
        sticker: PlacedSticker,
        stickerBitmaps: Map<String, Bitmap>,
        width: Int,
        height: Int,
        shortSide: Float
    ) {
        val source = sticker.source ?: return
        val bitmap = stickerBitmaps[source.key] ?: return
        if (bitmap.isRecycled || bitmap.width <= 0) return

        val (targetWidth, targetHeight) = imageStickerSize(bitmap, sticker.transform.scale, shortSide)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (sticker.transform.alpha.coerceIn(0f, 1f) * 255).toInt()
        }

        canvas.save()
        canvas.translate(sticker.transform.centerX * width, sticker.transform.centerY * height)
        canvas.rotate(sticker.transform.rotation)
        if (sticker.transform.flipHorizontal) canvas.scale(-1f, 1f)
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(-targetWidth / 2f, -targetHeight / 2f, targetWidth / 2f, targetHeight / 2f),
            paint
        )
        canvas.restore()
    }

    private fun drawTextSticker(
        canvas: Canvas,
        sticker: PlacedSticker,
        width: Int,
        height: Int,
        shortSide: Float
    ) {
        val spec = sticker.text ?: return
        if (spec.text.isBlank()) return

        val textSize = max(8f, sticker.transform.scale * shortSide)
        val safeAlpha = (sticker.transform.alpha.coerceIn(0f, 1f) * 255).toInt()
        val fillPaint = buildTextPaint(spec, textSize).apply { alpha = safeAlpha }
        val strokePaint = if (spec.strokeEnabled) Paint(fillPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = textSize * TEXT_STROKE_RATIO
            color = spec.strokeColor
            alpha = safeAlpha
        } else {
            null
        }

        val lines = spec.text.split('\n')
        val metrics = fillPaint.fontMetrics
        val lineHeight = (metrics.descent - metrics.ascent) * TEXT_LINE_SPACING
        val firstBaseline = -(lineHeight * lines.size) / 2f - metrics.ascent

        canvas.save()
        canvas.translate(sticker.transform.centerX * width, sticker.transform.centerY * height)
        canvas.rotate(sticker.transform.rotation)
        if (sticker.transform.flipHorizontal) canvas.scale(-1f, 1f)
        lines.forEachIndexed { index, line ->
            val baseline = firstBaseline + index * lineHeight
            strokePaint?.let { canvas.drawText(line, 0f, baseline, it) }
            canvas.drawText(line, 0f, baseline, fillPaint)
        }
        canvas.restore()
    }

    private fun buildTextPaint(spec: TextStickerSpec, textSize: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = spec.color
            this.textSize = textSize
            typeface = spec.font.typeface
            textAlign = Paint.Align.CENTER
        }
}
