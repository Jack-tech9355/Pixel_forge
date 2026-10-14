package com.example.model

enum class SymmetryMode(
    val title: String,
    val description: String
) {
    NONE("Off", "Normal single stroke drawing"),
    HORIZONTAL("Horizontal", "Mirror left and right across vertical center"),
    VERTICAL("Vertical", "Mirror top and bottom across horizontal center"),
    QUAD("4-Way", "Mirror across both horizontal and vertical axes")
}
