package com.example.data.repository

import android.content.Context
import com.example.data.BuiltInPalettes
import com.example.model.PixelPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class PaletteRepository(private val context: Context) {

    private val customPalettesFile = File(context.filesDir, "custom_palettes.json")
    private val _palettesFlow = MutableStateFlow<List<PixelPalette>>(emptyList())
    val palettesFlow: StateFlow<List<PixelPalette>> = _palettesFlow.asStateFlow()

    init {
        loadPalettes()
    }

    private fun loadPalettes() {
        val customList = mutableListOf<PixelPalette>()
        try {
            if (customPalettesFile.exists()) {
                val jsonStr = customPalettesFile.readText()
                val jsonArr = JSONArray(jsonStr)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val id = obj.optString("id", UUID.randomUUID().toString())
                    val name = obj.optString("name", "Custom Palette")
                    val colorsArr = obj.getJSONArray("colors")
                    val colors = mutableListOf<Int>()
                    for (c in 0 until colorsArr.length()) {
                        colors.add(colorsArr.getInt(c))
                    }
                    customList.add(
                        PixelPalette(
                            id = id,
                            name = name,
                            isCustom = true,
                            colors = colors
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _palettesFlow.value = BuiltInPalettes.ALL_PRESETS + customList
    }

    private suspend fun persistCustomPalettes(customList: List<PixelPalette>) = withContext(Dispatchers.IO) {
        try {
            val jsonArr = JSONArray()
            for (pal in customList) {
                val obj = JSONObject()
                obj.put("id", pal.id)
                obj.put("name", pal.name)
                val colorsArr = JSONArray()
                for (c in pal.colors) {
                    colorsArr.put(c)
                }
                obj.put("colors", colorsArr)
                jsonArr.put(obj)
            }
            customPalettesFile.writeText(jsonArr.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveCustomPalette(name: String, colors: List<Int>): PixelPalette = withContext(Dispatchers.IO) {
        val newPal = PixelPalette(
            id = "custom_${UUID.randomUUID()}",
            name = name.trim().ifEmpty { "My Palette" },
            isCustom = true,
            colors = colors
        )
        val currentCustom = _palettesFlow.value.filter { it.isCustom }.toMutableList()
        currentCustom.add(newPal)
        persistCustomPalettes(currentCustom)
        _palettesFlow.value = BuiltInPalettes.ALL_PRESETS + currentCustom
        newPal
    }

    suspend fun updateCustomPalette(palette: PixelPalette) = withContext(Dispatchers.IO) {
        val currentCustom = _palettesFlow.value.filter { it.isCustom }.toMutableList()
        val index = currentCustom.indexOfFirst { it.id == palette.id }
        if (index >= 0) {
            currentCustom[index] = palette.copy(isCustom = true)
            persistCustomPalettes(currentCustom)
            _palettesFlow.value = BuiltInPalettes.ALL_PRESETS + currentCustom
        }
    }

    suspend fun deleteCustomPalette(id: String) = withContext(Dispatchers.IO) {
        val currentCustom = _palettesFlow.value.filter { it.isCustom && it.id != id }
        persistCustomPalettes(currentCustom)
        _palettesFlow.value = BuiltInPalettes.ALL_PRESETS + currentCustom
    }

    suspend fun addColorToPalette(paletteId: String, color: Int): PixelPalette? = withContext(Dispatchers.IO) {
        val pal = _palettesFlow.value.find { it.id == paletteId } ?: return@withContext null
        if (!pal.isCustom) {
            // Fork into a new custom palette
            val newColors = pal.colors.toMutableList().apply { add(color) }
            saveCustomPalette("${pal.name} (Custom)", newColors)
        } else {
            val updated = pal.copy(colors = pal.colors + color)
            updateCustomPalette(updated)
            updated
        }
    }

    suspend fun removeColorFromPalette(paletteId: String, colorIndex: Int): PixelPalette? = withContext(Dispatchers.IO) {
        val pal = _palettesFlow.value.find { it.id == paletteId } ?: return@withContext null
        if (!pal.isCustom || colorIndex !in pal.colors.indices) return@withContext null
        val updatedColors = pal.colors.toMutableList().apply { removeAt(colorIndex) }
        val updated = pal.copy(colors = updatedColors)
        updateCustomPalette(updated)
        updated
    }

    fun getPaletteById(id: String): PixelPalette {
        return _palettesFlow.value.find { it.id == id } ?: BuiltInPalettes.CYBER_NEON
    }
}
