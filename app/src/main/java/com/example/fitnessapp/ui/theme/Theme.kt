package com.example.fitnessapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF1F4E79)   // deep blue
private val PrimaryContainer = Color(0xFF2A6AA3)
private val Surface = Color(0xFFF4F7FB)
private val OnSurface = Color(0xFF0F172A)
private val Outline = Color(0xFFE5EAF2)

private val LightColors = lightColorScheme(
    primary = Primary,
    primaryContainer = PrimaryContainer,
    surface = Surface,
    onSurface = OnSurface,
    outline = Outline,
    background = Surface,
    onBackground = OnSurface,
)

@Composable
fun FitnessTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}
