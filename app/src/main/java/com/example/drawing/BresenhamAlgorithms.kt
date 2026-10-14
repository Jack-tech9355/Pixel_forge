package com.example.drawing

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * High-performance, pixel-exact geometry algorithms for pixel art rasterization.
 * Operates strictly in integer grid coordinates with zero anti-aliasing.
 */
object BresenhamAlgorithms {

    data class Point(val x: Int, val y: Int)

    /**
     * Bresenham's Line Algorithm:
     * Generates a continuous, non-broken line of pixels between (x0, y0) and (x1, y1).
     */
    fun getLinePixels(x0: Int, y0: Int, x1: Int, y1: Int): List<Point> {
        val points = mutableListOf<Point>()
        var currX = x0
        var currY = y0
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy

        while (true) {
            points.add(Point(currX, currY))
            if (currX == x1 && currY == y1) break
            val e2 = 2 * err
            if (e2 >= dy) {
                err += dy
                currX += sx
            }
            if (e2 <= dx) {
                err += dx
                currY += sy
            }
        }
        return points
    }

    /**
     * Rectangle outline or filled rectangle pixels.
     */
    fun getRectanglePixels(x0: Int, y0: Int, x1: Int, y1: Int, filled: Boolean): List<Point> {
        val points = mutableListOf<Point>()
        val minX = minOf(x0, x1)
        val maxX = maxOf(x0, x1)
        val minY = minOf(y0, y1)
        val maxY = maxOf(y0, y1)

        if (filled) {
            for (y in minY..maxY) {
                for (x in minX..maxX) {
                    points.add(Point(x, y))
                }
            }
        } else {
            for (x in minX..maxX) {
                points.add(Point(x, minY))
                if (minY != maxY) {
                    points.add(Point(x, maxY))
                }
            }
            for (y in (minY + 1) until maxY) {
                points.add(Point(minX, y))
                if (minX != maxX) {
                    points.add(Point(maxX, y))
                }
            }
        }
        return points
    }

    /**
     * Midpoint circle / ellipse algorithm with optional filling.
     */
    fun getCirclePixels(x0: Int, y0: Int, x1: Int, y1: Int, filled: Boolean): List<Point> {
        val points = mutableListOf<Point>()
        val minX = minOf(x0, x1)
        val maxX = maxOf(x0, x1)
        val minY = minOf(y0, y1)
        val maxY = maxOf(y0, y1)

        val cx = (minX + maxX) / 2.0
        val cy = (minY + maxY) / 2.0
        val rx = maxOf(1.0, (maxX - minX) / 2.0)
        val ry = maxOf(1.0, (maxY - minY) / 2.0)

        if (filled) {
            for (y in minY..maxY) {
                for (x in minX..maxX) {
                    val normalizedX = (x - cx) / rx
                    val normalizedY = (y - cy) / ry
                    if ((normalizedX * normalizedX + normalizedY * normalizedY) <= 1.05) {
                        points.add(Point(x, y))
                    }
                }
            }
        } else {
            val pointSet = mutableSetOf<Point>()
            val steps = (maxOf(rx, ry) * 8).toInt().coerceAtLeast(16)
            for (i in 0 until steps) {
                val theta = 2.0 * Math.PI * i / steps
                val px = (cx + rx * Math.cos(theta) + 0.5).toInt()
                val py = (cy + ry * Math.sin(theta) + 0.5).toInt()
                pointSet.add(Point(px, py))
            }
            points.addAll(pointSet)
        }
        return points
    }

    /**
     * Color distance between two ARGB 32-bit colors on a 0..100 scale.
     */
    fun colorDistance(c1: Int, c2: Int): Double {
        val a1 = (c1 ushr 24) and 0xFF
        val a2 = (c2 ushr 24) and 0xFF
        if (a1 == 0 && a2 == 0) return 0.0
        if (a1 == 0 || a2 == 0) return 100.0

        val r1 = (c1 ushr 16) and 0xFF
        val g1 = (c1 ushr 8) and 0xFF
        val b1 = c1 and 0xFF

        val r2 = (c2 ushr 16) and 0xFF
        val g2 = (c2 ushr 8) and 0xFF
        val b2 = c2 and 0xFF

        val dr = (r1 - r2).toDouble()
        val dg = (g1 - g2).toDouble()
        val db = (b1 - b2).toDouble()
        val da = (a1 - a2).toDouble()

        return sqrt(dr * dr + dg * dg + db * db + da * da) / 510.0 * 100.0
    }

    /**
     * Stack-based 4-way flood fill.
     */
    fun floodFill(
        pixels: IntArray,
        width: Int,
        height: Int,
        startX: Int,
        startY: Int,
        targetColor: Int,
        replacementColor: Int,
        tolerance: Int = 0
    ) {
        if (targetColor == replacementColor) return
        if (startX !in 0 until width || startY !in 0 until height) return

        val startIndex = startY * width + startX
        val baseColor = pixels[startIndex]
        if (tolerance == 0 && baseColor != targetColor) return
        if (tolerance > 0 && colorDistance(baseColor, targetColor) > tolerance) return

        val visited = BooleanArray(width * height)
        val queue = java.util.ArrayDeque<Point>()
        queue.push(Point(startX, startY))
        visited[startIndex] = true
        pixels[startIndex] = replacementColor

        val dx = intArrayOf(0, 0, -1, 1)
        val dy = intArrayOf(-1, 1, 0, 0)

        while (!queue.isEmpty()) {
            val pt = queue.pop()
            for (i in 0..3) {
                val nx = pt.x + dx[i]
                val ny = pt.y + dy[i]

                if (nx in 0 until width && ny in 0 until height) {
                    val nIndex = ny * width + nx
                    if (!visited[nIndex]) {
                        val pix = pixels[nIndex]
                        val matches = if (tolerance <= 0) {
                            pix == targetColor
                        } else {
                            colorDistance(pix, targetColor) <= tolerance
                        }
                        if (matches) {
                            visited[nIndex] = true
                            pixels[nIndex] = replacementColor
                            queue.push(Point(nx, ny))
                        }
                    }
                }
            }
        }
    }

    /**
     * Magic Wand connected pixel selection with configurable tolerance.
     */
    fun magicWandSelect(
        pixels: IntArray,
        width: Int,
        height: Int,
        startX: Int,
        startY: Int,
        tolerance: Int
    ): BooleanArray {
        val mask = BooleanArray(width * height)
        if (startX !in 0 until width || startY !in 0 until height) return mask

        val targetColor = pixels[startY * width + startX]
        val queue = java.util.ArrayDeque<Point>()
        queue.push(Point(startX, startY))
        mask[startY * width + startX] = true

        val dx = intArrayOf(0, 0, -1, 1)
        val dy = intArrayOf(-1, 1, 0, 0)

        while (!queue.isEmpty()) {
            val pt = queue.pop()
            for (i in 0..3) {
                val nx = pt.x + dx[i]
                val ny = pt.y + dy[i]
                if (nx in 0 until width && ny in 0 until height) {
                    val idx = ny * width + nx
                    if (!mask[idx]) {
                        val c = pixels[idx]
                        val diff = if (tolerance <= 0) {
                            if (c == targetColor) 0.0 else 100.0
                        } else {
                            colorDistance(c, targetColor)
                        }
                        if (diff <= tolerance) {
                            mask[idx] = true
                            queue.push(Point(nx, ny))
                        }
                    }
                }
            }
        }
        return mask
    }

    /**
     * Replaces a chosen color with another color across pixels array with optional tolerance.
     */
    fun replaceColor(
        pixels: IntArray,
        targetColor: Int,
        newColor: Int,
        tolerance: Int = 0,
        preserveAlpha: Boolean = true
    ): Int {
        var count = 0
        val targetA = (targetColor ushr 24) and 0xFF
        val newR = (newColor ushr 16) and 0xFF
        val newG = (newColor ushr 8) and 0xFF
        val newB = newColor and 0xFF

        for (i in pixels.indices) {
            val c = pixels[i]
            val diff = if (tolerance <= 0) {
                if (c == targetColor) 0.0 else 100.0
            } else {
                colorDistance(c, targetColor)
            }

            if (diff <= tolerance) {
                if (preserveAlpha) {
                    val srcA = (c ushr 24) and 0xFF
                    pixels[i] = (srcA shl 24) or (newR shl 16) or (newG shl 8) or newB
                } else {
                    pixels[i] = newColor
                }
                count++
            }
        }
        return count
    }
}
