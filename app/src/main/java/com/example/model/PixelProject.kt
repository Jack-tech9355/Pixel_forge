package com.example.model

import java.util.UUID

data class PixelProject(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "Untitled Artwork",
    val width: Int = 32,
    val height: Int = 32,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var layers: MutableList<PixelLayer> = mutableListOf(),
    var selectedColor: Int = 0xFF8B5CF6.toInt(),
    var paletteId: String = "classic_8bit",
    var backgroundColor: Int? = null,
    var previewBase64: String? = null,
    var frames: MutableList<AnimationFrame> = mutableListOf()
) {
    companion object {
        fun createEmpty(
            title: String = "New Canvas",
            width: Int = 32,
            height: Int = 32,
            backgroundColor: Int? = null
        ): PixelProject {
            val basePixels = IntArray(width * height) { backgroundColor ?: 0 }
            val firstLayer = PixelLayer(
                name = "Layer 1",
                pixels = basePixels
            )
            val firstFrame = AnimationFrame(
                durationMs = 125,
                layers = mutableListOf(firstLayer.snapshot())
            )
            return PixelProject(
                title = title,
                width = width,
                height = height,
                layers = mutableListOf(firstLayer),
                backgroundColor = backgroundColor,
                frames = mutableListOf(firstFrame)
            )
        }
    }
}
