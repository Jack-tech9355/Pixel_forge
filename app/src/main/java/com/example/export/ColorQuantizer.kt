package com.example.export

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max

/**
 * High quality offline color quantization using the classic Median Cut algorithm.
 * Extracts 8, 16, or 32 distinct dominant colors from an imported image without external APIs.
 */
object ColorQuantizer {

    private data class ColorBox(
        val colors: List<Int>
    ) {
        val count: Int get() = colors.size

        var minR = 255; var maxR = 0
        var minG = 255; var maxG = 0
        var minB = 255; var maxB = 0

        init {
            for (c in colors) {
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                if (r < minR) minR = r; if (r > maxR) maxR = r
                if (g < minG) minG = g; if (g > maxG) maxG = g
                if (b < minB) minB = b; if (b > maxB) maxB = b
            }
        }

        val rangeR: Int get() = maxR - minR
        val rangeG: Int get() = maxG - minG
        val rangeB: Int get() = maxB - minB

        val maxRange: Int get() = maxOf(rangeR, rangeG, rangeB)

        fun averageColor(): Int {
            if (colors.isEmpty()) return 0xFF000000.toInt()
            var sumR = 0L
            var sumG = 0L
            var sumB = 0L
            for (c in colors) {
                sumR += (c ushr 16) and 0xFF
                sumG += (c ushr 8) and 0xFF
                sumB += c and 0xFF
            }
            val count = colors.size.toLong()
            val avgR = (sumR / count).toInt().coerceIn(0, 255)
            val avgG = (sumG / count).toInt().coerceIn(0, 255)
            val avgB = (sumB / count).toInt().coerceIn(0, 255)
            return (0xFF shl 24) or (avgR shl 16) or (avgG shl 8) or avgB
        }
    }

    fun extractPalette(
        sourceBitmap: Bitmap,
        targetColors: Int = 16
    ): List<Int> {
        // Downscale to max 128x128 to ensure ultra-fast analysis and low RAM usage
        val maxDim = 128
        val scale = minOf(1.0f, maxDim.toFloat() / max(sourceBitmap.width, sourceBitmap.height))
        val scaledW = maxOf(1, (sourceBitmap.width * scale).toInt())
        val scaledH = maxOf(1, (sourceBitmap.height * scale).toInt())

        val scaled = Bitmap.createScaledBitmap(sourceBitmap, scaledW, scaledH, false)
        val pixels = IntArray(scaledW * scaledH)
        scaled.getPixels(pixels, 0, scaledW, 0, 0, scaledW, scaledH)

        // Filter out transparent pixels
        val opaqueColors = ArrayList<Int>(pixels.size)
        for (p in pixels) {
            val a = (p ushr 24) and 0xFF
            if (a > 64) {
                // Normalize alpha to 255
                opaqueColors.add(p or 0xFF000000.toInt())
            }
        }

        if (opaqueColors.isEmpty()) {
            return listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
        }

        val uniqueColors = opaqueColors.distinct()
        if (uniqueColors.size <= targetColors) {
            return uniqueColors.sortedBy { luminance(it) }
        }

        val boxes = java.util.PriorityQueue<ColorBox>(compareByDescending { it.maxRange })
        boxes.add(ColorBox(opaqueColors))

        while (boxes.size < targetColors) {
            val largest = boxes.poll() ?: break
            if (largest.count < 2 || largest.maxRange == 0) {
                boxes.add(largest)
                break
            }

            // Find dimension with largest range
            val rRange = largest.rangeR
            val gRange = largest.rangeG
            val bRange = largest.rangeB

            val sortedList = when {
                rRange >= gRange && rRange >= bRange -> {
                    largest.colors.sortedBy { (it ushr 16) and 0xFF }
                }
                gRange >= rRange && gRange >= bRange -> {
                    largest.colors.sortedBy { (it ushr 8) and 0xFF }
                }
                else -> {
                    largest.colors.sortedBy { it and 0xFF }
                }
            }

            val median = sortedList.size / 2
            val box1 = ColorBox(sortedList.subList(0, median))
            val box2 = ColorBox(sortedList.subList(median, sortedList.size))

            boxes.add(box1)
            boxes.add(box2)
        }

        return boxes.map { it.averageColor() }
            .distinct()
            .sortedBy { luminance(it) }
    }

    private fun luminance(color: Int): Float {
        val r = ((color ushr 16) and 0xFF) / 255f
        val g = ((color ushr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        return 0.299f * r + 0.587f * g + 0.114f * b
    }
}
