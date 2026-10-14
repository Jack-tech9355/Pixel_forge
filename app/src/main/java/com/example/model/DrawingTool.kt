package com.example.model

enum class DrawingTool(
    val title: String,
    val description: String
) {
    PENCIL("Pencil", "Draw individual pixels or continuous freehand strokes"),
    ERASER("Eraser", "Erase pixels back to full transparency"),
    DITHER("Dither Brush", "Draw deterministic checkerboard or ordered dither patterns"),
    SHADING_LIGHTEN("Lighten", "Increase brightness of existing painted pixels"),
    SHADING_DARKEN("Darken", "Decrease brightness of existing painted pixels"),
    BUCKET("Bucket Fill", "Flood fill connected contiguous matching pixels"),
    MAGIC_WAND("Magic Wand", "Select connected pixels with configurable tolerance"),
    RECT_SELECT("Box Select", "Select rectangular region to move, flip, rotate, copy"),
    LASSO_SELECT("Lasso Select", "Freehand lasso region selection"),
    EYEDROPPER("Eyedropper", "Pick color directly from canvas"),
    LINE("Line", "Draw crisp straight Bresenham pixel line"),
    RECTANGLE("Rectangle", "Draw pixel rectangle outline"),
    FILLED_RECTANGLE("Filled Rect", "Draw solid filled pixel rectangle"),
    CIRCLE("Circle", "Draw pixel circle/ellipse outline"),
    FILLED_CIRCLE("Filled Circle", "Draw solid filled pixel circle"),
    MOVE("Pan & Zoom", "Navigate and pan the canvas view")
}
