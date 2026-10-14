package com.example.data.serialization

import com.example.model.AnimationFrame
import com.example.model.PixelLayer
import com.example.model.PixelProject
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ProjectSerializer {

    const val CURRENT_FORMAT_VERSION = 2

    fun toJson(project: PixelProject): String {
        val root = JSONObject()
        root.put("formatVersion", CURRENT_FORMAT_VERSION)
        root.put("id", project.id)
        root.put("title", project.title)
        root.put("width", project.width)
        root.put("height", project.height)
        root.put("createdAt", project.createdAt)
        root.put("updatedAt", project.updatedAt)
        root.put("selectedColor", project.selectedColor)
        root.put("paletteId", project.paletteId)
        if (project.backgroundColor != null) {
            root.put("backgroundColor", project.backgroundColor!!)
        }

        // Active layers
        val layersArray = serializeLayers(project.layers)
        root.put("layers", layersArray)

        // Animation frames
        val framesArray = JSONArray()
        for (frame in project.frames) {
            val frameObj = JSONObject()
            frameObj.put("id", frame.id)
            frameObj.put("durationMs", frame.durationMs)
            frameObj.put("layers", serializeLayers(frame.layers))
            framesArray.put(frameObj)
        }
        root.put("frames", framesArray)

        return root.toString(2)
    }

    private fun serializeLayers(layers: List<PixelLayer>): JSONArray {
        val layersArray = JSONArray()
        for (layer in layers) {
            val layerObj = JSONObject()
            layerObj.put("id", layer.id)
            layerObj.put("name", layer.name)
            layerObj.put("isVisible", layer.isVisible)
            layerObj.put("opacity", layer.opacity.toDouble())

            val pixelsArray = JSONArray()
            for (pixel in layer.pixels) {
                pixelsArray.put(pixel)
            }
            layerObj.put("pixels", pixelsArray)
            layersArray.put(layerObj)
        }
        return layersArray
    }

    fun fromJson(jsonStr: String): PixelProject {
        val root = JSONObject(jsonStr)
        val id = root.optString("id")
        val title = root.optString("title", "Untitled")
        val width = root.optInt("width", 32)
        val height = root.optInt("height", 32)
        val createdAt = root.optLong("createdAt", System.currentTimeMillis())
        val updatedAt = root.optLong("updatedAt", System.currentTimeMillis())
        val selectedColor = root.optInt("selectedColor", 0xFF8B5CF6.toInt())
        val paletteId = root.optString("paletteId", "classic_8bit")
        val backgroundColor = if (root.has("backgroundColor")) root.getInt("backgroundColor") else null

        val layers = deserializeLayers(root.optJSONArray("layers"), width, height)

        val frames = mutableListOf<AnimationFrame>()
        val framesArray = root.optJSONArray("frames")
        if (framesArray != null && framesArray.length() > 0) {
            for (f in 0 until framesArray.length()) {
                val frameObj = framesArray.getJSONObject(f)
                val frameId = frameObj.optString("id")
                val durationMs = frameObj.optInt("durationMs", 125)
                val frameLayers = deserializeLayers(frameObj.optJSONArray("layers"), width, height)
                frames.add(
                    AnimationFrame(
                        id = frameId,
                        durationMs = durationMs,
                        layers = frameLayers
                    )
                )
            }
        } else {
            // Backward compatibility: create initial frame from active layers
            frames.add(
                AnimationFrame(
                    durationMs = 125,
                    layers = layers.map { it.snapshot() }.toMutableList()
                )
            )
        }

        return PixelProject(
            id = id,
            title = title,
            width = width,
            height = height,
            createdAt = createdAt,
            updatedAt = updatedAt,
            layers = layers,
            selectedColor = selectedColor,
            paletteId = paletteId,
            backgroundColor = backgroundColor,
            frames = frames
        )
    }

    private fun deserializeLayers(layersArray: JSONArray?, width: Int, height: Int): MutableList<PixelLayer> {
        val layers = mutableListOf<PixelLayer>()
        if (layersArray != null) {
            for (i in 0 until layersArray.length()) {
                val layerObj = layersArray.getJSONObject(i)
                val layerId = layerObj.optString("id")
                val layerName = layerObj.optString("name", "Layer ${i + 1}")
                val isVisible = layerObj.optBoolean("isVisible", true)
                val opacity = layerObj.optDouble("opacity", 1.0).toFloat()

                val pixelsArray = layerObj.optJSONArray("pixels")
                val totalPixels = width * height
                val pixels = IntArray(totalPixels)
                if (pixelsArray != null) {
                    for (p in 0 until minOf(totalPixels, pixelsArray.length())) {
                        pixels[p] = pixelsArray.getInt(p)
                    }
                }
                layers.add(
                    PixelLayer(
                        id = layerId,
                        name = layerName,
                        isVisible = isVisible,
                        opacity = opacity,
                        pixels = pixels
                    )
                )
            }
        }
        if (layers.isEmpty()) {
            layers.add(PixelLayer(name = "Layer 1", pixels = IntArray(width * height)))
        }
        return layers
    }

    /**
     * Atomically writes project JSON to destination file using temp file rename.
     */
    fun saveToFileAtomic(project: PixelProject, targetFile: File): Boolean {
        val parent = targetFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        val tempFile = File(parent, "${targetFile.name}.tmp")
        return try {
            val json = toJson(project)
            tempFile.writeText(json)
            if (tempFile.renameTo(targetFile)) {
                true
            } else {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
            false
        }
    }

    fun loadFromFile(targetFile: File): PixelProject? {
        return try {
            if (!targetFile.exists()) return null
            val json = targetFile.readText()
            fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
