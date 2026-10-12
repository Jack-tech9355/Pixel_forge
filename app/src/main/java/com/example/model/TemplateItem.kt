package com.example.model

data class TemplateItem(
    val id: String,
    val title: String,
    val category: String,
    val width: Int,
    val height: Int,
    val description: String,
    val pixelGenerator: () -> IntArray
)
