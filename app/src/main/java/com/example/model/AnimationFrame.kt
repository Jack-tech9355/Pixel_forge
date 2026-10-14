package com.example.model

import java.util.UUID

/**
 * Represents a single animation frame containing a set of layers and a duration.
 */
data class AnimationFrame(
    val id: String = UUID.randomUUID().toString(),
    var durationMs: Int = 125, // Default ~8 FPS
    var layers: MutableList<PixelLayer> = mutableListOf()
) {
    fun cloneFrame(): AnimationFrame {
        return AnimationFrame(
            id = UUID.randomUUID().toString(),
            durationMs = durationMs,
            layers = layers.map { it.copyLayer() }.toMutableList()
        )
    }

    fun snapshot(): AnimationFrame {
        return AnimationFrame(
            id = id,
            durationMs = durationMs,
            layers = layers.map { it.snapshot() }.toMutableList()
        )
    }
}
