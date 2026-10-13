package com.example.drawing

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.example.model.DrawingTool
import com.example.model.PixelLayer
import com.example.model.PixelProject

class PixelCanvasState(
    var project: PixelProject
) {
    var width by mutableIntStateOf(project.width)
    var height by mutableIntStateOf(project.height)

    val layers = mutableStateListOf<PixelLayer>().apply {
        addAll(project.layers)
    }

    var activeLayerIndex by mutableIntStateOf(0)
    var activeTool by mutableStateOf(DrawingTool.PENCIL)
    var currentColor by mutableIntStateOf(project.selectedColor)
    var brushSize by mutableIntStateOf(1)
    var isGridVisible by mutableStateOf(true)

    var zoom by mutableFloatStateOf(1f)
    var panOffset by mutableStateOf(Offset.Zero)

    val history = PixelHistory(maxSteps = 40)
    var hasUnsavedChanges by mutableStateOf(false)

    // Hardware-backed direct rendering bitmap & version counter
    var renderBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        private set
    private val compositedPixels = IntArray(width * height)
    var renderVersion by mutableLongStateOf(0L)
        private set

    // Preview for shape tools (Line, Rect, Circle)
    var isDrawingShape by mutableStateOf(false)
    var shapeStartPoint by mutableStateOf<BresenhamAlgorithms.Point?>(null)
    var shapeCurrentPoint by mutableStateOf<BresenhamAlgorithms.Point?>(null)

    // Eyedropper loupe state
    var pickedColorPreview by mutableStateOf<Int?>(null)

    // Last point for smooth stroke interpolation
    private var lastStrokePoint: BresenhamAlgorithms.Point? = null

    val activeLayer: PixelLayer?
        get() = layers.getOrNull(activeLayerIndex)

    val canUndo: Boolean get() = history.canUndo
    val canRedo: Boolean get() = history.canRedo

    init {
        if (layers.isEmpty()) {
            layers.add(PixelLayer(name = "Layer 1", pixels = IntArray(width * height)))
        }
        recompositeAll()
    }

    /**
     * Recomposites all layers into the persistent render bitmap.
     * Called on canvas init, layer changes, shape commits, and undo/redo.
     */
    fun recompositeAll() {
        val total = width * height
        if (compositedPixels.size != total) {
            renderBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

        // Fill background
        val bg = project.backgroundColor
        if (bg != null) {
            compositedPixels.fill(bg)
        } else {
            compositedPixels.fill(0)
        }

        // Composite layers bottom to top
        for (layer in layers) {
            if (!layer.isVisible || layer.opacity <= 0.001f) continue
            val opacity = layer.opacity
            for (i in 0 until minOf(total, layer.pixels.size)) {
                val srcColor = layer.pixels[i]
                val srcA = AndroidColor.alpha(srcColor)
                if (srcA == 0) continue

                val effectiveSrcA = (srcA * opacity).toInt().coerceIn(0, 255)
                if (effectiveSrcA == 0) continue

                val dstColor = compositedPixels[i]
                val dstA = AndroidColor.alpha(dstColor)

                if (dstA == 0 || effectiveSrcA == 255) {
                    val r = AndroidColor.red(srcColor)
                    val g = AndroidColor.green(srcColor)
                    val b = AndroidColor.blue(srcColor)
                    compositedPixels[i] = AndroidColor.argb(effectiveSrcA, r, g, b)
                } else {
                    val sa = effectiveSrcA / 255f
                    val da = dstA / 255f
                    val outA = sa + da * (1f - sa)

                    val sr = AndroidColor.red(srcColor)
                    val sg = AndroidColor.green(srcColor)
                    val sb = AndroidColor.blue(srcColor)

                    val dr = AndroidColor.red(dstColor)
                    val dg = AndroidColor.green(dstColor)
                    val db = AndroidColor.blue(dstColor)

                    val outR = ((sr * sa + dr * da * (1f - sa)) / outA).toInt().coerceIn(0, 255)
                    val outG = ((sg * sa + dg * da * (1f - sa)) / outA).toInt().coerceIn(0, 255)
                    val outB = ((sb * sa + db * da * (1f - sa)) / outA).toInt().coerceIn(0, 255)
                    val finalA = (outA * 255).toInt().coerceIn(0, 255)

                    compositedPixels[i] = AndroidColor.argb(finalA, outR, outG, outB)
                }
            }
        }

        renderBitmap.setPixels(compositedPixels, 0, width, 0, 0, width, height)
        renderVersion++
    }

    /**
     * Fast in-place update of a single pixel in the render bitmap without allocating anything.
     */
    private fun updateRenderPixel(x: Int, y: Int) {
        if (x !in 0 until width || y !in 0 until height) return
        val index = y * width + x

        // Re-blend just this pixel across layers
        var pixelColor = project.backgroundColor ?: 0
        for (layer in layers) {
            if (!layer.isVisible || layer.opacity <= 0.001f) continue
            val srcColor = layer.pixels[index]
            val srcA = AndroidColor.alpha(srcColor)
            if (srcA == 0) continue

            val effectiveSrcA = (srcA * layer.opacity).toInt().coerceIn(0, 255)
            if (effectiveSrcA == 0) continue

            val dstA = AndroidColor.alpha(pixelColor)
            if (dstA == 0 || effectiveSrcA == 255) {
                val r = AndroidColor.red(srcColor)
                val g = AndroidColor.green(srcColor)
                val b = AndroidColor.blue(srcColor)
                pixelColor = AndroidColor.argb(effectiveSrcA, r, g, b)
            } else {
                val sa = effectiveSrcA / 255f
                val da = dstA / 255f
                val outA = sa + da * (1f - sa)

                val sr = AndroidColor.red(srcColor)
                val sg = AndroidColor.green(srcColor)
                val sb = AndroidColor.blue(srcColor)

                val dr = AndroidColor.red(pixelColor)
                val dg = AndroidColor.green(pixelColor)
                val db = AndroidColor.blue(pixelColor)

                val outR = ((sr * sa + dr * da * (1f - sa)) / outA).toInt().coerceIn(0, 255)
                val outG = ((sg * sa + dg * da * (1f - sa)) / outA).toInt().coerceIn(0, 255)
                val outB = ((sb * sa + db * da * (1f - sa)) / outA).toInt().coerceIn(0, 255)
                val finalA = (outA * 255).toInt().coerceIn(0, 255)

                pixelColor = AndroidColor.argb(finalA, outR, outG, outB)
            }
        }

        compositedPixels[index] = pixelColor
        renderBitmap.setPixel(x, y, pixelColor)
    }

    fun syncBackToProject() {
        project.title = project.title
        project.layers = layers.toMutableList()
        project.selectedColor = currentColor
        project.updatedAt = System.currentTimeMillis()
    }

    fun onTouchDown(gridX: Int, gridY: Int) {
        if (gridX !in 0 until width || gridY !in 0 until height) return
        val currentActive = activeLayer ?: return

        when (activeTool) {
            DrawingTool.PENCIL, DrawingTool.ERASER -> {
                history.pushState(layers, activeLayerIndex, "Stroke")
                val drawColor = if (activeTool == DrawingTool.PENCIL) currentColor else 0
                setPixelWithBrush(currentActive, gridX, gridY, drawColor)
                lastStrokePoint = BresenhamAlgorithms.Point(gridX, gridY)
                renderVersion++
            }
            DrawingTool.BUCKET -> {
                history.pushState(layers, activeLayerIndex, "Bucket Fill")
                val targetColor = currentActive.getPixel(gridX, gridY, width)
                if (targetColor != currentColor) {
                    BresenhamAlgorithms.floodFill(
                        currentActive.pixels,
                        width,
                        height,
                        gridX,
                        gridY,
                        targetColor,
                        currentColor
                    )
                    recompositeAll()
                    hasUnsavedChanges = true
                }
            }
            DrawingTool.EYEDROPPER -> {
                pickColorAt(gridX, gridY)
            }
            DrawingTool.LINE, DrawingTool.RECTANGLE, DrawingTool.FILLED_RECTANGLE,
            DrawingTool.CIRCLE, DrawingTool.FILLED_CIRCLE -> {
                isDrawingShape = true
                shapeStartPoint = BresenhamAlgorithms.Point(gridX, gridY)
                shapeCurrentPoint = BresenhamAlgorithms.Point(gridX, gridY)
            }
            DrawingTool.MOVE -> {
                // Handled by pan gesture
            }
        }
    }

    fun onTouchMove(gridX: Int, gridY: Int) {
        val currentActive = activeLayer ?: return

        when (activeTool) {
            DrawingTool.PENCIL, DrawingTool.ERASER -> {
                val clampedX = gridX.coerceIn(0, width - 1)
                val clampedY = gridY.coerceIn(0, height - 1)
                val drawColor = if (activeTool == DrawingTool.PENCIL) currentColor else 0

                val last = lastStrokePoint
                if (last != null) {
                    // Smooth stroke interpolation via Bresenham Line
                    val points = BresenhamAlgorithms.getLinePixels(last.x, last.y, clampedX, clampedY)
                    for (pt in points) {
                        setPixelWithBrush(currentActive, pt.x, pt.y, drawColor)
                    }
                } else {
                    setPixelWithBrush(currentActive, clampedX, clampedY, drawColor)
                }
                lastStrokePoint = BresenhamAlgorithms.Point(clampedX, clampedY)
                renderVersion++
            }
            DrawingTool.EYEDROPPER -> {
                if (gridX in 0 until width && gridY in 0 until height) {
                    pickColorAt(gridX, gridY)
                }
            }
            DrawingTool.LINE, DrawingTool.RECTANGLE, DrawingTool.FILLED_RECTANGLE,
            DrawingTool.CIRCLE, DrawingTool.FILLED_CIRCLE -> {
                if (isDrawingShape) {
                    shapeCurrentPoint = BresenhamAlgorithms.Point(
                        gridX.coerceIn(0, width - 1),
                        gridY.coerceIn(0, height - 1)
                    )
                }
            }
            else -> {}
        }
    }

    fun onTouchUp() {
        val currentActive = activeLayer ?: return

        if (isDrawingShape && shapeStartPoint != null && shapeCurrentPoint != null) {
            val start = shapeStartPoint!!
            val end = shapeCurrentPoint!!

            val pointsToDraw = when (activeTool) {
                DrawingTool.LINE -> BresenhamAlgorithms.getLinePixels(start.x, start.y, end.x, end.y)
                DrawingTool.RECTANGLE -> BresenhamAlgorithms.getRectanglePixels(start.x, start.y, end.x, end.y, false)
                DrawingTool.FILLED_RECTANGLE -> BresenhamAlgorithms.getRectanglePixels(start.x, start.y, end.x, end.y, true)
                DrawingTool.CIRCLE -> BresenhamAlgorithms.getCirclePixels(start.x, start.y, end.x, end.y, false)
                DrawingTool.FILLED_CIRCLE -> BresenhamAlgorithms.getCirclePixels(start.x, start.y, end.x, end.y, true)
                else -> emptyList()
            }

            if (pointsToDraw.isNotEmpty()) {
                history.pushState(layers, activeLayerIndex, activeTool.title)
                for (pt in pointsToDraw) {
                    if (pt.x in 0 until width && pt.y in 0 until height) {
                        currentActive.setPixel(pt.x, pt.y, width, currentColor)
                        updateRenderPixel(pt.x, pt.y)
                    }
                }
                renderVersion++
                hasUnsavedChanges = true
            }
        } else if (activeTool == DrawingTool.PENCIL || activeTool == DrawingTool.ERASER) {
            hasUnsavedChanges = true
        }

        isDrawingShape = false
        shapeStartPoint = null
        shapeCurrentPoint = null
        lastStrokePoint = null
    }

    private fun setPixelWithBrush(layer: PixelLayer, cx: Int, cy: Int, color: Int) {
        if (brushSize <= 1) {
            layer.setPixel(cx, cy, width, color)
            updateRenderPixel(cx, cy)
        } else {
            val radius = brushSize - 1
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val px = cx + dx
                    val py = cy + dy
                    if (px in 0 until width && py in 0 until height) {
                        layer.setPixel(px, py, width, color)
                        updateRenderPixel(px, py)
                    }
                }
            }
        }
    }

    private fun pickColorAt(x: Int, y: Int) {
        for (i in layers.indices.reversed()) {
            val layer = layers[i]
            if (!layer.isVisible) continue
            val pixel = layer.getPixel(x, y, width)
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha > 0) {
                currentColor = pixel
                pickedColorPreview = pixel
                return
            }
        }
    }

    fun undo() {
        val snapshot = history.undo(layers, activeLayerIndex) ?: return
        layers.clear()
        layers.addAll(snapshot.layers)
        activeLayerIndex = snapshot.activeLayerIndex.coerceIn(0, maxOf(0, layers.size - 1))
        recompositeAll()
        hasUnsavedChanges = true
    }

    fun redo() {
        val snapshot = history.redo(layers, activeLayerIndex) ?: return
        layers.clear()
        layers.addAll(snapshot.layers)
        activeLayerIndex = snapshot.activeLayerIndex.coerceIn(0, maxOf(0, layers.size - 1))
        recompositeAll()
        hasUnsavedChanges = true
    }

    fun clearActiveLayer() {
        val active = activeLayer ?: return
        history.pushState(layers, activeLayerIndex, "Clear Layer")
        active.pixels.fill(0)
        recompositeAll()
        hasUnsavedChanges = true
    }

    fun clearAllLayers() {
        history.pushState(layers, activeLayerIndex, "Clear Canvas")
        for (layer in layers) {
            layer.pixels.fill(0)
        }
        recompositeAll()
        hasUnsavedChanges = true
    }

    // Layer Operations
    fun addLayer(name: String = "Layer ${layers.size + 1}") {
        history.pushState(layers, activeLayerIndex, "Add Layer")
        val newLayer = PixelLayer(
            name = name,
            pixels = IntArray(width * height)
        )
        layers.add(newLayer)
        activeLayerIndex = layers.size - 1
        recompositeAll()
        hasUnsavedChanges = true
    }

    fun duplicateActiveLayer() {
        val active = activeLayer ?: return
        history.pushState(layers, activeLayerIndex, "Duplicate Layer")
        val copy = active.copyLayer()
        layers.add(activeLayerIndex + 1, copy)
        activeLayerIndex++
        recompositeAll()
        hasUnsavedChanges = true
    }

    fun deleteActiveLayer() {
        if (layers.size <= 1) return
        history.pushState(layers, activeLayerIndex, "Delete Layer")
        layers.removeAt(activeLayerIndex)
        activeLayerIndex = activeLayerIndex.coerceIn(0, layers.size - 1)
        recompositeAll()
        hasUnsavedChanges = true
    }

    fun moveActiveLayerUp() {
        if (activeLayerIndex < layers.size - 1) {
            history.pushState(layers, activeLayerIndex, "Move Layer Up")
            val item = layers.removeAt(activeLayerIndex)
            layers.add(activeLayerIndex + 1, item)
            activeLayerIndex++
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun moveActiveLayerDown() {
        if (activeLayerIndex > 0) {
            history.pushState(layers, activeLayerIndex, "Move Layer Down")
            val item = layers.removeAt(activeLayerIndex)
            layers.add(activeLayerIndex - 1, item)
            activeLayerIndex--
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun mergeActiveLayerDown() {
        if (activeLayerIndex <= 0) return
        history.pushState(layers, activeLayerIndex, "Merge Layer Down")
        val upperLayer = layers[activeLayerIndex]
        val lowerLayer = layers[activeLayerIndex - 1]

        for (i in lowerLayer.pixels.indices) {
            val upperColor = upperLayer.pixels[i]
            val upperA = (upperColor ushr 24) and 0xFF
            if (upperA > 0) {
                lowerLayer.pixels[i] = upperColor
            }
        }
        layers.removeAt(activeLayerIndex)
        activeLayerIndex--
        recompositeAll()
        hasUnsavedChanges = true
    }

    fun setLayerVisibility(index: Int, isVisible: Boolean) {
        if (index in layers.indices) {
            layers[index] = layers[index].copy(isVisible = isVisible)
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun setLayerOpacity(index: Int, opacity: Float) {
        if (index in layers.indices) {
            layers[index] = layers[index].copy(opacity = opacity)
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun renameLayer(index: Int, newName: String) {
        if (index in layers.indices) {
            layers[index] = layers[index].copy(name = newName)
            hasUnsavedChanges = true
        }
    }

    fun resetView() {
        zoom = 1f
        panOffset = Offset.Zero
    }
}
