package com.ishaan.paperBird.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// Accent palettes
object AccentColors {
    val Rose = Color(0xFFC67A9E)
    val Lavender = Color(0xFF9D8DF1)
    val Sage = Color(0xFF7BC47F)
    val Sky = Color(0xFF61C0BF)
    val Amber = Color(0xFFF2C14E)
    val Slate = Color(0xFF6E7FA8)

    val all = mapOf(
        "Rose" to Rose,
        "Lavender" to Lavender,
        "Sage" to Sage,
        "Sky" to Sky,
        "Amber" to Amber,
        "Slate" to Slate
    )
}

val LocalAccentColor = staticCompositionLocalOf { AccentColors.Rose }

private fun darkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = accent.copy(alpha = 0.2f),
    onPrimaryContainer = accent,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE8E0E5),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE8E0E5),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB0A8B0),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2E2E2E),
    secondary = accent.copy(alpha = 0.8f),
    onSecondary = Color(0xFF1A1A1A),
    tertiary = accent.copy(alpha = 0.6f),
    error = Color(0xFFCF6679),
)

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.15f),
    onPrimaryContainer = accent,
    background = Color(0xFFFAF8FB),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF3EFF4),
    onSurfaceVariant = Color(0xFF6E6E6E),
    outline = Color(0xFFDDD8DD),
    outlineVariant = Color(0xFFEEEEEE),
    secondary = accent.copy(alpha = 0.8f),
    onSecondary = Color.White,
    tertiary = accent.copy(alpha = 0.6f),
    error = Color(0xFFB00020),
)

@Composable
fun paperBirdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Color = AccentColors.Rose,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkScheme(accent) else lightScheme(accent)

    CompositionLocalProvider(LocalAccentColor provides accent) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = paperBirdTypography,
            content = content
        )
    }
}
