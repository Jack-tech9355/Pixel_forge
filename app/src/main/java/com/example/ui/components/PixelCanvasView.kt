package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.drawing.BresenhamAlgorithms
import com.example.drawing.PixelCanvasState
import com.example.export.PngExporter
import com.example.model.DrawingTool

@Composable
fun PixelCanvasView(
    state: PixelCanvasState,
    modifier: Modifier = Modifier
) {
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

        if (containerAspect > canvasAspect) {
            baseCanvasDisplayHeight = containerHeight * 0.85f
            baseCanvasDisplayWidth = baseCanvasDisplayHeight * canvasAspect
        } else {
            baseCanvasDisplayWidth = containerWidth * 0.85f
            baseCanvasDisplayHeight = baseCanvasDisplayWidth / canvasAspect
        }

        // Render buffer bitmap
        val compositedBitmap = remember(
            state.width,
            state.height,
            state.layers.map { it.hashCode() },
            state.activeLayer?.pixels?.hashCode(),
            state.hasUnsavedChanges
        ) {
            state.syncBackToProject()
            PngExporter.compositeToBitmap(state.project, includeBackground = true)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.activeTool) {
                    if (state.activeTool == DrawingTool.MOVE) {
                        detectTransformGestures { _, pan, zoomDelta, _ ->
                            state.zoom = (state.zoom * zoomDelta).coerceIn(0.5f, 32f)
                            state.panOffset += pan
                        }
                    } else {
                        // Drawing gestures with coordinate transformation
                        detectDragGestures(
                            onDragStart = { offset ->
                                val gridPoint = screenToGrid(
                                    screenOffset = offset,
                                    containerWidth = containerWidth,
                                    containerHeight = containerHeight,
                                    canvasDisplayWidth = baseCanvasDisplayWidth * state.zoom,
                                    canvasDisplayHeight = baseCanvasDisplayHeight * state.zoom,
                                    panOffset = state.panOffset,
                                    gridWidth = state.width,
                                    gridHeight = state.height
                                )
                                if (gridPoint != null) {
                                    state.onTouchDown(gridPoint.x, gridPoint.y)
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val gridPoint = screenToGrid(
                                    screenOffset = change.position,
                                    containerWidth = containerWidth,
                                    containerHeight = containerHeight,
                                    canvasDisplayWidth = baseCanvasDisplayWidth * state.zoom,
                                    canvasDisplayHeight = baseCanvasDisplayHeight * state.zoom,
                                    panOffset = state.panOffset,
                                    gridWidth = state.width,
                                    gridHeight = state.height
                                )
                                if (gridPoint != null) {
                                    state.onTouchMove(gridPoint.x, gridPoint.y)
                                }
                            },
                            onDragEnd = {
                                state.onTouchUp()
                            },
                            onDragCancel = {
                                state.onTouchUp()
                            }
                        )
                    }
                }
        ) {
            val currentDisplayW = baseCanvasDisplayWidth * state.zoom
            val currentDisplayH = baseCanvasDisplayHeight * state.zoom

            val canvasLeft = (containerWidth - currentDisplayW) / 2f + state.panOffset.x
            val canvasTop = (containerHeight - currentDisplayH) / 2f + state.panOffset.y

            // 1. Draw Checkerboard pattern behind canvas
            drawCheckerboard(
                topLeft = Offset(canvasLeft, canvasTop),
                size = Size(currentDisplayW, currentDisplayH),
                gridCols = state.width,
                gridRows = state.height
            )

            // 2. Draw crisp pixel bitmap with FilterQuality.None
            drawImage(
                image = compositedBitmap.asImageBitmap(),
                dstOffset = androidx.compose.ui.unit.IntOffset(canvasLeft.toInt(), canvasTop.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(currentDisplayW.toInt(), currentDisplayH.toInt()),
                filterQuality = FilterQuality.None
            )

            // 3. Draw in-progress shape preview
            if (state.isDrawingShape && state.shapeStartPoint != null && state.shapeCurrentPoint != null) {
                drawShapePreview(
                    state = state,
                    canvasLeft = canvasLeft,
                    canvasTop = canvasTop,
                    pixelSizeX = currentDisplayW / state.width,
                    pixelSizeY = currentDisplayH / state.height
                )
            }

            // 4. Draw crisp grid lines if enabled and sufficiently zoomed in
            if (state.isGridVisible && currentDisplayW >= 120f) {
                drawGridLines(
                    topLeft = Offset(canvasLeft, canvasTop),
                    width = currentDisplayW,
                    height = currentDisplayH,
                    cols = state.width,
                    rows = state.height
                )
            }

            // 5. Border around canvas
            drawRect(
                color = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                topLeft = Offset(canvasLeft - 1f, canvasTop - 1f),
                size = Size(currentDisplayW + 2f, currentDisplayH + 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
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

private fun DrawScope.drawCheckerboard(
    topLeft: Offset,
    size: Size,
    gridCols: Int,
    gridRows: Int
) {
    val colSize = size.width / gridCols
    val rowSize = size.height / gridRows
    val lightCheck = Color(0xFF28354E)
    val darkCheck = Color(0xFF1B2337)

    for (r in 0 until gridRows) {
        for (c in 0 until gridCols) {
            val color = if ((r + c) % 2 == 0) lightCheck else darkCheck
            drawRect(
                color = color,
                topLeft = Offset(topLeft.x + c * colSize, topLeft.y + r * rowSize),
                size = Size(colSize + 0.5f, rowSize + 0.5f)
            )
        }
    }
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
        if (pt.x in 0 until state.width && pt.y in 0 until state.height) {
            drawRect(
                color = previewColor,
                topLeft = Offset(canvasLeft + pt.x * pixelSizeX, canvasTop + pt.y * pixelSizeY),
                size = Size(pixelSizeX, pixelSizeY)
            )
        }
    }
}
