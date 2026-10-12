package com.example

import com.example.drawing.BresenhamAlgorithms
import com.example.drawing.PixelCanvasState
import com.example.drawing.PixelHistory
import com.example.model.DrawingTool
import com.example.model.PixelLayer
import com.example.model.PixelProject
import com.example.data.serialization.ProjectSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelForgeUnitTests {

    @Test
    fun testBresenhamLine_horizontalAndDiagonal() {
        val lineHoriz = BresenhamAlgorithms.getLinePixels(0, 0, 4, 0)
        assertEquals(5, lineHoriz.size)
        assertEquals(BresenhamAlgorithms.Point(0, 0), lineHoriz.first())
        assertEquals(BresenhamAlgorithms.Point(4, 0), lineHoriz.last())

        val lineDiag = BresenhamAlgorithms.getLinePixels(0, 0, 3, 3)
        assertEquals(4, lineDiag.size)
        assertEquals(BresenhamAlgorithms.Point(3, 3), lineDiag.last())
    }

    @Test
    fun testBresenhamRectangle_outlineAndFilled() {
        val outline = BresenhamAlgorithms.getRectanglePixels(0, 0, 3, 3, filled = false)
        // 4x4 rectangle perimeter: 4 top + 4 bottom + 2 left + 2 right = 12 points
        assertEquals(12, outline.size)

        val filled = BresenhamAlgorithms.getRectanglePixels(0, 0, 3, 3, filled = true)
        // 4x4 filled = 16 points
        assertEquals(16, filled.size)
    }

    @Test
    fun testFloodFill_boundedArea() {
        val width = 4
        val height = 4
        val pixels = IntArray(width * height) { 0 }

        // Draw a boundary on column 2: x=2, y=0..3
        for (y in 0 until height) {
            pixels[y * width + 2] = 0xFFFFFFFF.toInt()
        }

        // Flood fill from (0,0) with color 0xFF8B5CF6
        val fillCol = 0xFF8B5CF6.toInt()
        BresenhamAlgorithms.floodFill(pixels, width, height, 0, 0, 0, fillCol)

        // Pixels on x=0 and x=1 should be filled
        assertEquals(fillCol, pixels[0 * width + 0])
        assertEquals(fillCol, pixels[1 * width + 1])

        // Boundary should still be boundary
        assertEquals(0xFFFFFFFF.toInt(), pixels[0 * width + 2])

        // Right side (x=3) should remain untouched (0)
        assertEquals(0, pixels[0 * width + 3])
    }

    @Test
    fun testUndoRedoHistory() {
        val history = PixelHistory(maxSteps = 10)
        val initialPixels = IntArray(16) { 0 }
        val layer = PixelLayer(name = "L1", pixels = initialPixels)
        val layers = mutableListOf(layer)

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)

        // Push state before mutation
        history.pushState(layers, 0, "First Stroke")
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)

        // Mutate layer
        layer.pixels[0] = 0xFF22D3EE.toInt()

        // Undo
        val undone = history.undo(layers, 0)
        assertNotNull(undone)
        assertEquals(0, undone!!.layers[0].pixels[0])
        assertTrue(history.canRedo)

        // Redo
        val redone = history.redo(undone.layers, 0)
        assertNotNull(redone)
        assertEquals(0xFF22D3EE.toInt(), redone!!.layers[0].pixels[0])
    }

    @Test
    fun testProjectSerialization_roundTrip() {
        val project = PixelProject.createEmpty(
            title = "Pixel Hero",
            width = 16,
            height = 16,
            backgroundColor = 0xFF080B18.toInt()
        )
        project.layers[0].setPixel(5, 5, 16, 0xFF8B5CF6.toInt())

        val json = ProjectSerializer.toJson(project)
        val restored = ProjectSerializer.fromJson(json)

        assertEquals(project.id, restored.id)
        assertEquals(project.title, restored.title)
        assertEquals(project.width, restored.width)
        assertEquals(project.height, restored.height)
        assertEquals(0xFF080B18.toInt(), restored.backgroundColor)
        assertEquals(0xFF8B5CF6.toInt(), restored.layers[0].getPixel(5, 5, 16))
    }

    @Test
    fun testCorruptedJsonFallback() {
        val fallback = try {
            ProjectSerializer.fromJson("{ invalid json content }")
        } catch (_: Exception) {
            null
        }
        assertNull(fallback)
    }

    @Test
    fun testCanvasState_drawingAndTools() {
        val project = PixelProject.createEmpty("Test Studio", 16, 16)
        val state = PixelCanvasState(project)

        state.activeTool = DrawingTool.PENCIL
        state.currentColor = 0xFF34D399.toInt()

        state.onTouchDown(2, 2)
        state.onTouchMove(3, 2)
        state.onTouchUp()

        assertEquals(0xFF34D399.toInt(), state.activeLayer?.getPixel(2, 2, 16))
        assertEquals(0xFF34D399.toInt(), state.activeLayer?.getPixel(3, 2, 16))
        assertTrue(state.canUndo)

        // Eraser tool
        state.activeTool = DrawingTool.ERASER
        state.onTouchDown(2, 2)
        state.onTouchUp()
        assertEquals(0, state.activeLayer?.getPixel(2, 2, 16))
    }
}
