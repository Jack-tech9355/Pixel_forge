package com.example.model

enum class DitheringPattern(
    val title: String,
    val description: String
) {
    CHECKER_50("Checkerboard 50%", "Alternating 50% density checkerboard"),
    FINE_DOT_25("Fine Dot 25%", "Light 25% ordered dot matrix"),
    DENSE_DOT_75("Dense Dot 75%", "Heavy 75% coverage dot pattern"),
    H_STRIPE("Horizontal Lines", "Alternating 1px horizontal stripes"),
    V_STRIPE("Vertical Lines", "Alternating 1px vertical stripes");

    fun matches(x: Int, y: Int): Boolean {
        return when (this) {
            CHECKER_50 -> (x + y) % 2 == 0
            FINE_DOT_25 -> (x % 2 == 0) && (y % 2 == 0)
            DENSE_DOT_75 -> !((x % 2 == 0) && (y % 2 == 0))
            H_STRIPE -> y % 2 == 0
            V_STRIPE -> x % 2 == 0
        }
    }
}
