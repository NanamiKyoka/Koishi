package com.nanami.koishi.feature.tools.image_obfuscation.engine

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Arrays
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sqrt

object ImageObfuscator {

    suspend fun process(
        source: Bitmap,
        mode: ObfuscationMode,
        action: ObfuscationAction,
        key: String
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val isEncrypt = (action == ObfuscationAction.OBFUSCATE)

        when (mode) {
            ObfuscationMode.TOMATO_GILBERT -> {
                val keyDouble = key.toDoubleOrNull() ?: 1.0
                val resultPixels = processTomato(pixels, width, height, keyDouble, isEncrypt)
                Bitmap.createBitmap(resultPixels, width, height, Bitmap.Config.ARGB_8888)
            }
            ObfuscationMode.ROW_PIXEL -> {
                val seed = key.ifBlank { "0.666" }
                val resultPixels = processRowPixel(pixels, width, height, seed, isEncrypt)
                Bitmap.createBitmap(resultPixels, width, height, Bitmap.Config.ARGB_8888)
            }
            ObfuscationMode.PER_PIXEL -> {
                val seed = key.ifBlank { "0.666" }
                val resultPixels = processPerPixel(pixels, width, height, seed, isEncrypt)
                Bitmap.createBitmap(resultPixels, width, height, Bitmap.Config.ARGB_8888)
            }
            ObfuscationMode.BLOCK -> {
                val seed = key.ifBlank { "0.666" }
                val (resultPixels, newW, newH) = processBlock(pixels, width, height, seed, isEncrypt)
                Bitmap.createBitmap(resultPixels, newW, newH, Bitmap.Config.ARGB_8888)
            }
            ObfuscationMode.PIC_ENCRYPT_ROW -> {
                val keyDouble = (key.toDoubleOrNull() ?: 0.666).coerceIn(0.0001, 0.9999)
                val resultPixels = processPicEncryptRow(pixels, width, height, keyDouble, isEncrypt)
                Bitmap.createBitmap(resultPixels, width, height, Bitmap.Config.ARGB_8888)
            }
            ObfuscationMode.PIC_ENCRYPT_ROW_AND_COLUMN -> {
                val keyDouble = (key.toDoubleOrNull() ?: 0.666).coerceIn(0.0001, 0.9999)
                val resultPixels = processPicEncryptRowColumn(pixels, width, height, keyDouble, isEncrypt)
                Bitmap.createBitmap(resultPixels, width, height, Bitmap.Config.ARGB_8888)
            }
        }
    }

    internal fun processTomato(
        pixels: IntArray,
        width: Int,
        height: Int,
        key: Double,
        isEncrypt: Boolean
    ): IntArray {
        val pixelCount = width * height
        val offset = round((sqrt(5.0) - 1.0) / 2.0 * pixelCount * key).toInt()
        val positions = IntArray(pixelCount)
        var pos = 0

        fun generate2d(x: Int, y: Int, ax: Int, ay: Int, bx: Int, by: Int) {
            val w = abs(ax + ay)
            val h = abs(bx + by)
            val dax = ax.sign
            val day = ay.sign
            val dbx = bx.sign
            val dby = by.sign

            if (h == 1) {
                var curX = x
                var curY = y
                for (i in 0 until w) {
                    positions[pos++] = curX + curY * width
                    curX += dax
                    curY += day
                }
                return
            }

            if (w == 1) {
                var curX = x
                var curY = y
                for (i in 0 until h) {
                    positions[pos++] = curX + curY * width
                    curX += dbx
                    curY += dby
                }
                return
            }

            var ax2 = Math.floorDiv(ax, 2)
            var ay2 = Math.floorDiv(ay, 2)
            var bx2 = Math.floorDiv(bx, 2)
            var by2 = Math.floorDiv(by, 2)
            val w2 = abs(ax2 + ay2)
            val h2 = abs(bx2 + by2)

            if (2 * w > 3 * h) {
                if ((w2 and 1) == 1 && w > 2) {
                    ax2 += dax
                    ay2 += day
                }
                generate2d(x, y, ax2, ay2, bx, by)
                generate2d(x + ax2, y + ay2, ax - ax2, ay - ay2, bx, by)
            } else {
                if ((h2 and 1) == 1 && h > 2) {
                    bx2 += dbx
                    by2 += dby
                }
                generate2d(x, y, bx2, by2, ax2, ay2)
                generate2d(x + bx2, y + by2, ax, ay, bx - bx2, by - by2)
                generate2d(
                    x + (ax - dax) + (bx2 - dbx),
                    y + (ay - day) + (by2 - dby),
                    -bx2,
                    -by2,
                    -(ax - ax2),
                    -(ay - ay2)
                )
            }
        }

        if (width >= height) {
            generate2d(0, 0, width, 0, 0, height)
        } else {
            generate2d(0, 0, 0, height, width, 0)
        }

        val loopPosition = pixelCount - offset
        val newPixels = IntArray(pixelCount)

        if (pixelCount > 10000) {
            val taskCount = Runtime.getRuntime().availableProcessors()
            val tasks = ArrayList<Callable<Unit>>(taskCount)
            val step = ceil(pixelCount.toDouble() / taskCount).toInt()

            for (i in 0 until taskCount) {
                val begin = step * i
                val end = (begin + step).coerceAtMost(pixelCount)

                tasks.add(Callable {
                    if (isEncrypt) {
                        if (begin >= loopPosition) {
                            for (j in begin until end) {
                                newPixels[positions[j - loopPosition]] = pixels[positions[j]]
                            }
                        } else if (end <= loopPosition) {
                            for (j in begin until end) {
                                newPixels[positions[j + offset]] = pixels[positions[j]]
                            }
                        } else {
                            for (j in begin until loopPosition) {
                                newPixels[positions[j + offset]] = pixels[positions[j]]
                            }
                            for (j in loopPosition until end) {
                                newPixels[positions[j - loopPosition]] = pixels[positions[j]]
                            }
                        }
                    } else {
                        if (begin >= loopPosition) {
                            for (j in begin until end) {
                                newPixels[positions[j]] = pixels[positions[j - loopPosition]]
                            }
                        } else if (end <= loopPosition) {
                            for (j in begin until end) {
                                newPixels[positions[j]] = pixels[positions[j + offset]]
                            }
                        } else {
                            for (j in begin until loopPosition) {
                                newPixels[positions[j]] = pixels[positions[j + offset]]
                            }
                            for (j in loopPosition until end) {
                                newPixels[positions[j]] = pixels[positions[j - loopPosition]]
                            }
                        }
                    }
                })
            }

            val executor = Executors.newFixedThreadPool(taskCount)
            try {
                executor.invokeAll(tasks)
            } finally {
                executor.shutdown()
            }
        } else {
            if (isEncrypt) {
                for (i in 0 until loopPosition) {
                    newPixels[positions[i + offset]] = pixels[positions[i]]
                }
                for (i in loopPosition until pixelCount) {
                    newPixels[positions[i - loopPosition]] = pixels[positions[i]]
                }
            } else {
                for (i in 0 until loopPosition) {
                    newPixels[positions[i]] = pixels[positions[i + offset]]
                }
                for (i in loopPosition until pixelCount) {
                    newPixels[positions[i]] = pixels[positions[i - loopPosition]]
                }
            }
        }

        return newPixels
    }

    private fun md5Shuffle(length: Int, key: String): IntArray {
        val arr = IntArray(length) { it }
        val digest = MessageDigest.getInstance("MD5")
        for (i in length - 1 downTo 1) {
            val hashBytes = digest.digest("$key$i".toByteArray(Charsets.UTF_8))
            var hex = BigInteger(1, hashBytes).toString(16)
            if (hex.length < 32) {
                hex = "0".repeat(32 - hex.length) + hex
            }
            val rand = (hex.substring(0, 7).toInt(16)) % (i + 1)
            val tmp = arr[rand]
            arr[rand] = arr[i]
            arr[i] = tmp
        }
        return arr
    }

    internal fun processRowPixel(
        pixels: IntArray,
        width: Int,
        height: Int,
        key: String,
        isEncrypt: Boolean
    ): IntArray {
        val xArray = md5Shuffle(width, key)
        val newPixels = IntArray(width * height)

        val coreCount = Runtime.getRuntime().availableProcessors()
        val taskCount = width.coerceAtMost(coreCount)
        val step = ceil(width.toDouble() / taskCount).toInt()
        val tasks = ArrayList<Callable<Unit>>(taskCount)

        for (k in 0 until taskCount) {
            val begin = k * step
            val end = (begin + step).coerceAtMost(width)
            tasks.add(Callable {
                if (isEncrypt) {
                    for (i in begin until end) {
                        for (j in 0 until height) {
                            val m = xArray[(xArray[j % width] + i) % width]
                            newPixels[i + j * width] = pixels[m + j * width]
                        }
                    }
                } else {
                    for (i in begin until end) {
                        for (j in 0 until height) {
                            val m = xArray[(xArray[j % width] + i) % width]
                            newPixels[m + j * width] = pixels[i + j * width]
                        }
                    }
                }
            })
        }

        val executor = Executors.newFixedThreadPool(taskCount)
        try {
            executor.invokeAll(tasks)
        } finally {
            executor.shutdown()
        }
        return newPixels
    }

    internal fun processPerPixel(
        pixels: IntArray,
        width: Int,
        height: Int,
        key: String,
        isEncrypt: Boolean
    ): IntArray {
        val xArray = md5Shuffle(width, key)
        val yArray = md5Shuffle(height, key)
        val newPixels = IntArray(width * height)

        val coreCount = Runtime.getRuntime().availableProcessors()
        val taskCount = width.coerceAtMost(coreCount)
        val step = ceil(width.toDouble() / taskCount).toInt()
        val tasks = ArrayList<Callable<Unit>>(taskCount)

        for (k in 0 until taskCount) {
            val begin = k * step
            val end = (begin + step).coerceAtMost(width)
            tasks.add(Callable {
                if (isEncrypt) {
                    for (i in begin until end) {
                        for (j in 0 until height) {
                            val m = xArray[(xArray[j % width] + i) % width]
                            val n = yArray[(yArray[m % height] + j) % height]
                            newPixels[i + j * width] = pixels[m + n * width]
                        }
                    }
                } else {
                    for (i in begin until end) {
                        for (j in 0 until height) {
                            val m = xArray[(xArray[j % width] + i) % width]
                            val n = yArray[(yArray[m % height] + j) % height]
                            newPixels[m + n * width] = pixels[i + j * width]
                        }
                    }
                }
            })
        }

        val executor = Executors.newFixedThreadPool(taskCount)
        try {
            executor.invokeAll(tasks)
        } finally {
            executor.shutdown()
        }
        return newPixels
    }

    internal data class BlockResult(val pixels: IntArray, val width: Int, val height: Int)

    internal fun processBlock(
        pixels: IntArray,
        width: Int,
        height: Int,
        key: String,
        isEncrypt: Boolean,
        xBlockCount: Int = 32,
        yBlockCount: Int = 32
    ): BlockResult {
        val xArray = md5Shuffle(xBlockCount, key)
        val yArray = md5Shuffle(yBlockCount, key)

        val newWidth = if (width % xBlockCount > 0) width + xBlockCount - width % xBlockCount else width
        val newHeight = if (height % yBlockCount > 0) height + yBlockCount - height % yBlockCount else height

        val blockWidth = newWidth / xBlockCount
        val blockHeight = newHeight / yBlockCount
        val newPixels = IntArray(newWidth * newHeight)

        val coreCount = Runtime.getRuntime().availableProcessors()
        val taskCount = newWidth.coerceAtMost(coreCount)
        val step = ceil(newWidth.toDouble() / taskCount).toInt()
        val tasks = ArrayList<Callable<Unit>>(taskCount)

        for (k in 0 until taskCount) {
            val begin = k * step
            val end = (begin + step).coerceAtMost(newWidth)
            tasks.add(Callable {
                if (isEncrypt) {
                    for (i in begin until end) {
                        for (j in 0 until newHeight) {
                            var n = j
                            var m = (xArray[(n / blockHeight) % xBlockCount] * blockWidth + i) % newWidth
                            m = xArray[m / blockWidth] * blockWidth + m % blockWidth
                            n = (yArray[m / blockWidth % yBlockCount] * blockHeight + n) % newHeight
                            n = yArray[n / blockHeight] * blockHeight + n % blockHeight
                            newPixels[i + j * newWidth] = pixels[(m % width) + (n % height) * width]
                        }
                    }
                } else {
                    for (i in begin until end) {
                        for (j in 0 until newHeight) {
                            var n = j
                            var m = (xArray[(n / blockHeight) % xBlockCount] * blockWidth + i) % newWidth
                            m = xArray[m / blockWidth] * blockWidth + m % blockWidth
                            n = (yArray[m / blockWidth % yBlockCount] * blockHeight + n) % newHeight
                            n = yArray[n / blockHeight] * blockHeight + n % blockHeight
                            newPixels[m + n * newWidth] = pixels[(i % width) + (j % height) * width]
                        }
                    }
                }
            })
        }

        val executor = Executors.newFixedThreadPool(taskCount)
        try {
            executor.invokeAll(tasks)
        } finally {
            executor.shutdown()
        }
        return BlockResult(newPixels, newWidth, newHeight)
    }

    private fun generateLogistic(x1: Double, n: Int): Array<DoubleArray> {
        val arr = Array(n) { DoubleArray(2) }
        var x = x1
        arr[0][0] = x
        arr[0][1] = 0.0
        for (i in 1 until n) {
            x = 3.9999999 * x * (1.0 - x)
            arr[i][0] = x
            arr[i][1] = i.toDouble()
        }
        return arr
    }

    private fun getSortedPositions(logisticMap: Array<DoubleArray>, size: Int): IntArray {
        Arrays.sort(logisticMap) { a, b -> if (a[0] > b[0]) 1 else -1 }
        val positions = IntArray(size)
        for (i in 0 until size) {
            positions[i] = logisticMap[i][1].toInt()
        }
        return positions
    }

    internal fun processPicEncryptRow(
        pixels: IntArray,
        width: Int,
        height: Int,
        key: Double,
        isEncrypt: Boolean
    ): IntArray {
        val logisticMap = generateLogistic(key, width)
        val positions = getSortedPositions(logisticMap, width)
        val newPixels = IntArray(width * height)
        val offset = (height - 1) * width

        val taskCount = Runtime.getRuntime().availableProcessors()
        val step = ceil(width.toDouble() / taskCount).toInt()
        val tasks = ArrayList<Callable<Unit>>(taskCount)

        for (k in 0 until taskCount) {
            val begin = step * k
            val end = (begin + step).coerceAtMost(width)
            tasks.add(Callable {
                if (isEncrypt) {
                    for (i in begin until end) {
                        val m = positions[i]
                        for (j in offset downTo 0 step width) {
                            newPixels[i + j] = pixels[m + j]
                        }
                    }
                } else {
                    for (i in begin until end) {
                        val m = positions[i]
                        for (j in offset downTo 0 step width) {
                            newPixels[m + j] = pixels[i + j]
                        }
                    }
                }
            })
        }

        val executor = Executors.newFixedThreadPool(taskCount)
        try {
            executor.invokeAll(tasks)
        } finally {
            executor.shutdown()
        }
        return newPixels
    }

    internal fun processPicEncryptRowColumn(
        pixels: IntArray,
        width: Int,
        height: Int,
        key: Double,
        isEncrypt: Boolean
    ): IntArray {
        val maxTaskCount = 50
        val coreCount = Runtime.getRuntime().availableProcessors()

        return if (isEncrypt) {
            val newPixels = pixels.clone()
            val curPixels = pixels.clone()
            var x = key

            val executor = Executors.newFixedThreadPool(coreCount)
            val tasks = ArrayList<Callable<Unit>>()

            for (j in 0 until height) {
                val offset = j * width
                val lmap = generateLogistic(x, width)
                x = lmap[width - 1][0]

                tasks.add(Callable {
                    val positions = getSortedPositions(lmap, width)
                    for (i in 0 until width) {
                        curPixels[i + offset] = newPixels[positions[i] + offset]
                    }
                })

                if (tasks.size >= maxTaskCount) {
                    executor.invokeAll(tasks)
                    tasks.clear()
                }
            }

            if (tasks.isNotEmpty()) {
                executor.invokeAll(tasks)
                tasks.clear()
            }

            x = key
            for (i in 0 until width) {
                val lmap = generateLogistic(x, height)
                x = lmap[height - 1][0]

                tasks.add(Callable {
                    val positions = getSortedPositions(lmap, height)
                    for (j in 0 until height) {
                        newPixels[i + j * width] = curPixels[i + positions[j] * width]
                    }
                })

                if (tasks.size >= maxTaskCount) {
                    executor.invokeAll(tasks)
                    tasks.clear()
                }
            }

            if (tasks.isNotEmpty()) {
                executor.invokeAll(tasks)
                tasks.clear()
            }

            executor.shutdown()
            newPixels
        } else {
            val newPixels = pixels.clone()
            val curPixels = pixels.clone()
            var x = key

            val executor = Executors.newFixedThreadPool(coreCount)
            val tasks = ArrayList<Callable<Unit>>()

            for (i in 0 until width) {
                val lmap = generateLogistic(x, height)
                x = lmap[height - 1][0]

                tasks.add(Callable {
                    val positions = getSortedPositions(lmap, height)
                    for (j in 0 until height) {
                        curPixels[i + positions[j] * width] = newPixels[i + j * width]
                    }
                })

                if (tasks.size >= maxTaskCount) {
                    executor.invokeAll(tasks)
                    tasks.clear()
                }
            }

            if (tasks.isNotEmpty()) {
                executor.invokeAll(tasks)
                tasks.clear()
            }

            x = key
            for (j in 0 until height) {
                val offset = j * width
                val lmap = generateLogistic(x, width)
                x = lmap[width - 1][0]

                tasks.add(Callable {
                    val positions = getSortedPositions(lmap, width)
                    for (i in 0 until width) {
                        newPixels[positions[i] + offset] = curPixels[i + offset]
                    }
                })

                if (tasks.size >= maxTaskCount) {
                    executor.invokeAll(tasks)
                    tasks.clear()
                }
            }

            if (tasks.isNotEmpty()) {
                executor.invokeAll(tasks)
                tasks.clear()
            }

            executor.shutdown()
            newPixels
        }
    }
}
