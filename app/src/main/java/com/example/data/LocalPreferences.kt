package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pixelforge_settings")

class LocalPreferences(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "DARK", "LIGHT", "SYSTEM"
        val KEY_GRID_VISIBLE = booleanPreferencesKey("grid_visible")
        val KEY_DEFAULT_CANVAS_SIZE = intPreferencesKey("default_canvas_size") // 16, 32, 64
        val KEY_DEFAULT_EXPORT_SCALE = intPreferencesKey("default_export_scale") // 1, 2, 4, 8, 16, 32
        val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val KEY_AUTOSAVE_ENABLED = booleanPreferencesKey("autosave_enabled")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "DARK"
    }

    val gridVisibleDefault: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_GRID_VISIBLE] ?: true
    }

    val defaultCanvasSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_CANVAS_SIZE] ?: 32
    }

    val defaultExportScale: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_EXPORT_SCALE] ?: 8
    }

    val hapticFeedbackEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAPTIC_FEEDBACK] ?: true
    }

    val autosaveEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTOSAVE_ENABLED] ?: true
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setGridVisible(visible: Boolean) {
        context.dataStore.edit { it[KEY_GRID_VISIBLE] = visible }
    }

    suspend fun setDefaultCanvasSize(size: Int) {
        context.dataStore.edit { it[KEY_DEFAULT_CANVAS_SIZE] = size }
    }

    suspend fun setDefaultExportScale(scale: Int) {
        context.dataStore.edit { it[KEY_DEFAULT_EXPORT_SCALE] = scale }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setAutosaveEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTOSAVE_ENABLED] = enabled }
    }
}
