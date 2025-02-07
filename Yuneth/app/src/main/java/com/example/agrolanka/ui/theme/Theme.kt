package com.example.agrolanka.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50), // Green
    secondary = Color(0xFF388E3C), // Dark Green
    background = Color(0xFFFFFFFF), // White
    surface = Color(0xFFFFFFFF), // White
    onPrimary = Color(0xFFFFFFFF), // White Text on Green
    onSecondary = Color(0xFFFFFFFF), // White Text on Dark Green
    onBackground = Color(0xFF000000), // Black Text on White
    onSurface = Color(0xFF000000) // Black Text on White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF388E3C), // Dark Green
    secondary = Color(0xFF4CAF50), // Green
    background = Color(0xFF000000), // Black Background
    surface = Color(0xFF121212), // Dark Grey Surface
    onPrimary = Color(0xFF000000), // Black Text on Green
    onSecondary = Color(0xFF000000), // Black Text on Dark Green
    onBackground = Color(0xFFFFFFFF), // White Text on Black
    onSurface = Color(0xFFFFFFFF) // White Text on Dark Grey
)

@Composable
fun AgroLankaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
