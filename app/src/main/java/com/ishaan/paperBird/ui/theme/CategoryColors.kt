package com.ishaan.paperBird.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalCategoryColors = staticCompositionLocalOf<Map<String, Long>> {
    emptyMap()
}

val CategoryPalette = listOf(
    0xFFD77FA1, // Rose
    0xFF7BC47F, // Green
    0xFFF2C14E, // Yellow
    0xFF9D8DF1, // Purple
    0xFF61C0BF, // Teal
    0xFF6E7FA8, // Blue
    0xFFE98973, // Coral
    0xFF9A9A9A, // Gray
    0xFFB2A4FF, // Lavender
    0xFFFFB4B4, // Peach
    0xFFBFF6C3, // Mint
    0xFFF8C4B4, // Sand
)
