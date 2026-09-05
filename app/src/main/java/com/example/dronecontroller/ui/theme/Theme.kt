package com.example.dronecontroller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DroneColors = darkColorScheme(
    primary = Color(0xFF66E3FF),
    onPrimary = Color(0xFF00232B),
    secondary = Color(0xFFFFB86B),
    onSecondary = Color(0xFF311400),
    background = Color(0xFF071118),
    surface = Color(0xFF0D1B23),
    surfaceVariant = Color(0xFF172A34),
    onSurface = Color(0xFFE3F3F7),
    onSurfaceVariant = Color(0xFFA5C0C8),
    error = Color(0xFFFF7B72)
)

@Composable
fun DroneControllerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DroneColors,
        content = content
    )
}