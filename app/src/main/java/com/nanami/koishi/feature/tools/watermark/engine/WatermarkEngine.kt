package com.nanami.koishi.feature.tools.watermark.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.nanami.koishi.R
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

enum class WatermarkType {
    TEXT,
    IMAGE
}

enum class WatermarkFont(val labelRes: Int) {
    DEFAULT(R.string.watermark_font_default),
    BOLD(R.string.watermark_font_bold),
    SERIF(R.string.watermark_font_serif),
    SANS_SERIF(R.string.watermark_font_sans_serif),
    MONOSPACE(R.string.watermark_font_monospace);

    val typeface: Typeface
        get() = when (this) {
            DEFAULT -> Typeface.DEFAULT
            BOLD -> Typeface.DEFAULT_BOLD
            SERIF -> Typeface.SERIF
            SANS_SERIF -> Typeface.SANS_SERIF
            MONOSPACE -> Typeface.MONOSPACE
        }
}

data class WatermarkConfig(
    val type: WatermarkType = WatermarkType.TEXT,
    val text: String = "Koishi Watermark",
    val textColor: Int = android.graphics.Color.WHITE,
    val textSize: Float = 36f,
    val font: WatermarkFont = WatermarkFont.DEFAULT,
    val watermarkBitmap: Bitmap? = null,
    val imageScale: Float = 1.0f,
    val alpha: Float = 0.35f,
    val rotation: Float = -30f,
    val horizontalSpacing: Float = 120f,
    val verticalSpacing: Float = 100f
)

/**
 * 水印核心处理引擎
 * 提供全屏对角线包围平铺 (Tiling) 算法，支持文字水印与图片水印
 */
object WatermarkEngine {

    private const val REFERENCE_WIDTH = 1080f

    /**
     * 将水印绘制到背景图像上
     *
     * @param background 原图 Bitmap
     * @param config 水印参数配置
     * @return 叠加了水印的全新 Bitmap
     */
    fun applyWatermark(background: Bitmap, config: WatermarkConfig): Bitmap {
        val width = background.width
        val height = background.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 绘制底层背景图
        canvas.drawBitmap(background, 0f, 0f, null)

        // 绘制平铺水印
        drawWatermarkTiles(canvas, width.toFloat(), height.toFloat(), config)

        return output
    }

    /**
     * 生成单独的水印图层（透明背景）
     */
    fun createWatermarkLayer(width: Int, height: Int, config: WatermarkConfig): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        drawWatermarkTiles(canvas, width.toFloat(), height.toFloat(), config)
        return output
    }

    /**
     * 在指定 Canvas 上全屏平铺绘制水印
     */
    fun drawWatermarkTiles(
        canvas: Canvas,
        canvasWidth: Float,
        canvasHeight: Float,
        config: WatermarkConfig
    ) {
        if (canvasWidth <= 0 || canvasHeight <= 0) return

        // 基于基准宽度等比缩放字号与间距，保障不同分辨率图像上的视觉表现一致 (WYSIWYG)
        val scaleFactor = max(0.2f, canvasWidth / REFERENCE_WIDTH)
        val effectiveHSpacing = max(10f, config.horizontalSpacing * scaleFactor)
        val effectiveVSpacing = max(10f, config.verticalSpacing * scaleFactor)
        val safeAlpha = (config.alpha.coerceIn(0f, 1f) * 255).roundToInt()

        val itemWidth: Float
        val itemHeight: Float
        val textPaint: Paint?
        val imagePaint: Paint?
        val textCenterOffsetY: Float

        when (config.type) {
            WatermarkType.TEXT -> {
                if (config.text.isEmpty()) return
                val effectiveTextSize = max(8f, config.textSize * scaleFactor)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = config.textColor
                    alpha = safeAlpha
                    textSize = effectiveTextSize
                    typeface = config.font.typeface
                    textAlign = Paint.Align.CENTER
                }
                textPaint = paint
                imagePaint = null

                itemWidth = max(10f, paint.measureText(config.text))
                val fontMetrics = paint.fontMetrics
                itemHeight = max(10f, fontMetrics.descent - fontMetrics.ascent)
                textCenterOffsetY = (fontMetrics.descent + fontMetrics.ascent) / 2f
            }
            WatermarkType.IMAGE -> {
                val wm = config.watermarkBitmap ?: return
                if (wm.isRecycled || wm.width <= 0 || wm.height <= 0) return

                val effectiveImageScale = max(0.05f, config.imageScale * scaleFactor)
                itemWidth = max(8f, wm.width * effectiveImageScale)
                itemHeight = max(8f, wm.height * effectiveImageScale)
                textPaint = null
                textCenterOffsetY = 0f
                imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = safeAlpha
                }
            }
        }

        val stepX = itemWidth + effectiveHSpacing
        val stepY = itemHeight + effectiveVSpacing
        if (stepX <= 0 || stepY <= 0) return

        // 计算全图旋转对角线包围范围，保证在任意旋转角下四角无死角平铺
        val diagonal = hypot(canvasWidth.toDouble(), canvasHeight.toDouble()).toFloat()
        val halfDiag = diagonal / 2f

        canvas.save()
        // 将坐标原点平移到画布中心后进行整体旋转
        canvas.translate(canvasWidth / 2f, canvasHeight / 2f)
        canvas.rotate(config.rotation)

        var y = -halfDiag - stepY
        var rowIndex = 0
        while (y <= halfDiag + stepY) {
            // 奇偶行交错平移 (Staggered Tiling) 增强美观度与防伪视觉效果
            val rowOffset = if (rowIndex % 2 == 1) stepX / 2f else 0f
            var x = -halfDiag - stepX + rowOffset
            while (x <= halfDiag + stepX) {
                when (config.type) {
                    WatermarkType.TEXT -> {
                        textPaint?.let { paint ->
                            canvas.drawText(config.text, x, y - textCenterOffsetY, paint)
                        }
                    }
                    WatermarkType.IMAGE -> {
                        val wm = config.watermarkBitmap
                        val paint = imagePaint
                        if (wm != null && paint != null) {
                            val rect = RectF(
                                x - itemWidth / 2f,
                                y - itemHeight / 2f,
                                x + itemWidth / 2f,
                                y + itemHeight / 2f
                            )
                            canvas.drawBitmap(wm, null, rect, paint)
                        }
                    }
                }
                x += stepX
            }
            y += stepY
            rowIndex++
        }

        canvas.restore()
    }
}
