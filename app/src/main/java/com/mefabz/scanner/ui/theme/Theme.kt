package com.mefabz.scanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MefabzDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonIndigo,
    background = Slate950,
    surface = Slate900,
    onPrimary = Slate950,
    onSecondary = Ink,
    onBackground = Ink,
    onSurface = Ink,
    error = ErrorRed,
    onError = Slate950
)

private val MefabzLightColorScheme = lightColorScheme(
    primary = NeonIndigo, // Use deeper indigo for better contrast on light primary
    secondary = NeonCyan,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightSurface,
    onSecondary = LightInk,
    onBackground = LightInk,
    onSurface = LightInk,
    error = ErrorRed,
    onError = LightSurface
)

@Composable
fun MefabzScannerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        MefabzDarkColorScheme
    } else {
        MefabzLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
