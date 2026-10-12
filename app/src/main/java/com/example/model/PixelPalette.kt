package com.example.model

import java.util.UUID

data class PixelPalette(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isCustom: Boolean = false,
    val colors: List<Int>
)
