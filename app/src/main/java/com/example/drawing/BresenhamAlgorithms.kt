package com.example.drawing

import kotlin.math.abs

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
            // Horizontal edges
            for (x in minX..maxX) {
                points.add(Point(x, minY))
                if (minY != maxY) {
                    points.add(Point(x, maxY))
                }
            }
            // Vertical edges
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
            // Bresenham outline for circle
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
     * Stack-based 4-way flood fill.
     * Prevents StackOverflowError and efficiently fills bounded pixel clusters.
     */
    fun floodFill(
        pixels: IntArray,
        width: Int,
        height: Int,
        startX: Int,
        startY: Int,
        targetColor: Int,
        replacementColor: Int
    ) {
        if (targetColor == replacementColor) return
        if (startX !in 0 until width || startY !in 0 until height) return

        val startIndex = startY * width + startX
        if (pixels[startIndex] != targetColor) return

        val queue = java.util.ArrayDeque<Point>()
        queue.push(Point(startX, startY))
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
                    if (pixels[nIndex] == targetColor) {
                        pixels[nIndex] = replacementColor
                        queue.push(Point(nx, ny))
                    }
                }
            }
        }
    }
}
