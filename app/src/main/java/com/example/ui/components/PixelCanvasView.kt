package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.drawing.BresenhamAlgorithms
import com.example.drawing.PixelCanvasState
import com.example.export.PngExporter
import com.example.model.DrawingTool
import com.example.model.SymmetryMode

@Composable
fun PixelCanvasView(
    state: PixelCanvasState,
    modifier: Modifier = Modifier
) {
    // Cached 1-draw-call checkerboard paint with repeating shader
    val checkerboardPaint = remember {
        val tile = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val light = 0xFF28354E.toInt()
        val dark = 0xFF1B2337.toInt()
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val isLight = ((x / 8) + (y / 8)) % 2 == 0
                tile.setPixel(x, y, if (isLight) light else dark)
            }
        }
        val shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        Paint().apply {
            this.shader = shader
            isAntiAlias = false
            isDither = false
        }
    }

    // Nearest-neighbor hardware bitmap paint
    val bitmapPaint = remember {
        Paint().apply {
            isFilterBitmap = false
            isDither = false
            isAntiAlias = false
        }
    }

    // Reference overlay paint
    val refPaint = remember {
        Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }
    }

    val srcRect = remember { Rect() }
    val dstRectF = remember { RectF() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060913))
            .testTag("pixel_canvas_viewport"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        val canvasAspect = state.width.toFloat() / state.height.toFloat()
        val containerAspect = containerWidth / containerHeight

        val baseCanvasDisplayWidth: Float
        val baseCanvasDisplayHeight: Float

        val viewportScale = if (state.isTilemapMode) 0.30f else 0.88f
        if (containerAspect > canvasAspect) {
            baseCanvasDisplayHeight = containerHeight * viewportScale
            baseCanvasDisplayWidth = baseCanvasDisplayHeight * canvasAspect
        } else {
            baseCanvasDisplayWidth = containerWidth * viewportScale
            baseCanvasDisplayHeight = baseCanvasDisplayWidth / canvasAspect
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.activeTool) {
                    if (state.activeTool == DrawingTool.MOVE) {
                        detectTransformGestures { _, pan, zoomDelta, _ ->
                            state.zoom = (state.zoom * zoomDelta).coerceIn(0.2f, 32f)
                            state.panOffset += pan
                        }
                    } else {
                        // High-performance direct gesture loop without touch slop delay
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downGrid = screenToGrid(
                                screenOffset = down.position,
                                containerWidth = containerWidth,
                                containerHeight = containerHeight,
                                canvasDisplayWidth = baseCanvasDisplayWidth * state.zoom,
                                canvasDisplayHeight = baseCanvasDisplayHeight * state.zoom,
                                panOffset = state.panOffset,
                                gridWidth = state.width,
                                gridHeight = state.height
                            )
                            if (downGrid != null) {
                                state.onTouchDown(downGrid.x, downGrid.y)
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    state.onTouchUp()
                                    break
                                }
                                change.consume()
                                val moveGrid = screenToGrid(
                                    screenOffset = change.position,
                                    containerWidth = containerWidth,
                                    containerHeight = containerHeight,
                                    canvasDisplayWidth = baseCanvasDisplayWidth * state.zoom,
                                    canvasDisplayHeight = baseCanvasDisplayHeight * state.zoom,
                                    panOffset = state.panOffset,
                                    gridWidth = state.width,
                                    gridHeight = state.height
                                )
                                if (moveGrid != null) {
                                    state.onTouchMove(moveGrid.x, moveGrid.y)
                                }
                            }
                        }
                    }
                }
        ) {
            @Suppress("UNUSED_VARIABLE")
            val version = state.renderVersion

            val currentDisplayW = baseCanvasDisplayWidth * state.zoom
            val currentDisplayH = baseCanvasDisplayHeight * state.zoom

            val canvasLeft = (containerWidth - currentDisplayW) / 2f + state.panOffset.x
            val canvasTop = (containerHeight - currentDisplayH) / 2f + state.panOffset.y

            // Feature I: Tilemap 3x3 Seamless Repeating Mode
            if (state.isTilemapMode) {
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val tileLeft = canvasLeft + dx * currentDisplayW
                        val tileTop = canvasTop + dy * currentDisplayH

                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawRect(
                                tileLeft,
                                tileTop,
                                tileLeft + currentDisplayW,
                                tileTop + currentDisplayH,
                                checkerboardPaint
                            )
                            srcRect.set(0, 0, state.width, state.height)
                            dstRectF.set(tileLeft, tileTop, tileLeft + currentDisplayW, tileTop + currentDisplayH)
                            canvas.nativeCanvas.drawBitmap(state.renderBitmap, srcRect, dstRectF, bitmapPaint)
                        }

                        // Boundary for neighbor tiles
                        if (dx != 0 || dy != 0) {
                            drawRect(
                                color = Color.White.copy(alpha = 0.1f),
                                topLeft = Offset(tileLeft, tileTop),
                                size = Size(currentDisplayW, currentDisplayH),
                                style = Stroke(width = 1f)
                            )
                        }
                    }
                }
            } else {
                // 1. Single GPU call checkerboard background
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawRect(
                        canvasLeft,
                        canvasTop,
                        canvasLeft + currentDisplayW,
                        canvasTop + currentDisplayH,
                        checkerboardPaint
                    )

                    // Feature H: Onion Skinning (Previous Frame in faint cyan, Next Frame in faint orange)
                    if (state.isOnionSkinEnabled && state.frames.size > 1) {
                        // Previous frame
                        if (state.currentFrameIndex > 0) {
                            val prevFrame = state.frames[state.currentFrameIndex - 1]
                            val prevProj = state.project.copy(layers = prevFrame.layers)
                            val prevBitmap = PngExporter.compositeToBitmap(prevProj, false)
                            val prevPaint = Paint().apply {
                                alpha = (state.onionSkinOpacity * 255).toInt().coerceIn(0, 255)
                                isFilterBitmap = false
                            }
                            srcRect.set(0, 0, prevBitmap.width, prevBitmap.height)
                            dstRectF.set(canvasLeft, canvasTop, canvasLeft + currentDisplayW, canvasTop + currentDisplayH)
                            canvas.nativeCanvas.drawBitmap(prevBitmap, srcRect, dstRectF, prevPaint)
                        }

                        // Next frame
                        if (state.currentFrameIndex < state.frames.size - 1) {
                            val nextFrame = state.frames[state.currentFrameIndex + 1]
                            val nextProj = state.project.copy(layers = nextFrame.layers)
                            val nextBitmap = PngExporter.compositeToBitmap(nextProj, false)
                            val nextPaint = Paint().apply {
                                alpha = (state.onionSkinOpacity * 255).toInt().coerceIn(0, 255)
                                isFilterBitmap = false
                            }
                            srcRect.set(0, 0, nextBitmap.width, nextBitmap.height)
                            dstRectF.set(canvasLeft, canvasTop, canvasLeft + currentDisplayW, canvasTop + currentDisplayH)
                            canvas.nativeCanvas.drawBitmap(nextBitmap, srcRect, dstRectF, nextPaint)
                        }
                    }

                    // 2. Direct hardware blit with nearest-neighbor sampling
                    srcRect.set(0, 0, state.width, state.height)
                    dstRectF.set(
                        canvasLeft,
                        canvasTop,
                        canvasLeft + currentDisplayW,
                        canvasTop + currentDisplayH
                    )
                    canvas.nativeCanvas.drawBitmap(state.renderBitmap, srcRect, dstRectF, bitmapPaint)

                    // Feature F: Non-destructive Reference Image Overlay
                    if (state.isReferenceVisible && state.referenceBitmap != null) {
                        val refBmp = state.referenceBitmap!!
                        refPaint.alpha = (state.referenceOpacity * 255).toInt().coerceIn(0, 255)
                        val refW = currentDisplayW * state.referenceScale
                        val refH = currentDisplayH * state.referenceScale
                        val refLeft = canvasLeft + state.referenceOffset.x
                        val refTop = canvasTop + state.referenceOffset.y

                        srcRect.set(0, 0, refBmp.width, refBmp.height)
                        dstRectF.set(refLeft, refTop, refLeft + refW, refTop + refH)
                        canvas.nativeCanvas.drawBitmap(refBmp, srcRect, dstRectF, refPaint)
                    }
                }
            }

            val pixelSizeX = currentDisplayW / state.width
            val pixelSizeY = currentDisplayH / state.height

            // 3. Draw in-progress live shape preview (including Rect selection box)
            if (state.isDrawingShape && state.shapeStartPoint != null && state.shapeCurrentPoint != null) {
                if (state.activeTool == DrawingTool.RECT_SELECT) {
                    val s = state.shapeStartPoint!!
                    val e = state.shapeCurrentPoint!!
                    val minX = minOf(s.x, e.x)
                    val maxX = maxOf(s.x, e.x)
                    val minY = minOf(s.y, e.y)
                    val maxY = maxOf(s.y, e.y)

                    drawRect(
                        color = Color(0xFF38BDF8),
                        topLeft = Offset(canvasLeft + minX * pixelSizeX, canvasTop + minY * pixelSizeY),
                        size = Size((maxX - minX + 1) * pixelSizeX, (maxY - minY + 1) * pixelSizeY),
                        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
                    )
                } else {
                    drawShapePreview(
                        state = state,
                        canvasLeft = canvasLeft,
                        canvasTop = canvasTop,
                        pixelSizeX = pixelSizeX,
                        pixelSizeY = pixelSizeY
                    )
                }
            }

            // Feature B: Lasso points live path
            if (state.activeTool == DrawingTool.LASSO_SELECT && state.lassoPoints.size > 1) {
                for (i in 0 until state.lassoPoints.size - 1) {
                    val p1 = state.lassoPoints[i]
                    val p2 = state.lassoPoints[i + 1]
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(canvasLeft + (p1.x + 0.5f) * pixelSizeX, canvasTop + (p1.y + 0.5f) * pixelSizeY),
                        end = Offset(canvasLeft + (p2.x + 0.5f) * pixelSizeX, canvasTop + (p2.y + 0.5f) * pixelSizeY),
                        strokeWidth = 2f
                    )
                }
            }

            // Feature B: Render active selection borders / floating bounding box
            if (state.selection.hasActiveMask) {
                val b = state.selection.bounds
                val selLeft = canvasLeft + b.left * pixelSizeX
                val selTop = canvasTop + b.top * pixelSizeY
                val selW = b.width() * pixelSizeX
                val selH = b.height() * pixelSizeY

                drawRect(
                    color = Color(0xFFFACC15),
                    topLeft = Offset(selLeft, selTop),
                    size = Size(selW, selH),
                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
                )
            }

            // 4. Pixel grid lines
            if (state.isGridVisible && pixelSizeX >= 6f) {
                drawGridLines(
                    topLeft = Offset(canvasLeft, canvasTop),
                    width = currentDisplayW,
                    height = currentDisplayH,
                    cols = state.width,
                    rows = state.height
                )
            }

            // Feature A: Symmetry Axis Indicators
            if (state.symmetryMode != SymmetryMode.NONE) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                val axisColor = Color(0xFF22D3EE).copy(alpha = 0.85f)

                if (state.symmetryMode == SymmetryMode.HORIZONTAL || state.symmetryMode == SymmetryMode.QUAD) {
                    val midX = canvasLeft + currentDisplayW / 2f
                    drawLine(
                        color = axisColor,
                        start = Offset(midX, canvasTop - 8f),
                        end = Offset(midX, canvasTop + currentDisplayH + 8f),
                        strokeWidth = 2f,
                        pathEffect = dash
                    )
                }
                if (state.symmetryMode == SymmetryMode.VERTICAL || state.symmetryMode == SymmetryMode.QUAD) {
                    val midY = canvasTop + currentDisplayH / 2f
                    drawLine(
                        color = axisColor,
                        start = Offset(canvasLeft - 8f, midY),
                        end = Offset(canvasLeft + currentDisplayW + 8f, midY),
                        strokeWidth = 2f,
                        pathEffect = dash
                    )
                }
            }

            // 5. Crisp neon outline around main artwork canvas
            drawRect(
                color = if (state.isTilemapMode) Color(0xFF22D3EE) else Color(0xFF8B5CF6).copy(alpha = 0.6f),
                topLeft = Offset(canvasLeft - 1f, canvasTop - 1f),
                size = Size(currentDisplayW + 2f, currentDisplayH + 2f),
                style = Stroke(width = if (state.isTilemapMode) 2.5f else 2f)
            )
        }
    }
}

private fun screenToGrid(
    screenOffset: Offset,
    containerWidth: Float,
    containerHeight: Float,
    canvasDisplayWidth: Float,
    canvasDisplayHeight: Float,
    panOffset: Offset,
    gridWidth: Int,
    gridHeight: Int
): BresenhamAlgorithms.Point? {
    val canvasLeft = (containerWidth - canvasDisplayWidth) / 2f + panOffset.x
    val canvasTop = (containerHeight - canvasDisplayHeight) / 2f + panOffset.y

    val localX = screenOffset.x - canvasLeft
    val localY = screenOffset.y - canvasTop

    if (localX < 0 || localX >= canvasDisplayWidth || localY < 0 || localY >= canvasDisplayHeight) {
        return null
    }

    val gridX = (localX / canvasDisplayWidth * gridWidth).toInt().coerceIn(0, gridWidth - 1)
    val gridY = (localY / canvasDisplayHeight * gridHeight).toInt().coerceIn(0, gridHeight - 1)

    return BresenhamAlgorithms.Point(gridX, gridY)
}

private fun DrawScope.drawGridLines(
    topLeft: Offset,
    width: Float,
    height: Float,
    cols: Int,
    rows: Int
) {
    val colStep = width / cols
    val rowStep = height / rows
    val gridColor = Color(0xFFFFFFFF).copy(alpha = 0.15f)

    for (c in 0..cols) {
        val x = topLeft.x + c * colStep
        drawLine(
            color = gridColor,
            start = Offset(x, topLeft.y),
            end = Offset(x, topLeft.y + height),
            strokeWidth = 1f
        )
    }

    for (r in 0..rows) {
        val y = topLeft.y + r * rowStep
        drawLine(
            color = gridColor,
            start = Offset(topLeft.x, y),
            end = Offset(topLeft.x + width, y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawShapePreview(
    state: PixelCanvasState,
    canvasLeft: Float,
    canvasTop: Float,
    pixelSizeX: Float,
    pixelSizeY: Float
) {
    val start = state.shapeStartPoint ?: return
    val current = state.shapeCurrentPoint ?: return

    val previewPoints = when (state.activeTool) {
        DrawingTool.LINE -> BresenhamAlgorithms.getLinePixels(start.x, start.y, current.x, current.y)
        DrawingTool.RECTANGLE -> BresenhamAlgorithms.getRectanglePixels(start.x, start.y, current.x, current.y, false)
        DrawingTool.FILLED_RECTANGLE -> BresenhamAlgorithms.getRectanglePixels(start.x, start.y, current.x, current.y, true)
        DrawingTool.CIRCLE -> BresenhamAlgorithms.getCirclePixels(start.x, start.y, current.x, current.y, false)
        DrawingTool.FILLED_CIRCLE -> BresenhamAlgorithms.getCirclePixels(start.x, start.y, current.x, current.y, true)
        else -> emptyList()
    }

    val previewColor = Color(state.currentColor).copy(alpha = 0.85f)
    for (pt in previewPoints) {
        val symmetric = state.getSymmetricPoints(pt.x, pt.y)
        for (spt in symmetric) {
            if (spt.x in 0 until state.width && spt.y in 0 until state.height) {
                drawRect(
                    color = previewColor,
                    topLeft = Offset(canvasLeft + spt.x * pixelSizeX, canvasTop + spt.y * pixelSizeY),
                    size = Size(pixelSizeX, pixelSizeY)
                )
            }
        }
    }
}
