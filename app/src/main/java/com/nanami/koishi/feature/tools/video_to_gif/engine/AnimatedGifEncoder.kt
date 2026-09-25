package com.nanami.koishi.feature.tools.video_to_gif.engine

import android.graphics.Bitmap
import java.io.IOException
import java.io.OutputStream
import kotlin.math.max

class AnimatedGifEncoder {

    private var width: Int = 0
    private var height: Int = 0
    private var delayCentiseconds: Int = 10
    private var repeatCount: Int = 0
    private var sampleStep: Int = 10
    private var outputStream: OutputStream? = null

    private var colorTable: ByteArray? = null
    private var indexedPixels: ByteArray? = null
    private var colorDepth: Int = 8
    private val paletteBitSize: Int = 7
    private var isStarted: Boolean = false
    private var isFirstFrame: Boolean = true

    fun start(os: OutputStream): Boolean {
        outputStream = os
        return try {
            writeString("GIF89a")
            isStarted = true
            isFirstFrame = true
            true
        } catch (_: IOException) {
            false
        }
    }

    fun setDelay(delayMillis: Int) {
        delayCentiseconds = max(1, Math.round(delayMillis / 10f))
    }

    fun setRepeat(repeat: Int) {
        repeatCount = repeat
    }

    fun setQuality(quality: Int) {
        sampleStep = max(1, quality)
    }

    fun setSize(w: Int, h: Int) {
        width = w
        height = h
    }

    fun addFrame(bitmap: Bitmap): Boolean {
        val out = outputStream
        if (!isStarted || out == null) return false

        return try {
            width = bitmap.width
            height = bitmap.height

            analyzePixels(bitmap)

            if (isFirstFrame) {
                writeLogicalScreenDescriptor()
                writePalette()
                if (repeatCount >= 0) {
                    writeNetscapeExtension()
                }
            }

            writeGraphicControlExtension()
            writeImageDescriptor()
            if (!isFirstFrame) {
                writePalette()
            }
            writePixels()

            isFirstFrame = false
            true
        } catch (_: IOException) {
            false
        }
    }

    fun finish(): Boolean {
        if (!isStarted) return false
        isStarted = false
        val out = outputStream ?: return false
        return try {
            out.write(0x3B)
            out.flush()
            true
        } catch (_: IOException) {
            false
        } finally {
            colorTable = null
            indexedPixels = null
            outputStream = null
            isFirstFrame = true
        }
    }

    private fun analyzePixels(bitmap: Bitmap) {
        val w = bitmap.width
        val h = bitmap.height
        val pixelsInt = IntArray(w * h)
        bitmap.getPixels(pixelsInt, 0, w, 0, 0, w, h)

        val rawBgr = ByteArray(pixelsInt.size * 3)
        var byteIndex = 0
        for (pixel in pixelsInt) {
            rawBgr[byteIndex++] = (pixel and 0xFF).toByte()
            rawBgr[byteIndex++] = ((pixel shr 8) and 0xFF).toByte()
            rawBgr[byteIndex++] = ((pixel shr 16) and 0xFF).toByte()
        }

        val quantizer = NeuQuant(rawBgr, rawBgr.size, sampleStep)
        val palette = quantizer.process()

        for (i in palette.indices step 3) {
            val temp = palette[i]
            palette[i] = palette[i + 2]
            palette[i + 2] = temp
        }
        colorTable = palette

        val indexed = ByteArray(pixelsInt.size)
        var pixelIdx = 0
        for (i in indexed.indices) {
            val b = rawBgr[pixelIdx++].toInt() and 0xFF
            val g = rawBgr[pixelIdx++].toInt() and 0xFF
            val r = rawBgr[pixelIdx++].toInt() and 0xFF
            indexed[i] = quantizer.map(b, g, r).toByte()
        }

        indexedPixels = indexed
        colorDepth = 8
    }

    private fun writeLogicalScreenDescriptor() {
        val out = outputStream ?: return
        writeShort(width)
        writeShort(height)
        out.write(0x80 or 0x70 or 0x00 or paletteBitSize)
        out.write(0)
        out.write(0)
    }

    private fun writeNetscapeExtension() {
        val out = outputStream ?: return
        out.write(0x21)
        out.write(0xFF)
        out.write(11)
        writeString("NETSCAPE2.0")
        out.write(3)
        out.write(1)
        writeShort(repeatCount)
        out.write(0)
    }

    private fun writeGraphicControlExtension() {
        val out = outputStream ?: return
        out.write(0x21)
        out.write(0xF9)
        out.write(4)
        out.write(0)
        writeShort(delayCentiseconds)
        out.write(0)
        out.write(0)
    }

    private fun writeImageDescriptor() {
        val out = outputStream ?: return
        out.write(0x2C)
        writeShort(0)
        writeShort(0)
        writeShort(width)
        writeShort(height)
        if (isFirstFrame) {
            out.write(0)
        } else {
            out.write(0x80 or paletteBitSize)
        }
    }

    private fun writePalette() {
        val out = outputStream ?: return
        val table = colorTable ?: return
        out.write(table, 0, table.size)
        val pad = (3 * 256) - table.size
        for (i in 0 until pad) {
            out.write(0)
        }
    }

    private fun writePixels() {
        val out = outputStream ?: return
        val pixels = indexedPixels ?: return
        LzwEncoder(width, height, pixels, colorDepth).encode(out)
    }

    private fun writeShort(value: Int) {
        val out = outputStream ?: return
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }

    private fun writeString(s: String) {
        val out = outputStream ?: return
        for (i in 0 until s.length) {
            out.write(s[i].code)
        }
    }

    internal class NeuQuant(
        private val thepicture: ByteArray,
        private val lengthcount: Int,
        private var samplefac: Int
    ) {
        private val netsize = 256
        private val prime1 = 499
        private val prime2 = 491
        private val prime3 = 487
        private val prime4 = 503
        private val minpicturebytes = 3 * prime4
        private val maxnetpos = netsize - 1
        private val netbiasshift = 4
        private val ncycles = 100
        private val intbiasshift = 16
        private val intbias = 1 shl intbiasshift
        private val gammashift = 10
        private val betashift = 10
        private val beta = intbias shr betashift
        private val betagamma = intbias shl (gammashift - betashift)
        private val initrad = netsize shr 3
        private val radiusbiasshift = 6
        private val radiusbias = 1 shl radiusbiasshift
        private val initradius = initrad * radiusbias
        private val radiusdec = 30
        private val alphabiasshift = 10
        private val initalpha = 1 shl alphabiasshift
        private var alphadec = 0
        private val radbiasshift = 8
        private val radbias = 1 shl radbiasshift
        private val alpharadbshift = alphabiasshift + radbiasshift
        private val alpharadbias = 1 shl alpharadbshift

        private val network = Array(netsize) { IntArray(4) }
        private val netindex = IntArray(256)
        private val bias = IntArray(netsize)
        private val freq = IntArray(netsize)
        private val radpower = IntArray(33)

        init {
            for (i in 0 until netsize) {
                val p = network[i]
                val initVal = (i shl (netbiasshift + 8)) / netsize
                p[0] = initVal
                p[1] = initVal
                p[2] = initVal
                freq[i] = intbias / netsize
                bias[i] = 0
            }
        }

        fun process(): ByteArray {
            learn()
            unbiasnet()
            inxbuild()
            return colorMap()
        }

        fun map(b: Int, g: Int, r: Int): Int {
            var bestd = 1000
            var best = -1
            var i = netindex[g]
            var j = i - 1

            while (i < netsize || j >= 0) {
                if (i < netsize) {
                    val p = network[i]
                    var dist = p[1] - g
                    if (dist >= bestd) {
                        i = netsize
                    } else {
                        i++
                        if (dist < 0) dist = -dist
                        var a = p[0] - b
                        if (a < 0) a = -a
                        dist += a
                        if (dist < bestd) {
                            a = p[2] - r
                            if (a < 0) a = -a
                            dist += a
                            if (dist < bestd) {
                                bestd = dist
                                best = p[3]
                            }
                        }
                    }
                }
                if (j >= 0) {
                    val p = network[j]
                    var dist = g - p[1]
                    if (dist >= bestd) {
                        j = -1
                    } else {
                        j--
                        if (dist < 0) dist = -dist
                        var a = p[0] - b
                        if (a < 0) a = -a
                        dist += a
                        if (dist < bestd) {
                            a = p[2] - r
                            if (a < 0) a = -a
                            dist += a
                            if (dist < bestd) {
                                bestd = dist
                                best = p[3]
                            }
                        }
                    }
                }
            }
            return best
        }

        private fun colorMap(): ByteArray {
            val map = ByteArray(3 * netsize)
            val index = IntArray(netsize)
            for (i in 0 until netsize) {
                index[network[i][3]] = i
            }
            var k = 0
            for (i in 0 until netsize) {
                val j = index[i]
                map[k++] = network[j][0].toByte()
                map[k++] = network[j][1].toByte()
                map[k++] = network[j][2].toByte()
            }
            return map
        }

        private fun inxbuild() {
            var previouscol = 0
            var startpos = 0
            for (i in 0 until netsize) {
                val p = network[i]
                var smallpos = i
                var smallval = p[1]
                for (j in i + 1 until netsize) {
                    val q = network[j]
                    if (q[1] < smallval) {
                        smallpos = j
                        smallval = q[1]
                    }
                }
                val q = network[smallpos]
                if (i != smallpos) {
                    var tmp = p[0]; p[0] = q[0]; q[0] = tmp
                    tmp = p[1]; p[1] = q[1]; q[1] = tmp
                    tmp = p[2]; p[2] = q[2]; q[2] = tmp
                    tmp = p[3]; p[3] = q[3]; q[3] = tmp
                }
                if (smallval != previouscol) {
                    netindex[previouscol] = (startpos + i) shr 1
                    for (j in previouscol + 1 until smallval) {
                        netindex[j] = i
                    }
                    previouscol = smallval
                    startpos = i
                }
            }
            netindex[previouscol] = (startpos + maxnetpos) shr 1
            for (j in previouscol + 1 until 256) {
                netindex[j] = maxnetpos
            }
        }

        private fun learn() {
            if (lengthcount < minpicturebytes) samplefac = 1
            alphadec = 30 + ((samplefac - 1) / 3)
            val p = thepicture
            var pix = 0
            val lim = lengthcount
            val samplepixels = lengthcount / (3 * samplefac)
            var delta = samplepixels / ncycles
            var alpha = initalpha
            var radius = initradius

            var rad = radius shr radiusbiasshift
            if (rad <= 1) rad = 0
            for (i in 0 until rad) {
                radpower[i] = alpha * (((rad * rad - i * i) * radbias) / (rad * rad))
            }

            val step = if (lengthcount < minpicturebytes) {
                3
            } else if ((lengthcount % prime1) != 0) {
                3 * prime1
            } else if ((lengthcount % prime2) != 0) {
                3 * prime2
            } else if ((lengthcount % prime3) != 0) {
                3 * prime3
            } else {
                3 * prime4
            }

            var i = 0
            while (i < samplepixels) {
                val b = (p[pix + 0].toInt() and 0xFF) shl netbiasshift
                val g = (p[pix + 1].toInt() and 0xFF) shl netbiasshift
                val r = (p[pix + 2].toInt() and 0xFF) shl netbiasshift
                val j = contest(b, g, r)

                altersingle(alpha, j, b, g, r)
                if (rad != 0) {
                    alterneigh(rad, j, b, g, r)
                }

                pix += step
                if (pix >= lim) pix -= lengthcount

                i++
                if (delta == 0) delta = 1
                if (i % delta == 0) {
                    alpha -= alpha / alphadec
                    radius -= radius / radiusdec
                    rad = radius shr radiusbiasshift
                    if (rad <= 1) rad = 0
                    for (k in 0 until rad) {
                        radpower[k] = alpha * (((rad * rad - k * k) * radbias) / (rad * rad))
                    }
                }
            }
        }

        private fun altersingle(alpha: Int, i: Int, b: Int, g: Int, r: Int) {
            val n = network[i]
            n[0] -= (alpha * (n[0] - b)) / initalpha
            n[1] -= (alpha * (n[1] - g)) / initalpha
            n[2] -= (alpha * (n[2] - r)) / initalpha
        }

        private fun alterneigh(rad: Int, i: Int, b: Int, g: Int, r: Int) {
            var lo = i - rad
            if (lo < -1) lo = -1
            var hi = i + rad
            if (hi > netsize) hi = netsize

            var j = i + 1
            var k = i - 1
            var m = 1
            while (j < hi || k > lo) {
                val a = if (m < radpower.size) radpower[m++] else 0
                if (j < hi) {
                    val p = network[j++]
                    p[0] -= (a * (p[0] - b)) / alpharadbias
                    p[1] -= (a * (p[1] - g)) / alpharadbias
                    p[2] -= (a * (p[2] - r)) / alpharadbias
                }
                if (k > lo) {
                    val p = network[k--]
                    p[0] -= (a * (p[0] - b)) / alpharadbias
                    p[1] -= (a * (p[1] - g)) / alpharadbias
                    p[2] -= (a * (p[2] - r)) / alpharadbias
                }
            }
        }

        private fun contest(b: Int, g: Int, r: Int): Int {
            var bestd = Int.MAX_VALUE
            var bestbiasd = bestd
            var bestpos = -1
            var bestbiaspos = bestpos

            for (i in 0 until netsize) {
                val n = network[i]
                var dist = n[0] - b
                if (dist < 0) dist = -dist
                var a = n[1] - g
                if (a < 0) a = -a
                dist += a
                a = n[2] - r
                if (a < 0) a = -a
                dist += a
                if (dist < bestd) {
                    bestd = dist
                    bestpos = i
                }
                val biasdist = dist - (bias[i] shr (intbiasshift - netbiasshift))
                if (biasdist < bestbiasd) {
                    bestbiasd = biasdist
                    bestbiaspos = i
                }
                val betafreq = freq[i] shr betashift
                freq[i] -= betafreq
                bias[i] += betafreq shl gammashift
            }
            freq[bestpos] += beta
            bias[bestpos] -= betagamma
            return bestbiaspos
        }

        private fun unbiasnet() {
            for (i in 0 until netsize) {
                network[i][0] = network[i][0] shr netbiasshift
                network[i][1] = network[i][1] shr netbiasshift
                network[i][2] = network[i][2] shr netbiasshift
                network[i][3] = i
            }
        }
    }

    internal class LzwEncoder(
        private val imgW: Int,
        private val imgH: Int,
        private val pixAry: ByteArray,
        colorDepth: Int
    ) {
        private val initCodeSize = max(2, colorDepth)
        private var remaining = imgW * imgH
        private var curPixel = 0

        private var nBits = 0
        private val maxbits = 12
        private var maxcode = 0
        private val maxmaxcode = 1 shl 12

        private val htab = IntArray(5003)
        private val codetab = IntArray(5003)
        private val hsize = 5003
        private var freeEnt = 0
        private var clearFlg = false

        private var gInitBits = 0
        private var clearCode = 0
        private var eofCode = 0

        private var curAccum = 0
        private var curBits = 0

        private val masks = intArrayOf(
            0x0000, 0x0001, 0x0003, 0x0007, 0x000F,
            0x001F, 0x003F, 0x007F, 0x00FF,
            0x01FF, 0x03FF, 0x07FF, 0x0FFF,
            0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF
        )

        private var aCount = 0
        private val accum = ByteArray(256)

        fun encode(os: OutputStream) {
            os.write(initCodeSize)
            remaining = imgW * imgH
            curPixel = 0
            compress(initCodeSize + 1, os)
            os.write(0)
        }

        private fun charOut(c: Byte, os: OutputStream) {
            accum[aCount++] = c
            if (aCount >= 254) {
                flushChar(os)
            }
        }

        private fun clBlock(os: OutputStream) {
            clHash(hsize)
            freeEnt = clearCode + 2
            clearFlg = true
            output(clearCode, os)
        }

        private fun clHash(size: Int) {
            for (i in 0 until size) {
                htab[i] = -1
            }
        }

        private fun compress(initBits: Int, os: OutputStream) {
            gInitBits = initBits
            clearFlg = false
            nBits = gInitBits
            maxcode = maxCode(nBits)

            clearCode = 1 shl (initBits - 1)
            eofCode = clearCode + 1
            freeEnt = clearCode + 2

            aCount = 0

            var ent = nextPixel()

            var hshift = 0
            var fcode = hsize
            while (fcode < 65536) {
                hshift++
                fcode *= 2
            }
            hshift = 8 - hshift

            val hsizeReg = hsize
            clHash(hsizeReg)

            output(clearCode, os)

            var c = nextPixel()
            while (c != -1) {
                fcode = (c shl maxbits) + ent
                var i = (c shl hshift) xor ent

                if (htab[i] == fcode) {
                    ent = codetab[i]
                    c = nextPixel()
                    continue
                } else if (htab[i] >= 0) {
                    var disp = hsizeReg - i
                    if (i == 0) disp = 1
                    var matched = false
                    while (true) {
                        i -= disp
                        if (i < 0) i += hsizeReg
                        if (htab[i] == fcode) {
                            ent = codetab[i]
                            matched = true
                            break
                        }
                        if (htab[i] < 0) break
                    }
                    if (matched) {
                        c = nextPixel()
                        continue
                    }
                }

                output(ent, os)
                ent = c
                if (freeEnt < maxmaxcode) {
                    codetab[i] = freeEnt++
                    htab[i] = fcode
                } else {
                    clBlock(os)
                }
                c = nextPixel()
            }

            output(ent, os)
            output(eofCode, os)
        }

        private fun flushChar(os: OutputStream) {
            if (aCount > 0) {
                os.write(aCount)
                os.write(accum, 0, aCount)
                aCount = 0
            }
        }

        private fun maxCode(bits: Int): Int = (1 shl bits) - 1

        private fun nextPixel(): Int {
            if (remaining == 0) return -1
            remaining--
            return pixAry[curPixel++].toInt() and 0xFF
        }

        private fun output(code: Int, os: OutputStream) {
            curAccum = curAccum and masks[curBits]
            if (curBits > 0) {
                curAccum = curAccum or (code shl curBits)
            } else {
                curAccum = code
            }
            curBits += nBits

            while (curBits >= 8) {
                charOut((curAccum and 0xFF).toByte(), os)
                curAccum = curAccum shr 8
                curBits -= 8
            }

            if (freeEnt > maxcode || clearFlg) {
                if (clearFlg) {
                    nBits = gInitBits
                    maxcode = maxCode(nBits)
                    clearFlg = false
                } else {
                    nBits++
                    maxcode = if (nBits == maxbits) maxmaxcode else maxCode(nBits)
                }
            }

            if (code == eofCode) {
                while (curBits > 0) {
                    charOut((curAccum and 0xFF).toByte(), os)
                    curAccum = curAccum shr 8
                    curBits -= 8
                }
                flushChar(os)
            }
        }
    }
}
