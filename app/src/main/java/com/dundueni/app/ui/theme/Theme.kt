package com.dundueni.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryMidnight,
    primaryContainer = PrimaryContainer,
    secondary = ProtectiveBlue,

    background = CanvasSubLayer,
    surface = SurfaceLight,

    error = Danger
)

@Composable
fun DundueniFETheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}