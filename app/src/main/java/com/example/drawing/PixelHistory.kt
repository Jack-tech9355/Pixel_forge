package com.example.drawing

import com.example.model.PixelLayer

/**
 * Stores canvas layer snapshots for fast, robust undo/redo.
 */
class PixelHistory(private val maxSteps: Int = 30) {

    data class HistorySnapshot(
        val layers: List<PixelLayer>,
        val activeLayerIndex: Int,
        val description: String = ""
    )

    private val undoStack = java.util.ArrayDeque<HistorySnapshot>()
    private val redoStack = java.util.ArrayDeque<HistorySnapshot>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size

    /**
     * Saves current state onto undo stack before a mutation is applied.
     * Clears redo stack since a new action branch is started.
     */
    fun pushState(layers: List<PixelLayer>, activeLayerIndex: Int, description: String = "") {
        val snapshotLayers = layers.map { it.snapshot() }
        if (undoStack.size >= maxSteps) {
            undoStack.removeLast()
        }
        undoStack.push(HistorySnapshot(snapshotLayers, activeLayerIndex, description))
        redoStack.clear()
    }

    /**
     * Reverts to the previous snapshot, saving current state onto redo stack.
     */
    fun undo(
        currentLayers: List<PixelLayer>,
        currentActiveIndex: Int
    ): HistorySnapshot? {
        if (undoStack.isEmpty()) return null
        val currentSnapshot = HistorySnapshot(
            currentLayers.map { it.snapshot() },
            currentActiveIndex,
            "Current before undo"
        )
        redoStack.push(currentSnapshot)
        return undoStack.pop()
    }

    /**
     * Re-applies the undone snapshot, saving current state onto undo stack.
     */
    fun redo(
        currentLayers: List<PixelLayer>,
        currentActiveIndex: Int
    ): HistorySnapshot? {
        if (redoStack.isEmpty()) return null
        val currentSnapshot = HistorySnapshot(
            currentLayers.map { it.snapshot() },
            currentActiveIndex,
            "Current before redo"
        )
        undoStack.push(currentSnapshot)
        return redoStack.pop()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
