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
import com.example.model.AnimationFrame
import com.example.model.DitheringPattern
import com.example.model.DrawingTool
import com.example.model.PixelLayer
import com.example.model.PixelProject
import com.example.model.SymmetryMode

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

    // Feature A: Symmetry & Mirror Drawing
    var symmetryMode by mutableStateOf(SymmetryMode.NONE)

    // Feature B & C: Selection and Transform Tools
    val selection = PixelSelection(width, height)
    var isDraggingSelection by mutableStateOf(false)
    private var selectionDragStart: BresenhamAlgorithms.Point? = null
    var lassoPoints = mutableListOf<BresenhamAlgorithms.Point>()
    var magicWandTolerance by mutableIntStateOf(20)

    // Feature E: Dithering & Shading Brushes
    var ditheringPattern by mutableStateOf(DitheringPattern.CHECKER_50)
    var shadingStrength by mutableFloatStateOf(0.15f) // 15% lighten/darken

    // Feature F: Reference Image Overlay
    var referenceBitmap by mutableStateOf<Bitmap?>(null)
    var isReferenceVisible by mutableStateOf(false)
    var referenceOpacity by mutableFloatStateOf(0.5f)
    var referenceOffset by mutableStateOf(Offset.Zero)
    var referenceScale by mutableFloatStateOf(1.0f)

    // Feature H: Animation Frames & Timeline
    val frames = mutableStateListOf<AnimationFrame>().apply {
        if (project.frames.isNotEmpty()) {
            addAll(project.frames)
        } else {
            add(AnimationFrame(durationMs = 125, layers = project.layers.map { it.copyLayer() }.toMutableList()))
        }
    }
    var currentFrameIndex by mutableIntStateOf(0)
    var isPlayingAnimation by mutableStateOf(false)
    var animationFps by mutableIntStateOf(8)
    var isOnionSkinEnabled by mutableStateOf(false)
    var onionSkinOpacity by mutableFloatStateOf(0.35f)

    // Feature I: Tilemap & Seamless Pattern Mode
    var isTilemapMode by mutableStateOf(false)

    // Navigation & Viewport
    var zoom by mutableFloatStateOf(1f)
    var panOffset by mutableStateOf(Offset.Zero)

    val history = PixelHistory(maxSteps = 40)
    var hasUnsavedChanges by mutableStateOf(false)

    // Hardware-backed direct rendering bitmap & version counter
    var renderBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        private set
    private var compositedPixels = IntArray(width * height)
    var renderVersion by mutableLongStateOf(0L)
        private set

    // Preview for shape tools (Line, Rect, Circle, Box Select)
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
     * Accurately reallocates pixel buffers on resize without memory corruption.
     */
    fun recompositeAll() {
        val total = width * height
        if (compositedPixels.size != total) {
            compositedPixels = IntArray(total)
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

        // Composite floating selection preview on top if present
        if (selection.isFloating && selection.floatingPixels != null) {
            val buf = selection.floatingPixels!!
            val fw = selection.floatingWidth
            val fh = selection.floatingHeight
            val fx = selection.floatingX
            val fy = selection.floatingY
            for (y in 0 until fh) {
                val cy = fy + y
                if (cy !in 0 until height) continue
                for (x in 0 until fw) {
                    val cx = fx + x
                    if (cx !in 0 until width) continue
                    val src = buf[y * fw + x]
                    val srcA = AndroidColor.alpha(src)
                    if (srcA > 0) {
                        val idx = cy * width + cx
                        compositedPixels[idx] = src
                    }
                }
            }
        }

        renderBitmap.setPixels(compositedPixels, 0, width, 0, 0, width, height)
        renderVersion++
    }

    /**
     * In-place pixel update for ultra-low latency response.
     */
    private fun updateRenderPixel(x: Int, y: Int) {
        if (x !in 0 until width || y !in 0 until height) return
        val index = y * width + x
        if (index !in compositedPixels.indices) return

        if (layers.size == 1 && !selection.isFloating) {
            val layer = layers[0]
            val src = layer.pixels[index]
            val pixelColor = if (!layer.isVisible) {
                project.backgroundColor ?: 0
            } else if (AndroidColor.alpha(src) == 0) {
                project.backgroundColor ?: 0
            } else {
                src
            }
            compositedPixels[index] = pixelColor
            renderBitmap.setPixel(x, y, pixelColor)
            return
        }

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
        if (frames.isNotEmpty()) {
            frames[currentFrameIndex].layers = layers.map { it.copyLayer() }.toMutableList()
            project.frames = frames.toMutableList()
        }
        project.title = project.title
        project.layers = layers.toMutableList()
        project.selectedColor = currentColor
        project.updatedAt = System.currentTimeMillis()
    }

    /**
     * Map point (x, y) through current symmetry mode.
     * Prevents duplicate coordinates on symmetry axes.
     */
    fun getSymmetricPoints(x: Int, y: Int): List<BresenhamAlgorithms.Point> {
        if (symmetryMode == SymmetryMode.NONE) {
            return listOf(BresenhamAlgorithms.Point(x, y))
        }
        val pts = LinkedHashSet<BresenhamAlgorithms.Point>()
        pts.add(BresenhamAlgorithms.Point(x, y))
        val mirrorX = width - 1 - x
        val mirrorY = height - 1 - y

        when (symmetryMode) {
            SymmetryMode.HORIZONTAL -> {
                pts.add(BresenhamAlgorithms.Point(mirrorX, y))
            }
            SymmetryMode.VERTICAL -> {
                pts.add(BresenhamAlgorithms.Point(x, mirrorY))
            }
            SymmetryMode.QUAD -> {
                pts.add(BresenhamAlgorithms.Point(mirrorX, y))
                pts.add(BresenhamAlgorithms.Point(x, mirrorY))
                pts.add(BresenhamAlgorithms.Point(mirrorX, mirrorY))
            }
            SymmetryMode.NONE -> {}
        }
        return pts.toList()
    }

    fun onTouchDown(gridX: Int, gridY: Int) {
        if (gridX !in 0 until width || gridY !in 0 until height) return
        val currentActive = activeLayer ?: return

        when (activeTool) {
            DrawingTool.PENCIL, DrawingTool.ERASER, DrawingTool.DITHER,
            DrawingTool.SHADING_LIGHTEN, DrawingTool.SHADING_DARKEN -> {
                history.pushState(layers, activeLayerIndex, width, height, activeTool.title)
                applyStrokePixel(currentActive, gridX, gridY)
                lastStrokePoint = BresenhamAlgorithms.Point(gridX, gridY)
                renderVersion++
            }
            DrawingTool.BUCKET -> {
                history.pushState(layers, activeLayerIndex, width, height, "Bucket Fill")
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
            DrawingTool.MAGIC_WAND -> {
                val mask = BresenhamAlgorithms.magicWandSelect(
                    currentActive.pixels,
                    width,
                    height,
                    gridX,
                    gridY,
                    magicWandTolerance
                )
                selection.setMaskFromBooleans(mask)
                renderVersion++
            }
            DrawingTool.RECT_SELECT -> {
                if (selection.isFloating && selection.bounds.contains(gridX, gridY)) {
                    isDraggingSelection = true
                    selectionDragStart = BresenhamAlgorithms.Point(gridX, gridY)
                } else {
                    isDrawingShape = true
                    shapeStartPoint = BresenhamAlgorithms.Point(gridX, gridY)
                    shapeCurrentPoint = BresenhamAlgorithms.Point(gridX, gridY)
                }
            }
            DrawingTool.LASSO_SELECT -> {
                lassoPoints.clear()
                lassoPoints.add(BresenhamAlgorithms.Point(gridX, gridY))
                renderVersion++
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
            DrawingTool.PENCIL, DrawingTool.ERASER, DrawingTool.DITHER,
            DrawingTool.SHADING_LIGHTEN, DrawingTool.SHADING_DARKEN -> {
                val clampedX = gridX.coerceIn(0, width - 1)
                val clampedY = gridY.coerceIn(0, height - 1)

                val last = lastStrokePoint
                if (last != null) {
                    val points = BresenhamAlgorithms.getLinePixels(last.x, last.y, clampedX, clampedY)
                    for (pt in points) {
                        applyStrokePixel(currentActive, pt.x, pt.y)
                    }
                } else {
                    applyStrokePixel(currentActive, clampedX, clampedY)
                }
                lastStrokePoint = BresenhamAlgorithms.Point(clampedX, clampedY)
                renderVersion++
            }
            DrawingTool.RECT_SELECT -> {
                if (isDraggingSelection && selectionDragStart != null) {
                    val dx = gridX - selectionDragStart!!.x
                    val dy = gridY - selectionDragStart!!.y
                    if (dx != 0 || dy != 0) {
                        selection.move(dx, dy)
                        selectionDragStart = BresenhamAlgorithms.Point(gridX, gridY)
                        recompositeAll()
                    }
                } else if (isDrawingShape) {
                    shapeCurrentPoint = BresenhamAlgorithms.Point(
                        gridX.coerceIn(0, width - 1),
                        gridY.coerceIn(0, height - 1)
                    )
                }
            }
            DrawingTool.LASSO_SELECT -> {
                val pt = BresenhamAlgorithms.Point(gridX.coerceIn(0, width - 1), gridY.coerceIn(0, height - 1))
                if (lassoPoints.isEmpty() || lassoPoints.last() != pt) {
                    lassoPoints.add(pt)
                    renderVersion++
                }
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

        if (activeTool == DrawingTool.RECT_SELECT) {
            if (isDraggingSelection) {
                isDraggingSelection = false
                selectionDragStart = null
            } else if (isDrawingShape && shapeStartPoint != null && shapeCurrentPoint != null) {
                selection.setRect(
                    shapeStartPoint!!.x,
                    shapeStartPoint!!.y,
                    shapeCurrentPoint!!.x,
                    shapeCurrentPoint!!.y
                )
                renderVersion++
            }
        } else if (activeTool == DrawingTool.LASSO_SELECT) {
            if (lassoPoints.size >= 3) {
                selection.setLasso(lassoPoints)
                renderVersion++
            }
            lassoPoints.clear()
        } else if (isDrawingShape && shapeStartPoint != null && shapeCurrentPoint != null) {
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
                history.pushState(layers, activeLayerIndex, width, height, activeTool.title)
                for (pt in pointsToDraw) {
                    val symmetric = getSymmetricPoints(pt.x, pt.y)
                    for (spt in symmetric) {
                        if (spt.x in 0 until width && spt.y in 0 until height) {
                            if (!selection.hasActiveMask || selection.contains(spt.x, spt.y)) {
                                currentActive.setPixel(spt.x, spt.y, width, currentColor)
                                updateRenderPixel(spt.x, spt.y)
                            }
                        }
                    }
                }
                renderVersion++
                hasUnsavedChanges = true
            }
        } else if (activeTool == DrawingTool.PENCIL || activeTool == DrawingTool.ERASER ||
            activeTool == DrawingTool.DITHER || activeTool == DrawingTool.SHADING_LIGHTEN ||
            activeTool == DrawingTool.SHADING_DARKEN) {
            hasUnsavedChanges = true
        }

        isDrawingShape = false
        shapeStartPoint = null
        shapeCurrentPoint = null
        lastStrokePoint = null
    }

    private fun applyStrokePixel(layer: PixelLayer, cx: Int, cy: Int) {
        val symmetric = getSymmetricPoints(cx, cy)
        for (pt in symmetric) {
            applySinglePointWithBrush(layer, pt.x, pt.y)
        }
    }

    private fun applySinglePointWithBrush(layer: PixelLayer, cx: Int, cy: Int) {
        val radius = if (brushSize <= 1) 0 else brushSize - 1
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val px = cx + dx
                val py = cy + dy
                if (px in 0 until width && py in 0 until height) {
                    if (selection.hasActiveMask && !selection.contains(px, py)) continue

                    when (activeTool) {
                        DrawingTool.PENCIL -> {
                            if (layer.getPixel(px, py, width) != currentColor) {
                                layer.setPixel(px, py, width, currentColor)
                                updateRenderPixel(px, py)
                            }
                        }
                        DrawingTool.ERASER -> {
                            if (layer.getPixel(px, py, width) != 0) {
                                layer.setPixel(px, py, width, 0)
                                updateRenderPixel(px, py)
                            }
                        }
                        DrawingTool.DITHER -> {
                            if (ditheringPattern.matches(px, py)) {
                                if (layer.getPixel(px, py, width) != currentColor) {
                                    layer.setPixel(px, py, width, currentColor)
                                    updateRenderPixel(px, py)
                                }
                            }
                        }
                        DrawingTool.SHADING_LIGHTEN -> {
                            val curr = layer.getPixel(px, py, width)
                            val a = (curr ushr 24) and 0xFF
                            if (a > 0) {
                                val r = (((curr ushr 16) and 0xFF) * (1f + shadingStrength)).toInt().coerceIn(0, 255)
                                val g = (((curr ushr 8) and 0xFF) * (1f + shadingStrength)).toInt().coerceIn(0, 255)
                                val b = ((curr and 0xFF) * (1f + shadingStrength)).toInt().coerceIn(0, 255)
                                val newColor = (a shl 24) or (r shl 16) or (g shl 8) or b
                                layer.setPixel(px, py, width, newColor)
                                updateRenderPixel(px, py)
                            }
                        }
                        DrawingTool.SHADING_DARKEN -> {
                            val curr = layer.getPixel(px, py, width)
                            val a = (curr ushr 24) and 0xFF
                            if (a > 0) {
                                val r = (((curr ushr 16) and 0xFF) * (1f - shadingStrength)).toInt().coerceIn(0, 255)
                                val g = (((curr ushr 8) and 0xFF) * (1f - shadingStrength)).toInt().coerceIn(0, 255)
                                val b = ((curr and 0xFF) * (1f - shadingStrength)).toInt().coerceIn(0, 255)
                                val newColor = (a shl 24) or (r shl 16) or (g shl 8) or b
                                layer.setPixel(px, py, width, newColor)
                                updateRenderPixel(px, py)
                            }
                        }
                        else -> {}
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

    // Selection Transform Operations
    fun liftSelection() {
        val active = activeLayer ?: return
        if (selection.hasActiveMask && !selection.isFloating) {
            history.pushState(layers, activeLayerIndex, width, height, "Lift Selection")
            selection.lift(active)
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun flipSelectionHorizontal() {
        val active = activeLayer ?: return
        if (selection.hasActiveMask) {
            if (!selection.isFloating) liftSelection()
            selection.flipHorizontal()
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun flipSelectionVertical() {
        val active = activeLayer ?: return
        if (selection.hasActiveMask) {
            if (!selection.isFloating) liftSelection()
            selection.flipVertical()
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun rotateSelection90() {
        val active = activeLayer ?: return
        if (selection.hasActiveMask) {
            if (!selection.isFloating) liftSelection()
            selection.rotate90Clockwise()
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun rotateSelection180() {
        val active = activeLayer ?: return
        if (selection.hasActiveMask) {
            if (!selection.isFloating) liftSelection()
            selection.rotate180()
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun rotateSelection270() {
        val active = activeLayer ?: return
        if (selection.hasActiveMask) {
            if (!selection.isFloating) liftSelection()
            selection.rotate270()
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun cutSelection() {
        val active = activeLayer ?: return
        if (selection.hasActiveMask) {
            history.pushState(layers, activeLayerIndex, width, height, "Cut Selection")
            selection.cut(active)
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun copySelection() {
        val active = activeLayer ?: return
        if (selection.hasActiveMask) {
            selection.copy(active)
        }
    }

    fun pasteSelection() {
        val active = activeLayer ?: return
        if (PixelSelection.clipboard != null) {
            history.pushState(layers, activeLayerIndex, width, height, "Paste")
            selection.paste(active)
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun clearSelection() {
        val active = activeLayer ?: return
        if (selection.hasActiveMask) {
            history.pushState(layers, activeLayerIndex, width, height, "Clear Selection")
            selection.clear(active)
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun deselect() {
        val active = activeLayer ?: return
        if (selection.hasActiveMask) {
            selection.deselect(active)
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    // Replace Color Tool
    fun replaceColor(
        targetColor: Int,
        newColor: Int,
        tolerance: Int,
        allLayers: Boolean
    ): Int {
        history.pushState(layers, activeLayerIndex, width, height, "Replace Color")
        var count = 0
        if (allLayers) {
            for (layer in layers) {
                count += BresenhamAlgorithms.replaceColor(layer.pixels, targetColor, newColor, tolerance, true)
            }
        } else {
            activeLayer?.let {
                count += BresenhamAlgorithms.replaceColor(it.pixels, targetColor, newColor, tolerance, true)
            }
        }
        recompositeAll()
        hasUnsavedChanges = true
        return count
    }

    // Undo / Redo
    fun undo() {
        val snapshot = history.undo(layers, activeLayerIndex, width, height) ?: return
        layers.clear()
        layers.addAll(snapshot.layers)
        activeLayerIndex = snapshot.activeLayerIndex.coerceIn(0, maxOf(0, layers.size - 1))
        if (width != snapshot.width || height != snapshot.height) {
            width = snapshot.width
            height = snapshot.height
        }
        recompositeAll()
        hasUnsavedChanges = true
    }

    fun redo() {
        val snapshot = history.redo(layers, activeLayerIndex, width, height) ?: return
        layers.clear()
        layers.addAll(snapshot.layers)
        activeLayerIndex = snapshot.activeLayerIndex.coerceIn(0, maxOf(0, layers.size - 1))
        if (width != snapshot.width || height != snapshot.height) {
            width = snapshot.width
            height = snapshot.height
        }
        recompositeAll()
        hasUnsavedChanges = true
    }

    fun clearActiveLayer() {
        val active = activeLayer ?: return
        history.pushState(layers, activeLayerIndex, width, height, "Clear Layer")
        active.pixels.fill(0)
        recompositeAll()
        hasUnsavedChanges = true
    }

    fun clearAllLayers() {
        history.pushState(layers, activeLayerIndex, width, height, "Clear Canvas")
        for (layer in layers) {
            layer.pixels.fill(0)
        }
        recompositeAll()
        hasUnsavedChanges = true
    }

    // Layer Operations with Proper Alpha Blending
    fun addLayer(name: String = "Layer ${layers.size + 1}") {
        history.pushState(layers, activeLayerIndex, width, height, "Add Layer")
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
        history.pushState(layers, activeLayerIndex, width, height, "Duplicate Layer")
        val copy = active.copyLayer()
        layers.add(activeLayerIndex + 1, copy)
        activeLayerIndex++
        recompositeAll()
        hasUnsavedChanges = true
    }

    fun deleteActiveLayer() {
        if (layers.size <= 1) return
        history.pushState(layers, activeLayerIndex, width, height, "Delete Layer")
        layers.removeAt(activeLayerIndex)
        activeLayerIndex = activeLayerIndex.coerceIn(0, layers.size - 1)
        recompositeAll()
        hasUnsavedChanges = true
    }

    fun moveActiveLayerUp() {
        if (activeLayerIndex < layers.size - 1) {
            history.pushState(layers, activeLayerIndex, width, height, "Move Layer Up")
            val item = layers.removeAt(activeLayerIndex)
            layers.add(activeLayerIndex + 1, item)
            activeLayerIndex++
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun moveActiveLayerDown() {
        if (activeLayerIndex > 0) {
            history.pushState(layers, activeLayerIndex, width, height, "Move Layer Down")
            val item = layers.removeAt(activeLayerIndex)
            layers.add(activeLayerIndex - 1, item)
            activeLayerIndex--
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    /**
     * Fixes the alpha blending bug when merging layers down using Porter-Duff Source-Over.
     */
    fun mergeActiveLayerDown() {
        if (activeLayerIndex <= 0) return
        history.pushState(layers, activeLayerIndex, width, height, "Merge Layer Down")
        val upperLayer = layers[activeLayerIndex]
        val lowerLayer = layers[activeLayerIndex - 1]

        val upperOpacity = upperLayer.opacity
        val lowerOpacity = lowerLayer.opacity

        for (i in lowerLayer.pixels.indices) {
            val upperColor = upperLayer.pixels[i]
            val upperA = (upperColor ushr 24) and 0xFF
            if (upperA == 0) continue

            val effUpperA = (upperA * upperOpacity).toInt().coerceIn(0, 255)
            if (effUpperA == 0) continue

            val lowerColor = lowerLayer.pixels[i]
            val lowerA = (lowerColor ushr 24) and 0xFF
            val effLowerA = (lowerA * lowerOpacity).toInt().coerceIn(0, 255)

            if (effLowerA == 0) {
                val r = (upperColor ushr 16) and 0xFF
                val g = (upperColor ushr 8) and 0xFF
                val b = upperColor and 0xFF
                lowerLayer.pixels[i] = (effUpperA shl 24) or (r shl 16) or (g shl 8) or b
            } else {
                val sa = effUpperA / 255f
                val da = effLowerA / 255f
                val outA = sa + da * (1f - sa)

                val sr = (upperColor ushr 16) and 0xFF
                val sg = (upperColor ushr 8) and 0xFF
                val sb = upperColor and 0xFF

                val dr = (lowerColor ushr 16) and 0xFF
                val dg = (lowerColor ushr 8) and 0xFF
                val db = lowerColor and 0xFF

                val outR = ((sr * sa + dr * da * (1f - sa)) / outA).toInt().coerceIn(0, 255)
                val outG = ((sg * sa + dg * da * (1f - sa)) / outA).toInt().coerceIn(0, 255)
                val outB = ((sb * sa + db * da * (1f - sa)) / outA).toInt().coerceIn(0, 255)
                val finalA = (outA * 255).toInt().coerceIn(0, 255)

                lowerLayer.pixels[i] = (finalA shl 24) or (outR shl 16) or (outG shl 8) or outB
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

    // Animation Frame Operations
    fun switchFrame(targetIndex: Int) {
        if (targetIndex !in frames.indices || targetIndex == currentFrameIndex) return
        frames[currentFrameIndex].layers = layers.map { it.copyLayer() }.toMutableList()
        currentFrameIndex = targetIndex
        layers.clear()
        layers.addAll(frames[targetIndex].layers.map { it.copyLayer() })
        activeLayerIndex = activeLayerIndex.coerceIn(0, maxOf(0, layers.size - 1))
        recompositeAll()
    }

    fun addFrame() {
        frames[currentFrameIndex].layers = layers.map { it.copyLayer() }.toMutableList()
        val emptyLayers = mutableListOf(
            PixelLayer(name = "Layer 1", pixels = IntArray(width * height))
        )
        val newFrame = AnimationFrame(
            durationMs = (1000 / animationFps).coerceIn(40, 1000),
            layers = emptyLayers
        )
        frames.add(newFrame)
        switchFrame(frames.size - 1)
        hasUnsavedChanges = true
    }

    fun duplicateCurrentFrame() {
        frames[currentFrameIndex].layers = layers.map { it.copyLayer() }.toMutableList()
        val clone = frames[currentFrameIndex].cloneFrame()
        frames.add(currentFrameIndex + 1, clone)
        switchFrame(currentFrameIndex + 1)
        hasUnsavedChanges = true
    }

    fun deleteCurrentFrame() {
        if (frames.size <= 1) return
        frames.removeAt(currentFrameIndex)
        val targetIndex = currentFrameIndex.coerceIn(0, frames.size - 1)
        currentFrameIndex = -1 // Force reload
        switchFrame(targetIndex)
        hasUnsavedChanges = true
    }

    fun moveFrameLeft() {
        if (currentFrameIndex > 0) {
            frames[currentFrameIndex].layers = layers.map { it.copyLayer() }.toMutableList()
            val item = frames.removeAt(currentFrameIndex)
            frames.add(currentFrameIndex - 1, item)
            currentFrameIndex--
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun moveFrameRight() {
        if (currentFrameIndex < frames.size - 1) {
            frames[currentFrameIndex].layers = layers.map { it.copyLayer() }.toMutableList()
            val item = frames.removeAt(currentFrameIndex)
            frames.add(currentFrameIndex + 1, item)
            currentFrameIndex++
            recompositeAll()
            hasUnsavedChanges = true
        }
    }

    fun resetView() {
        zoom = 1f
        panOffset = Offset.Zero
    }
}
