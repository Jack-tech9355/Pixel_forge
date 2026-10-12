package com.example.model

import java.util.UUID

/**
 * Represents a single drawing layer in the pixel art canvas.
 * Pixels are stored as ARGB 32-bit integers in row-major order: index = y * width + x.
 */
data class PixelLayer(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Layer 1",
    var isVisible: Boolean = true,
    var opacity: Float = 1.0f,
    val pixels: IntArray
) {
    fun copyLayer(): PixelLayer {
        return PixelLayer(
            id = UUID.randomUUID().toString(),
            name = "$name Copy",
            isVisible = isVisible,
            opacity = opacity,
            pixels = pixels.clone()
        )
    }

    fun snapshot(): PixelLayer {
        return PixelLayer(
            id = id,
            name = name,
            isVisible = isVisible,
            opacity = opacity,
            pixels = pixels.clone()
        )
    }

    fun getPixel(x: Int, y: Int, width: Int): Int {
        val index = y * width + x
        if (index in pixels.indices) {
            return pixels[index]
        }
        return 0
    }

    fun setPixel(x: Int, y: Int, width: Int, color: Int) {
        val index = y * width + x
        if (index in pixels.indices) {
            pixels[index] = color
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PixelLayer

        if (id != other.id) return false
        if (name != other.name) return false
        if (isVisible != other.isVisible) return false
        if (opacity != other.opacity) return false
        if (!pixels.contentEquals(other.pixels)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + isVisible.hashCode()
        result = 31 * result + opacity.hashCode()
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}
