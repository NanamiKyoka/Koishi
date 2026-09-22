package com.nanami.koishi.feature.tools.qr_tool.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.ColorInt
import qrcode.internals.QRCodeRegion
import qrcode.internals.QRCodeSquare
import qrcode.internals.QRCodeSquareType
import qrcode.raw.ErrorCorrectionLevel
import qrcode.raw.QRCodeProcessor
import kotlin.math.min

/**
 * 数据点形状枚举
 */
enum class QrDotStyle {
    SQUARE, // 方形
    CIRCLE  // 圆形
}

/**
 * 二维码生成配置参数
 */
data class QrConfig(
    val content: String = "https://github.com",
    val outputSize: Int = 1024,
    @ColorInt val darkColor: Int = 0xFF3D6B57.toInt(),       // 深色 (参考图墨绿)
    @ColorInt val lightColor: Int = 0xFFEAF2EC.toInt(),      // 浅色 (码内浅色背景)
    @ColorInt val backgroundColor: Int = 0xFFFFFFFF.toInt(), // 背景色 (整张图背景)
    val dotStyle: QrDotStyle = QrDotStyle.SQUARE,
    val dotScale: Float = 0.85f,                             // 数据点比例 (0.2 ~ 1.0)
    val marginPx: Int = 48,                                  // 外边距
    val logoBitmap: Bitmap? = null,                          // 中心 Logo
    val bgBitmap: Bitmap? = null,                            // 底层背景图
    val bgAlpha: Float = 0.6f,                               // 背景图透明度 (0.0 ~ 1.0)
    val isPickFromBg: Boolean = false                        // 是否从背景图取色
)

/**
 * 二维码生成工具单例
 */
object QrGeneratorUtil {

    /**
     * 根据配置生成高质量 Bitmap
     */
    fun generate(config: QrConfig): Bitmap {
        val content = config.content.ifEmpty { " " }
        val ecl = if (config.logoBitmap != null) {
            ErrorCorrectionLevel.HIGH
        } else {
            ErrorCorrectionLevel.MEDIUM
        }

        // 使用 qrcode-kotlin 的 QRCodeProcessor 进行编码解析
        val processor = QRCodeProcessor(
            data = content,
            errorCorrectionLevel = ecl
        )
        val rawData = processor.encode()
        val matrixSize = rawData.size

        val targetSize = config.outputSize.coerceAtLeast(256)
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. 绘制整体背景色
        val bgPaint = Paint().apply {
            color = config.backgroundColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, targetSize.toFloat(), targetSize.toFloat(), bgPaint)

        // 2. 如果有底层背景图，按透明度绘制在背景上
        config.bgBitmap?.let { bg ->
            if (!bg.isRecycled) {
                val alphaInt = (config.bgAlpha.coerceIn(0f, 1f) * 255).toInt()
                val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha = alphaInt
                    isFilterBitmap = true
                }
                val srcRect = calculateCenterCropRect(bg.width, bg.height, targetSize, targetSize)
                val dstRect = Rect(0, 0, targetSize, targetSize)
                canvas.drawBitmap(bg, srcRect, dstRect, imagePaint)
            }
        }

        // 3. 计算二维码渲染区域与单元格尺寸
        val qrAreaSize = targetSize - config.marginPx * 2
        val cellSize = qrAreaSize.toFloat() / matrixSize.toFloat()
        val startOffset = config.marginPx.toFloat()

        // 4. 绘制码区浅色背景
        val hasBgImage = config.bgBitmap != null && !config.bgBitmap.isRecycled
        if (!hasBgImage) {
            // 没有背景图时，正常绘制码区浅色底
            if (Color.alpha(config.lightColor) > 0) {
                val lightPaint = Paint().apply {
                    color = config.lightColor
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRect(
                    startOffset,
                    startOffset,
                    startOffset + qrAreaSize,
                    startOffset + qrAreaSize,
                    lightPaint
                )
            }
        } else {
            // 有背景图时：为三个定位角绘制高对比度浅色衬底，保证扫码鲁棒性，同时让普通数据点区完全透出背景图
            val probeBgPaint = Paint().apply {
                // 85% 半透明浅色衬底，既透出背景图又保持定位角对比度
                color = Color.argb(
                    215,
                    Color.red(config.lightColor),
                    Color.green(config.lightColor),
                    Color.blue(config.lightColor)
                )
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val probeSize = 7 * cellSize
            val cornerRadius = cellSize * 0.8f

            // 左上角衬底
            canvas.drawRoundRect(
                RectF(startOffset, startOffset, startOffset + probeSize, startOffset + probeSize),
                cornerRadius, cornerRadius, probeBgPaint
            )
            // 右上角衬底
            val trLeft = startOffset + (matrixSize - 7) * cellSize
            canvas.drawRoundRect(
                RectF(trLeft, startOffset, trLeft + probeSize, startOffset + probeSize),
                cornerRadius, cornerRadius, probeBgPaint
            )
            // 左下角衬底
            val blTop = startOffset + (matrixSize - 7) * cellSize
            canvas.drawRoundRect(
                RectF(startOffset, blTop, startOffset + probeSize, blTop + probeSize),
                cornerRadius, cornerRadius, probeBgPaint
            )
        }

        // 5. 数据点与定位点画笔
        val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.darkColor
            style = Paint.Style.FILL
        }

        val scale = config.dotScale.coerceIn(0.2f, 1.0f)
        val cellRadius = (cellSize * scale) / 2f

        // 计算中心 Logo 区域 (若有)，避免在 Logo 覆盖区域绘制杂乱的数据点
        val logoBounds = if (config.logoBitmap != null) {
            val logoDisplaySize = qrAreaSize * 0.22f
            val cx = targetSize / 2f
            val cy = targetSize / 2f
            val half = logoDisplaySize / 2f
            RectF(cx - half, cy - half, cx + half, cy + half)
        } else null

        // 6. 逐单元格绘制
        for (row in 0 until matrixSize) {
            val rowData = rawData[row]
            for (col in 0 until matrixSize) {
                val square: QRCodeSquare = rowData[col]
                val x = startOffset + col * cellSize
                val y = startOffset + row * cellSize

                // 如果位于 Logo 区域中心，跳过绘制
                if (logoBounds != null && logoBounds.intersects(x, y, x + cellSize, y + cellSize)) {
                    continue
                }

                val isProbe = square.squareInfo.type == QRCodeSquareType.POSITION_PROBE

                if (isProbe) {
                    // 定位角保持清晰的方框结构
                    if (square.dark) {
                        canvas.drawRect(x, y, x + cellSize, y + cellSize, darkPaint)
                    }
                } else if (square.dark) {
                    val cx = x + cellSize / 2f
                    val cy = y + cellSize / 2f

                    when (config.dotStyle) {
                        QrDotStyle.CIRCLE -> {
                            canvas.drawCircle(cx, cy, cellRadius, darkPaint)
                        }
                        QrDotStyle.SQUARE -> {
                            val half = cellSize * scale / 2f
                            val rect = RectF(cx - half, cy - half, cx + half, cy + half)
                            val corner = cellSize * scale * 0.15f
                            canvas.drawRoundRect(rect, corner, corner, darkPaint)
                        }
                    }
                }
            }
        }

        // 7. 绘制中心 Logo (带圆角与衬底)
        config.logoBitmap?.let { logo ->
            if (!logo.isRecycled) {
                val logoSize = qrAreaSize * 0.22f
                val padding = logoSize * 0.1f
                val cx = targetSize / 2f
                val cy = targetSize / 2f

                // 衬底卡片 (白色纯底 + 柔和边框)
                val cardRect = RectF(
                    cx - (logoSize / 2f) - padding,
                    cy - (logoSize / 2f) - padding,
                    cx + (logoSize / 2f) + padding,
                    cy + (logoSize / 2f) + padding
                )
                val padPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                val cardCorner = logoSize * 0.2f
                canvas.drawRoundRect(cardRect, cardCorner, cardCorner, padPaint)

                // 衬底浅灰微边框
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x22000000
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawRoundRect(cardRect, cardCorner, cardCorner, borderPaint)

                // 绘制剪裁为圆角的 Logo
                val logoDst = RectF(
                    cx - (logoSize / 2f),
                    cy - (logoSize / 2f),
                    cx + (logoSize / 2f),
                    cy + (logoSize / 2f)
                )
                val roundedLogo = getRoundedBitmap(logo, (logoSize * 0.18f).toInt())
                canvas.drawBitmap(roundedLogo, null, logoDst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            }
        }

        return bitmap
    }

    /**
     * 计算背景图居中裁剪区域
     */
    private fun calculateCenterCropRect(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Rect {
        val srcAspect = srcW.toFloat() / srcH.toFloat()
        val dstAspect = dstW.toFloat() / dstH.toFloat()

        return if (srcAspect > dstAspect) {
            val cropW = (srcH * dstAspect).toInt()
            val left = (srcW - cropW) / 2
            Rect(left, 0, left + cropW, srcH)
        } else {
            val cropH = (srcW / dstAspect).toInt()
            val top = (srcH - cropH) / 2
            Rect(0, top, srcW, top + cropH)
        }
    }

    /**
     * 生成圆角 Bitmap
     */
    private fun getRoundedBitmap(source: Bitmap, cornerRadius: Int): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, source.width, source.height)
        val rectF = RectF(rect)

        canvas.drawRoundRect(rectF, cornerRadius.toFloat(), cornerRadius.toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, rect, rect, paint)
        return output
    }

    /**
     * 从 Bitmap 提取主色调 (用于“从背景图取色”)
     */
    fun extractDominantColor(bitmap: Bitmap): Int {
        if (bitmap.isRecycled) return 0xFF3D6B57.toInt()
        val small = Bitmap.createScaledBitmap(bitmap, 24, 24, false)
        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L
        val count = small.width * small.height

        for (x in 0 until small.width) {
            for (y in 0 until small.height) {
                val pixel = small.getPixel(x, y)
                redSum += Color.red(pixel)
                greenSum += Color.green(pixel)
                blueSum += Color.blue(pixel)
            }
        }
        small.recycle()

        val r = (redSum / count).toInt()
        val g = (greenSum / count).toInt()
        val b = (blueSum / count).toInt()

        // 确保前景色具备足够的对比度，如果太亮则适当调深
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        return if (luminance > 160) {
            Color.rgb((r * 0.6).toInt(), (g * 0.6).toInt(), (b * 0.6).toInt())
        } else {
            Color.rgb(r, g, b)
        }
    }
}
