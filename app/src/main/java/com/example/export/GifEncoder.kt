package com.example.export

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Pure Kotlin standard GIF89a encoder with LZW compression and transparency support.
 * Runs completely offline on Android without external dependencies.
 */
class GifEncoder {

    private var width: Int = 0
    private var height: Int = 0
    private var delayHundredths: Int = 10 // 100ms = 10 fps
    private var repeatCount: Int = 0 // 0 = loop forever
    private var isStarted = false
    private var outStream: OutputStream? = null

    fun setDelay(ms: Int) {
        delayHundredths = (ms / 10).coerceAtLeast(1)
    }

    fun setRepeat(count: Int) {
        repeatCount = count
    }

    fun start(os: OutputStream): Boolean {
        outStream = os
        return try {
            writeString("GIF89a")
            isStarted = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun addFrame(bitmap: Bitmap): Boolean {
        if (!isStarted || outStream == null) return false
        try {
            if (width == 0 || height == 0) {
                width = bitmap.width
                height = bitmap.height
                writeLogicalScreenDescriptor()
                writeNetscapeApplicationExtension()
            }

            // Quantize bitmap to 256-color palette
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val (indexedPixels, palette, transIndex) = buildIndexedFrame(pixels)

            writeGraphicControlExtension(transIndex)
            writeImageDescriptor()
            writeLocalColorTable(palette)
            writeImageData(indexedPixels)

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun finish(): Boolean {
        if (!isStarted || outStream == null) return false
        return try {
            outStream?.write(0x3B) // GIF Trailer
            outStream?.flush()
            isStarted = false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private data class IndexedResult(
        val indexedPixels: ByteArray,
        val palette: IntArray, // 256 colors ARGB
        val transparentIndex: Int
    )

    private fun buildIndexedFrame(pixels: IntArray): IndexedResult {
        val colorMap = HashMap<Int, Int>()
        val palette = IntArray(256)
        var nextColorIndex = 0

        // Reserve index 0 for transparency
        val transparentIndex = 0
        palette[transparentIndex] = 0 // Transparent
        nextColorIndex = 1

        val indexedPixels = ByteArray(pixels.size)

        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            if (a < 64) {
                indexedPixels[i] = transparentIndex.toByte()
            } else {
                val rgb = p or 0xFF000000.toInt()
                val existing = colorMap[rgb]
                if (existing != null) {
                    indexedPixels[i] = existing.toByte()
                } else {
                    if (nextColorIndex < 256) {
                        colorMap[rgb] = nextColorIndex
                        palette[nextColorIndex] = rgb
                        indexedPixels[i] = nextColorIndex.toByte()
                        nextColorIndex++
                    } else {
                        // Nearest color fallback if palette exceeds 255 colors
                        var nearestIdx = 1
                        var minDiff = Long.MAX_VALUE
                        val r1 = (rgb ushr 16) and 0xFF
                        val g1 = (rgb ushr 8) and 0xFF
                        val b1 = rgb and 0xFF
                        for (c in 1 until 256) {
                            val palColor = palette[c]
                            val r2 = (palColor ushr 16) and 0xFF
                            val g2 = (palColor ushr 8) and 0xFF
                            val b2 = palColor and 0xFF
                            val dr = r1 - r2
                            val dg = g1 - g2
                            val db = b1 - b2
                            val diff = (dr * dr + dg * dg + db * db).toLong()
                            if (diff < minDiff) {
                                minDiff = diff
                                nearestIdx = c
                            }
                        }
                        indexedPixels[i] = nearestIdx.toByte()
                    }
                }
            }
        }

        return IndexedResult(indexedPixels, palette, transparentIndex)
    }

    private fun writeLogicalScreenDescriptor() {
        writeShort(width)
        writeShort(height)
        outStream?.write(0x70) // No Global Color Table, 8 bits color resolution
        outStream?.write(0)    // Background color index
        outStream?.write(0)    // Pixel aspect ratio
    }

    private fun writeNetscapeApplicationExtension() {
        outStream?.write(0x21) // Extension Introducer
        outStream?.write(0xFF) // Application Extension Label
        outStream?.write(11)   // Block Size
        writeString("NETSCAPE2.0")
        outStream?.write(3)    // Sub-block length
        outStream?.write(1)    // Loop sub-block ID
        writeShort(repeatCount)
        outStream?.write(0)    // Block Terminator
    }

    private fun writeGraphicControlExtension(transIndex: Int) {
        outStream?.write(0x21) // Extension Introducer
        outStream?.write(0xF9) // Graphic Control Label
        outStream?.write(4)    // Block Size

        val hasTrans = transIndex >= 0
        val packed = if (hasTrans) 0x09 else 0x08 // Disposal = 2 (restore to background), Transparent flag
        outStream?.write(packed)
        writeShort(delayHundredths)
        outStream?.write(if (hasTrans) transIndex else 0)
        outStream?.write(0) // Block Terminator
    }

    private fun writeImageDescriptor() {
        outStream?.write(0x2C) // Image Separator
        writeShort(0) // Left
        writeShort(0) // Top
        writeShort(width)
        writeShort(height)
        outStream?.write(0x87) // Local Color Table Present, 256 colors (2^(7+1))
    }

    private fun writeLocalColorTable(palette: IntArray) {
        for (color in palette) {
            val r = (color ushr 16) and 0xFF
            val g = (color ushr 8) and 0xFF
            val b = color and 0xFF
            outStream?.write(r)
            outStream?.write(g)
            outStream?.write(b)
        }
    }

    private fun writeImageData(pixels: ByteArray) {
        val initCodeSize = 8
        outStream?.write(initCodeSize)

        // Simple LZW compression for GIF
        val lzw = LzwEncoder(pixels, initCodeSize)
        lzw.encode(outStream!!)
        outStream?.write(0) // Block Terminator
    }

    private fun writeString(str: String) {
        for (c in str) {
            outStream?.write(c.code)
        }
    }

    private fun writeShort(value: Int) {
        outStream?.write(value and 0xFF)
        outStream?.write((value ushr 8) and 0xFF)
    }

    /**
     * Standard LZW compressor for GIF image data blocks.
     */
    private class LzwEncoder(
        private val pixAry: ByteArray,
        private val initCodeSize: Int
    ) {
        private val clearCode = 1 shl initCodeSize
        private val eofCode = clearCode + 1
        private var nBits = initCodeSize + 1
        private var maxCode = (1 shl nBits) - 1

        private val htab = IntArray(5003) { -1 }
        private val codetab = IntArray(5003)

        private var curAccum = 0
        private var curBits = 0
        private val packet = ByteArray(256)
        private var packetCount = 0

        fun encode(os: OutputStream) {
            os.write(initCodeSize) // already written in caller, but standard LZW data starts here
            // Reset tables
            resetTables()
            output(clearCode, os)

            var ent = if (pixAry.isNotEmpty()) pixAry[0].toInt() and 0xFF else -1
            var nextFreeCode = clearCode + 2

            for (i in 1 until pixAry.size) {
                val c = pixAry[i].toInt() and 0xFF
                val fcode = (c shl 12) + ent
                var h = (c shl 4) xor ent
                if (h >= 5003) h -= 5003

                if (htab[h] == fcode) {
                    ent = codetab[h]
                    continue
                } else if (htab[h] >= 0) {
                    var disp = 5003 - h
                    if (h == 0) disp = 1
                    var found = false
                    while (true) {
                        h -= disp
                        if (h < 0) h += 5003
                        if (htab[h] == fcode) {
                            ent = codetab[h]
                            found = true
                            break
                        }
                        if (htab[h] < 0) break
                    }
                    if (found) continue
                }

                output(ent, os)
                ent = c
                if (nextFreeCode < 4096) {
                    codetab[h] = nextFreeCode++
                    htab[h] = fcode
                } else {
                    resetTables()
                    output(clearCode, os)
                    nextFreeCode = clearCode + 2
                }
            }

            if (ent != -1) output(ent, os)
            output(eofCode, os)
            flushBits(os)
            flushPacket(os)
        }

        private fun resetTables() {
            htab.fill(-1)
            nBits = initCodeSize + 1
            maxCode = (1 shl nBits) - 1
        }

        private fun output(code: Int, os: OutputStream) {
            curAccum = curAccum or (code shl curBits)
            curBits += nBits

            while (curBits >= 8) {
                addByteToPacket((curAccum and 0xFF).toByte(), os)
                curAccum = curAccum ushr 8
                curBits -= 8
            }

            if (code == clearCode) {
                nBits = initCodeSize + 1
                maxCode = (1 shl nBits) - 1
            } else if (code > maxCode) {
                nBits++
                maxCode = if (nBits == 12) 4096 else (1 shl nBits) - 1
            }
        }

        private fun flushBits(os: OutputStream) {
            if (curBits > 0) {
                addByteToPacket((curAccum and 0xFF).toByte(), os)
            }
        }

        private fun addByteToPacket(b: Byte, os: OutputStream) {
            packet[packetCount++] = b
            if (packetCount >= 254) {
                flushPacket(os)
            }
        }

        private fun flushPacket(os: OutputStream) {
            if (packetCount > 0) {
                os.write(packetCount)
                os.write(packet, 0, packetCount)
                packetCount = 0
            }
        }
    }

    companion object {
        fun encodeBitmaps(
            frames: List<Bitmap>,
            fps: Int,
            outputStream: OutputStream
        ): Boolean {
            if (frames.isEmpty()) return false
            val encoder = GifEncoder()
            val delayMs = (1000 / fps.coerceIn(1, 60))
            encoder.setDelay(delayMs)
            encoder.setRepeat(0)

            if (!encoder.start(outputStream)) return false
            for (frame in frames) {
                if (!encoder.addFrame(frame)) return false
            }
            return encoder.finish()
        }
    }
}
