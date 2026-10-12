package com.example.drawing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
                hasUnsavedChanges = true
                val drawColor = if (activeTool == DrawingTool.PENCIL) currentColor else 0
                setPixelWithBrush(currentActive, gridX, gridY, drawColor)
                lastStrokePoint = BresenhamAlgorithms.Point(gridX, gridY)
            }
            DrawingTool.BUCKET -> {
                history.pushState(layers, activeLayerIndex, "Bucket Fill")
                hasUnsavedChanges = true
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
                hasUnsavedChanges = true
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
                hasUnsavedChanges = true
                for (pt in pointsToDraw) {
                    if (pt.x in 0 until width && pt.y in 0 until height) {
                        currentActive.setPixel(pt.x, pt.y, width, currentColor)
                    }
                }
            }
        }

        isDrawingShape = false
        shapeStartPoint = null
        shapeCurrentPoint = null
        lastStrokePoint = null
    }

    private fun setPixelWithBrush(layer: PixelLayer, cx: Int, cy: Int, color: Int) {
        if (brushSize <= 1) {
            layer.setPixel(cx, cy, width, color)
        } else {
            val radius = brushSize - 1
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val px = cx + dx
                    val py = cy + dy
                    if (px in 0 until width && py in 0 until height) {
                        layer.setPixel(px, py, width, color)
                    }
                }
            }
        }
    }

    private fun pickColorAt(x: Int, y: Int) {
        // Sample composited pixel color from top to bottom
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
        hasUnsavedChanges = true
    }

    fun redo() {
        val snapshot = history.redo(layers, activeLayerIndex) ?: return
        layers.clear()
        layers.addAll(snapshot.layers)
        activeLayerIndex = snapshot.activeLayerIndex.coerceIn(0, maxOf(0, layers.size - 1))
        hasUnsavedChanges = true
    }

    fun clearActiveLayer() {
        val active = activeLayer ?: return
        history.pushState(layers, activeLayerIndex, "Clear Layer")
        active.pixels.fill(0)
        hasUnsavedChanges = true
    }

    fun clearAllLayers() {
        history.pushState(layers, activeLayerIndex, "Clear Canvas")
        for (layer in layers) {
            layer.pixels.fill(0)
        }
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
        hasUnsavedChanges = true
    }

    fun duplicateActiveLayer() {
        val active = activeLayer ?: return
        history.pushState(layers, activeLayerIndex, "Duplicate Layer")
        val copy = active.copyLayer()
        layers.add(activeLayerIndex + 1, copy)
        activeLayerIndex++
        hasUnsavedChanges = true
    }

    fun deleteActiveLayer() {
        if (layers.size <= 1) return
        history.pushState(layers, activeLayerIndex, "Delete Layer")
        layers.removeAt(activeLayerIndex)
        activeLayerIndex = activeLayerIndex.coerceIn(0, layers.size - 1)
        hasUnsavedChanges = true
    }

    fun moveActiveLayerUp() {
        if (activeLayerIndex < layers.size - 1) {
            history.pushState(layers, activeLayerIndex, "Move Layer Up")
            val item = layers.removeAt(activeLayerIndex)
            layers.add(activeLayerIndex + 1, item)
            activeLayerIndex++
            hasUnsavedChanges = true
        }
    }

    fun moveActiveLayerDown() {
        if (activeLayerIndex > 0) {
            history.pushState(layers, activeLayerIndex, "Move Layer Down")
            val item = layers.removeAt(activeLayerIndex)
            layers.add(activeLayerIndex - 1, item)
            activeLayerIndex--
            hasUnsavedChanges = true
        }
    }

    fun mergeActiveLayerDown() {
        if (activeLayerIndex <= 0) return
        history.pushState(layers, activeLayerIndex, "Merge Layer Down")
        val upperLayer = layers[activeLayerIndex]
        val lowerLayer = layers[activeLayerIndex - 1]

        // Alpha blend upper layer into lower layer
        for (i in lowerLayer.pixels.indices) {
            val upperColor = upperLayer.pixels[i]
            val upperA = (upperColor ushr 24) and 0xFF
            if (upperA > 0) {
                lowerLayer.pixels[i] = upperColor
            }
        }
        layers.removeAt(activeLayerIndex)
        activeLayerIndex--
        hasUnsavedChanges = true
    }

    fun setLayerVisibility(index: Int, isVisible: Boolean) {
        if (index in layers.indices) {
            layers[index] = layers[index].copy(isVisible = isVisible)
            hasUnsavedChanges = true
        }
    }

    fun setLayerOpacity(index: Int, opacity: Float) {
        if (index in layers.indices) {
            layers[index] = layers[index].copy(opacity = opacity)
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
