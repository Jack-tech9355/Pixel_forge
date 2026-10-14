package com.example.drawing

import android.graphics.Rect
import com.example.model.PixelLayer

class PixelSelection(
    val canvasWidth: Int,
    val canvasHeight: Int
) {
    data class ClipboardData(
        val width: Int,
        val height: Int,
        val pixels: IntArray
    )

    var mask: BooleanArray = BooleanArray(canvasWidth * canvasHeight)
        private set

    var hasActiveMask: Boolean = false
        private set

    var isFloating: Boolean = false
        private set

    var floatingPixels: IntArray? = null
        private set
    var floatingWidth: Int = 0
        private set
    var floatingHeight: Int = 0
        private set
    var floatingX: Int = 0
        private set
    var floatingY: Int = 0
        private set

    val bounds: Rect = Rect()

    companion object {
        var clipboard: ClipboardData? = null
    }

    fun setRect(x1: Int, y1: Int, x2: Int, y2: Int) {
        val minX = minOf(x1, x2).coerceIn(0, canvasWidth - 1)
        val maxX = maxOf(x1, x2).coerceIn(0, canvasWidth - 1)
        val minY = minOf(y1, y2).coerceIn(0, canvasHeight - 1)
        val maxY = maxOf(y1, y2).coerceIn(0, canvasHeight - 1)

        mask.fill(false)
        for (y in minY..maxY) {
            val row = y * canvasWidth
            for (x in minX..maxX) {
                mask[row + x] = true
            }
        }
        bounds.set(minX, minY, maxX + 1, maxY + 1)
        hasActiveMask = true
    }

    fun setMaskFromBooleans(booleanArray: BooleanArray) {
        if (booleanArray.size != mask.size) return
        System.arraycopy(booleanArray, 0, mask, 0, mask.size)
        recomputeBounds()
    }

    fun setLasso(points: List<BresenhamAlgorithms.Point>) {
        if (points.size < 3) return
        mask.fill(false)

        // Find bounding box of lasso points
        var minX = canvasWidth
        var maxX = 0
        var minY = canvasHeight
        var maxY = 0
        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }
        minX = minX.coerceIn(0, canvasWidth - 1)
        maxX = maxX.coerceIn(0, canvasWidth - 1)
        minY = minY.coerceIn(0, canvasHeight - 1)
        maxY = maxY.coerceIn(0, canvasHeight - 1)

        // Point-in-polygon raycasting algorithm for pixel art lasso
        val nvert = points.size
        for (y in minY..maxY) {
            val py = y + 0.5
            for (x in minX..maxX) {
                val px = x + 0.5
                var inside = false
                var j = nvert - 1
                for (i in 0 until nvert) {
                    val xi = points[i].x.toDouble()
                    val yi = points[i].y.toDouble()
                    val xj = points[j].x.toDouble()
                    val yj = points[j].y.toDouble()

                    val intersect = ((yi > py) != (yj > py)) &&
                            (px < (xj - xi) * (py - yi) / (yj - yi) + xi)
                    if (intersect) inside = !inside
                    j = i
                }
                if (inside) {
                    mask[y * canvasWidth + x] = true
                }
            }
        }
        recomputeBounds()
    }

    private fun recomputeBounds() {
        var minX = canvasWidth
        var maxX = -1
        var minY = canvasHeight
        var maxY = -1

        var count = 0
        for (y in 0 until canvasHeight) {
            val row = y * canvasWidth
            for (x in 0 until canvasWidth) {
                if (mask[row + x]) {
                    count++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (count > 0 && maxX >= minX && maxY >= minY) {
            bounds.set(minX, minY, maxX + 1, maxY + 1)
            hasActiveMask = true
        } else {
            bounds.setEmpty()
            hasActiveMask = false
        }
    }

    fun contains(x: Int, y: Int): Boolean {
        if (!hasActiveMask) return false
        if (x !in 0 until canvasWidth || y !in 0 until canvasHeight) return false
        return mask[y * canvasWidth + x]
    }

    /**
     * Lifts masked pixels from layer into floating buffer, erasing them from layer.
     */
    fun lift(layer: PixelLayer) {
        if (!hasActiveMask || isFloating) return
        val w = bounds.width()
        val h = bounds.height()
        if (w <= 0 || h <= 0) return

        val buf = IntArray(w * h)
        for (y in 0 until h) {
            val canvasY = bounds.top + y
            for (x in 0 until w) {
                val canvasX = bounds.left + x
                val idx = canvasY * canvasWidth + canvasX
                if (canvasX in 0 until canvasWidth && canvasY in 0 until canvasHeight && mask[idx]) {
                    buf[y * w + x] = layer.pixels[idx]
                    layer.pixels[idx] = 0 // Erase from layer
                } else {
                    buf[y * w + x] = 0
                }
            }
        }

        floatingPixels = buf
        floatingWidth = w
        floatingHeight = h
        floatingX = bounds.left
        floatingY = bounds.top
        isFloating = true
    }

    fun move(dx: Int, dy: Int) {
        if (!isFloating) return
        floatingX += dx
        floatingY += dy
        bounds.offset(dx, dy)
    }

    fun flipHorizontal() {
        val buf = floatingPixels ?: return
        val w = floatingWidth
        val h = floatingHeight
        val newBuf = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                newBuf[y * w + (w - 1 - x)] = buf[y * w + x]
            }
        }
        floatingPixels = newBuf
    }

    fun flipVertical() {
        val buf = floatingPixels ?: return
        val w = floatingWidth
        val h = floatingHeight
        val newBuf = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                newBuf[(h - 1 - y) * w + x] = buf[y * w + x]
            }
        }
        floatingPixels = newBuf
    }

    fun rotate90Clockwise() {
        val buf = floatingPixels ?: return
        val oldW = floatingWidth
        val oldH = floatingHeight
        val newW = oldH
        val newH = oldW
        val newBuf = IntArray(newW * newH)

        for (y in 0 until oldH) {
            for (x in 0 until oldW) {
                val newX = oldH - 1 - y
                val newY = x
                newBuf[newY * newW + newX] = buf[y * oldW + x]
            }
        }

        floatingPixels = newBuf
        floatingWidth = newW
        floatingHeight = newH
        bounds.set(floatingX, floatingY, floatingX + newW, floatingY + newH)
    }

    fun rotate180() {
        rotate90Clockwise()
        rotate90Clockwise()
    }

    fun rotate270() {
        rotate90Clockwise()
        rotate90Clockwise()
        rotate90Clockwise()
    }

    fun cut(layer: PixelLayer) {
        copy(layer)
        clear(layer)
        deselect(layer)
    }

    fun copy(layer: PixelLayer) {
        if (isFloating && floatingPixels != null) {
            clipboard = ClipboardData(
                width = floatingWidth,
                height = floatingHeight,
                pixels = floatingPixels!!.clone()
            )
            return
        }

        if (!hasActiveMask) return
        val w = bounds.width()
        val h = bounds.height()
        if (w <= 0 || h <= 0) return

        val buf = IntArray(w * h)
        for (y in 0 until h) {
            val canvasY = bounds.top + y
            for (x in 0 until w) {
                val canvasX = bounds.left + x
                val idx = canvasY * canvasWidth + canvasX
                if (canvasX in 0 until canvasWidth && canvasY in 0 until canvasHeight && mask[idx]) {
                    buf[y * w + x] = layer.pixels[idx]
                }
            }
        }
        clipboard = ClipboardData(w, h, buf)
    }

    fun paste(layer: PixelLayer) {
        val clip = clipboard ?: return
        commit(layer) // Commit existing if any

        floatingPixels = clip.pixels.clone()
        floatingWidth = clip.width
        floatingHeight = clip.height
        floatingX = (canvasWidth - clip.width) / 2
        floatingY = (canvasHeight - clip.height) / 2
        bounds.set(floatingX, floatingY, floatingX + clip.width, floatingY + clip.height)
        isFloating = true
        hasActiveMask = true

        // Update mask to match floating area
        mask.fill(false)
        for (y in 0 until clip.height) {
            val cy = floatingY + y
            if (cy in 0 until canvasHeight) {
                for (x in 0 until clip.width) {
                    val cx = floatingX + x
                    if (cx in 0 until canvasWidth) {
                        val pix = clip.pixels[y * clip.width + x]
                        if ((pix ushr 24) > 0) {
                            mask[cy * canvasWidth + cx] = true
                        }
                    }
                }
            }
        }
    }

    fun clear(layer: PixelLayer) {
        if (isFloating) {
            floatingPixels = null
            isFloating = false
            hasActiveMask = false
            bounds.setEmpty()
            mask.fill(false)
            return
        }
        if (!hasActiveMask) return
        for (i in mask.indices) {
            if (mask[i] && i in layer.pixels.indices) {
                layer.pixels[i] = 0
            }
        }
        deselect(layer)
    }

    fun commit(layer: PixelLayer) {
        if (!isFloating) return
        val buf = floatingPixels ?: return
        val w = floatingWidth
        val h = floatingHeight

        for (y in 0 until h) {
            val canvasY = floatingY + y
            if (canvasY !in 0 until canvasHeight) continue
            for (x in 0 until w) {
                val canvasX = floatingX + x
                if (canvasX !in 0 until canvasWidth) continue

                val srcColor = buf[y * w + x]
                val srcA = (srcColor ushr 24) and 0xFF
                if (srcA > 0) {
                    val idx = canvasY * canvasWidth + canvasX
                    layer.pixels[idx] = srcColor
                }
            }
        }

        floatingPixels = null
        isFloating = false
    }

    fun deselect(layer: PixelLayer) {
        commit(layer)
        mask.fill(false)
        hasActiveMask = false
        bounds.setEmpty()
    }
}
